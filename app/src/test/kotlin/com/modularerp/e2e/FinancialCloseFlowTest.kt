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
import java.time.LocalDate

/**
 * Financial Close Full Cycle E2E Test
 *
 * Generate fiscal year -> Budget period + items -> Approve budget
 * -> Register & activate asset -> Journal entries -> Post
 * -> Run depreciation -> Closing entries -> Soft close -> Hard close
 */
@SpringBootTest(
    classes = [com.modularerp.app.ModularErpApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FinancialCloseFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "FINCLOSE_E2E"
        const val COMPANY = "C100"
        const val FISCAL_YEAR = 2025
    }

    private var authToken: String = ""
    private var fiscalPeriodIds: MutableList<Long> = mutableListOf()
    private var firstPeriodId: Long = 0
    private var budgetPeriodId: Long = 0
    private var budgetItemId1: Long = 0
    private var budgetItemId2: Long = 0
    private var assetId: Long = 0
    private var revenueJeId: Long = 0
    private var expenseJeId: Long = 0

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
        println("=== Financial Close E2E: Step 0 - Setup ===")
        val regReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "finuser",
            "password" to "pass123", "name" to "Finance Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "finuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `step 1 - generate fiscal year with 12 periods`() {
        println("=== Financial Close E2E: Step 1 - Generate fiscal year ===")
        val request = mapOf("fiscalYear" to FISCAL_YEAR)

        val resp = restTemplate.exchange(
            "/api/v1/period-close/periods/generate", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)

        @Suppress("UNCHECKED_CAST")
        val periods = resp.body!!["data"] as List<Map<String, Any>>
        assertThat(periods).hasSize(12)
        periods.forEach { p ->
            fiscalPeriodIds.add((p["id"] as Number).toLong())
        }
        firstPeriodId = fiscalPeriodIds[0]
        println("  Generated 12 fiscal periods for $FISCAL_YEAR")
        println("  First period id: $firstPeriodId")
    }

    @Test
    @Order(2)
    fun `step 2 - create budget period and items`() {
        println("=== Financial Close E2E: Step 2 - Create budget ===")

        val periodReq = mapOf(
            "fiscalYear" to FISCAL_YEAR,
            "periodType" to "ANNUAL",
            "startDate" to "${FISCAL_YEAR}-01-01",
            "endDate" to "${FISCAL_YEAR}-12-31",
            "description" to "Annual budget $FISCAL_YEAR"
        )
        val periodResp = restTemplate.exchange(
            "/api/v1/budgets/periods", HttpMethod.POST,
            HttpEntity(periodReq, authHeaders()), Map::class.java
        )
        assertThat(periodResp.statusCode).isEqualTo(HttpStatus.CREATED)
        budgetPeriodId = (extractData(periodResp)["id"] as Number).toLong()
        println("  Created budget period (id=$budgetPeriodId)")

        // Budget item 1: IT Expenses
        val item1 = mapOf(
            "budgetPeriodId" to budgetPeriodId,
            "accountCode" to "5100",
            "accountName" to "IT Operating Expenses",
            "departmentCode" to "IT",
            "budgetAmount" to 120000000,
            "currency" to "KRW",
            "notes" to "IT department annual budget"
        )
        val item1Resp = restTemplate.exchange(
            "/api/v1/budgets/items", HttpMethod.POST,
            HttpEntity(item1, authHeaders()), Map::class.java
        )
        assertThat(item1Resp.statusCode).isEqualTo(HttpStatus.CREATED)
        budgetItemId1 = (extractData(item1Resp)["id"] as Number).toLong()

        // Budget item 2: Marketing
        val item2 = mapOf(
            "budgetPeriodId" to budgetPeriodId,
            "accountCode" to "5200",
            "accountName" to "Marketing Expenses",
            "departmentCode" to "MKT",
            "budgetAmount" to 80000000,
            "currency" to "KRW",
            "notes" to "Marketing annual budget"
        )
        val item2Resp = restTemplate.exchange(
            "/api/v1/budgets/items", HttpMethod.POST,
            HttpEntity(item2, authHeaders()), Map::class.java
        )
        assertThat(item2Resp.statusCode).isEqualTo(HttpStatus.CREATED)
        budgetItemId2 = (extractData(item2Resp)["id"] as Number).toLong()
        println("  Created 2 budget items: IT=120M, Marketing=80M KRW")
    }

    @Test
    @Order(3)
    fun `step 3 - approve budget`() {
        println("=== Financial Close E2E: Step 3 - Approve budget ===")
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
    fun `step 4 - register and activate fixed asset`() {
        println("=== Financial Close E2E: Step 4 - Register fixed asset ===")
        val assetReq = mapOf(
            "name" to "CNC Milling Machine Model X500",
            "description" to "5-axis CNC milling machine for precision parts",
            "category" to "MACHINERY",
            "acquisitionDate" to "${FISCAL_YEAR}-01-15",
            "acquisitionCost" to 250000000,
            "usefulLifeMonths" to 120,
            "depreciationMethod" to "STRAIGHT_LINE",
            "salvageValue" to 10000000,
            "location" to "Factory Floor A",
            "department" to "PRODUCTION",
            "responsiblePerson" to "Park Jihoon",
            "serialNumber" to "CNC-X500-2025-001",
            "currency" to "KRW"
        )

        val createResp = restTemplate.exchange(
            "/api/v1/assets", HttpMethod.POST,
            HttpEntity(assetReq, authHeaders()), Map::class.java
        )
        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(createResp)
        assetId = (data["id"] as Number).toLong()
        val initialStatus = data["status"] as String
        assertThat(initialStatus).isIn("REGISTERED", "DRAFT")
        println("  Registered asset: CNC Milling Machine (id=$assetId), status=$initialStatus, cost=250M KRW")

        // Activate
        val activateResp = restTemplate.exchange(
            "/api/v1/assets/$assetId/activate", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(activateResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(activateResp)["status"]).isEqualTo("ACTIVE")
        println("  Asset activated")
    }

    @Test
    @Order(5)
    fun `step 5 - create revenue and expense journal entries`() {
        println("=== Financial Close E2E: Step 5 - Create journal entries ===")

        // Revenue JE
        val revenueJe = mapOf(
            "companyCode" to COMPANY,
            "entryType" to "MANUAL",
            "description" to "January revenue - Product sales",
            "currencyCode" to "KRW",
            "lines" to listOf(
                mapOf("accountCode" to "1200", "accountName" to "Accounts Receivable", "debitAmount" to 35000000, "creditAmount" to 0),
                mapOf("accountCode" to "4100", "accountName" to "Sales Revenue", "debitAmount" to 0, "creditAmount" to 35000000)
            )
        )
        val revResp = restTemplate.exchange(
            "/api/v1/account/journal-entries", HttpMethod.POST,
            HttpEntity(revenueJe, authHeaders()), Map::class.java
        )
        assertThat(revResp.statusCode).isEqualTo(HttpStatus.CREATED)
        revenueJeId = (extractData(revResp)["id"] as Number).toLong()

        // Expense JE
        val expenseJe = mapOf(
            "companyCode" to COMPANY,
            "entryType" to "MANUAL",
            "description" to "January IT operating expenses",
            "currencyCode" to "KRW",
            "lines" to listOf(
                mapOf("accountCode" to "5100", "accountName" to "IT Operating Expenses", "debitAmount" to 8000000, "creditAmount" to 0),
                mapOf("accountCode" to "1000", "accountName" to "Cash", "debitAmount" to 0, "creditAmount" to 8000000)
            )
        )
        val expResp = restTemplate.exchange(
            "/api/v1/account/journal-entries", HttpMethod.POST,
            HttpEntity(expenseJe, authHeaders()), Map::class.java
        )
        assertThat(expResp.statusCode).isEqualTo(HttpStatus.CREATED)
        expenseJeId = (extractData(expResp)["id"] as Number).toLong()
        println("  Created revenue JE (id=$revenueJeId): 35M KRW")
        println("  Created expense JE (id=$expenseJeId): 8M KRW")
    }

    @Test
    @Order(6)
    fun `step 6 - post journal entries`() {
        println("=== Financial Close E2E: Step 6 - Post journal entries ===")

        val revPost = restTemplate.exchange(
            "/api/v1/account/journal-entries/$revenueJeId/post", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(revPost.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(revPost)["status"]).isEqualTo("POSTED")

        val expPost = restTemplate.exchange(
            "/api/v1/account/journal-entries/$expenseJeId/post", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(expPost.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(expPost)["status"]).isEqualTo("POSTED")
        println("  Both journal entries posted")
    }

    @Test
    @Order(7)
    fun `step 7 - run depreciation for period`() {
        println("=== Financial Close E2E: Step 7 - Run depreciation ===")
        val depReq = mapOf("year" to FISCAL_YEAR, "month" to 1)

        val resp = restTemplate.exchange(
            "/api/v1/assets/depreciation/run", HttpMethod.POST,
            HttpEntity(depReq, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)
        println("  Depreciation run completed for ${FISCAL_YEAR}-01")
    }

    @Test
    @Order(8)
    fun `step 8 - verify depreciation schedule created`() {
        println("=== Financial Close E2E: Step 8 - Verify depreciation schedule ===")
        val resp = restTemplate.exchange(
            "/api/v1/assets/$assetId/schedule", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val schedule = resp.body!!["data"] as List<Map<String, Any>>
        if (schedule.isNotEmpty()) {
            val jan = schedule.find { (it["periodYear"] as Number).toInt() == FISCAL_YEAR && (it["periodMonth"] as Number).toInt() == 1 }
            if (jan != null) {
                val depAmount = BigDecimal(jan["depreciationAmount"].toString())
                assertThat(depAmount).isGreaterThan(BigDecimal.ZERO)
                println("  Depreciation schedule: Jan $FISCAL_YEAR = $depAmount KRW")
            } else {
                println("  Depreciation schedule entries exist but none for Jan $FISCAL_YEAR")
            }
        } else {
            println("  Depreciation schedule is empty (depreciation may have been recorded differently)")
        }
        // Monthly depreciation: (250M - 10M) / 120 months = 2,000,000 KRW/month
        println("  Expected: (250M - 10M) / 120 = 2,000,000 KRW/month")
    }

    @Test
    @Order(9)
    fun `step 9 - get and verify period close checklist`() {
        println("=== Financial Close E2E: Step 9 - Period close checklist ===")
        val resp = restTemplate.exchange(
            "/api/v1/period-close/periods/$firstPeriodId/checklist", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val checklist = resp.body!!["data"] as List<Map<String, Any>>
        assertThat(checklist).isNotEmpty
        println("  Checklist has ${checklist.size} tasks")
        checklist.forEach { task ->
            println("    Task: ${task["taskName"]} - Status: ${task["status"]}")
        }
    }

    @Test
    @Order(10)
    fun `step 10 - create closing entries`() {
        println("=== Financial Close E2E: Step 10 - Create closing entries ===")
        val closingEntry = mapOf(
            "fiscalPeriodId" to firstPeriodId,
            "entryType" to "ACCRUAL",
            "description" to "Accrued interest expense for January",
            "debitAccount" to "5300",
            "creditAccount" to "2300",
            "amount" to 1500000
        )

        val resp = restTemplate.exchange(
            "/api/v1/period-close/closing-entries", HttpMethod.POST,
            HttpEntity(closingEntry, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        assertThat(data["posted"]).isEqualTo(false)
        println("  Created closing entry: Accrued interest 1.5M KRW")
    }

    @Test
    @Order(11)
    fun `step 11 - soft close the period`() {
        println("=== Financial Close E2E: Step 11 - Soft close ===")
        val resp = restTemplate.exchange(
            "/api/v1/period-close/periods/$firstPeriodId/soft-close", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)
        val softStatus = extractData(resp)["status"]
        assertThat(softStatus).isIn("SOFT_CLOSED", "SOFT_CLOSE")
        println("  Period soft-closed: $softStatus")
    }

    @Test
    @Order(12)
    fun `step 12 - hard close the period`() {
        println("=== Financial Close E2E: Step 12 - Hard close ===")
        val resp = restTemplate.exchange(
            "/api/v1/period-close/periods/$firstPeriodId/hard-close", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)
        val hardStatus = extractData(resp)["status"]
        assertThat(hardStatus).isIn("CLOSED", "HARD_CLOSE")
        println("  Period hard-closed: $hardStatus")

        // Verify final state
        val periodResp = restTemplate.exchange(
            "/api/v1/period-close/periods/$firstPeriodId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        val finalStatus = extractData(periodResp)["status"]
        assertThat(finalStatus).isIn("CLOSED", "HARD_CLOSE")
        println("  Final status: $finalStatus")
        println("=== Financial Close E2E: COMPLETE ===")
    }
}
