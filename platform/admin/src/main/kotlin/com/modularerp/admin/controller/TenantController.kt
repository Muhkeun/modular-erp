package com.modularerp.admin.controller

import com.modularerp.admin.dto.*
import com.modularerp.admin.service.TenantManagementService
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/tenants")
@Tag(name = "Admin - Tenants", description = "멀티테넌트 관리 콘솔")
@PreAuthorize("hasRole('ADMIN')")
class TenantController(
    private val tenantService: TenantManagementService
) {

    @GetMapping
    @Operation(summary = "전체 테넌트 목록")
    fun getAll(): ApiResponse<List<TenantResponse>> =
        ApiResponse.ok(tenantService.getAll())

    @GetMapping("/{tenantId}")
    @Operation(summary = "테넌트 상세 조회")
    fun getById(@PathVariable tenantId: String): ApiResponse<TenantResponse> =
        ApiResponse.ok(tenantService.getById(tenantId))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "테넌트 생성")
    fun create(@Valid @RequestBody request: CreateTenantRequest): ApiResponse<TenantResponse> =
        ApiResponse.ok(tenantService.create(request))

    @PutMapping("/{tenantId}")
    @Operation(summary = "테넌트 수정")
    fun update(@PathVariable tenantId: String, @Valid @RequestBody request: UpdateTenantRequest): ApiResponse<TenantResponse> =
        ApiResponse.ok(tenantService.update(tenantId, request))

    @PostMapping("/{tenantId}/suspend")
    @Operation(summary = "테넌트 정지")
    fun suspend(@PathVariable tenantId: String): ApiResponse<String> {
        tenantService.suspend(tenantId)
        return ApiResponse.ok("suspended")
    }

    @PostMapping("/{tenantId}/activate")
    @Operation(summary = "테넌트 활성화")
    fun activate(@PathVariable tenantId: String): ApiResponse<String> {
        tenantService.activate(tenantId)
        return ApiResponse.ok("activated")
    }
}
