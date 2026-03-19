package com.modularerp.admin.controller

import com.modularerp.admin.dto.*
import com.modularerp.admin.service.MenuProfileService
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/menu-profiles")
@Tag(name = "Admin - Menu Profiles", description = "메뉴 프로필 관리")
@PreAuthorize("hasRole('ADMIN')")
class MenuProfileController(
    private val menuProfileService: MenuProfileService
) {

    @GetMapping
    @Operation(summary = "전체 메뉴 프로필 목록")
    fun getAll(): ApiResponse<List<MenuProfileResponse>> =
        ApiResponse.ok(menuProfileService.getAll())

    @GetMapping("/{code}")
    @Operation(summary = "메뉴 프로필 상세")
    fun getByCode(@PathVariable code: String): ApiResponse<MenuProfileResponse> =
        ApiResponse.ok(menuProfileService.getByCode(code))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "메뉴 프로필 생성")
    fun create(@Valid @RequestBody request: CreateMenuProfileRequest): ApiResponse<MenuProfileResponse> =
        ApiResponse.ok(menuProfileService.create(request))

    @PutMapping("/{code}")
    @Operation(summary = "메뉴 프로필 수정")
    fun update(@PathVariable code: String, @Valid @RequestBody request: UpdateMenuProfileRequest): ApiResponse<MenuProfileResponse> =
        ApiResponse.ok(menuProfileService.update(code, request))

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "메뉴 프로필 삭제")
    fun delete(@PathVariable code: String) = menuProfileService.delete(code)
}
