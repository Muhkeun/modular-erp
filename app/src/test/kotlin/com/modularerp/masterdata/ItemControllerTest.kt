package com.modularerp.masterdata

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
class ItemControllerTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "ITEM_TEST"
        const val LOGIN_ID = "itemuser"
        const val PASSWORD = "pass123"
        var authToken: String = ""
        var createdItemId: Long = 0
    }

    private fun authHeaders(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(authToken)
        set("X-Tenant-Id", TENANT_ID)
        contentType = MediaType.APPLICATION_JSON
    }

    @Test
    @Order(0)
    fun setup() {
        // Register user
        val regReq = mapOf(
            "tenantId" to TENANT_ID,
            "loginId" to LOGIN_ID,
            "password" to PASSWORD,
            "name" to "Item Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        // Login
        val loginReq = mapOf(
            "tenantId" to TENANT_ID,
            "loginId" to LOGIN_ID,
            "password" to PASSWORD
        )
        val loginResp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        authToken = (loginResp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `create item`() {
        val request = mapOf(
            "code" to "MAT-001",
            "itemType" to "MATERIAL",
            "unitOfMeasure" to "EA",
            "specification" to "Test Material",
            "translations" to listOf(
                mapOf("locale" to "ko", "name" to "테스트자재", "description" to "테스트"),
                mapOf("locale" to "en", "name" to "Test Material", "description" to "Test")
            )
        )

        val response = restTemplate.exchange(
            "/api/v1/master-data/items",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as Map<String, Any>
        assertThat(data["code"]).isEqualTo("MAT-001")
        assertThat(data["itemType"]).isEqualTo("MATERIAL")
        createdItemId = (data["id"] as Number).toLong()
        assertThat(createdItemId).isGreaterThan(0)
    }

    @Test
    @Order(2)
    fun `get item by ID`() {
        val response = restTemplate.exchange(
            "/api/v1/master-data/items/$createdItemId",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as Map<String, Any>
        assertThat(data["code"]).isEqualTo("MAT-001")
    }

    @Test
    @Order(3)
    fun `list items with pagination`() {
        // Create a second item
        val request = mapOf(
            "code" to "MAT-002",
            "itemType" to "PRODUCT",
            "unitOfMeasure" to "KG",
            "translations" to listOf(
                mapOf("locale" to "ko", "name" to "제품A")
            )
        )
        restTemplate.exchange(
            "/api/v1/master-data/items",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        val response = restTemplate.exchange(
            "/api/v1/master-data/items?page=0&size=10",
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

        @Suppress("UNCHECKED_CAST")
        val meta = body["meta"] as Map<String, Any>
        assertThat((meta["totalElements"] as Number).toLong()).isGreaterThanOrEqualTo(2)
    }

    @Test
    @Order(4)
    fun `update item`() {
        val request = mapOf(
            "specification" to "Updated Spec",
            "itemGroup" to "RAW_MATERIAL"
        )

        val response = restTemplate.exchange(
            "/api/v1/master-data/items/$createdItemId",
            HttpMethod.PUT,
            HttpEntity(request, authHeaders()),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as Map<String, Any>
        assertThat(data["specification"]).isEqualTo("Updated Spec")
        assertThat(data["itemGroup"]).isEqualTo("RAW_MATERIAL")
    }

    @Test
    @Order(5)
    fun `delete item (soft delete)`() {
        val response = restTemplate.exchange(
            "/api/v1/master-data/items/$createdItemId",
            HttpMethod.DELETE,
            HttpEntity<Any>(authHeaders()),
            Void::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}
