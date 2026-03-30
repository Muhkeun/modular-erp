package com.modularerp.purchase.controller

import com.modularerp.admin.service.DataScopeService
import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.purchase.domain.PoStatus
import com.modularerp.purchase.dto.*
import com.modularerp.purchase.service.PurchaseRequestService
import com.modularerp.purchase.service.PurchaseOrderService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/purchase/orders")
@Tag(name = "Purchase Orders", description = "Purchase Order (PO) management")
class PurchaseOrderController(
    private val poService: PurchaseOrderService,
    private val dataScopeService: DataScopeService,
    private val prService: PurchaseRequestService
) {

    @GetMapping
    @Operation(summary = "Search purchase orders")
    fun search(
        @RequestParam(required = false) status: PoStatus?,
        @RequestParam(required = false) vendorCode: String?,
        @RequestParam(required = false) documentNo: String?,
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<PoResponse>> {
        val roles = authentication.authorities.map { it.authority.removePrefix("ROLE_") }
        val scopeFilter = dataScopeService.resolveSearchFilter(roles, "purchase-orders", TenantContext.getUserId())
        val page = poService.search(status, vendorCode, documentNo, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PO by ID")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<PoResponse> {
        val scopeFilter = resolveScope(authentication, "purchase-orders")
        val response = poService.getById(id)
        assertAccessible(scopeFilter, response.createdBy, response.companyCode, null, response.plantCode)
        return ApiResponse.ok(response)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new PO")
    fun create(@Valid @RequestBody request: CreatePoRequest, authentication: Authentication): ApiResponse<PoResponse> {
        assertWritable(authentication, "purchase-orders", request.companyCode, null, request.plantCode)
        return ApiResponse.ok(poService.create(request))
    }

    @PostMapping("/from-pr/{prId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create PO from an approved PR")
    fun createFromPr(
        @PathVariable prId: Long,
        @Valid @RequestBody request: CreatePoFromPrRequest,
        authentication: Authentication
    ): ApiResponse<PoResponse> {
        val source = prService.getById(prId)
        assertAccessible(resolveScope(authentication, "purchase-requests"), source.requestedBy, source.companyCode, source.departmentCode, source.plantCode)
        assertWritable(authentication, "purchase-orders", source.companyCode, null, source.plantCode)
        return ApiResponse.ok(poService.createFromPr(prId, request))
    }

    @PostMapping("/{id}/submit")
    fun submit(@PathVariable id: Long, authentication: Authentication): ApiResponse<PoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(poService.submit(id))
    }

    @PostMapping("/{id}/approve")
    fun approve(@PathVariable id: Long, authentication: Authentication): ApiResponse<PoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(poService.approve(id))
    }

    @PostMapping("/{id}/reject")
    fun reject(@PathVariable id: Long, authentication: Authentication): ApiResponse<PoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(poService.reject(id))
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
