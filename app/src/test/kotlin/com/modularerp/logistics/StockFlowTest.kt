package com.modularerp.logistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

@SpringBootTest(
    classes = [com.modularerp.app.ModularErpApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StockFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "STOCK_TEST"
        const val PLANT = "P100"
        const val STORAGE = "WH01"
        const val ITEM_CODE = "MAT-STOCK-001"
        var authToken: String = ""
        var grId: Long = 0
        var giId: Long = 0
    }

    private fun authHeaders(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(authToken)
        set("X-Tenant-Id", TENANT_ID)
        contentType = MediaType.APPLICATION_JSON
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractData(response: ResponseEntity<Map<*, *>>): Map<String, Any> =
        response.body!!["data"] as Map<String, Any>

    @Test
    @Order(0)
    fun setup() {
        val regReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "stockuser",
            "password" to "pass123", "name" to "Stock Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "stockuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create goods receipt`() {
        val request = mapOf(
            "companyCode" to "C100",
            "plantCode" to PLANT,
            "storageLocation" to STORAGE,
            "vendorCode" to "V001",
            "vendorName" to "Test Vendor",
            "lines" to listOf(
                mapOf(
                    "itemCode" to ITEM_CODE,
                    "itemName" to "Stock Test Material",
                    "quantity" to 500,
                    "unitOfMeasure" to "EA",
                    "unitPrice" to 100
                )
            )
        )

        val response = restTemplate.exchange(
            "/api/v1/logistics/goods-receipts",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        grId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `confirm goods receipt - stock should increase`() {
        val response = restTemplate.exchange(
            "/api/v1/logistics/goods-receipts/$grId/confirm",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("CONFIRMED")
    }

    @Test
    @Order(3)
    fun `verify stock increased after GR confirmation`() {
        val response = restTemplate.exchange(
            "/api/v1/logistics/stock?itemCode=$ITEM_CODE&plantCode=$PLANT",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<Map<String, Any>>
        assertThat(data).isNotEmpty

        val stock = data.find { it["itemCode"] == ITEM_CODE }
        assertThat(stock).isNotNull
        val onHand = BigDecimal(stock!!["quantityOnHand"].toString())
        assertThat(onHand.compareTo(BigDecimal(500))).isEqualTo(0)
    }

    @Test
    @Order(4)
    fun `create goods issue`() {
        val request = mapOf(
            "companyCode" to "C100",
            "plantCode" to PLANT,
            "storageLocation" to STORAGE,
            "issueType" to "SALES",
            "lines" to listOf(
                mapOf(
                    "itemCode" to ITEM_CODE,
                    "itemName" to "Stock Test Material",
                    "quantity" to 200,
                    "unitOfMeasure" to "EA"
                )
            )
        )

        val response = restTemplate.exchange(
            "/api/v1/logistics/goods-issues",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        giId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(5)
    fun `confirm goods issue - stock should decrease`() {
        val response = restTemplate.exchange(
            "/api/v1/logistics/goods-issues/$giId/confirm",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("CONFIRMED")
    }

    @Test
    @Order(6)
    fun `verify stock decreased after GI confirmation`() {
        val response = restTemplate.exchange(
            "/api/v1/logistics/stock?itemCode=$ITEM_CODE&plantCode=$PLANT",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<Map<String, Any>>
        val stock = data.find { it["itemCode"] == ITEM_CODE }
        assertThat(stock).isNotNull

        val onHand = BigDecimal(stock!!["quantityOnHand"].toString())
        // 500 received - 200 issued = 300
        assertThat(onHand.compareTo(BigDecimal(300))).isEqualTo(0)
    }
}
