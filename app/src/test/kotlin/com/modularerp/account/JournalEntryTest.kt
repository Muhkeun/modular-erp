package com.modularerp.account

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
class JournalEntryTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "JE_TEST"
        var authToken: String = ""
        var jeId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "jeuser",
            "password" to "pass123", "name" to "JE Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "jeuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create balanced journal entry`() {
        val request = mapOf(
            "companyCode" to "C100",
            "entryType" to "MANUAL",
            "description" to "Test journal entry",
            "currencyCode" to "KRW",
            "lines" to listOf(
                mapOf(
                    "accountCode" to "1100",
                    "accountName" to "Cash",
                    "debitAmount" to 50000,
                    "creditAmount" to 0,
                    "description" to "Cash debit"
                ),
                mapOf(
                    "accountCode" to "4100",
                    "accountName" to "Sales Revenue",
                    "debitAmount" to 0,
                    "creditAmount" to 50000,
                    "description" to "Revenue credit"
                )
            )
        )

        val response = restTemplate.exchange(
            "/api/v1/account/journal-entries",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        assertThat(data["isBalanced"]).isEqualTo(true)
        assertThat(data["documentNo"] as String).startsWith("JE")

        val totalDebit = BigDecimal(data["totalDebit"].toString())
        val totalCredit = BigDecimal(data["totalCredit"].toString())
        assertThat(totalDebit.compareTo(totalCredit)).isEqualTo(0)

        jeId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `post balanced journal entry`() {
        val response = restTemplate.exchange(
            "/api/v1/account/journal-entries/$jeId/post",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("POSTED")
    }

    @Test
    @Order(3)
    fun `reverse posted journal entry`() {
        val response = restTemplate.exchange(
            "/api/v1/account/journal-entries/$jeId/reverse",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("REVERSED")
    }

    @Test
    @Order(4)
    fun `create unbalanced journal entry - post should fail`() {
        // Create an unbalanced entry
        val request = mapOf(
            "companyCode" to "C100",
            "entryType" to "MANUAL",
            "description" to "Unbalanced entry",
            "lines" to listOf(
                mapOf(
                    "accountCode" to "1100",
                    "accountName" to "Cash",
                    "debitAmount" to 30000,
                    "creditAmount" to 0
                ),
                mapOf(
                    "accountCode" to "4100",
                    "accountName" to "Revenue",
                    "debitAmount" to 0,
                    "creditAmount" to 20000
                )
            )
        )

        val createResp = restTemplate.exchange(
            "/api/v1/account/journal-entries",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(createResp)
        assertThat(data["isBalanced"]).isEqualTo(false)
        val unbalancedId = (data["id"] as Number).toLong()

        // Try to post - should fail because not balanced
        val postResp = restTemplate.exchange(
            "/api/v1/account/journal-entries/$unbalancedId/post",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        // Should return error (400 or 500 depending on exception handling for IllegalStateException)
        assertThat(postResp.statusCode.value()).isGreaterThanOrEqualTo(400)
    }

    @Test
    @Order(5)
    fun `search journal entries`() {
        val response = restTemplate.exchange(
            "/api/v1/account/journal-entries?page=0&size=10",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as List<*>
        assertThat(data).hasSizeGreaterThanOrEqualTo(2)
    }
}
