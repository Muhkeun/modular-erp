package com.modularerp.admin.controller

import com.modularerp.admin.dto.*
import com.modularerp.admin.service.DataScopeService
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/data-scopes")
@Tag(name = "Admin - Data Scopes", description = "데이터 범위 권한 (RLS)")
class DataScopeController(
    private val dataScopeService: DataScopeService
) {

    @GetMapping("/{roleCode}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "역할별 데이터 범위 조회")
    fun getByRole(@PathVariable roleCode: String): ApiResponse<List<DataScopeResponse>> =
        ApiResponse.ok(dataScopeService.getByRole(roleCode))

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "데이터 범위 일괄 저장")
    fun save(@Valid @RequestBody request: DataScopeBatchRequest): ApiResponse<String> {
        dataScopeService.saveForRole(request.roleCode, request.resource, request.scopes)
        return ApiResponse.ok("saved")
    }

    @GetMapping("/resolved")
    @Operation(summary = "역할 목록에 대한 통합 데이터 범위 조회")
    fun getResolved(
        @RequestParam roles: List<String>,
        @RequestParam resource: String
    ): ApiResponse<ResolvedDataScopeResponse> {
        val resolved = dataScopeService.getMergedDataScope(roles, resource)
        val filter = dataScopeService.resolveSearchFilter(roles, resource, null)
        return ApiResponse.ok(
            ResolvedDataScopeResponse(
                type = resolved.type,
                values = resolved.values,
                ownUserId = filter.ownUserId,
                companyCodes = filter.companyCodes,
                departmentCodes = filter.departmentCodes,
                plantCodes = filter.plantCodes,
                denyAll = filter.denyAll
            )
        )
    }
}
