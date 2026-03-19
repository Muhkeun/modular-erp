package com.modularerp.budget

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
class BudgetTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "BUDGET_TEST"
        var authToken: String = ""
        var periodId: Long = 0
        var itemId: Long = 0
        var item2Id: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "budgetuser",
            "password" to "pass123", "name" to "Budget Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "budgetuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create budget period`() {
        val request = mapOf(
            "fiscalYear" to 2026,
            "periodType" to "ANNUAL",
            "startDate" to "2026-01-01",
            "endDate" to "2026-12-31",
            "description" to "FY2026 Annual Budget"
        )

        val response = restTemplate.exchange(
            "/api/v1/budgets/periods", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        assertThat(data["fiscalYear"]).isEqualTo(2026)
        periodId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `create budget items`() {
        val item1 = mapOf(
            "budgetPeriodId" to periodId,
            "accountCode" to "5100",
            "accountName" to "Operating Expenses",
            "budgetAmount" to 1000000,
            "departmentCode" to "IT"
        )

        val resp1 = restTemplate.exchange(
            "/api/v1/budgets/items", HttpMethod.POST,
            HttpEntity(item1, authHeaders()), Map::class.java
        )
        assertThat(resp1.statusCode).isEqualTo(HttpStatus.CREATED)
        itemId = (extractData(resp1)["id"] as Number).toLong()

        val item2 = mapOf(
            "budgetPeriodId" to periodId,
            "accountCode" to "5200",
            "accountName" to "Marketing Expenses",
            "budgetAmount" to 500000,
            "departmentCode" to "MKT"
        )

        val resp2 = restTemplate.exchange(
            "/api/v1/budgets/items", HttpMethod.POST,
            HttpEntity(item2, authHeaders()), Map::class.java
        )
        assertThat(resp2.statusCode).isEqualTo(HttpStatus.CREATED)
        item2Id = (extractData(resp2)["id"] as Number).toLong()
    }

    @Test
    @Order(3)
    fun `approve budget period`() {
        val response = restTemplate.exchange(
            "/api/v1/budgets/periods/$periodId/approve", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("ACTIVE")
    }

    @Test
    @Order(4)
    fun `transfer budget between items`() {
        val request = mapOf(
            "fromBudgetItemId" to itemId,
            "toBudgetItemId" to item2Id,
            "amount" to 100000,
            "reason" to "Reallocate to marketing campaign"
        )

        val response = restTemplate.exchange(
            "/api/v1/budgets/transfers", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("COMPLETED")
    }

    @Test
    @Order(5)
    fun `search budget periods`() {
        val response = restTemplate.exchange(
            "/api/v1/budgets/periods?page=0&size=10", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!["success"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<*>
        assertThat(data).hasSizeGreaterThanOrEqualTo(1)
    }
}
