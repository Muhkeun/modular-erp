package com.modularerp.crm

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
class CrmFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "CRM_TEST"
        var authToken: String = ""
        var customerId: Long = 0
        var leadId: Long = 0
        var opportunityId: Long = 0
        var activityId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to "crmuser",
            "password" to "pass123", "name" to "CRM Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "crmuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create customer`() {
        val request = mapOf(
            "customerCode" to "CUST-001",
            "customerName" to "Test Corporation",
            "customerType" to "CORPORATE",
            "industry" to "Manufacturing",
            "email" to "info@test.com",
            "creditLimit" to 1000000,
            "paymentTermDays" to 30,
            "status" to "ACTIVE"
        )

        val response = restTemplate.exchange(
            "/api/v1/crm/customers", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["customerCode"]).isEqualTo("CUST-001")
        assertThat(data["status"]).isEqualTo("ACTIVE")
        customerId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `create lead and convert to customer`() {
        val request = mapOf(
            "contactName" to "John Doe",
            "companyName" to "Prospect Inc.",
            "contactEmail" to "john@prospect.com",
            "source" to "WEB",
            "estimatedValue" to 500000
        )

        val response = restTemplate.exchange(
            "/api/v1/crm/leads", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("NEW")
        leadId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(3)
    fun `create opportunity`() {
        val request = mapOf(
            "customerId" to customerId,
            "title" to "Big Deal",
            "stage" to "PROSPECTING",
            "probability" to 30,
            "expectedAmount" to 1000000
        )

        val response = restTemplate.exchange(
            "/api/v1/crm/opportunities", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["stage"]).isEqualTo("PROSPECTING")
        opportunityId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(4)
    fun `update opportunity stage`() {
        val request = mapOf("stage" to "QUALIFICATION")

        val response = restTemplate.exchange(
            "/api/v1/crm/opportunities/$opportunityId/stage", HttpMethod.PUT,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["stage"]).isEqualTo("QUALIFICATION")
    }

    @Test
    @Order(5)
    fun `create and complete activity`() {
        val request = mapOf(
            "activityType" to "CALL",
            "subject" to "Follow-up call",
            "referenceType" to "CUSTOMER",
            "referenceId" to customerId
        )

        val createResp = restTemplate.exchange(
            "/api/v1/crm/activities", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        activityId = (extractData(createResp)["id"] as Number).toLong()

        val completeResp = restTemplate.exchange(
            "/api/v1/crm/activities/$activityId/complete", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(completeResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(completeResp)["completed"]).isEqualTo(true)
    }
}
