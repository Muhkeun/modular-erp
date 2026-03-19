package com.modularerp.batch

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
class BatchFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "BATCH_TEST"
        var authToken: String = ""
        var jobId: Long = 0
        var executionId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "batchuser",
            "password" to "pass123", "name" to "Batch Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "batchuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create batch job`() {
        val request = mapOf(
            "jobCode" to "GL_POSTING",
            "jobName" to "General Ledger Posting",
            "jobType" to "GL_POSTING",
            "cronExpression" to "0 0 2 * * ?",
            "description" to "Nightly GL posting batch"
        )

        val response = restTemplate.exchange(
            "/api/v1/batch/jobs", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["jobCode"]).isEqualTo("GL_POSTING")
        assertThat(data["enabled"]).isEqualTo(true)
        jobId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `execute batch job`() {
        val response = restTemplate.exchange(
            "/api/v1/batch/jobs/$jobId/execute", HttpMethod.POST,
            HttpEntity(mapOf("parameters" to """{"period":"2026-03"}"""), authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("QUEUED")
        assertThat(data["triggeredBy"]).isEqualTo("USER")
        executionId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(3)
    fun `get execution status`() {
        val response = restTemplate.exchange(
            "/api/v1/batch/executions/$executionId/status", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat(data["jobCode"]).isEqualTo("GL_POSTING")
    }

    @Test
    @Order(4)
    fun `disable and enable job`() {
        val disableResp = restTemplate.exchange(
            "/api/v1/batch/jobs/$jobId/disable", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(disableResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(disableResp)["enabled"]).isEqualTo(false)

        val enableResp = restTemplate.exchange(
            "/api/v1/batch/jobs/$jobId/enable", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(enableResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(enableResp)["enabled"]).isEqualTo(true)
    }

    @Test
    @Order(5)
    fun `get execution history`() {
        val response = restTemplate.exchange(
            "/api/v1/batch/jobs/$jobId/executions", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<Map<String, Any>>
        assertThat(data).isNotEmpty
    }
}
