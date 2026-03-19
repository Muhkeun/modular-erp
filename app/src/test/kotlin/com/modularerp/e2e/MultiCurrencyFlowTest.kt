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
 * Multi-Currency Transaction E2E Test
 *
 * Create KRW (base) + USD currencies -> Set exchange rate
 * -> Create PO in USD -> Convert amounts -> Update rate
 * -> Create revaluation -> Post revaluation
 */
@SpringBootTest(
    classes = [com.modularerp.app.ModularErpApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MultiCurrencyFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "MCURR_E2E"
        const val COMPANY = "C100"
        const val PLANT = "P100"
    }

    private var authToken: String = ""
    private var krwId: Long = 0
    private var usdId: Long = 0
    private var poId: Long = 0
    private var revalId: Long = 0

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
        println("=== Multi-Currency E2E: Step 0 - Setup ===")
        val regReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "fxuser",
            "password" to "pass123", "name" to "FX Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "fxuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `step 1 - create base currency KRW and foreign currency USD`() {
        println("=== Multi-Currency E2E: Step 1 - Create currencies ===")

        val krw = mapOf(
            "currencyCode" to "KRW",
            "currencyName" to "Korean Won",
            "symbol" to "₩",
            "decimalPlaces" to 0,
            "isBaseCurrency" to true
        )
        val krwResp = restTemplate.exchange(
            "/api/v1/currencies", HttpMethod.POST,
            HttpEntity(krw, authHeaders()), Map::class.java
        )
        assertThat(krwResp.statusCode).isEqualTo(HttpStatus.CREATED)
        krwId = (extractData(krwResp)["id"] as Number).toLong()
        assertThat(extractData(krwResp)["isBaseCurrency"]).isEqualTo(true)
        println("  Created KRW (base currency, id=$krwId)")

        val usd = mapOf(
            "currencyCode" to "USD",
            "currencyName" to "US Dollar",
            "symbol" to "$",
            "decimalPlaces" to 2,
            "isBaseCurrency" to false
        )
        val usdResp = restTemplate.exchange(
            "/api/v1/currencies", HttpMethod.POST,
            HttpEntity(usd, authHeaders()), Map::class.java
        )
        assertThat(usdResp.statusCode).isEqualTo(HttpStatus.CREATED)
        usdId = (extractData(usdResp)["id"] as Number).toLong()
        println("  Created USD (foreign currency, id=$usdId)")
    }

    @Test
    @Order(2)
    fun `step 2 - set exchange rate 1 USD = 1350 KRW`() {
        println("=== Multi-Currency E2E: Step 2 - Set exchange rate ===")
        val rateReq = mapOf(
            "fromCurrency" to "USD",
            "toCurrency" to "KRW",
            "exchangeRate" to 1350,
            "rateType" to "SPOT",
            "source" to "BOK"
        )

        val resp = restTemplate.exchange(
            "/api/v1/currencies/exchange-rates", HttpMethod.POST,
            HttpEntity(rateReq, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        assertThat(BigDecimal(data["exchangeRate"].toString()).compareTo(BigDecimal(1350))).isEqualTo(0)
        println("  Set rate: 1 USD = 1,350 KRW")
    }

    @Test
    @Order(3)
    fun `step 3 - create PO in USD`() {
        println("=== Multi-Currency E2E: Step 3 - Create PO in USD ===")
        val poRequest = mapOf(
            "companyCode" to COMPANY,
            "plantCode" to PLANT,
            "vendorCode" to "V-US-001",
            "vendorName" to "US Tech Components Inc.",
            "currencyCode" to "USD",
            "paymentTerms" to "Net 60",
            "remark" to "USD purchase order",
            "lines" to listOf(
                mapOf(
                    "itemCode" to "COMP-USD-001",
                    "itemName" to "Semiconductor Chip A100",
                    "quantity" to 1000,
                    "unitOfMeasure" to "EA",
                    "unitPrice" to 25
                )
            )
        )

        val resp = restTemplate.exchange(
            "/api/v1/purchase/orders", HttpMethod.POST,
            HttpEntity(poRequest, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        poId = (data["id"] as Number).toLong()
        assertThat(data["currencyCode"]).isEqualTo("USD")
        // Total: 1000 * $25 = $25,000
        val total = BigDecimal(data["totalAmount"].toString())
        assertThat(total.compareTo(BigDecimal(25000))).isEqualTo(0)
        println("  Created PO (id=$poId) in USD, total=$25,000")
        println("  KRW equivalent at 1350: 33,750,000 KRW")
    }

    @Test
    @Order(4)
    fun `step 4 - convert USD to KRW and verify`() {
        println("=== Multi-Currency E2E: Step 4 - Currency conversion ===")
        val convertReq = mapOf(
            "amount" to 25000,
            "fromCurrency" to "USD",
            "toCurrency" to "KRW"
        )

        val resp = restTemplate.exchange(
            "/api/v1/currencies/convert", HttpMethod.POST,
            HttpEntity(convertReq, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(resp)
        val toAmount = BigDecimal(data["toAmount"].toString())
        // $25,000 * 1350 = 33,750,000 KRW
        assertThat(toAmount.compareTo(BigDecimal(33750000))).isEqualTo(0)
        println("  Converted: $25,000 USD = $toAmount KRW")
    }

    @Test
    @Order(5)
    fun `step 5 - update exchange rate to 1400 KRW`() {
        println("=== Multi-Currency E2E: Step 5 - Update exchange rate ===")
        val newRate = mapOf(
            "fromCurrency" to "USD",
            "toCurrency" to "KRW",
            "exchangeRate" to 1400,
            "rateType" to "SPOT",
            "source" to "BOK"
        )

        val resp = restTemplate.exchange(
            "/api/v1/currencies/exchange-rates", HttpMethod.POST,
            HttpEntity(newRate, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        println("  Updated rate: 1 USD = 1,400 KRW (was 1,350)")
    }

    @Test
    @Order(6)
    fun `step 6 - create currency revaluation`() {
        println("=== Multi-Currency E2E: Step 6 - Create revaluation ===")
        // Unrealized loss: $25,000 * (1400 - 1350) = 1,250,000 KRW loss
        val revalReq = mapOf(
            "fiscalYear" to 2025,
            "period" to 1,
            "fromCurrency" to "USD",
            "toCurrency" to "KRW",
            "originalRate" to 1350,
            "revaluationRate" to 1400,
            "unrealizedGainLoss" to -1250000
        )

        val resp = restTemplate.exchange(
            "/api/v1/currencies/revaluations", HttpMethod.POST,
            HttpEntity(revalReq, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        revalId = (data["id"] as Number).toLong()
        assertThat(data["status"]).isEqualTo("DRAFT")
        val gainLoss = BigDecimal(data["unrealizedGainLoss"].toString())
        assertThat(gainLoss.compareTo(BigDecimal(-1250000))).isEqualTo(0)
        println("  Created revaluation (id=$revalId): unrealized loss = -1,250,000 KRW")
    }

    @Test
    @Order(7)
    fun `step 7 - post revaluation and verify`() {
        println("=== Multi-Currency E2E: Step 7 - Post revaluation ===")
        val resp = restTemplate.exchange(
            "/api/v1/currencies/revaluations/$revalId/post", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(resp)["status"]).isEqualTo("POSTED")
        println("  Revaluation posted")

        // Verify exchange rates exist
        val ratesResp = restTemplate.exchange(
            "/api/v1/currencies/exchange-rates?fromCurrency=USD&toCurrency=KRW",
            HttpMethod.GET, HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(ratesResp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val rates = ratesResp.body!!["data"] as List<Map<String, Any>>
        assertThat(rates).hasSizeGreaterThanOrEqualTo(2)
        val rateValues = rates.map { BigDecimal(it["exchangeRate"].toString()).toInt() }.toSet()
        assertThat(rateValues).contains(1350, 1400)
        println("  Exchange rates confirmed: $rateValues")

        println("=== Multi-Currency E2E: COMPLETE ===")
    }
}
