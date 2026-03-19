package com.modularerp.dashboard

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
class DashboardControllerTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    private fun registerAndGetToken(): String {
        val registerReq = mapOf(
            "tenantId" to "DASH_TEST",
            "loginId" to "dashuser",
            "password" to "password123",
            "name" to "Dashboard Test User"
        )
        restTemplate.postForEntity("/api/v1/auth/register", registerReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to "DASH_TEST",
            "loginId" to "dashuser",
            "password" to "password123"
        )
        val loginRes = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        return (loginRes.body!!["data"] as Map<String, Any>)["token"] as String
    }

    private fun authHeaders(): HttpHeaders {
        val token = registerAndGetToken()
        return HttpHeaders().apply {
            setBearerAuth(token)
            set("X-Tenant-Id", "DASH_TEST")
        }
    }

    @Test
    @Order(1)
    fun `summary endpoint returns proper structure`() {
        val headers = authHeaders()
        val response = restTemplate.exchange(
            "/api/v1/dashboard/summary",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as Map<String, Any>
        assertThat(data).containsKey("purchaseOrders")
        assertThat(data).containsKey("salesOrders")
        assertThat(data).containsKey("workOrders")
        assertThat(data).containsKey("pendingApprovals")
        assertThat(data).containsKey("lowStockItems")
        assertThat(data).containsKey("overdueDeliveries")
        assertThat(data).containsKey("revenueThisMonth")
        assertThat(data).containsKey("expenseThisMonth")
        assertThat(data).containsKey("topCustomers")
        assertThat(data).containsKey("topProducts")
        assertThat(data).containsKey("recentActivities")
        assertThat(data).containsKey("openOpportunities")
        assertThat(data).containsKey("opportunityValue")

        @Suppress("UNCHECKED_CAST")
        val po = data["purchaseOrders"] as Map<String, Any>
        assertThat(po).containsKeys("total", "draft", "submitted", "approved", "thisMonth")
    }

    @Test
    @Order(2)
    fun `sales trend returns monthly data`() {
        val headers = authHeaders()
        val response = restTemplate.exchange(
            "/api/v1/dashboard/charts/sales-trend?months=3",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as List<Map<String, Any>>
        assertThat(data).hasSize(3)
        assertThat(data[0]).containsKeys("month", "count", "amount")
    }

    @Test
    @Order(3)
    fun `purchase trend returns monthly data`() {
        val headers = authHeaders()
        val response = restTemplate.exchange(
            "/api/v1/dashboard/charts/purchase-trend?months=6",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val body = response.body!!
        assertThat(body["success"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val data = body["data"] as List<Map<String, Any>>
        assertThat(data).hasSize(6)
    }

    @Test
    @Order(4)
    fun `summary endpoint requires authentication`() {
        val response = restTemplate.getForEntity(
            "/api/v1/dashboard/summary",
            Map::class.java
        )
        assertThat(response.statusCode.value()).isIn(401, 403)
    }
}
