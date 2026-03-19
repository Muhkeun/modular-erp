package com.modularerp.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

/**
 * CRM-to-Sales Pipeline E2E Test
 *
 * Lead (WEB) -> Create Customer -> Create Opportunity
 * -> Stage progression (PROSPECTING -> CLOSED_WON)
 * -> Create SO for won opportunity -> Approve SO
 *
 * Note: Lead conversion requires QUALIFIED status, but no API exists
 * to update lead status. The test creates customer and opportunity
 * directly to test the full pipeline flow.
 */
@SpringBootTest(
    classes = [com.modularerp.app.ModularErpApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CrmToSalesFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "CRM_E2E"
        const val COMPANY = "C100"
        const val PLANT = "P100"
    }

    private var authToken: String = ""
    private var leadId: Long = 0
    private var customerId: Long = 0
    private var customerCode: String = "CUST-CRM-001"
    private var opportunityId: Long = 0
    private var soId: Long = 0
    private var soDocNo: String = ""

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
        println("=== CRM-to-Sales E2E: Step 0 - Setup ===")
        val regReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "crmuser",
            "password" to "pass123", "name" to "CRM Sales Rep"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "crmuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `step 1 - create a lead from web source`() {
        println("=== CRM-to-Sales E2E: Step 1 - Create Lead ===")
        val leadReq = mapOf(
            "companyName" to "GreenTech Solutions",
            "contactName" to "Lee Soyeon",
            "contactEmail" to "soyeon.lee@greentech.co.kr",
            "contactPhone" to "010-9876-5432",
            "source" to "WEB",
            "estimatedValue" to 15000000,
            "assignedTo" to "crmuser",
            "notes" to "Inquiry from website contact form about bulk order of IT equipment"
        )

        val resp = restTemplate.exchange(
            "/api/v1/crm/leads", HttpMethod.POST,
            HttpEntity(leadReq, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        leadId = (data["id"] as Number).toLong()
        assertThat(data["source"]).isEqualTo("WEB")
        assertThat(data["status"]).isEqualTo("NEW")
        println("  Created lead: ${data["leadNo"]} - GreenTech Solutions (id=$leadId)")
    }

    @Test
    @Order(2)
    fun `step 2 - create customer from lead information`() {
        println("=== CRM-to-Sales E2E: Step 2 - Create Customer ===")
        // In production, this would happen via lead conversion.
        // Since no qualify-lead API exists, we create the customer directly.
        val custReq = mapOf(
            "customerCode" to customerCode,
            "customerName" to "GreenTech Solutions",
            "customerType" to "CORPORATE",
            "industry" to "Technology",
            "email" to "soyeon.lee@greentech.co.kr",
            "phone" to "010-9876-5432",
            "contactPerson" to "Lee Soyeon",
            "creditLimit" to 50000000,
            "paymentTermDays" to 30,
            "status" to "ACTIVE",
            "notes" to "Converted from lead"
        )

        val resp = restTemplate.exchange(
            "/api/v1/crm/customers", HttpMethod.POST,
            HttpEntity(custReq, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        customerId = (data["id"] as Number).toLong()
        assertThat(data["customerName"]).isEqualTo("GreenTech Solutions")
        assertThat(data["status"]).isEqualTo("ACTIVE")
        println("  Created customer: $customerCode - GreenTech Solutions (id=$customerId)")
    }

    @Test
    @Order(3)
    fun `step 3 - create opportunity for customer`() {
        println("=== CRM-to-Sales E2E: Step 3 - Create Opportunity ===")
        val oppReq = mapOf(
            "customerId" to customerId,
            "title" to "IT Equipment Bulk Order - GreenTech Solutions",
            "description" to "Bulk order of enterprise server racks",
            "stage" to "PROSPECTING",
            "probability" to 20,
            "expectedAmount" to 15000000,
            "assignedTo" to "crmuser"
        )

        val resp = restTemplate.exchange(
            "/api/v1/crm/opportunities", HttpMethod.POST,
            HttpEntity(oppReq, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        opportunityId = (data["id"] as Number).toLong()
        assertThat(data["stage"]).isEqualTo("PROSPECTING")
        assertThat((data["customerId"] as Number).toLong()).isEqualTo(customerId)
        println("  Created opportunity: ${data["opportunityNo"]} (id=$opportunityId), stage=PROSPECTING")
    }

    @Test
    @Order(4)
    fun `step 4 - progress opportunity through stages to CLOSED_WON`() {
        println("=== CRM-to-Sales E2E: Step 4 - Progress opportunity stages ===")

        val stages = listOf("QUALIFICATION", "PROPOSAL", "NEGOTIATION", "CLOSED_WON")
        stages.forEach { stage ->
            val stageReq = mutableMapOf<String, Any>("stage" to stage)
            if (stage == "CLOSED_WON") {
                stageReq["actualAmount"] = 15000000
            }

            val resp = restTemplate.exchange(
                "/api/v1/crm/opportunities/$opportunityId/stage", HttpMethod.PUT,
                HttpEntity(stageReq, authHeaders()), Map::class.java
            )
            assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(extractData(resp)["stage"]).isEqualTo(stage)
            println("  Stage -> $stage")
        }
        println("  Opportunity won with actual amount 15M KRW")
    }

    @Test
    @Order(5)
    fun `step 5 - create item for sales order`() {
        println("=== CRM-to-Sales E2E: Step 5 - Create product item ===")
        val item = mapOf(
            "code" to "PROD-CRM-001",
            "itemType" to "PRODUCT",
            "itemGroup" to "IT_EQUIPMENT",
            "unitOfMeasure" to "EA",
            "translations" to listOf(mapOf("locale" to "en", "name" to "Enterprise Server Rack"))
        )
        val resp = restTemplate.exchange(
            "/api/v1/master-data/items", HttpMethod.POST,
            HttpEntity(item, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        println("  Created item: Enterprise Server Rack")
    }

    @Test
    @Order(6)
    fun `step 6 - create sales order for won opportunity`() {
        println("=== CRM-to-Sales E2E: Step 6 - Create SO ===")
        val soRequest = mapOf(
            "companyCode" to COMPANY,
            "plantCode" to PLANT,
            "customerCode" to customerCode,
            "customerName" to "GreenTech Solutions",
            "currencyCode" to "KRW",
            "paymentTerms" to "Net 30",
            "remark" to "Order from CRM opportunity - GreenTech Solutions",
            "lines" to listOf(
                mapOf(
                    "itemCode" to "PROD-CRM-001",
                    "itemName" to "Enterprise Server Rack",
                    "quantity" to 5,
                    "unitOfMeasure" to "EA",
                    "unitPrice" to 3000000
                )
            )
        )

        val resp = restTemplate.exchange(
            "/api/v1/sales/orders", HttpMethod.POST,
            HttpEntity(soRequest, authHeaders()), Map::class.java
        )
        assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(resp)
        soId = (data["id"] as Number).toLong()
        soDocNo = data["documentNo"] as String
        assertThat(data["customerCode"]).isEqualTo(customerCode)
        println("  Created SO: $soDocNo (id=$soId) for $customerCode")
    }

    @Test
    @Order(7)
    fun `step 7 - submit and approve SO`() {
        println("=== CRM-to-Sales E2E: Step 7 - Submit and approve SO ===")

        val submitResp = restTemplate.exchange(
            "/api/v1/sales/orders/$soId/submit", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(submitResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(submitResp)["status"]).isEqualTo("SUBMITTED")

        val approveResp = restTemplate.exchange(
            "/api/v1/sales/orders/$soId/approve", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(approveResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(approveResp)["status"]).isEqualTo("APPROVED")
        println("  SO $soDocNo submitted and approved")
    }

    @Test
    @Order(8)
    fun `step 8 - verify sales pipeline`() {
        println("=== CRM-to-Sales E2E: Step 8 - Verify pipeline ===")

        // Verify pipeline summary
        val pipeResp = restTemplate.exchange(
            "/api/v1/crm/pipeline", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(pipeResp.statusCode).isEqualTo(HttpStatus.OK)
        println("  Pipeline data retrieved")

        // Verify opportunity is CLOSED_WON
        val oppResp = restTemplate.exchange(
            "/api/v1/crm/opportunities/$opportunityId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(extractData(oppResp)["stage"]).isEqualTo("CLOSED_WON")
        println("  Opportunity: CLOSED_WON")

        // Verify SO is APPROVED
        val soResp = restTemplate.exchange(
            "/api/v1/sales/orders/$soId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(extractData(soResp)["status"]).isEqualTo("APPROVED")
        println("  SO $soDocNo: APPROVED")

        // Verify lead still exists as NEW (conversion not done via API)
        val leadResp = restTemplate.exchange(
            "/api/v1/crm/leads/$leadId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(leadResp.statusCode).isEqualTo(HttpStatus.OK)
        println("  Lead: ${extractData(leadResp)["status"]}")

        println("=== CRM-to-Sales E2E: COMPLETE ===")
        println("  Pipeline: Lead (WEB) -> Customer (ACTIVE) -> Opportunity (CLOSED_WON) -> SO (APPROVED)")
    }
}
