package com.modularerp.sso

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SsoControllerTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `providers endpoint returns supported providers`() {
        val response = restTemplate.getForEntity(
            "/api/v1/auth/sso/providers",
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as List<Map<String, Any>>
        assertThat(data).isNotEmpty
        assertThat(data.map { it["id"] }).contains("google", "azure-ad", "okta", "saml")

        // All should be disabled since SSO is not configured
        data.forEach { provider ->
            assertThat(provider["enabled"]).isEqualTo(false)
        }
    }

    @Test
    fun `authorize endpoint returns error when SSO not configured`() {
        val response = restTemplate.getForEntity(
            "/api/v1/auth/sso/authorize/google",
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(false)

        @Suppress("UNCHECKED_CAST")
        val error = body["error"] as Map<String, Any>
        assertThat(error["code"]).isEqualTo("SSO_NOT_CONFIGURED")
    }

    @Test
    fun `callback endpoint returns error when SSO not configured`() {
        val request = mapOf("code" to "test-code", "state" to "test-state")
        val response = restTemplate.postForEntity(
            "/api/v1/auth/sso/callback/google",
            request,
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(false)

        @Suppress("UNCHECKED_CAST")
        val error = body["error"] as Map<String, Any>
        assertThat(error["code"]).isEqualTo("SSO_NOT_CONFIGURED")
    }
}
