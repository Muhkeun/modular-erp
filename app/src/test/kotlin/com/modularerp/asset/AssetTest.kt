package com.modularerp.asset

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
class AssetTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "ASSET_TEST"
        var authToken: String = ""
        var assetId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "assetuser",
            "password" to "pass123", "name" to "Asset Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "assetuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `register new asset`() {
        val request = mapOf(
            "name" to "Office Laptop",
            "category" to "IT_EQUIPMENT",
            "acquisitionDate" to "2026-01-15",
            "acquisitionCost" to 2000000,
            "usefulLifeMonths" to 36,
            "depreciationMethod" to "STRAIGHT_LINE",
            "salvageValue" to 200000,
            "location" to "HQ-3F",
            "department" to "IT",
            "serialNumber" to "SN-2026-001"
        )

        val response = restTemplate.exchange(
            "/api/v1/assets", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        assertThat(data["name"]).isEqualTo("Office Laptop")
        assertThat((data["assetNo"] as String)).startsWith("FA")
        assetId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `activate asset`() {
        val response = restTemplate.exchange(
            "/api/v1/assets/$assetId/activate", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("ACTIVE")
    }

    @Test
    @Order(3)
    fun `run depreciation`() {
        val request = mapOf("year" to 2026, "month" to 2)
        val response = restTemplate.exchange(
            "/api/v1/assets/depreciation/run", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<*>
        assertThat(data).hasSizeGreaterThanOrEqualTo(1)
    }

    @Test
    @Order(4)
    fun `get depreciation schedule`() {
        val response = restTemplate.exchange(
            "/api/v1/assets/$assetId/schedule", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<*>
        assertThat(data).hasSizeGreaterThanOrEqualTo(1)
    }

    @Test
    @Order(5)
    fun `dispose asset`() {
        val request = mapOf(
            "disposalDate" to "2026-06-30",
            "disposalType" to "SALE",
            "disposalAmount" to 1500000,
            "reason" to "Upgrade to new model"
        )
        val response = restTemplate.exchange(
            "/api/v1/assets/$assetId/dispose", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat(data["disposalType"]).isEqualTo("SALE")
    }
}
