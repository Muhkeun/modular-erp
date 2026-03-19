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
 * Procure-to-Pay (P2P) Full Cycle E2E Test
 *
 * PR (2 lines) -> Submit -> Approve -> PO from PR -> Submit PO -> Approve PO
 * -> GR referencing PO -> Confirm GR -> verify stock -> JE (AP posting) -> Post JE
 * -> verify complete audit trail
 */
@SpringBootTest(
    classes = [com.modularerp.app.ModularErpApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class P2PFlowTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        const val TENANT_ID = "P2P_E2E"
        const val COMPANY = "C100"
        const val PLANT = "P100"
        const val STORAGE = "WH01"
    }

    private var authToken: String = ""
    private var itemId1: Long = 0
    private var itemId2: Long = 0
    private var prId: Long = 0
    private var prDocNo: String = ""
    private var poId: Long = 0
    private var poDocNo: String = ""
    private var grId: Long = 0
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
        println("=== P2P E2E: Step 0 - Register and login ===")
        val regReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "p2puser",
            "password" to "pass123", "name" to "P2P Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", regReq, Map::class.java)

        val loginReq = mapOf(
            "tenantId" to TENANT_ID, "loginId" to "p2puser", "password" to "pass123"
        )
        val resp = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)
        assertThat(resp.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        authToken = (resp.body!!["data"] as Map<String, Any>)["token"] as String
        assertThat(authToken).isNotBlank()
        println("  Auth token obtained successfully")
    }

    @Test
    @Order(1)
    fun `step 1 - create master data items`() {
        println("=== P2P E2E: Step 1 - Create master data items ===")

        // Item 1: Steel Plate
        val item1 = mapOf(
            "code" to "MAT-P2P-001",
            "itemType" to "MATERIAL",
            "itemGroup" to "RAW_MATERIAL",
            "unitOfMeasure" to "KG",
            "translations" to listOf(
                mapOf("locale" to "en", "name" to "Steel Plate A36", "description" to "Hot rolled steel plate")
            )
        )
        val resp1 = restTemplate.exchange(
            "/api/v1/master-data/items", HttpMethod.POST,
            HttpEntity(item1, authHeaders()), Map::class.java
        )
        assertThat(resp1.statusCode).isEqualTo(HttpStatus.CREATED)
        itemId1 = (extractData(resp1)["id"] as Number).toLong()
        println("  Created item: Steel Plate A36 (id=$itemId1)")

        // Item 2: Copper Wire
        val item2 = mapOf(
            "code" to "MAT-P2P-002",
            "itemType" to "MATERIAL",
            "itemGroup" to "RAW_MATERIAL",
            "unitOfMeasure" to "M",
            "translations" to listOf(
                mapOf("locale" to "en", "name" to "Copper Wire 2.5mm", "description" to "Electrical copper wire")
            )
        )
        val resp2 = restTemplate.exchange(
            "/api/v1/master-data/items", HttpMethod.POST,
            HttpEntity(item2, authHeaders()), Map::class.java
        )
        assertThat(resp2.statusCode).isEqualTo(HttpStatus.CREATED)
        itemId2 = (extractData(resp2)["id"] as Number).toLong()
        println("  Created item: Copper Wire 2.5mm (id=$itemId2)")
    }

    @Test
    @Order(2)
    fun `step 2 - create purchase request with 2 line items`() {
        println("=== P2P E2E: Step 2 - Create Purchase Request ===")
        val request = mapOf(
            "companyCode" to COMPANY,
            "plantCode" to PLANT,
            "departmentCode" to "PRODUCTION",
            "prType" to "STANDARD",
            "description" to "Raw materials for Q1 production",
            "lines" to listOf(
                mapOf(
                    "itemCode" to "MAT-P2P-001",
                    "itemName" to "Steel Plate A36",
                    "quantity" to 500,
                    "unitOfMeasure" to "KG",
                    "unitPrice" to 2500
                ),
                mapOf(
                    "itemCode" to "MAT-P2P-002",
                    "itemName" to "Copper Wire 2.5mm",
                    "quantity" to 1000,
                    "unitOfMeasure" to "M",
                    "unitPrice" to 800
                )
            )
        )

        val response = restTemplate.exchange(
            "/api/v1/purchase/requests", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        prId = (data["id"] as Number).toLong()
        prDocNo = data["documentNo"] as String
        assertThat(prDocNo).startsWith("PR")

        @Suppress("UNCHECKED_CAST")
        val lines = data["lines"] as List<Map<String, Any>>
        assertThat(lines).hasSize(2)
        println("  Created PR: $prDocNo (id=$prId) with 2 lines")
    }

    @Test
    @Order(3)
    fun `step 3 - submit PR for approval`() {
        println("=== P2P E2E: Step 3 - Submit PR ===")
        val response = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId/submit", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("SUBMITTED")
        println("  PR $prDocNo submitted successfully")
    }

    @Test
    @Order(4)
    fun `step 4 - approve PR`() {
        println("=== P2P E2E: Step 4 - Approve PR ===")
        val response = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId/approve", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(response)["status"]).isEqualTo("APPROVED")
        println("  PR $prDocNo approved")
    }

    @Test
    @Order(5)
    fun `step 5 - create PO from approved PR`() {
        println("=== P2P E2E: Step 5 - Create PO from PR ===")
        val request = mapOf(
            "vendorCode" to "V-STEEL-001",
            "vendorName" to "Korea Steel Supply Co.",
            "currencyCode" to "KRW"
        )

        val response = restTemplate.exchange(
            "/api/v1/purchase/orders/from-pr/$prId", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        assertThat(data["vendorCode"]).isEqualTo("V-STEEL-001")
        poId = (data["id"] as Number).toLong()
        poDocNo = data["documentNo"] as String
        assertThat(poDocNo).startsWith("PO")

        @Suppress("UNCHECKED_CAST")
        val lines = data["lines"] as List<Map<String, Any>>
        assertThat(lines).hasSize(2)
        println("  Created PO: $poDocNo (id=$poId) with 2 lines from PR $prDocNo")
    }

    @Test
    @Order(6)
    fun `step 6 - verify PR open quantity consumed`() {
        println("=== P2P E2E: Step 6 - Verify PR open quantity consumed ===")
        val response = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = extractData(response)

        @Suppress("UNCHECKED_CAST")
        val lines = data["lines"] as List<Map<String, Any>>
        lines.forEach { line ->
            val openQty = BigDecimal(line["openQuantity"].toString())
            assertThat(openQty.compareTo(BigDecimal.ZERO)).isEqualTo(0)
        }
        println("  All PR lines have openQuantity = 0 (fully converted to PO)")
    }

    @Test
    @Order(7)
    fun `step 7 - submit and approve PO`() {
        println("=== P2P E2E: Step 7 - Submit and Approve PO ===")

        // Submit
        val submitResp = restTemplate.exchange(
            "/api/v1/purchase/orders/$poId/submit", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(submitResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(submitResp)["status"]).isEqualTo("SUBMITTED")
        println("  PO $poDocNo submitted")

        // Approve
        val approveResp = restTemplate.exchange(
            "/api/v1/purchase/orders/$poId/approve", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(approveResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(approveResp)["status"]).isEqualTo("APPROVED")
        println("  PO $poDocNo approved")
    }

    @Test
    @Order(8)
    fun `step 8 - create goods receipt referencing PO`() {
        println("=== P2P E2E: Step 8 - Create Goods Receipt ===")
        val request = mapOf(
            "companyCode" to COMPANY,
            "plantCode" to PLANT,
            "storageLocation" to STORAGE,
            "poDocumentNo" to poDocNo,
            "vendorCode" to "V-STEEL-001",
            "vendorName" to "Korea Steel Supply Co.",
            "remark" to "Receiving materials from PO $poDocNo",
            "lines" to listOf(
                mapOf(
                    "itemCode" to "MAT-P2P-001",
                    "itemName" to "Steel Plate A36",
                    "quantity" to 500,
                    "unitOfMeasure" to "KG",
                    "unitPrice" to 2500,
                    "poLineNo" to 1
                ),
                mapOf(
                    "itemCode" to "MAT-P2P-002",
                    "itemName" to "Copper Wire 2.5mm",
                    "quantity" to 1000,
                    "unitOfMeasure" to "M",
                    "unitPrice" to 800,
                    "poLineNo" to 2
                )
            )
        )

        val response = restTemplate.exchange(
            "/api/v1/logistics/goods-receipts", HttpMethod.POST,
            HttpEntity(request, authHeaders()), Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val data = extractData(response)
        assertThat(data["status"]).isEqualTo("DRAFT")
        assertThat(data["poDocumentNo"]).isEqualTo(poDocNo)
        grId = (data["id"] as Number).toLong()
        println("  Created GR (id=$grId) referencing PO $poDocNo")
    }

    @Test
    @Order(9)
    fun `step 9 - confirm GR and verify stock increased`() {
        println("=== P2P E2E: Step 9 - Confirm GR and verify stock ===")

        val confirmResp = restTemplate.exchange(
            "/api/v1/logistics/goods-receipts/$grId/confirm", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(confirmResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(confirmResp)["status"]).isEqualTo("CONFIRMED")
        println("  GR confirmed")

        // Verify stock for item 1
        val stockResp1 = restTemplate.exchange(
            "/api/v1/logistics/stock?itemCode=MAT-P2P-001&plantCode=$PLANT",
            HttpMethod.GET, HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(stockResp1.statusCode).isEqualTo(HttpStatus.OK)

        @Suppress("UNCHECKED_CAST")
        val stockData1 = stockResp1.body!!["data"] as List<Map<String, Any>>
        val stock1 = stockData1.find { it["itemCode"] == "MAT-P2P-001" }
        assertThat(stock1).isNotNull
        val onHand1 = BigDecimal(stock1!!["quantityOnHand"].toString())
        assertThat(onHand1.compareTo(BigDecimal(500))).isEqualTo(0)
        println("  Stock MAT-P2P-001: $onHand1 KG on hand")

        // Verify stock for item 2
        val stockResp2 = restTemplate.exchange(
            "/api/v1/logistics/stock?itemCode=MAT-P2P-002&plantCode=$PLANT",
            HttpMethod.GET, HttpEntity<Any>(authHeaders()), Map::class.java
        )
        @Suppress("UNCHECKED_CAST")
        val stockData2 = stockResp2.body!!["data"] as List<Map<String, Any>>
        val stock2 = stockData2.find { it["itemCode"] == "MAT-P2P-002" }
        assertThat(stock2).isNotNull
        val onHand2 = BigDecimal(stock2!!["quantityOnHand"].toString())
        assertThat(onHand2.compareTo(BigDecimal(1000))).isEqualTo(0)
        println("  Stock MAT-P2P-002: $onHand2 M on hand")
    }

    @Test
    @Order(10)
    fun `step 10 - create and post journal entry for AP`() {
        println("=== P2P E2E: Step 10 - Create and post AP journal entry ===")
        // Total: Steel 500*2500 = 1,250,000 + Copper 1000*800 = 800,000 = 2,050,000 KRW
        val jeRequest = mapOf(
            "companyCode" to COMPANY,
            "entryType" to "MANUAL",
            "referenceDocNo" to poDocNo,
            "referenceDocType" to "PO",
            "description" to "AP posting for purchase order $poDocNo",
            "currencyCode" to "KRW",
            "lines" to listOf(
                mapOf(
                    "accountCode" to "1400",
                    "accountName" to "Raw Materials Inventory",
                    "debitAmount" to 2050000,
                    "creditAmount" to 0,
                    "description" to "Materials received"
                ),
                mapOf(
                    "accountCode" to "2100",
                    "accountName" to "Accounts Payable",
                    "debitAmount" to 0,
                    "creditAmount" to 2050000,
                    "description" to "Payable to Korea Steel Supply Co."
                )
            )
        )

        val createResp = restTemplate.exchange(
            "/api/v1/account/journal-entries", HttpMethod.POST,
            HttpEntity(jeRequest, authHeaders()), Map::class.java
        )
        assertThat(createResp.statusCode).isEqualTo(HttpStatus.CREATED)
        val jeData = extractData(createResp)
        assertThat(jeData["status"]).isEqualTo("DRAFT")
        assertThat(jeData["isBalanced"]).isEqualTo(true)
        jeId = (jeData["id"] as Number).toLong()
        println("  Created JE (id=$jeId), balanced: ${jeData["isBalanced"]}")

        // Post the journal entry
        val postResp = restTemplate.exchange(
            "/api/v1/account/journal-entries/$jeId/post", HttpMethod.POST,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(postResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(postResp)["status"]).isEqualTo("POSTED")
        println("  JE posted successfully")
    }

    @Test
    @Order(11)
    fun `step 11 - verify complete P2P audit trail`() {
        println("=== P2P E2E: Step 11 - Verify complete audit trail ===")

        // Verify PR is APPROVED
        val prResp = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(prResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(prResp)["status"]).isEqualTo("APPROVED")
        println("  PR $prDocNo: APPROVED")

        // Verify PO is APPROVED
        val poResp = restTemplate.exchange(
            "/api/v1/purchase/orders/$poId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(poResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(poResp)["status"]).isEqualTo("APPROVED")
        println("  PO $poDocNo: APPROVED")

        // Verify GR is CONFIRMED
        val grResp = restTemplate.exchange(
            "/api/v1/logistics/goods-receipts/$grId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(grResp.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(extractData(grResp)["status"]).isEqualTo("CONFIRMED")
        println("  GR: CONFIRMED")

        // Verify JE is POSTED
        val jeResp = restTemplate.exchange(
            "/api/v1/account/journal-entries/$jeId", HttpMethod.GET,
            HttpEntity<Any>(authHeaders()), Map::class.java
        )
        assertThat(jeResp.statusCode).isEqualTo(HttpStatus.OK)
        val jeData = extractData(jeResp)
        assertThat(jeData["status"]).isEqualTo("POSTED")
        assertThat(jeData["referenceDocNo"]).isEqualTo(poDocNo)
        println("  JE: POSTED, referencing $poDocNo")

        println("=== P2P E2E: COMPLETE - All steps verified ===")
    }
}
