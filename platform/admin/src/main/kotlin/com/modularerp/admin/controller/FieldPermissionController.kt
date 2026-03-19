package com.modularerp.admin.controller

import com.modularerp.admin.dto.*
import com.modularerp.admin.service.FieldPermissionService
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/field-permissions")
@Tag(name = "Admin - Field Permissions", description = "필드 레벨 권한 관리")
@PreAuthorize("hasRole('ADMIN')")
class FieldPermissionController(
    private val fieldPermService: FieldPermissionService
) {

    @GetMapping("/{roleCode}/{resource}")
    @Operation(summary = "역할+리소스별 필드 권한 조회")
    fun get(@PathVariable roleCode: String, @PathVariable resource: String): ApiResponse<List<FieldPermissionResponse>> =
        ApiResponse.ok(fieldPermService.getByRoleAndResource(roleCode, resource))

    @PutMapping
    @Operation(summary = "필드 권한 일괄 저장")
    fun save(@Valid @RequestBody request: FieldPermissionBatchRequest): ApiResponse<String> {
        fieldPermService.saveForRole(request.roleCode, request.resource, request.fields)
        return ApiResponse.ok("saved")
    }

    @GetMapping("/merged")
    @Operation(summary = "역할 목록에 대한 통합 필드 권한 조회 (프론트엔드용)")
    fun getMerged(
        @RequestParam roles: List<String>,
        @RequestParam resource: String
    ): ApiResponse<Map<String, String>> {
        val merged = fieldPermService.getMergedFieldPermissions(roles, resource)
        return ApiResponse.ok(merged.mapValues { it.value.name })
    }
}
