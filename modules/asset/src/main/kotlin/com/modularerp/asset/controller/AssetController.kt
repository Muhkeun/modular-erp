package com.modularerp.asset.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.asset.domain.*
import com.modularerp.asset.dto.*
import com.modularerp.asset.service.AssetService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Asset Management")
class AssetController(
    private val assetService: AssetService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(
        @RequestParam(required = false) status: AssetStatus?,
        @RequestParam(required = false) category: AssetCategory?,
        @RequestParam(required = false) name: String?,
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<AssetResponse>> {
        val scopeFilter = resolveAssetScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else assetService.search(status, category, name, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<AssetResponse> {
        val asset = assetService.getById(id)
        assertAssetAccessible(authentication, asset.department)
        return ApiResponse.ok(asset)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody req: CreateAssetRequest, authentication: Authentication): ApiResponse<AssetResponse> {
        assertAssetWritable(authentication, req.department)
        return ApiResponse.ok(assetService.registerAsset(req))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateAssetRequest,
        authentication: Authentication
    ): ApiResponse<AssetResponse> {
        val asset = assetService.getById(id)
        assertAssetAccessible(authentication, asset.department)
        assertAssetWritable(authentication, req.department ?: asset.department)
        return ApiResponse.ok(assetService.updateAsset(id, req))
    }

    @PostMapping("/{id}/activate")
    fun activate(@PathVariable id: Long, authentication: Authentication): ApiResponse<AssetResponse> {
        val asset = assetService.getById(id)
        assertAssetAccessible(authentication, asset.department)
        return ApiResponse.ok(assetService.activateAsset(id))
    }

    @PostMapping("/{id}/dispose")
    fun dispose(
        @PathVariable id: Long,
        @Valid @RequestBody req: DisposeAssetRequest,
        authentication: Authentication
    ): ApiResponse<AssetDisposalResponse> {
        val asset = assetService.getById(id)
        assertAssetAccessible(authentication, asset.department)
        return ApiResponse.ok(assetService.disposeAsset(id, req))
    }

    @PostMapping("/depreciation/run")
    fun runDepreciation(
        @Valid @RequestBody req: RunDepreciationRequest,
        authentication: Authentication
    ): ApiResponse<List<DepreciationScheduleResponse>> {
        val scopeFilter = resolveAssetScope(authentication)
        return ApiResponse.ok(
            if (scopeFilter.denyAll) emptyList()
            else assetService.runDepreciation(req.year, req.month, scopeFilter)
        )
    }

    @GetMapping("/{id}/schedule")
    fun getSchedule(@PathVariable id: Long, authentication: Authentication): ApiResponse<List<DepreciationScheduleResponse>> {
        val asset = assetService.getById(id)
        assertAssetAccessible(authentication, asset.department)
        return ApiResponse.ok(assetService.getDepreciationSchedule(id))
    }

    @GetMapping("/summary")
    fun getSummary(authentication: Authentication): ApiResponse<AssetSummaryResponse> {
        val scopeFilter = resolveAssetScope(authentication)
        return ApiResponse.ok(
            if (scopeFilter.denyAll) {
                AssetSummaryResponse(
                    totalAssets = 0,
                    totalAcquisitionCost = java.math.BigDecimal.ZERO,
                    totalAccumulatedDepreciation = java.math.BigDecimal.ZERO,
                    totalBookValue = java.math.BigDecimal.ZERO,
                    byCategory = emptyMap()
                )
            } else assetService.getAssetSummary(scopeFilter)
        )
    }

    private fun resolveAssetScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "assets",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = false,
            supportsDepartment = true,
            supportsPlant = false
        )

    private fun assertAssetAccessible(authentication: Authentication, departmentCode: String?) {
        val scopeFilter = resolveAssetScope(authentication)
        if (!scopeFilter.matchesSupported(
                departmentCode = departmentCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = true,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertAssetWritable(authentication: Authentication, departmentCode: String?) {
        val scopeFilter = resolveAssetScope(authentication)
        if (!scopeFilter.matchesSupported(
                departmentCode = departmentCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = true,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}
