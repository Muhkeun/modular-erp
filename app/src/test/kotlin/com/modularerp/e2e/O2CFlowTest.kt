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
 * Order-to-Cash (O2C) Full Cycle E2E Test
 *
 * Create items + customer -> SO (3 lines) -> Confirm SO
 * -> GR to stock materials -> GI for SO -> Confirm GI -> verify stock decreased
 * -> JE (AR posting) -> Post JE -> verify complete trail
 */
@SpringBootTest(
    classes = [com.modularerp.app.ModularErpApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class O2CFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "O2C_E2E"
        const val COMPANY = "C100"
        const val PLANT = "P100"
        const val STORAGE = "WH01"
    }

    private var authToken: String = ""
    private var customerId: Long = 0
    private var soId: Long = 0
    private var soDocNo: String = ""
    private var grId: Long = 0
    private var giId: Long = 0
    private var jeId: Long = 0

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
        println("=== O2C E2E: Step 0 - Register and login ===")
        val regReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "o2cuser",
            "password" to "pass123", "name" to "O2C Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "o2cuser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
    }

    @Test
    @Order(1)
    fun `step 1 - create master data - items and customer`() {
        println("=== O2C E2E: Step 1 - Create items and customer ===")

        // Create 3 products
        listOf(
            Triple("PROD-O2C-001", "Wireless Keyboard", "PRODUCT"),
            Triple("PROD-O2C-002", "Wireless Mouse", "PRODUCT"),
            Triple("PROD-O2C-003", "USB Hub 4-Port", "PRODUCT")
        ).forEach { (code, name, type) ->
            val item = mapOf(
                "code" to code, "itemType" to type, "itemGroup" to "ELECTRONICS",
                "unitOfMeasure" to "EA",
                "translations" to listOf(mapOf("locale" to "en", "name" to name))
            )
            val resp = restTemplate.exchange(
                "/api/v1/master-data/items", HttpMethod.POST,
                HttpEntity(item, authHeaders()), Map::class.java
            )
            assertThat(resp.statusCode).isEqualTo(HttpStatus.CREATED)
            println("  Created item: $name")
        }

        // Create customer
        val customer = mapOf(
            "customerCode" to "CUST-O2C-001",
            "customerName" to "TechMart Electronics",
            "customerType" to "CORPORATE",
            "industry" to "Retail",
            "email" to "orders@techmart.co.kr",
            "phone" to "02-1234-5678",
            "contactPerson" to "Kim Minsoo",
            "creditLimit" to 50000000,
            "paymentTermDays" to 30,
            "status" to "ACTIVE"
        )
        val custResp = restTemplate.exchange(
            "/api/v1/crm/customers", HttpMethod.POST,
            HttpEntity(customer, authHeaders()), Map::class.java
        )
        assertThat(custResp.statusCode).isEqualTo(HttpStatus.CREATED)
        customerId = (extractData(custResp)["id"] as Number).toLong()
        println("  Created customer: TechMart Electronics (id=$customerId)")
    }

    @Test
    @Order(2)
    fun `step 2 - stock materials via goods receipt`() {
        println("=== O2C E2E: Step 2 - Stock products via GR ===")
        val grRequest = mapOf(
            "companyCode" to COMPANY,
            "plantCode" to PLANT,
            "storageLocation" to STORAGE,
            "vendorCode" to "V-INTERNAL",
            "vendorName" to "Internal Production",
            "lines" to listOf(
                mapOf("itemCode" to "PROD-O2C-001", "itemName" to "Wireless Keyboard", "quantity" to 200, "unitOfMeasure" to "EA", "unitPrice" to 25000),
                mapOf("itemCode" to "PROD-O2C-002", "itemName" to "Wireless Mouse", "quantity" to 300, "unitOfMeasure" to "EA", "unitPrice" to 15000),
                mapOf("itemCode" to "PROD-O2C-003", "itemName" to "USB Hub 4-Port", "quantity" to 150, "unitOfMeasure" to "EA", "unitPrice" to 12000)
            )
        )

        val createResp = restTemplate.exchange(
            "/api/v1/logistics/goods-receipts", HttpMethod.POST,
            HttpEntity(grRequest, authHeaders()), Map::class.java
        )
        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        grId = (extractData(createResp)["id"] as Number).toLong()

        val confirmResp = restTemplate.exchange(
            "/api/v1/logistics/goods-receipts/$grId/confirm", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(confirmResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(confirmResp)["status"]).isEqualTo("CONFIRMED")
        println("  Stocked: Keyboard=200, Mouse=300, USB Hub=150")
    }

    @Test
    @Order(3)
    fun `step 3 - create sales order with 3 line items`() {
        println("=== O2C E2E: Step 3 - Create Sales Order ===")
        val soRequest = mapOf(
            "companyCode" to COMPANY,
            "plantCode" to PLANT,
            "customerCode" to "CUST-O2C-001",
            "customerName" to "TechMart Electronics",
            "currencyCode" to "KRW",
            "paymentTerms" to "Net 30",
            "shippingAddress" to "Seoul, Gangnam-gu, Teheran-ro 123",
            "remark" to "Q1 bulk order",
            "lines" to listOf(
                mapOf("itemCode" to "PROD-O2C-001", "itemName" to "Wireless Keyboard", "quantity" to 50, "unitOfMeasure" to "EA", "unitPrice" to 45000),
                mapOf("itemCode" to "PROD-O2C-002", "itemName" to "Wireless Mouse", "quantity" to 100, "unitOfMeasure" to "EA", "unitPrice" to 25000),
                mapOf("itemCode" to "PROD-O2C-003", "itemName" to "USB Hub 4-Port", "quantity" to 30, "unitOfMeasure" to "EA", "unitPrice" to 20000)
            )
        )

        val response = restTemplate.exchange(
            "/api/v1/sales/orders", HttpMethod.POST,
            HttpEntity(soRequest, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        soId = (data["id"] as Number).toLong()
        soDocNo = data["documentNo"] as String

        @Suppress("UNCHECKED_CAST")
        val lines = data["lines"] as List<Map<String, Any>>
        assertThat(lines).hasSize(3)
        println("  Created SO: $soDocNo (id=$soId) with 3 lines")
        println("  Total: Keyboard 50x45000 + Mouse 100x25000 + Hub 30x20000")
    }

    @Test
    @Order(4)
    fun `step 4 - submit and approve SO`() {
        println("=== O2C E2E: Step 4 - Submit and Approve SO ===")

        val submitResp = restTemplate.exchange(
            "/api/v1/sales/orders/$soId/submit", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(submitResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(submitResp)["status"]).isEqualTo("SUBMITTED")
        println("  SO $soDocNo submitted")

        val approveResp = restTemplate.exchange(
            "/api/v1/sales/orders/$soId/approve", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(approveResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(approveResp)["status"]).isEqualTo("APPROVED")
        println("  SO $soDocNo approved")
    }

    @Test
    @Order(5)
    fun `step 5 - create and confirm goods issue for SO`() {
        println("=== O2C E2E: Step 5 - Create and confirm GI ===")
        val giRequest = mapOf(
            "companyCode" to COMPANY,
            "plantCode" to PLANT,
            "storageLocation" to STORAGE,
            "issueType" to "SALES",
            "referenceDocNo" to soDocNo,
            "remark" to "Shipment for SO $soDocNo",
            "lines" to listOf(
                mapOf("itemCode" to "PROD-O2C-001", "itemName" to "Wireless Keyboard", "quantity" to 50, "unitOfMeasure" to "EA"),
                mapOf("itemCode" to "PROD-O2C-002", "itemName" to "Wireless Mouse", "quantity" to 100, "unitOfMeasure" to "EA"),
                mapOf("itemCode" to "PROD-O2C-003", "itemName" to "USB Hub 4-Port", "quantity" to 30, "unitOfMeasure" to "EA")
            )
        )

        val createResp = restTemplate.exchange(
            "/api/v1/logistics/goods-issues", HttpMethod.POST,
            HttpEntity(giRequest, authHeaders()), Map::class.java
        )
        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        giId = (extractData(createResp)["id"] as Number).toLong()
        println("  Created GI (id=$giId)")

        val confirmResp = restTemplate.exchange(
            "/api/v1/logistics/goods-issues/$giId/confirm", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(confirmResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(confirmResp)["status"]).isEqualTo("CONFIRMED")
        println("  GI confirmed - goods shipped")
    }

    @Test
    @Order(6)
    fun `step 6 - verify stock decreased after GI`() {
        println("=== O2C E2E: Step 6 - Verify stock decreased ===")

        // Keyboard: 200 - 50 = 150
        val resp1 = restTemplate.exchange(
            "/api/v1/logistics/stock?itemCode=PROD-O2C-001&plantCode=$PLANT",
            HttpMethod.GET, HttpEntity<Any>(authHeaders()), Map::class.java
        )
        @Suppress("UNCHECKED_CAST")
        val data1 = resp1.body!!["data"] as List<Map<String, Any>>
        val stock1 = data1.find { it["itemCode"] == "PROD-O2C-001" }!!
        assertThat(BigDecimal(stock1["quantityOnHand"].toString()).compareTo(BigDecimal(150))).isEqualTo(0)
        println("  Keyboard stock: 200 - 50 = 150 OK")

        // Mouse: 300 - 100 = 200
        val resp2 = restTemplate.exchange(
            "/api/v1/logistics/stock?itemCode=PROD-O2C-002&plantCode=$PLANT",
            HttpMethod.GET, HttpEntity<Any>(authHeaders()), Map::class.java
        )
        @Suppress("UNCHECKED_CAST")
        val data2 = resp2.body!!["data"] as List<Map<String, Any>>
        val stock2 = data2.find { it["itemCode"] == "PROD-O2C-002" }!!
        assertThat(BigDecimal(stock2["quantityOnHand"].toString()).compareTo(BigDecimal(200))).isEqualTo(0)
        println("  Mouse stock: 300 - 100 = 200 OK")

        // USB Hub: 150 - 30 = 120
        val resp3 = restTemplate.exchange(
            "/api/v1/logistics/stock?itemCode=PROD-O2C-003&plantCode=$PLANT",
            HttpMethod.GET, HttpEntity<Any>(authHeaders()), Map::class.java
        )
        @Suppress("UNCHECKED_CAST")
        val data3 = resp3.body!!["data"] as List<Map<String, Any>>
        val stock3 = data3.find { it["itemCode"] == "PROD-O2C-003" }!!
        assertThat(BigDecimal(stock3["quantityOnHand"].toString()).compareTo(BigDecimal(120))).isEqualTo(0)
        println("  USB Hub stock: 150 - 30 = 120 OK")
    }

    @Test
    @Order(7)
    fun `step 7 - create and post AR journal entry`() {
        println("=== O2C E2E: Step 7 - Create and post AR journal entry ===")
        // Total: 50*45000 + 100*25000 + 30*20000 = 2,250,000 + 2,500,000 + 600,000 = 5,350,000
        val jeRequest = mapOf(
            "companyCode" to COMPANY,
            "entryType" to "MANUAL",
            "referenceDocNo" to soDocNo,
            "referenceDocType" to "SO",
            "description" to "AR posting for sales order $soDocNo - TechMart Electronics",
            "currencyCode" to "KRW",
            "lines" to listOf(
                mapOf(
                    "accountCode" to "1200",
                    "accountName" to "Accounts Receivable",
                    "debitAmount" to 5350000,
                    "creditAmount" to 0,
                    "description" to "AR from TechMart Electronics"
                ),
                mapOf(
                    "accountCode" to "4100",
                    "accountName" to "Sales Revenue",
                    "debitAmount" to 0,
                    "creditAmount" to 5350000,
                    "description" to "Revenue from SO $soDocNo"
                )
            )
        )

        val createResp = restTemplate.exchange(
            "/api/v1/account/journal-entries", HttpMethod.POST,
            HttpEntity(jeRequest, authHeaders()), Map::class.java
        )
        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        val jeData = extractData(createResp)
        assertThat(jeData["isBalanced"]).isEqualTo(true)
        jeId = (jeData["id"] as Number).toLong()

        val postResp = restTemplate.exchange(
            "/api/v1/account/journal-entries/$jeId/post", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(postResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(postResp)["status"]).isEqualTo("POSTED")
        println("  JE created and posted: AR 5,350,000 / Revenue 5,350,000")
    }

    @Test
    @Order(8)
    fun `step 8 - verify complete O2C trail`() {
        println("=== O2C E2E: Step 8 - Verify complete trail ===")

        // SO approved
        val soResp = restTemplate.exchange(
            "/api/v1/sales/orders/$soId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(extractData(soResp)["status"]).isEqualTo("APPROVED")
        println("  SO $soDocNo: APPROVED")

        // GI confirmed
        val giResp = restTemplate.exchange(
            "/api/v1/logistics/goods-issues/$giId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(extractData(giResp)["status"]).isEqualTo("CONFIRMED")
        println("  GI: CONFIRMED")

        // JE posted with reference to SO
        val jeResp = restTemplate.exchange(
            "/api/v1/account/journal-entries/$jeId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        val jeData = extractData(jeResp)
        assertThat(jeData["status"]).isEqualTo("POSTED")
        assertThat(jeData["referenceDocNo"]).isEqualTo(soDocNo)
        println("  JE: POSTED, referencing SO $soDocNo")

        println("=== O2C E2E: COMPLETE ===")
    }
}
