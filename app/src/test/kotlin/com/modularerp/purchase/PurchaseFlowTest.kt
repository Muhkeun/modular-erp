package com.modularerp.purchase

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
class PurchaseFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "PR_TEST"
        var authToken: String = ""
        var prId: Long = 0
        var poId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "pruser",
            "password" to "pass123", "name" to "PR Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "pruser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create purchase request`() {
        val request = mapOf(
            "companyCode" to "C100",
            "plantCode" to "P100",
            "departmentCode" to "IT",
            "prType" to "STANDARD",
            "description" to "Test PR",
            "lines" to listOf(
                mapOf(
                    "itemCode" to "MAT-001",
                    "itemName" to "Test Material",
                    "quantity" to 100,
                    "unitOfMeasure" to "EA",
                    "unitPrice" to 1000
                )
            )
        )

        val response = restTemplate.exchange(
            "/api/v1/purchase/requests",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        assertThat(data["documentNo"] as String).startsWith("PR")
        prId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `submit PR for approval`() {
        val response = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId/submit",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("SUBMITTED")
    }

    @Test
    @Order(3)
    fun `approve PR`() {
        val response = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId/approve",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("APPROVED")
    }

    @Test
    @Order(4)
    fun `create PO from approved PR`() {
        val request = mapOf(
            "vendorCode" to "V001",
            "vendorName" to "Test Vendor",
            "currencyCode" to "KRW"
        )

        val response = restTemplate.exchange(
            "/api/v1/purchase/orders/from-pr/$prId",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        assertThat(data["vendorCode"]).isEqualTo("V001")
        assertThat(data["documentNo"] as String).startsWith("PO")
        poId = (data["id"] as Number).toLong()

        // Verify PO lines contain PR line data
        @Suppress("UNCHECKED_CAST")
        val lines = data["lines"] as List<Map<String, Any>>
        assertThat(lines).hasSize(1)
        assertThat(lines[0]["itemCode"]).isEqualTo("MAT-001")
    }

    @Test
    @Order(5)
    fun `verify PR open quantity consumed after PO creation`() {
        val response = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)

        @Suppress("UNCHECKED_CAST")
        val lines = data["lines"] as List<Map<String, Any>>
        // openQuantity should be 0 since all quantity was converted to PO
        val openQty = BigDecimal(lines[0]["openQuantity"].toString())
        assertThat(openQty.compareTo(BigDecimal.ZERO)).isEqualTo(0)
    }

    @Test
    @Order(6)
    fun `submit and approve PO`() {
        // Submit
        val submitResp = restTemplate.exchange(
            "/api/v1/purchase/orders/$poId/submit",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )
        assertThat(submitResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(submitResp)["status"]).isEqualTo("SUBMITTED")

        // Approve
        val approveResp = restTemplate.exchange(
            "/api/v1/purchase/orders/$poId/approve",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )
        assertThat(approveResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(approveResp)["status"]).isEqualTo("APPROVED")
    }
}
