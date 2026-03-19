package com.modularerp.notification

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
class NotificationFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "NOTIF_TEST"
        const val USER_ID = "notifuser"
        var authToken: String = ""
        var templateId: Long = 0
        var notificationId: Long = 0
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
            "tenantId" to TENANT_ID, "loginId" to USER_ID,
            "password" to "pass123", "name" to "Notif Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to USER_ID, "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create notification template`() {
        val request = mapOf(
            "templateCode" to "PO_APPROVED",
            "templateName" to "PO Approval Notification",
            "channel" to "IN_APP",
            "eventType" to "PO_APPROVED",
            "subject" to "PO \${documentNo} has been approved",
            "body" to "Purchase Order \${documentNo} for \${vendorName} has been approved."
        )

        val response = restTemplate.exchange(
            "/api/v1/notifications/templates", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["templateCode"]).isEqualTo("PO_APPROVED")
        templateId = (data["id"] as Number).toLong()
    }

    @Test
    @Order(2)
    fun `list templates`() {
        val response = restTemplate.exchange(
            "/api/v1/notifications/templates", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<Map<String, Any>>
        assertThat(data).isNotEmpty
    }

    @Test
    @Order(3)
    fun `get unread count - initially zero`() {
        val response = restTemplate.exchange(
            "/api/v1/notifications/unread-count?recipientId=$USER_ID", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat((data["count"] as Number).toLong()).isEqualTo(0)
    }

    @Test
    @Order(4)
    fun `update notification preference`() {
        val request = mapOf(
            "eventType" to "PO_APPROVED",
            "channelInApp" to true,
            "channelEmail" to true,
            "channelSms" to false,
            "channelPush" to false
        )

        val response = restTemplate.exchange(
            "/api/v1/notifications/preferences?userId=$USER_ID", HttpMethod.PUT,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)
        assertThat(data["channelInApp"]).isEqualTo(true)
        assertThat(data["channelEmail"]).isEqualTo(true)
    }

    @Test
    @Order(5)
    fun `get preferences`() {
        val response = restTemplate.exchange(
            "/api/v1/notifications/preferences?userId=$USER_ID", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as List<Map<String, Any>>
        assertThat(data).hasSize(1)
        assertThat(data[0]["eventType"]).isEqualTo("PO_APPROVED")
    }
}
