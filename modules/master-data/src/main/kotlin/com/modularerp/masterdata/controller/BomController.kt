package com.modularerp.masterdata.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.masterdata.domain.BomStatus
import com.modularerp.masterdata.dto.*
import com.modularerp.masterdata.service.BomService
import com.modularerp.masterdata.service.ExplodedBomLine
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/master-data/boms")
@Tag(name = "Bill of Materials", description = "BOM management and explosion")
class BomController(
    private val bomService: BomService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(
        @RequestParam(required = false) productCode: String?,
        @RequestParam(required = false) status: BomStatus?,
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<BomResponse>> {
        val scopeFilter = resolveBomScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else bomService.search(productCode, status, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<BomResponse> {
        val response = bomService.getById(id)
        assertPlantAccessible(authentication, response.plantCode)
        return ApiResponse.ok(response)
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateBomRequest, authentication: Authentication): ApiResponse<BomResponse> {
        assertPlantWritable(authentication, req.plantCode)
        return ApiResponse.ok(bomService.create(req))
    }

    @PostMapping("/{id}/release")
    fun release(@PathVariable id: Long, authentication: Authentication): ApiResponse<BomResponse> {
        getById(id, authentication)
        return ApiResponse.ok(bomService.release(id))
    }

    @GetMapping("/explode")
    @Operation(summary = "Multi-level BOM explosion", description = "Recursively explodes BOM for a product, handling phantom items")
    fun explode(
        @RequestParam productCode: String,
        @RequestParam plantCode: String,
        @RequestParam(defaultValue = "1") quantity: BigDecimal,
        authentication: Authentication
    ): ApiResponse<List<ExplodedBomLine>> =
        ApiResponse.ok(
            bomService.explode(productCode, plantCode.also { assertPlantAccessible(authentication, it) }, quantity)
        )

    private fun resolveBomScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "boms",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = false,
            supportsDepartment = false,
            supportsPlant = true
        )

    private fun assertPlantAccessible(authentication: Authentication, plantCode: String?) {
        val scopeFilter = resolveBomScope(authentication)
        if (!scopeFilter.matchesSupported(
                plantCode = plantCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = false,
                supportsPlant = true
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertPlantWritable(authentication: Authentication, plantCode: String?) {
        val scopeFilter = resolveBomScope(authentication)
        if (!scopeFilter.matchesSupported(
                plantCode = plantCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = false,
                supportsPlant = true
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}
