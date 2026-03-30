package com.modularerp.logistics.controller

import com.modularerp.admin.service.DataScopeService
import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.logistics.domain.*
import com.modularerp.logistics.dto.*
import com.modularerp.logistics.repository.StockSummaryRepository
import com.modularerp.logistics.service.*
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Page
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/logistics/goods-receipts")
@Tag(name = "Goods Receipts")
class GoodsReceiptController(
    private val grService: GoodsReceiptService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(@RequestParam(required = false) status: GrStatus?,
               @RequestParam(required = false) documentNo: String?,
               authentication: Authentication,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<GrResponse>> {
        val roles = authentication.authorities.map { it.authority.removePrefix("ROLE_") }
        val scopeFilter = dataScopeService.resolveSearchFilter(roles, "goods-receipts", TenantContext.getUserId())
        val page = grService.search(status, documentNo, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<GrResponse> {
        val scopeFilter = resolveScope(authentication, "goods-receipts")
        val response = grService.getById(id)
        assertAccessible(scopeFilter, response.createdBy, response.companyCode, null, response.plantCode)
        return ApiResponse.ok(response)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateGrRequest, authentication: Authentication): ApiResponse<GrResponse> {
        assertWritable(authentication, "goods-receipts", req.companyCode, null, req.plantCode)
        return ApiResponse.ok(grService.create(req))
    }

    @PostMapping("/{id}/confirm")
    fun confirm(@PathVariable id: Long, authentication: Authentication): ApiResponse<GrResponse> {
        getById(id, authentication)
        return ApiResponse.ok(grService.confirm(id))
    }

    private fun resolveScope(authentication: Authentication, resource: String) =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            resource,
            TenantContext.getUserId()
        )

    private fun assertAccessible(
        scopeFilter: DataScopeSearchFilter,
        ownerId: String?,
        companyCode: String?,
        departmentCode: String?,
        plantCode: String?
    ) {
        if (!scopeFilter.matches(ownerId, companyCode, departmentCode, plantCode)) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertWritable(
        authentication: Authentication,
        resource: String,
        companyCode: String?,
        departmentCode: String?,
        plantCode: String?
    ) {
        val scopeFilter = resolveScope(authentication, resource)
        if (!scopeFilter.matches(TenantContext.getUserId(), companyCode, departmentCode, plantCode)) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}

@RestController
@RequestMapping("/api/v1/logistics/goods-issues")
@Tag(name = "Goods Issues")
class GoodsIssueController(
    private val giService: GoodsIssueService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(@RequestParam(required = false) status: GiStatus?,
               @RequestParam(required = false) documentNo: String?,
               authentication: Authentication,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<GiResponse>> {
        val roles = authentication.authorities.map { it.authority.removePrefix("ROLE_") }
        val scopeFilter = dataScopeService.resolveSearchFilter(roles, "goods-issues", TenantContext.getUserId())
        val page = giService.search(status, documentNo, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<GiResponse> {
        val scopeFilter = resolveScope(authentication, "goods-issues")
        val response = giService.getById(id)
        assertAccessible(scopeFilter, response.createdBy, response.companyCode, null, response.plantCode)
        return ApiResponse.ok(response)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateGiRequest, authentication: Authentication): ApiResponse<GiResponse> {
        assertWritable(authentication, "goods-issues", req.companyCode, null, req.plantCode)
        return ApiResponse.ok(giService.create(req))
    }

    @PostMapping("/{id}/confirm")
    fun confirm(@PathVariable id: Long, authentication: Authentication): ApiResponse<GiResponse> {
        getById(id, authentication)
        return ApiResponse.ok(giService.confirm(id))
    }

    private fun resolveScope(authentication: Authentication, resource: String) =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            resource,
            TenantContext.getUserId()
        )

    private fun assertAccessible(
        scopeFilter: DataScopeSearchFilter,
        ownerId: String?,
        companyCode: String?,
        departmentCode: String?,
        plantCode: String?
    ) {
        if (!scopeFilter.matches(ownerId, companyCode, departmentCode, plantCode)) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertWritable(
        authentication: Authentication,
        resource: String,
        companyCode: String?,
        departmentCode: String?,
        plantCode: String?
    ) {
        val scopeFilter = resolveScope(authentication, resource)
        if (!scopeFilter.matches(TenantContext.getUserId(), companyCode, departmentCode, plantCode)) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}

@RestController
@RequestMapping("/api/v1/logistics/stock")
@Tag(name = "Stock")
class StockController(
    private val stockRepository: StockSummaryRepository,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(@RequestParam(required = false) plantCode: String?,
               @RequestParam(required = false) itemCode: String?,
               authentication: Authentication,
               @PageableDefault(size = 50) pageable: Pageable): ApiResponse<List<StockResponse>> {
        val roles = authentication.authorities.map { it.authority.removePrefix("ROLE_") }
        val scopeFilter = dataScopeService.resolveSearchFilter(roles, "stock", TenantContext.getUserId())
            .narrowToSupported(
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = false,
                supportsPlant = true
            )
        val page =
            if (scopeFilter.denyAll) {
                Page.empty(pageable)
            } else {
                stockRepository.search(
                    tenantId = TenantContext.getTenantId(),
                    plantCode = plantCode,
                    itemCode = itemCode,
                    applyPlantScope = scopeFilter.plantCodes.isNotEmpty(),
                    plantCodes = scopeFilter.scopedPlantCodes(),
                    pageable = pageable
                )
            }
        return ApiResponse.ok(page.content.map { it.toResponse() },
            PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }
}
