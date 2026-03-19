package com.modularerp.currency

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
class CurrencyFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "CURR_TEST"
        var authToken: String = ""
        var krwId: Long = 0
        var usdId: Long = 0
        var revalId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "curruser",
            "password" to "pass123", "name" to "Currency Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "curruser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create currencies`() {
        val krw = mapOf(
            "currencyCode" to "KRW", "currencyName" to "Korean Won",
            "symbol" to "\u20a9", "decimalPlaces" to 0, "isBaseCurrency" to true
        )
        val krwResp = restTemplate.exchange(
            "/api/v1/currencies", HttpMethod.POST,
            HttpEntity(krw, authHeaders()), Map::class.java
        )
        assertThat(krwResp.statusCode).isEqualTo(HttpStatus.CREATED)
        krwId = (extractData(krwResp)["id"] as Number).toLong()

        val usd = mapOf(
            "currencyCode" to "USD", "currencyName" to "US Dollar",
            "symbol" to "$", "decimalPlaces" to 2, "isBaseCurrency" to false
        )
        val usdResp = restTemplate.exchange(
            "/api/v1/currencies", HttpMethod.POST,
            HttpEntity(usd, authHeaders()), Map::class.java
        )
        assertThat(usdResp.statusCode).isEqualTo(HttpStatus.CREATED)
        usdId = (extractData(usdResp)["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `create exchange rate`() {
        val request = mapOf(
            "fromCurrency" to "USD", "toCurrency" to "KRW",
            "rateDate" to "2025-03-01", "exchangeRate" to 1350.50,
            "rateType" to "SPOT", "source" to "BANK_OF_KOREA"
        )

        val response = restTemplate.exchange(
            "/api/v1/currencies/exchange-rates", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["fromCurrency"]).isEqualTo("USD")
        assertThat(data["toCurrency"]).isEqualTo("KRW")
    }

    @Test
    @Order(3)
    fun `convert currency`() {
        val request = mapOf(
            "amount" to 1000, "fromCurrency" to "USD",
            "toCurrency" to "KRW", "date" to "2025-03-01"
        )

        val response = restTemplate.exchange(
            "/api/v1/currencies/convert", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat(data["fromCurrency"]).isEqualTo("USD")
        assertThat(data["toCurrency"]).isEqualTo("KRW")
    }

    @Test
    @Order(4)
    fun `create and post revaluation`() {
        val request = mapOf(
            "fiscalYear" to 2025, "period" to 3,
            "fromCurrency" to "USD", "toCurrency" to "KRW",
            "originalRate" to 1350.50, "revaluationRate" to 1370.00,
            "unrealizedGainLoss" to 19500
        )

        val createResp = restTemplate.exchange(
            "/api/v1/currencies/revaluations", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(createResp)
        assertThat(data["status"]).isEqualTo("DRAFT")
        revalId = (data["id"] as Number).toLong()

        // Post
        val postResp = restTemplate.exchange(
            "/api/v1/currencies/revaluations/$revalId/post", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(postResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(postResp)["status"]).isEqualTo("POSTED")
    }

    @Test
    @Order(5)
    fun `reverse revaluation`() {
        val response = restTemplate.exchange(
            "/api/v1/currencies/revaluations/$revalId/reverse", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("REVERSED")
    }
}
