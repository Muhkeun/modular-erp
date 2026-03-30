package com.modularerp.security

import com.modularerp.admin.domain.DataScope
import com.modularerp.admin.domain.DataScopeType
import com.modularerp.admin.domain.OrgType
import com.modularerp.admin.domain.Organization
import com.modularerp.admin.repository.DataScopeRepository
import com.modularerp.admin.repository.OrganizationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [com.modularerp.app.ModularErpApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataScopeEnforcementTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var dataScopeRepository: DataScopeRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Test
    fun `organization scope expands through tree and guards purchase request create`() {
        val tenantId = "DS_PR_SCOPE"
        val token = registerAndLogin(tenantId, "scope-pr-user")

        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C100",
            plantCode = "P100",
            departmentCode = "D100"
        )
        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C200",
            plantCode = "P200",
            departmentCode = "D200"
        )

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "purchase-requests",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C100"
            ).apply { assignTenant(tenantId) }
        )

        val allowedRequest = mapOf(
            "companyCode" to "C100",
            "plantCode" to "P100",
            "departmentCode" to "D100",
            "prType" to "STANDARD",
            "description" to "Allowed by organization scope",
            "lines" to listOf(
                mapOf(
                    "itemCode" to "MAT-ORG-001",
                    "itemName" to "Scoped Material",
                    "quantity" to 10,
                    "unitOfMeasure" to "EA",
                    "unitPrice" to 1000
                )
            )
        )

        val allowedResponse = restTemplate.exchange(
            "/api/v1/purchase/requests",
            HttpMethod.POST,
            HttpEntity(allowedRequest, authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(allowedResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val blockedRequest = mapOf(
            "companyCode" to "C200",
            "plantCode" to "P200",
            "departmentCode" to "D200",
            "prType" to "STANDARD",
            "description" to "Blocked by organization scope",
            "lines" to listOf(
                mapOf(
                    "itemCode" to "MAT-ORG-002",
                    "itemName" to "Blocked Material",
                    "quantity" to 10,
                    "unitOfMeasure" to "EA",
                    "unitPrice" to 1000
                )
            )
        )

        val blockedResponse = restTemplate.exchange(
            "/api/v1/purchase/requests",
            HttpMethod.POST,
            HttpEntity(blockedRequest, authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(blockedResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `detail and action endpoints return forbidden when plant scope no longer matches`() {
        val tenantId = "DS_WO_SCOPE"
        val token = registerAndLogin(tenantId, "scope-wo-user")

        val createRequest = mapOf(
            "companyCode" to "C100",
            "plantCode" to "P100",
            "productCode" to "PROD-SCOPE-001",
            "productName" to "Scoped Product",
            "plannedQuantity" to 5,
            "unitOfMeasure" to "EA",
            "orderType" to "STANDARD",
            "priority" to "NORMAL",
            "autoPopulate" to false
        )

        val createResponse = restTemplate.exchange(
            "/api/v1/production/work-orders",
            HttpMethod.POST,
            HttpEntity(createRequest, authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val workOrderId = (created["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "work-orders",
                scopeType = DataScopeType.PLANT,
                scopeValues = "P999"
            ).apply { assignTenant(tenantId) }
        )

        val detailResponse = restTemplate.exchange(
            "/api/v1/production/work-orders/$workOrderId",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(detailResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        val releaseResponse = restTemplate.exchange(
            "/api/v1/production/work-orders/$workOrderId/release",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(releaseResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `purchase request delete returns forbidden when department scope no longer matches`() {
        val tenantId = "DS_PR_DELETE_SCOPE"
        val token = registerAndLogin(tenantId, "scope-pr-delete-user")

        val createRequest = mapOf(
            "companyCode" to "C100",
            "plantCode" to "P100",
            "departmentCode" to "D100",
            "prType" to "STANDARD",
            "description" to "Delete guard scope test",
            "lines" to listOf(
                mapOf(
                    "itemCode" to "MAT-DELETE-001",
                    "itemName" to "Delete Guard Material",
                    "quantity" to 3,
                    "unitOfMeasure" to "EA",
                    "unitPrice" to 1000
                )
            )
        )

        val createResponse = restTemplate.exchange(
            "/api/v1/purchase/requests",
            HttpMethod.POST,
            HttpEntity(createRequest, authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val prId = (created["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "purchase-requests",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D999"
            ).apply { assignTenant(tenantId) }
        )

        val deleteResponse = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId",
            HttpMethod.DELETE,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `journal entry search is filtered by company scope`() {
        val tenantId = "DS_JE_SEARCH_SCOPE"
        val token = registerAndLogin(tenantId, "scope-je-search-user")

        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C100",
            plantCode = "P100",
            departmentCode = "D100"
        )
        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C200",
            plantCode = "P200",
            departmentCode = "D200"
        )

        assertThat(createJournalEntry(token, tenantId, "C100").statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createJournalEntry(token, tenantId, "C200").statusCode).isEqualTo(HttpStatus.CREATED)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "journal-entries",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C100"
            ).apply { assignTenant(tenantId) }
        )

        val searchResponse = restTemplate.exchange(
            "/api/v1/account/journal-entries?page=0&size=20",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(searchResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val searchData = searchResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(searchData).hasSize(1)
        assertThat(searchData).extracting<String> { it["companyCode"] as String }.containsExactly("C100")
    }

    @Test
    fun `journal entry detail and post return forbidden when company scope no longer matches`() {
        val tenantId = "DS_JE_DETAIL_SCOPE"
        val token = registerAndLogin(tenantId, "scope-je-detail-user")

        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C100",
            plantCode = "P100",
            departmentCode = "D100"
        )
        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C999",
            plantCode = "P999",
            departmentCode = "D999"
        )

        val createResponse = createJournalEntry(token, tenantId, "C100")
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val journalEntryId = (created["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "journal-entries",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C999"
            ).apply { assignTenant(tenantId) }
        )

        val detailResponse = restTemplate.exchange(
            "/api/v1/account/journal-entries/$journalEntryId",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(detailResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        val postResponse = restTemplate.exchange(
            "/api/v1/account/journal-entries/$journalEntryId/post",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `period search is filtered by company scope`() {
        val tenantId = "DS_PERIOD_SEARCH_SCOPE"
        val token = registerAndLogin(tenantId, "scope-period-search-user")

        createOrganizationTree(tenantId, "C100", "P100", "D100")
        createOrganizationTree(tenantId, "C200", "P200", "D200")

        assertThat(generateFiscalYear(token, tenantId, "C100", 2029).statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(generateFiscalYear(token, tenantId, "C200", 2029).statusCode).isEqualTo(HttpStatus.CREATED)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "period-close",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C100"
            ).apply { assignTenant(tenantId) }
        )

        val searchResponse = restTemplate.exchange(
            "/api/v1/period-close/periods?fiscalYear=2029&size=50",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(searchResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val searchData = searchResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(searchData).hasSize(12)
        assertThat(searchData).extracting<String> { it["companyCode"] as String }.containsOnly("C100")
    }

    @Test
    fun `period detail and closing entry create return forbidden when company scope no longer matches`() {
        val tenantId = "DS_PERIOD_DETAIL_SCOPE"
        val token = registerAndLogin(tenantId, "scope-period-detail-user")

        createOrganizationTree(tenantId, "C100", "P100", "D100")
        createOrganizationTree(tenantId, "C999", "P999", "D999")

        val allowedGenerateResponse = generateFiscalYear(token, tenantId, "C100", 2030)
        val blockedGenerateResponse = generateFiscalYear(token, tenantId, "C999", 2030)
        assertThat(allowedGenerateResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(blockedGenerateResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        @Suppress("UNCHECKED_CAST")
        val allowedPeriods = allowedGenerateResponse.body!!["data"] as List<Map<String, Any>>
        @Suppress("UNCHECKED_CAST")
        val blockedPeriods = blockedGenerateResponse.body!!["data"] as List<Map<String, Any>>
        val allowedPeriodId = (allowedPeriods.first()["id"] as Number).toLong()
        val blockedPeriodId = (blockedPeriods.first()["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "period-close",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C100"
            ).apply { assignTenant(tenantId) }
        )

        val allowedDetailResponse = restTemplate.exchange(
            "/api/v1/period-close/periods/$allowedPeriodId",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(allowedDetailResponse.statusCode).isEqualTo(HttpStatus.OK)

        val blockedDetailResponse = restTemplate.exchange(
            "/api/v1/period-close/periods/$blockedPeriodId",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(blockedDetailResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        val blockedEntryResponse = restTemplate.exchange(
            "/api/v1/period-close/closing-entries",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "fiscalPeriodId" to blockedPeriodId,
                    "entryType" to "CLOSING",
                    "description" to "Blocked close entry",
                    "debitAccount" to "999001",
                    "creditAccount" to "999002",
                    "amount" to 1000
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )
        assertThat(blockedEntryResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `currency revaluation search is filtered by company scope`() {
        val tenantId = "DS_REVAL_SEARCH_SCOPE"
        val token = registerAndLogin(tenantId, "scope-reval-search-user")

        createOrganizationTree(tenantId, "C100", "P100", "D100")
        createOrganizationTree(tenantId, "C200", "P200", "D200")

        assertThat(createCurrencyRevaluation(token, tenantId, "C100", 2031).statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createCurrencyRevaluation(token, tenantId, "C200", 2031).statusCode).isEqualTo(HttpStatus.CREATED)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "currency-revaluations",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C100"
            ).apply { assignTenant(tenantId) }
        )

        val searchResponse = restTemplate.exchange(
            "/api/v1/currencies/revaluations?fiscalYear=2031&size=50",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(searchResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val searchData = searchResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(searchData).hasSize(1)
        assertThat(searchData).extracting<String> { it["companyCode"] as String }.containsExactly("C100")
    }

    @Test
    fun `currency revaluation post returns forbidden when company scope no longer matches`() {
        val tenantId = "DS_REVAL_POST_SCOPE"
        val token = registerAndLogin(tenantId, "scope-reval-post-user")

        createOrganizationTree(tenantId, "C100", "P100", "D100")
        createOrganizationTree(tenantId, "C999", "P999", "D999")

        val createResponse = createCurrencyRevaluation(token, tenantId, "C100", 2032)
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val revaluationId = (created["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "currency-revaluations",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C999"
            ).apply { assignTenant(tenantId) }
        )

        val postResponse = restTemplate.exchange(
            "/api/v1/currencies/revaluations/$revaluationId/post",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `batch job search is filtered by company scope`() {
        val tenantId = "DS_BATCH_SEARCH_SCOPE"
        val token = registerAndLogin(tenantId, "scope-batch-search-user")

        createOrganizationTree(tenantId, "C100", "P100", "D100")
        createOrganizationTree(tenantId, "C200", "P200", "D200")

        assertThat(createBatchJob(token, tenantId, "GL-C100", "C100", "P100", "D100").statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createBatchJob(token, tenantId, "GL-C200", "C200", "P200", "D200").statusCode).isEqualTo(HttpStatus.CREATED)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "batch-jobs",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C100"
            ).apply { assignTenant(tenantId) }
        )

        val searchResponse = restTemplate.exchange(
            "/api/v1/batch/jobs?size=50",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(searchResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val searchData = searchResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(searchData).hasSize(1)
        assertThat(searchData).extracting<String> { it["companyCode"] as String }.containsExactly("C100")
    }

    @Test
    fun `batch job search keeps company-scoped jobs visible to organization scope`() {
        val tenantId = "DS_BATCH_COMPANY_ONLY_SCOPE"
        val token = registerAndLogin(tenantId, "scope-batch-company-only-user")

        createOrganizationTree(tenantId, "C100", "P100", "D100")
        createOrganizationTree(tenantId, "C200", "P200", "D200")

        assertThat(createBatchJob(token, tenantId, "GL-C100-COMPANY", "C100").statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createBatchJob(token, tenantId, "GL-C200-COMPANY", "C200").statusCode).isEqualTo(HttpStatus.CREATED)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "batch-jobs",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C100"
            ).apply { assignTenant(tenantId) }
        )

        val searchResponse = restTemplate.exchange(
            "/api/v1/batch/jobs?size=50",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(searchResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val searchData = searchResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(searchData).hasSize(1)
        assertThat(searchData.first()["jobCode"]).isEqualTo("GL-C100-COMPANY")
    }

    @Test
    fun `batch job execute returns forbidden when company scope no longer matches`() {
        val tenantId = "DS_BATCH_EXEC_SCOPE"
        val token = registerAndLogin(tenantId, "scope-batch-exec-user")

        createOrganizationTree(tenantId, "C100", "P100", "D100")
        createOrganizationTree(tenantId, "C999", "P999", "D999")

        val createResponse = createBatchJob(token, tenantId, "GL-C100", "C100", "P100", "D100")
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val jobId = (created["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "batch-jobs",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C999"
            ).apply { assignTenant(tenantId) }
        )

        val executeResponse = restTemplate.exchange(
            "/api/v1/batch/jobs/$jobId/execute",
            HttpMethod.POST,
            HttpEntity(mapOf("parameters" to """{"source":"blocked"}"""), authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(executeResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `create from PR is forbidden when purchase request scope no longer allows source document`() {
        val tenantId = "DS_PO_FROM_PR_SCOPE"
        val token = registerAndLogin(tenantId, "scope-po-from-pr-user")

        val createPrRequest = mapOf(
            "companyCode" to "C100",
            "plantCode" to "P100",
            "departmentCode" to "D100",
            "prType" to "STANDARD",
            "description" to "PR for PO scope bridge test",
            "lines" to listOf(
                mapOf(
                    "itemCode" to "MAT-BRIDGE-001",
                    "itemName" to "Bridge Material",
                    "quantity" to 7,
                    "unitOfMeasure" to "EA",
                    "unitPrice" to 1500
                )
            )
        )

        val createPrResponse = restTemplate.exchange(
            "/api/v1/purchase/requests",
            HttpMethod.POST,
            HttpEntity(createPrRequest, authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(createPrResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val createdPr = createPrResponse.body!!["data"] as Map<String, Any>
        val prId = (createdPr["id"] as Number).toLong()

        val submitResponse = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId/submit",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(submitResponse.statusCode).isEqualTo(HttpStatus.OK)

        val approveResponse = restTemplate.exchange(
            "/api/v1/purchase/requests/$prId/approve",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(approveResponse.statusCode).isEqualTo(HttpStatus.OK)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "purchase-requests",
                scopeType = DataScopeType.PLANT,
                scopeValues = "P999"
            ).apply { assignTenant(tenantId) }
        )

        val createPoResponse = restTemplate.exchange(
            "/api/v1/purchase/orders/from-pr/$prId",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "vendorCode" to "V100",
                    "vendorName" to "Scoped Vendor",
                    "currencyCode" to "KRW",
                    "paymentTerms" to "NET30"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(createPoResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `organization scope expands to descendant plants for work center search and create`() {
        val tenantId = "DS_WORK_CENTER_SCOPE"
        val token = registerAndLogin(tenantId, "scope-work-center-user")

        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C100",
            plantCode = "P100",
            departmentCode = "D100"
        )
        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C200",
            plantCode = "P200",
            departmentCode = "D200"
        )

        createWorkCenter(token, tenantId, "WC-100", "P100")
        createWorkCenter(token, tenantId, "WC-200", "P200")

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "work-centers",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C100"
            ).apply { assignTenant(tenantId) }
        )

        val searchResponse = restTemplate.exchange(
            "/api/v1/production/work-centers",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(searchResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val searchData = searchResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(searchData).extracting<String> { it["plantCode"] as String }.containsExactly("P100")

        val allowedCreateResponse = createWorkCenter(token, tenantId, "WC-101", "P100")
        assertThat(allowedCreateResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val blockedCreateResponse = createWorkCenter(token, tenantId, "WC-201", "P200")
        assertThat(blockedCreateResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `organization scope expands to descendant plants for bom search and create`() {
        val tenantId = "DS_BOM_SCOPE"
        val token = registerAndLogin(tenantId, "scope-bom-user")

        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C100",
            plantCode = "P100",
            departmentCode = "D100"
        )
        createOrganizationTree(
            tenantId = tenantId,
            companyCode = "C200",
            plantCode = "P200",
            departmentCode = "D200"
        )

        assertThat(createBom(token, tenantId, "FG-BOM-100", "P100").statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createBom(token, tenantId, "FG-BOM-200", "P200").statusCode).isEqualTo(HttpStatus.CREATED)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "boms",
                scopeType = DataScopeType.ORGANIZATION,
                scopeValues = "C100"
            ).apply { assignTenant(tenantId) }
        )

        val searchResponse = restTemplate.exchange(
            "/api/v1/master-data/boms",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(searchResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val searchData = searchResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(searchData).extracting<String> { it["plantCode"] as String }.containsExactly("P100")

        val allowedCreateResponse = createBom(token, tenantId, "FG-BOM-101", "P100")
        assertThat(allowedCreateResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val blockedCreateResponse = createBom(token, tenantId, "FG-BOM-201", "P200")
        assertThat(blockedCreateResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `bom detail and explode return forbidden when plant scope no longer matches`() {
        val tenantId = "DS_BOM_DETAIL_SCOPE"
        val token = registerAndLogin(tenantId, "scope-bom-detail-user")

        val createResponse = createBom(token, tenantId, "FG-BOM-300", "P100")
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val bomId = (created["id"] as Number).toLong()

        val releaseResponse = restTemplate.exchange(
            "/api/v1/master-data/boms/$bomId/release",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(releaseResponse.statusCode).isEqualTo(HttpStatus.OK)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "boms",
                scopeType = DataScopeType.PLANT,
                scopeValues = "P999"
            ).apply { assignTenant(tenantId) }
        )

        val detailResponse = restTemplate.exchange(
            "/api/v1/master-data/boms/$bomId",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(detailResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        val explodeResponse = restTemplate.exchange(
            "/api/v1/master-data/boms/explode?productCode=FG-BOM-300&plantCode=P100&quantity=1",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(explodeResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `routing release returns forbidden when plant scope no longer matches`() {
        val tenantId = "DS_ROUTING_SCOPE"
        val token = registerAndLogin(tenantId, "scope-routing-user")

        val createResponse = restTemplate.exchange(
            "/api/v1/production/routings",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "productCode" to "FG-ROUTING-001",
                    "productName" to "Scoped Routing Product",
                    "plantCode" to "P100",
                    "revision" to "001",
                    "operations" to listOf(
                        mapOf(
                            "operationNo" to 10,
                            "operationName" to "Assembly",
                            "workCenterCode" to "WC-ROUTING-100",
                            "runTimePerUnit" to 1.5
                        )
                    )
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val routingId = (created["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "routings",
                scopeType = DataScopeType.PLANT,
                scopeValues = "P999"
            ).apply { assignTenant(tenantId) }
        )

        val releaseResponse = restTemplate.exchange(
            "/api/v1/production/routings/$routingId/release",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(releaseResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `budget item update returns forbidden when department scope no longer matches`() {
        val tenantId = "DS_BUDGET_ITEM_UPDATE_SCOPE"
        val token = registerAndLogin(tenantId, "scope-budget-update-user")

        val periodId = createBudgetPeriod(token, tenantId, 2027)
        val itemId = createBudgetItem(token, tenantId, periodId, "7100", "D100", null)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "budget-items",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D999"
            ).apply { assignTenant(tenantId) }
        )

        val updateResponse = restTemplate.exchange(
            "/api/v1/budgets/items/$itemId",
            HttpMethod.PUT,
            HttpEntity(
                mapOf(
                    "accountName" to "Updated Budget Name",
                    "budgetAmount" to 2000000
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `budget transfer returns forbidden when target item is outside plant scope`() {
        val tenantId = "DS_BUDGET_TRANSFER_SCOPE"
        val token = registerAndLogin(tenantId, "scope-budget-transfer-user")

        val periodId = createBudgetPeriod(token, tenantId, 2028)
        val fromItemId = createBudgetItem(token, tenantId, periodId, "7200", null, "P100")
        val toItemId = createBudgetItem(token, tenantId, periodId, "7300", null, "P200")

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "budget-items",
                scopeType = DataScopeType.PLANT,
                scopeValues = "P100"
            ).apply { assignTenant(tenantId) }
        )

        val transferResponse = restTemplate.exchange(
            "/api/v1/budgets/transfers",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "fromBudgetItemId" to fromItemId,
                    "toBudgetItemId" to toItemId,
                    "amount" to 100000,
                    "reason" to "Blocked by plant scope"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(transferResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `employee create returns forbidden when department scope does not allow payload`() {
        val tenantId = "DS_EMPLOYEE_SCOPE"
        val token = registerAndLogin(tenantId, "scope-employee-user")

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "employees",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D100"
            ).apply { assignTenant(tenantId) }
        )

        val allowedResponse = restTemplate.exchange(
            "/api/v1/hr/employees",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "employeeNo" to "E100",
                    "name" to "Scoped Employee",
                    "companyCode" to "C100",
                    "departmentCode" to "D100"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )
        assertThat(allowedResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val blockedResponse = restTemplate.exchange(
            "/api/v1/hr/employees",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "employeeNo" to "E200",
                    "name" to "Blocked Employee",
                    "companyCode" to "C100",
                    "departmentCode" to "D200"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )
        assertThat(blockedResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `cost center delete returns forbidden when department scope no longer matches`() {
        val tenantId = "DS_COST_CENTER_SCOPE"
        val token = registerAndLogin(tenantId, "scope-cost-center-user")

        val createResponse = restTemplate.exchange(
            "/api/v1/costing/cost-centers",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "costCenterCode" to "CC-100",
                    "costCenterName" to "Scoped Cost Center",
                    "departmentCode" to "D100",
                    "status" to "ACTIVE"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val costCenterId = (created["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "cost-centers",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D999"
            ).apply { assignTenant(tenantId) }
        )

        val deleteResponse = restTemplate.exchange(
            "/api/v1/costing/cost-centers/$costCenterId",
            HttpMethod.DELETE,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `standard and product cost searches are filtered by department scope`() {
        val tenantId = "DS_COSTING_SCOPE_SEARCH"
        val token = registerAndLogin(tenantId, "scope-costing-search-user")

        assertThat(createCostCenter(token, tenantId, "CC100", "D100").statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createCostCenter(token, tenantId, "CC200", "D200").statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createStandardCost(token, tenantId, "ITEM-COST-100", "CC100").statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createStandardCost(token, tenantId, "ITEM-COST-200", "CC200").statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(calculateProductCost(token, tenantId, "ITEM-COST-100", "CC100", 2026, 3).statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(calculateProductCost(token, tenantId, "ITEM-COST-200", "CC200", 2026, 3).statusCode).isEqualTo(HttpStatus.CREATED)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "standard-costs",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D100"
            ).apply { assignTenant(tenantId) }
        )
        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "product-costs",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D100"
            ).apply { assignTenant(tenantId) }
        )

        val standardSearchResponse = restTemplate.exchange(
            "/api/v1/costing/standard-costs",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(standardSearchResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val standardData = standardSearchResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(standardData).hasSize(1)
        assertThat(standardData).extracting<String> { it["costCenterCode"] as String }.containsExactly("CC100")

        val productSearchResponse = restTemplate.exchange(
            "/api/v1/costing/product-costs",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(productSearchResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val productData = productSearchResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(productData).hasSize(1)
        assertThat(productData).extracting<String> { it["costCenterCode"] as String }.containsExactly("CC100")
    }

    @Test
    fun `standard cost update and product cost calculate return forbidden when department scope no longer matches`() {
        val tenantId = "DS_COSTING_SCOPE_WRITE"
        val token = registerAndLogin(tenantId, "scope-costing-write-user")

        assertThat(createCostCenter(token, tenantId, "CC300", "D100").statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createCostCenter(token, tenantId, "CC399", "D999").statusCode).isEqualTo(HttpStatus.CREATED)

        val standardCostResponse = createStandardCost(token, tenantId, "ITEM-COST-300", "CC300")
        assertThat(standardCostResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val standardCostData = standardCostResponse.body!!["data"] as Map<String, Any>
        val standardCostId = (standardCostData["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "standard-costs",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D999"
            ).apply { assignTenant(tenantId) }
        )
        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "product-costs",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D999"
            ).apply { assignTenant(tenantId) }
        )

        val updateResponse = restTemplate.exchange(
            "/api/v1/costing/standard-costs/$standardCostId",
            HttpMethod.PUT,
            HttpEntity(
                mapOf(
                    "itemCode" to "ITEM-COST-300",
                    "costCenterCode" to "CC300",
                    "costType" to "MATERIAL",
                    "standardRate" to 1500,
                    "effectiveFrom" to "2026-03-01",
                    "currency" to "KRW"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )
        assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        val calculateResponse = restTemplate.exchange(
            "/api/v1/costing/product-costs/calculate",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "itemCode" to "ITEM-COST-300",
                    "costCenterCode" to "CC300",
                    "fiscalYear" to 2026,
                    "period" to 3,
                    "quantity" to 1
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )
        assertThat(calculateResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `asset register returns forbidden when department scope does not allow payload`() {
        val tenantId = "DS_ASSET_SCOPE"
        val token = registerAndLogin(tenantId, "scope-asset-user")

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "assets",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D100"
            ).apply { assignTenant(tenantId) }
        )

        val allowedResponse = createAsset(token, tenantId, "Allowed Asset", "D100")
        assertThat(allowedResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val blockedResponse = createAsset(token, tenantId, "Blocked Asset", "D200")
        assertThat(blockedResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `asset dispose returns forbidden when department scope no longer matches`() {
        val tenantId = "DS_ASSET_DISPOSE_SCOPE"
        val token = registerAndLogin(tenantId, "scope-asset-dispose-user")

        val createResponse = createAsset(token, tenantId, "Disposable Asset", "D100")
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val assetId = (created["id"] as Number).toLong()

        val activateResponse = restTemplate.exchange(
            "/api/v1/assets/$assetId/activate",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(activateResponse.statusCode).isEqualTo(HttpStatus.OK)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "assets",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D999"
            ).apply { assignTenant(tenantId) }
        )

        val disposeResponse = restTemplate.exchange(
            "/api/v1/assets/$assetId/dispose",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "disposalDate" to "2026-12-31",
                    "disposalType" to "SALE",
                    "disposalAmount" to 100000,
                    "reason" to "Blocked by asset scope"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(disposeResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `quality inspection complete returns forbidden when plant scope no longer matches`() {
        val tenantId = "DS_QUALITY_SCOPE"
        val token = registerAndLogin(tenantId, "scope-quality-user")

        val createResponse = createQualityInspection(token, tenantId, "P100")
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val inspectionId = (created["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "quality-inspections",
                scopeType = DataScopeType.PLANT,
                scopeValues = "P999"
            ).apply { assignTenant(tenantId) }
        )

        val completeResponse = restTemplate.exchange(
            "/api/v1/quality/inspections/$inspectionId/complete",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "acceptedQuantity" to 8,
                    "rejectedQuantity" to 2,
                    "result" to "FAIL",
                    "remarks" to "Blocked by quality scope"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(completeResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `mrp run returns forbidden when plant scope does not allow payload`() {
        val tenantId = "DS_MRP_SCOPE"
        val token = registerAndLogin(tenantId, "scope-mrp-user")

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "mrp-runs",
                scopeType = DataScopeType.PLANT,
                scopeValues = "P100"
            ).apply { assignTenant(tenantId) }
        )

        val allowedResponse = restTemplate.exchange(
            "/api/v1/planning/mrp/run",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "plantCode" to "P100",
                    "planningHorizonDays" to 14
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )
        assertThat(allowedResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val blockedResponse = restTemplate.exchange(
            "/api/v1/planning/mrp/run",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "plantCode" to "P200",
                    "planningHorizonDays" to 14
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )
        assertThat(blockedResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `crm customer create returns forbidden when own scope payload is assigned to another user`() {
        val tenantId = "DS_CRM_CUSTOMER_SCOPE"
        val loginId = "scope-crm-customer-user"
        val token = registerAndLogin(tenantId, loginId)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "crm-customers",
                scopeType = DataScopeType.OWN,
                scopeValues = null
            ).apply { assignTenant(tenantId) }
        )

        val allowedResponse = restTemplate.exchange(
            "/api/v1/crm/customers",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "customerCode" to "CUST-OWN-001",
                    "customerName" to "Scoped Customer",
                    "customerType" to "CORPORATE",
                    "assignedTo" to loginId
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )
        assertThat(allowedResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        val blockedResponse = restTemplate.exchange(
            "/api/v1/crm/customers",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "customerCode" to "CUST-OWN-002",
                    "customerName" to "Blocked Customer",
                    "customerType" to "CORPORATE",
                    "assignedTo" to "someone-else"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(blockedResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `crm opportunity stage update returns forbidden when own scope no longer matches`() {
        val tenantId = "DS_CRM_OPPORTUNITY_SCOPE"
        val loginId = "scope-crm-opp-user"
        val token = registerAndLogin(tenantId, loginId)

        val customerId = createCustomer(token, tenantId, "CUST-CRM-SCOPE", "other-user")
        val opportunityId = createOpportunity(token, tenantId, customerId, "other-user")

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "crm-opportunities",
                scopeType = DataScopeType.OWN,
                scopeValues = null
            ).apply { assignTenant(tenantId) }
        )

        val updateResponse = restTemplate.exchange(
            "/api/v1/crm/opportunities/$opportunityId/stage",
            HttpMethod.PUT,
            HttpEntity(
                mapOf(
                    "stage" to "NEGOTIATION"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(updateResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `cost allocation create returns forbidden when target cost center is outside department scope`() {
        val tenantId = "DS_COST_ALLOC_CREATE_SCOPE"
        val token = registerAndLogin(tenantId, "scope-cost-allocation-user")

        createCostCenter(token, tenantId, "CC-FROM-100", "D100")
        createCostCenter(token, tenantId, "CC-TO-200", "D200")

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "cost-centers",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D100"
            ).apply { assignTenant(tenantId) }
        )

        val createResponse = restTemplate.exchange(
            "/api/v1/costing/allocations",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "allocationDate" to "2026-03-25",
                    "fromCostCenter" to "CC-FROM-100",
                    "toCostCenter" to "CC-TO-200",
                    "allocationType" to "DIRECT",
                    "amount" to 100000,
                    "fiscalYear" to 2026,
                    "period" to 3
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `cost allocation post returns forbidden when department scope no longer matches linked cost centers`() {
        val tenantId = "DS_COST_ALLOC_POST_SCOPE"
        val token = registerAndLogin(tenantId, "scope-cost-allocation-post-user")

        createCostCenter(token, tenantId, "CC-FROM-300", "D300")
        createCostCenter(token, tenantId, "CC-TO-300", "D300")

        val createResponse = restTemplate.exchange(
            "/api/v1/costing/allocations",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "allocationDate" to "2026-03-25",
                    "fromCostCenter" to "CC-FROM-300",
                    "toCostCenter" to "CC-TO-300",
                    "allocationType" to "DIRECT",
                    "amount" to 150000,
                    "fiscalYear" to 2026,
                    "period" to 3
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val allocationId = (created["id"] as Number).toLong()

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "cost-centers",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D999"
            ).apply { assignTenant(tenantId) }
        )

        val postResponse = restTemplate.exchange(
            "/api/v1/costing/allocations/$allocationId/post",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(postResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `asset schedule returns forbidden when department scope no longer matches`() {
        val tenantId = "DS_ASSET_SCHEDULE_SCOPE"
        val token = registerAndLogin(tenantId, "scope-asset-schedule-user")

        val createResponse = createAsset(token, tenantId, "Scoped Schedule Asset", "D100")
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val created = createResponse.body!!["data"] as Map<String, Any>
        val assetId = (created["id"] as Number).toLong()

        val activateResponse = restTemplate.exchange(
            "/api/v1/assets/$assetId/activate",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )
        assertThat(activateResponse.statusCode).isEqualTo(HttpStatus.OK)

        val depreciationResponse = restTemplate.exchange(
            "/api/v1/assets/depreciation/run",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "year" to 2026,
                    "month" to 3
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )
        assertThat(depreciationResponse.statusCode).isEqualTo(HttpStatus.OK)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "assets",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D999"
            ).apply { assignTenant(tenantId) }
        )

        val scheduleResponse = restTemplate.exchange(
            "/api/v1/assets/$assetId/schedule",
            HttpMethod.GET,
            HttpEntity<Any>(authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(scheduleResponse.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `asset depreciation run returns only scoped department assets`() {
        val tenantId = "DS_ASSET_DEPRECIATION_SCOPE"
        val token = registerAndLogin(tenantId, "scope-asset-depreciation-user")

        val assetOneResponse = createAsset(token, tenantId, "Scoped Depreciation Asset 1", "D100")
        val assetTwoResponse = createAsset(token, tenantId, "Scoped Depreciation Asset 2", "D200")
        assertThat(assetOneResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(assetTwoResponse.statusCode).isEqualTo(HttpStatus.CREATED)

        @Suppress("UNCHECKED_CAST")
        val assetOne = assetOneResponse.body!!["data"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val assetTwo = assetTwoResponse.body!!["data"] as Map<String, Any>
        val assetOneId = (assetOne["id"] as Number).toLong()
        val assetTwoId = (assetTwo["id"] as Number).toLong()

        assertThat(
            restTemplate.exchange(
                "/api/v1/assets/$assetOneId/activate",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders(token, tenantId)),
                Map::class.java
            ).statusCode
        ).isEqualTo(HttpStatus.OK)
        assertThat(
            restTemplate.exchange(
                "/api/v1/assets/$assetTwoId/activate",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders(token, tenantId)),
                Map::class.java
            ).statusCode
        ).isEqualTo(HttpStatus.OK)

        dataScopeRepository.save(
            DataScope(
                roleCode = "USER",
                resource = "assets",
                scopeType = DataScopeType.DEPARTMENT,
                scopeValues = "D100"
            ).apply { assignTenant(tenantId) }
        )

        val depreciationResponse = restTemplate.exchange(
            "/api/v1/assets/depreciation/run",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "year" to 2026,
                    "month" to 4
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(depreciationResponse.statusCode).isEqualTo(HttpStatus.OK)
        @Suppress("UNCHECKED_CAST")
        val data = depreciationResponse.body!!["data"] as List<Map<String, Any>>
        assertThat(data).hasSize(1)
        assertThat((data.single()["assetId"] as Number).toLong()).isEqualTo(assetOneId)
    }

    private fun registerAndLogin(tenantId: String, loginId: String): String {
        val registerRequest = mapOf(
            "tenantId" to tenantId,
            "loginId" to loginId,
            "password" to "pass123",
            "name" to "Data Scope Tester"
        )
        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, Map::class.java)

        val loginRequest = mapOf(
            "tenantId" to tenantId,
            "loginId" to loginId,
            "password" to "pass123"
        )
        val loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginRequest, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        return (loginResponse.body!!["data"] as Map<String, Any>)["token"] as String
    }

    private fun authHeaders(token: String, tenantId: String) = HttpHeaders().apply {
        setBearerAuth(token)
        set("X-Tenant-Id", tenantId)
        contentType = MediaType.APPLICATION_JSON
    }

    private fun createOrganizationTree(
        tenantId: String,
        companyCode: String,
        plantCode: String,
        departmentCode: String
    ) {
        val company = organizationRepository.save(
            Organization(
                code = companyCode,
                name = "Company $companyCode",
                orgType = OrgType.COMPANY,
                sortOrder = 1
            ).apply { assignTenant(tenantId) }
        )

        val plant = organizationRepository.save(
            Organization(
                code = plantCode,
                name = "Plant $plantCode",
                orgType = OrgType.PLANT,
                parent = company,
                sortOrder = 1
            ).apply { assignTenant(tenantId) }
        )

        organizationRepository.save(
            Organization(
                code = departmentCode,
                name = "Department $departmentCode",
                orgType = OrgType.DEPARTMENT,
                parent = company,
                sortOrder = 2
            ).apply { assignTenant(tenantId) }
        )

        // Touch the plant variable to make the intent explicit: the organization scope should expand both company and plant descendants.
        assertThat(plant.code).isEqualTo(plantCode)
    }

    private fun createWorkCenter(
        token: String,
        tenantId: String,
        code: String,
        plantCode: String
    ) = restTemplate.exchange(
        "/api/v1/production/work-centers",
        HttpMethod.POST,
        HttpEntity(
            mapOf(
                "code" to code,
                "name" to "Work Center $code",
                "plantCode" to plantCode,
                "centerType" to "MACHINE",
                "capacityPerDay" to 8,
                "resourceCount" to 1,
                "costPerHour" to 0,
                "setupCost" to 0
            ),
            authHeaders(token, tenantId)
        ),
        Map::class.java
        )

    private fun createBom(token: String, tenantId: String, productCode: String, plantCode: String) =
        restTemplate.exchange(
            "/api/v1/master-data/boms",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "productCode" to productCode,
                    "productName" to "BOM Product $productCode",
                    "plantCode" to plantCode,
                    "revision" to "001",
                    "baseQuantity" to 1,
                    "baseUnit" to "EA",
                    "components" to listOf(
                        mapOf(
                            "itemCode" to "COMP-$productCode",
                            "itemName" to "Component $productCode",
                            "quantity" to 2,
                            "unitOfMeasure" to "EA",
                            "scrapRate" to 0,
                            "phantom" to false
                        )
                    )
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

    private fun createStandardCost(token: String, tenantId: String, itemCode: String, costCenterCode: String) =
        restTemplate.exchange(
            "/api/v1/costing/standard-costs",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "itemCode" to itemCode,
                    "costCenterCode" to costCenterCode,
                    "costType" to "MATERIAL",
                    "standardRate" to 1000,
                    "effectiveFrom" to "2026-03-01",
                    "currency" to "KRW"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

    private fun calculateProductCost(
        token: String,
        tenantId: String,
        itemCode: String,
        costCenterCode: String,
        fiscalYear: Int,
        period: Int
    ) = restTemplate.exchange(
        "/api/v1/costing/product-costs/calculate",
        HttpMethod.POST,
        HttpEntity(
            mapOf(
                "itemCode" to itemCode,
                "costCenterCode" to costCenterCode,
                "fiscalYear" to fiscalYear,
                "period" to period,
                "quantity" to 1
            ),
            authHeaders(token, tenantId)
        ),
        Map::class.java
    )

    private fun createCostCenter(token: String, tenantId: String, code: String, departmentCode: String?) =
        restTemplate.exchange(
            "/api/v1/costing/cost-centers",
            HttpMethod.POST,
            HttpEntity(
                linkedMapOf<String, Any>(
                    "costCenterCode" to code,
                    "costCenterName" to "Cost Center $code"
                ).apply {
                    departmentCode?.let { put("departmentCode", it) }
                },
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

    private fun createBudgetPeriod(token: String, tenantId: String, fiscalYear: Int): Long {
        val response = restTemplate.exchange(
            "/api/v1/budgets/periods",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "fiscalYear" to fiscalYear,
                    "periodType" to "ANNUAL",
                    "startDate" to "$fiscalYear-01-01",
                    "endDate" to "$fiscalYear-12-31",
                    "description" to "Scoped budget period"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as Map<String, Any>
        return (data["id"] as Number).toLong()
    }

    private fun generateFiscalYear(token: String, tenantId: String, companyCode: String, fiscalYear: Int) =
        restTemplate.exchange(
            "/api/v1/period-close/periods/generate",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "companyCode" to companyCode,
                    "fiscalYear" to fiscalYear
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

    private fun createCurrencyRevaluation(token: String, tenantId: String, companyCode: String, fiscalYear: Int) =
        restTemplate.exchange(
            "/api/v1/currencies/revaluations",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "companyCode" to companyCode,
                    "fiscalYear" to fiscalYear,
                    "period" to 1,
                    "fromCurrency" to "USD",
                    "toCurrency" to "KRW",
                    "originalRate" to 1350,
                    "revaluationRate" to 1400,
                    "unrealizedGainLoss" to -50000
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

    private fun createBatchJob(
        token: String,
        tenantId: String,
        jobCode: String,
        companyCode: String,
        plantCode: String? = null,
        departmentCode: String? = null
    ) =
        restTemplate.exchange(
            "/api/v1/batch/jobs",
            HttpMethod.POST,
            HttpEntity(
                mutableMapOf<String, Any>(
                    "jobCode" to jobCode,
                    "jobName" to "Batch $jobCode",
                    "jobType" to "GL_POSTING",
                    "companyCode" to companyCode,
                    "cronExpression" to "0 0 2 * * ?"
                ).apply {
                    plantCode?.let { put("plantCode", it) }
                    departmentCode?.let { put("departmentCode", it) }
                },
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

    private fun createBudgetItem(
        token: String,
        tenantId: String,
        periodId: Long,
        accountCode: String,
        departmentCode: String?,
        plantCode: String?
    ): Long {
        val payload = linkedMapOf<String, Any>(
            "budgetPeriodId" to periodId,
            "accountCode" to accountCode,
            "accountName" to "Budget $accountCode",
            "budgetAmount" to 1000000
        )
        departmentCode?.let { payload["departmentCode"] = it }
        plantCode?.let { payload["plantCode"] = it }

        val response = restTemplate.exchange(
            "/api/v1/budgets/items",
            HttpMethod.POST,
            HttpEntity(payload, authHeaders(token, tenantId)),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as Map<String, Any>
        return (data["id"] as Number).toLong()
    }

    private fun createJournalEntry(token: String, tenantId: String, companyCode: String) =
        restTemplate.exchange(
            "/api/v1/account/journal-entries",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "companyCode" to companyCode,
                    "postingDate" to "2026-03-25",
                    "entryType" to "MANUAL",
                    "description" to "Scoped journal entry",
                    "currencyCode" to "KRW",
                    "lines" to listOf(
                        mapOf(
                            "accountCode" to "111100",
                            "accountName" to "Cash",
                            "debitAmount" to 1000,
                            "creditAmount" to 0
                        ),
                        mapOf(
                            "accountCode" to "411100",
                            "accountName" to "Revenue",
                            "debitAmount" to 0,
                            "creditAmount" to 1000
                        )
                    )
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

    private fun createAsset(token: String, tenantId: String, name: String, department: String?) =
        restTemplate.exchange(
            "/api/v1/assets",
            HttpMethod.POST,
            HttpEntity(
                linkedMapOf<String, Any>(
                    "name" to name,
                    "category" to "IT_EQUIPMENT",
                    "acquisitionDate" to "2026-01-10",
                    "acquisitionCost" to 1200000,
                    "usefulLifeMonths" to 36,
                    "depreciationMethod" to "STRAIGHT_LINE",
                    "salvageValue" to 0,
                    "currency" to "KRW"
                ).apply {
                    department?.let { put("department", it) }
                },
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

    private fun createQualityInspection(token: String, tenantId: String, plantCode: String) =
        restTemplate.exchange(
            "/api/v1/quality/inspections",
            HttpMethod.POST,
            HttpEntity(
                mapOf(
                    "inspectionType" to "INCOMING",
                    "referenceDocNo" to "GR-001",
                    "itemCode" to "ITEM-QI-001",
                    "itemName" to "Scoped Inspection Item",
                    "plantCode" to plantCode,
                    "inspectedQuantity" to 10,
                    "inspectionDate" to "2026-03-25"
                ),
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

    private fun createCustomer(token: String, tenantId: String, customerCode: String, assignedTo: String?): Long {
        val response = restTemplate.exchange(
            "/api/v1/crm/customers",
            HttpMethod.POST,
            HttpEntity(
                linkedMapOf<String, Any>(
                    "customerCode" to customerCode,
                    "customerName" to "Scoped CRM Customer",
                    "customerType" to "CORPORATE"
                ).apply {
                    assignedTo?.let { put("assignedTo", it) }
                },
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as Map<String, Any>
        return (data["id"] as Number).toLong()
    }

    private fun createOpportunity(token: String, tenantId: String, customerId: Long, assignedTo: String?): Long {
        val response = restTemplate.exchange(
            "/api/v1/crm/opportunities",
            HttpMethod.POST,
            HttpEntity(
                linkedMapOf<String, Any>(
                    "customerId" to customerId,
                    "title" to "Scoped Opportunity",
                    "expectedAmount" to 100000
                ).apply {
                    assignedTo?.let { put("assignedTo", it) }
                },
                authHeaders(token, tenantId)
            ),
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        @Suppress("UNCHECKED_CAST")
        val data = response.body!!["data"] as Map<String, Any>
        return (data["id"] as Number).toLong()
    }
}
