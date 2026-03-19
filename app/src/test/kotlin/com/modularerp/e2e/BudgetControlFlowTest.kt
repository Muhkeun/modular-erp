package com.modularerp.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal

/**
 * Budget Control Flow E2E Test
 *
 * Create budget period -> Create budget items -> Approve budget
 * -> Verify budget availability -> Budget transfer -> Close budget period
 */
@SpringBootTest(
    classes = [com.modularerp.app.ModularErpApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BudgetControlFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "BUDGET_E2E"
        const val FISCAL_YEAR = 2025
    }

    private var authToken: String = ""
    private var budgetPeriodId: Long = 0
    private var budgetItemId1: Long = 0
    private var budgetItemId2: Long = 0
    private var budgetItemId3: Long = 0

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
    fun `step 0 - setup auth`() {
        println("=== Budget Control E2E: Step 0 - Setup ===")
        val regReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "budgetuser",
            "password" to "pass123", "name" to "Budget Controller"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "budgetuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `step 1 - create fiscal year budget period`() {
        println("=== Budget Control E2E: Step 1 - Create budget period ===")
        val periodReq = mapOf(
            "fiscalYear" to FISCAL_YEAR,
            "periodType" to "ANNUAL",
            "startDate" to "${FISCAL_YEAR}-01-01",
            "endDate" to "${FISCAL_YEAR}-12-31",
            "description" to "Annual operating budget $FISCAL_YEAR"
        )

        val resp = restTemplate.exchange(
            "/api/v1/budgets/periods", HttpMethod.POST,
            HttpEntity(periodReq, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        budgetPeriodId = (data["id"] as Number).toLong()
        assertThat(data["status"]).isEqualTo("DRAFT")
        assertThat(data["fiscalYear"]).isEqualTo(FISCAL_YEAR)
        println("  Created budget period (id=$budgetPeriodId) for $FISCAL_YEAR")
    }

    @Test
    @Order(2)
    fun `step 2 - create budget items for different accounts`() {
        println("=== Budget Control E2E: Step 2 - Create budget items ===")

        // R&D Expenses
        val item1 = mapOf(
            "budgetPeriodId" to budgetPeriodId,
            "accountCode" to "5100",
            "accountName" to "R&D Expenses",
            "departmentCode" to "RND",
            "budgetAmount" to 200000000,
            "currency" to "KRW",
            "notes" to "Research & development annual budget"
        )
        val resp1 = restTemplate.exchange(
            "/api/v1/budgets/items", HttpMethod.POST,
            HttpEntity(item1, authHeaders()), Map::class.java
        )
        assertThat(resp1.statusCode).isEqualTo(HttpStatus.CREATED)
        budgetItemId1 = (extractData(resp1)["id"] as Number).toLong()
        println("  Created: R&D Expenses = 200M KRW")

        // Sales & Marketing
        val item2 = mapOf(
            "budgetPeriodId" to budgetPeriodId,
            "accountCode" to "5200",
            "accountName" to "Sales & Marketing",
            "departmentCode" to "SALES",
            "budgetAmount" to 150000000,
            "currency" to "KRW",
            "notes" to "Sales and marketing campaigns"
        )
        val resp2 = restTemplate.exchange(
            "/api/v1/budgets/items", HttpMethod.POST,
            HttpEntity(item2, authHeaders()), Map::class.java
        )
        assertThat(resp2.statusCode).isEqualTo(HttpStatus.CREATED)
        budgetItemId2 = (extractData(resp2)["id"] as Number).toLong()
        println("  Created: Sales & Marketing = 150M KRW")

        // General Administration
        val item3 = mapOf(
            "budgetPeriodId" to budgetPeriodId,
            "accountCode" to "5300",
            "accountName" to "General Administration",
            "departmentCode" to "ADMIN",
            "budgetAmount" to 80000000,
            "currency" to "KRW",
            "notes" to "Office expenses and admin costs"
        )
        val resp3 = restTemplate.exchange(
            "/api/v1/budgets/items", HttpMethod.POST,
            HttpEntity(item3, authHeaders()), Map::class.java
        )
        assertThat(resp3.statusCode).isEqualTo(HttpStatus.CREATED)
        budgetItemId3 = (extractData(resp3)["id"] as Number).toLong()
        println("  Created: General Administration = 80M KRW")
        println("  Total budget: 430M KRW")
    }

    @Test
    @Order(3)
    fun `step 3 - approve budget`() {
        println("=== Budget Control E2E: Step 3 - Approve budget ===")
        val resp = restTemplate.exchange(
            "/api/v1/budgets/periods/$budgetPeriodId/approve", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)
        val status = extractData(resp)["status"]
        assertThat(status).isIn("APPROVED", "ACTIVE")
        println("  Budget period status: $status")
    }

    @Test
    @Order(4)
    fun `step 4 - verify budget items and remaining amounts`() {
        println("=== Budget Control E2E: Step 4 - Verify budget items ===")
        val resp = restTemplate.exchange(
            "/api/v1/budgets/periods/$budgetPeriodId/items", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val items = resp.body!!["data"] as List<Map<String, Any>>
        assertThat(items).hasSize(3)

        items.forEach { item ->
            val budget = BigDecimal(item["budgetAmount"].toString())
            val remaining = BigDecimal(item["remainingAmount"].toString())
            assertThat(remaining.compareTo(budget)).isEqualTo(0)
            println("  ${item["accountName"]}: budget=${budget}, remaining=${remaining}")
        }
    }

    @Test
    @Order(5)
    fun `step 5 - check budget availability`() {
        println("=== Budget Control E2E: Step 5 - Check budget availability ===")

        // Check R&D budget for a 50M purchase
        val resp = restTemplate.exchange(
            "/api/v1/budgets/availability?accountCode=5100&amount=50000000",
            HttpMethod.GET, HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)
        val body = resp.body!!
        println("  Budget availability check for 50M on R&D: ${body["data"]}")
    }

    @Test
    @Order(6)
    fun `step 6 - budget transfer between items`() {
        println("=== Budget Control E2E: Step 6 - Budget transfer ===")

        // Transfer 20M from Admin to R&D
        val transferReq = mapOf(
            "fromBudgetItemId" to budgetItemId3,
            "toBudgetItemId" to budgetItemId1,
            "amount" to 20000000,
            "reason" to "Additional R&D funding from admin savings"
        )

        val resp = restTemplate.exchange(
            "/api/v1/budgets/transfers", HttpMethod.POST,
            HttpEntity(transferReq, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        assertThat(data["amount"].toString()).contains("20000000")
        println("  Transfer: 20M KRW from Admin (5300) to R&D (5100)")
        println("  Transfer doc: ${data["documentNo"]}")
    }

    @Test
    @Order(7)
    fun `step 7 - verify budget after transfer`() {
        println("=== Budget Control E2E: Step 7 - Verify after transfer ===")

        // R&D should now be 220M (200 + 20)
        val rndResp = restTemplate.exchange(
            "/api/v1/budgets/items/$budgetItemId1", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        val rndData = extractData(rndResp)
        val rndRevised = BigDecimal(rndData["revisedAmount"].toString())
        assertThat(rndRevised.compareTo(BigDecimal(220000000))).isEqualTo(0)
        println("  R&D revised budget: $rndRevised (200M + 20M transfer in)")

        // Admin should now be 60M (80 - 20)
        val adminResp = restTemplate.exchange(
            "/api/v1/budgets/items/$budgetItemId3", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        val adminData = extractData(adminResp)
        val adminRevised = BigDecimal(adminData["revisedAmount"].toString())
        assertThat(adminRevised.compareTo(BigDecimal(60000000))).isEqualTo(0)
        println("  Admin revised budget: $adminRevised (80M - 20M transfer out)")
    }

    @Test
    @Order(8)
    fun `step 8 - close budget period`() {
        println("=== Budget Control E2E: Step 8 - Close budget period ===")
        val resp = restTemplate.exchange(
            "/api/v1/budgets/periods/$budgetPeriodId/close", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(resp)["status"]).isEqualTo("CLOSED")
        println("  Budget period closed")

        println("=== Budget Control E2E: COMPLETE ===")
    }
}
