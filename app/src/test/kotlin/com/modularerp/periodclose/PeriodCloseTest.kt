package com.modularerp.periodclose

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
class PeriodCloseTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "PCLOSE_TEST"
        var authToken: String = ""
        var periodId: Long = 0
        var taskId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "pcloseuser",
            "password" to "pass123", "name" to "PClose Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "pcloseuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `generate fiscal year`() {
        val request = mapOf("fiscalYear" to 2026)
        val response = restTemplate.exchange(
            "/api/v1/period-close/periods/generate", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<Map<String, Any>>
        assertThat(data).hasSize(12)
        assertThat(data[0]["periodName"]).isEqualTo("2026-01")
        assertThat(data[0]["status"]).isEqualTo("OPEN")
        periodId = (data[0]["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `get close checklist`() {
        val response = restTemplate.exchange(
            "/api/v1/period-close/periods/$periodId/checklist", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<Map<String, Any>>
        assertThat(data).hasSizeGreaterThanOrEqualTo(5)
        assertThat(data[0]["status"]).isEqualTo("PENDING")
        taskId = (data[0]["id"] as Number).toLong()
    }

    @Test
    @Order(3)
    fun `execute close task`() {
        val response = restTemplate.exchange(
            "/api/v1/period-close/periods/$periodId/tasks/$taskId/execute", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("COMPLETED")
    }

    @Test
    @Order(4)
    fun `create closing entry`() {
        val request = mapOf(
            "fiscalPeriodId" to periodId,
            "entryType" to "ACCRUAL",
            "description" to "Accrued salaries",
            "debitAccount" to "5100",
            "creditAccount" to "2100",
            "amount" to 5000000
        )
        val response = restTemplate.exchange(
            "/api/v1/period-close/closing-entries", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["entryType"]).isEqualTo("ACCRUAL")
        assertThat((data["documentNo"] as String)).startsWith("CE")
    }

    @Test
    @Order(5)
    fun `soft close and reopen period`() {
        // Soft close
        val closeResp = restTemplate.exchange(
            "/api/v1/period-close/periods/$periodId/soft-close", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(closeResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(closeResp)["status"]).isEqualTo("SOFT_CLOSE")

        // Reopen
        val reopenResp = restTemplate.exchange(
            "/api/v1/period-close/periods/$periodId/reopen", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(reopenResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(reopenResp)["status"]).isEqualTo("OPEN")
    }
}
