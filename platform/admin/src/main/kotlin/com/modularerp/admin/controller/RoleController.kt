package com.modularerp.admin.controller

import com.modularerp.admin.dto.*
import com.modularerp.admin.service.RoleService
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/roles")
@Tag(name = "Admin - Roles", description = "역할 및 권한 관리")
@PreAuthorize("hasRole('ADMIN')")
class RoleController(
    private val roleService: RoleService
) {

    @GetMapping
    @Operation(summary = "전체 역할 목록")
    fun getAll(): ApiResponse<List<RoleResponse>> =
        ApiResponse.ok(roleService.getAll())

    @GetMapping("/{code}")
    @Operation(summary = "역할 상세 조회")
    fun getByCode(@PathVariable code: String): ApiResponse<RoleResponse> =
        ApiResponse.ok(roleService.getByCode(code))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "역할 생성")
    fun create(@Valid @RequestBody request: CreateRoleRequest): ApiResponse<RoleResponse> =
        ApiResponse.ok(roleService.create(request))

    @PutMapping("/{code}")
    @Operation(summary = "역할 수정")
    fun update(@PathVariable code: String, @Valid @RequestBody request: UpdateRoleRequest): ApiResponse<RoleResponse> =
        ApiResponse.ok(roleService.update(code, request))

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "역할 삭제")
    fun delete(@PathVariable code: String) = roleService.delete(code)

    @GetMapping("/permissions")
    @Operation(summary = "역할 코드 목록으로 통합 권한 조회")
    fun getPermissions(@RequestParam roles: List<String>): ApiResponse<Map<String, Set<String>>> =
        ApiResponse.ok(roleService.getPermissionsForRoles(roles))
}
