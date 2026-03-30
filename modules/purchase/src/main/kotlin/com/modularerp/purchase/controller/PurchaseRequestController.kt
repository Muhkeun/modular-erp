package com.modularerp.purchase.controller

import com.modularerp.admin.service.DataScopeService
import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.purchase.domain.PrStatus
import com.modularerp.purchase.dto.*
import com.modularerp.security.tenant.TenantContext
import com.modularerp.purchase.service.PurchaseRequestService
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
@RequestMapping("/api/v1/purchase/requests")
@Tag(name = "Purchase Requests", description = "Purchase Request (PR) management")
class PurchaseRequestController(
    private val prService: PurchaseRequestService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    @Operation(summary = "Search purchase requests")
    fun search(
        @RequestParam(required = false) status: PrStatus?,
        @RequestParam(required = false) companyCode: String?,
        @RequestParam(required = false) documentNo: String?,
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<PrResponse>> {
        val roles = authentication.authorities.map { it.authority.removePrefix("ROLE_") }
        val scopeFilter = dataScopeService.resolveSearchFilter(roles, "purchase-requests", TenantContext.getUserId())
        val page = prService.search(status, companyCode, documentNo, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PR by ID")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<PrResponse> {
        val scopeFilter = resolveScope(authentication, "purchase-requests")
        val response = prService.getById(id)
        assertAccessible(scopeFilter, response.requestedBy, response.companyCode, response.departmentCode, response.plantCode)
        return ApiResponse.ok(response)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new PR")
    fun create(@Valid @RequestBody request: CreatePrRequest, authentication: Authentication): ApiResponse<PrResponse> {
        assertWritable(authentication, "purchase-requests", request.companyCode, request.departmentCode, request.plantCode)
        return ApiResponse.ok(prService.create(request))
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit PR for approval")
    fun submit(@PathVariable id: Long, authentication: Authentication): ApiResponse<PrResponse> {
        getById(id, authentication)
        return ApiResponse.ok(prService.submit(id))
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve PR")
    fun approve(@PathVariable id: Long, authentication: Authentication): ApiResponse<PrResponse> {
        getById(id, authentication)
        return ApiResponse.ok(prService.approve(id))
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject PR")
    fun reject(@PathVariable id: Long, authentication: Authentication): ApiResponse<PrResponse> {
        getById(id, authentication)
        return ApiResponse.ok(prService.reject(id))
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, authentication: Authentication) {
        getById(id, authentication)
        prService.delete(id)
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
