package com.modularerp.auth

import com.modularerp.web.dto.ApiResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
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
class AuthControllerTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "TEST_TENANT"
        const val LOGIN_ID = "authtest"
        const val PASSWORD = "password123"
        const val NAME = "Auth Test User"
    }

    @Test
    @Order(1)
    fun `register a new user returns token`() {
        val request = mapOf(
            "tenantId" to TENANT_ID,
            "loginId" to LOGIN_ID,
            "password" to PASSWORD,
            "name" to NAME,
            "locale" to "en"
        )

        val response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            request,
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as Map<String, Any>
        assertThat(data["token"]).isNotNull
        assertThat(data["userId"]).isEqualTo(LOGIN_ID)
        assertThat(data["tenantId"]).isEqualTo(TENANT_ID)
    }

    @Test
    @Order(2)
    fun `login with valid credentials returns JWT token`() {
        val request = mapOf(
            "tenantId" to TENANT_ID,
            "loginId" to LOGIN_ID,
            "password" to PASSWORD
        )

        val response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            request,
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as Map<String, Any>
        assertThat(data["token"] as String).isNotBlank()
        assertThat(data["name"]).isEqualTo(NAME)
    }

    @Test
    @Order(3)
    fun `login with invalid password returns 401`() {
        val request = mapOf(
            "tenantId" to TENANT_ID,
            "loginId" to LOGIN_ID,
            "password" to "wrongpassword"
        )

        val response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            request,
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    @Order(4)
    fun `access protected endpoint without token returns 401 or 403`() {
        val response = restTemplate.getForEntity(
            "/api/v1/master-data/items",
            Map::class.java
        )

        assertThat(response.statusCode.value()).isIn(401, 403)
    }

    @Test
    @Order(5)
    fun `access protected endpoint with valid token succeeds`() {
        // First login to get token
        val loginRequest = mapOf(
            "tenantId" to TENANT_ID,
            "loginId" to LOGIN_ID,
            "password" to PASSWORD
        )
        val loginResponse = restTemplate.postForEntity(
            "/api/v1/auth/login",
            loginRequest,
            Map::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val token = (loginResponse.body!!["data"] as Map<String, Any>)["token"] as String

        val headers = HttpHeaders().apply {
            setBearerAuth(token)
            set("X-Tenant-Id", TENANT_ID)
        }

        val response = restTemplate.exchange(
            "/api/v1/master-data/items",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!["success"]).isEqualTo(true)
    }
}
