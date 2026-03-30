package com.modularerp.admin.controller

import com.modularerp.admin.dto.*
import com.modularerp.admin.service.MenuProfileService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/menu-profiles")
@Tag(name = "Admin - Menu Profiles", description = "메뉴 프로필 관리")
class MenuProfileController(
    private val menuProfileService: MenuProfileService
) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "전체 메뉴 프로필 목록")
    fun getAll(): ApiResponse<List<MenuProfileResponse>> =
        ApiResponse.ok(menuProfileService.getAll())

    @GetMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "메뉴 프로필 상세")
    fun getByCode(@PathVariable code: String): ApiResponse<MenuProfileResponse> =
        ApiResponse.ok(menuProfileService.getByCode(code))

    @GetMapping("/resolved")
    @Operation(summary = "현재 사용자에게 적용되는 메뉴 프로필 조회")
    fun getResolved(authentication: Authentication): ApiResponse<ResolvedMenuProfileResponse> {
        val userId = TenantContext.getUserId()
            ?: throw IllegalStateException("User not authenticated")
        val roles = authentication.authorities
            .map { it.authority.removePrefix("ROLE_") }
            .filter { it.isNotBlank() }
        return ApiResponse.ok(menuProfileService.resolveForUser(userId, roles))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "메뉴 프로필 생성")
    fun create(@Valid @RequestBody request: CreateMenuProfileRequest): ApiResponse<MenuProfileResponse> =
        ApiResponse.ok(menuProfileService.create(request))

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "메뉴 프로필 수정")
    fun update(@PathVariable code: String, @Valid @RequestBody request: UpdateMenuProfileRequest): ApiResponse<MenuProfileResponse> =
        ApiResponse.ok(menuProfileService.update(code, request))

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "메뉴 프로필 삭제")
    fun delete(@PathVariable code: String) = menuProfileService.delete(code)
}
