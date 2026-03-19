package com.modularerp.production

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [com.modularerp.app.ModularErpApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkOrderTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "WO_TEST"
        var authToken: String = ""
        var woId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "wouser",
            "password" to "pass123", "name" to "WO Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "wouser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create work order`() {
        val request = mapOf(
            "companyCode" to "C100",
            "plantCode" to "P100",
            "productCode" to "PROD-001",
            "productName" to "Test Product",
            "plannedQuantity" to 100,
            "unitOfMeasure" to "EA",
            "orderType" to "STANDARD",
            "priority" to "NORMAL",
            "autoPopulate" to false  // No routing/BOM in test DB
        )

        val response = restTemplate.exchange(
            "/api/v1/production/work-orders",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("PLANNED")
        assertThat(data["documentNo"] as String).startsWith("WO")
        assertThat((data["plannedQuantity"] as Number).toInt()).isEqualTo(100)
        woId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `release work order`() {
        val response = restTemplate.exchange(
            "/api/v1/production/work-orders/$woId/release",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("RELEASED")
    }

    @Test
    @Order(3)
    fun `start work order`() {
        val response = restTemplate.exchange(
            "/api/v1/production/work-orders/$woId/start",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("IN_PROGRESS")
        assertThat(data["actualStartDate"]).isNotNull
    }

    @Test
    @Order(4)
    fun `report production`() {
        val request = mapOf(
            "goodQuantity" to 80,
            "scrapQuantity" to 5
        )

        val response = restTemplate.exchange(
            "/api/v1/production/work-orders/$woId/report",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat((data["completedQuantity"] as Number).toInt()).isEqualTo(80)
        assertThat((data["scrapQuantity"] as Number).toInt()).isEqualTo(5)
    }

    @Test
    @Order(5)
    fun `complete work order`() {
        val response = restTemplate.exchange(
            "/api/v1/production/work-orders/$woId/complete",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("COMPLETED")
        assertThat(data["actualEndDate"]).isNotNull
    }

    @Test
    @Order(6)
    fun `verify work order details after completion`() {
        val response = restTemplate.exchange(
            "/api/v1/production/work-orders/$woId",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("COMPLETED")
        assertThat((data["completedQuantity"] as Number).toInt()).isEqualTo(80)
        assertThat((data["remainingQuantity"] as Number).toInt()).isEqualTo(20)
    }
}
