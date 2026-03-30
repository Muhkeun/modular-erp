package com.modularerp.sales.controller

import com.modularerp.admin.service.DataScopeService
import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.sales.domain.SoStatus
import com.modularerp.sales.dto.*
import com.modularerp.sales.service.SalesOrderService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/sales/orders")
@Tag(name = "Sales Orders")
class SalesOrderController(
    private val soService: SalesOrderService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(@RequestParam(required = false) status: SoStatus?,
               @RequestParam(required = false) customerCode: String?,
               @RequestParam(required = false) documentNo: String?,
               authentication: Authentication,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<SoResponse>> {
        val roles = authentication.authorities.map { it.authority.removePrefix("ROLE_") }
        val scopeFilter = dataScopeService.resolveSearchFilter(roles, "sales-orders", TenantContext.getUserId())
        val page = soService.search(status, customerCode, documentNo, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<SoResponse> {
        val scopeFilter = resolveScope(authentication, "sales-orders")
        val response = soService.getById(id)
        assertAccessible(scopeFilter, response.createdBy, response.companyCode, null, response.plantCode)
        return ApiResponse.ok(response)
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateSoRequest, authentication: Authentication): ApiResponse<SoResponse> {
        assertWritable(authentication, "sales-orders", req.companyCode, null, req.plantCode)
        return ApiResponse.ok(soService.create(req))
    }

    @PostMapping("/{id}/submit")
    fun submit(@PathVariable id: Long, authentication: Authentication): ApiResponse<SoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(soService.submit(id))
    }

    @PostMapping("/{id}/approve")
    fun approve(@PathVariable id: Long, authentication: Authentication): ApiResponse<SoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(soService.approve(id))
    }

    @PostMapping("/{id}/reject")
    fun reject(@PathVariable id: Long, authentication: Authentication): ApiResponse<SoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(soService.reject(id))
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
