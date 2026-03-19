package com.modularerp.costing

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
class CostingFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "COST_TEST"
        var authToken: String = ""
        var costCenterId: Long = 0
        var standardCostId: Long = 0
        var allocationId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "costuser",
            "password" to "pass123", "name" to "Cost Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "costuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create cost center`() {
        val request = mapOf(
            "costCenterCode" to "CC-100",
            "costCenterName" to "Manufacturing Dept",
            "departmentCode" to "MFG",
            "managerName" to "Kim Manager"
        )

        val response = restTemplate.exchange(
            "/api/v1/costing/cost-centers", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["costCenterCode"]).isEqualTo("CC-100")
        costCenterId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `create standard cost`() {
        val request = mapOf(
            "itemCode" to "MAT-001",
            "costType" to "MATERIAL",
            "standardRate" to 5000,
            "effectiveFrom" to "2025-01-01",
            "currency" to "KRW"
        )

        val response = restTemplate.exchange(
            "/api/v1/costing/standard-costs", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["itemCode"]).isEqualTo("MAT-001")
        standardCostId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(3)
    fun `calculate product cost`() {
        val request = mapOf(
            "itemCode" to "MAT-001",
            "fiscalYear" to 2025,
            "period" to 1,
            "quantity" to 10
        )

        val response = restTemplate.exchange(
            "/api/v1/costing/product-costs/calculate", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["calculated"]).isEqualTo(true)
        assertThat(data["itemCode"]).isEqualTo("MAT-001")
    }

    @Test
    @Order(4)
    fun `create and post cost allocation`() {
        // Create second cost center for allocation target
        val cc2 = mapOf(
            "costCenterCode" to "CC-200",
            "costCenterName" to "Assembly Dept"
        )
        restTemplate.exchange(
            "/api/v1/costing/cost-centers", HttpMethod.POST,
            HttpEntity(cc2, authHeaders()), Map::class.java
        )

        val request = mapOf(
            "fromCostCenter" to "CC-100",
            "toCostCenter" to "CC-200",
            "allocationType" to "DIRECT",
            "amount" to 100000,
            "description" to "Overhead allocation",
            "fiscalYear" to 2025,
            "period" to 1
        )

        val createResp = restTemplate.exchange(
            "/api/v1/costing/allocations", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(createResp)
        assertThat(data["status"]).isEqualTo("DRAFT")
        allocationId = (data["id"] as Number).toLong()

        // Post allocation
        val postResp = restTemplate.exchange(
            "/api/v1/costing/allocations/$allocationId/post", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(postResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(postResp)["status"]).isEqualTo("POSTED")
    }

    @Test
    @Order(5)
    fun `get variance analysis`() {
        val response = restTemplate.exchange(
            "/api/v1/costing/variance?itemCode=MAT-001", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
}
