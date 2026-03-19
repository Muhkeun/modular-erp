package com.modularerp.preference.controller

import com.modularerp.preference.domain.PreferenceCategory
import com.modularerp.preference.dto.*
import com.modularerp.preference.service.PreferenceService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/preferences")
@Tag(name = "User Preferences", description = "사용자 개인 설정 관리")
class PreferenceController(
    private val preferenceService: PreferenceService
) {

    // ── General Preferences ──

    @GetMapping
    @Operation(summary = "전체 개인 설정 조회")
    fun getAll(): ApiResponse<List<PreferenceResponse>> =
        ApiResponse.ok(preferenceService.getAllPreferences(currentUserId()))

    @GetMapping("/category/{category}")
    @Operation(summary = "카테고리별 설정 조회")
    fun getByCategory(@PathVariable category: PreferenceCategory): ApiResponse<List<PreferenceResponse>> =
        ApiResponse.ok(preferenceService.getPreferencesByCategory(currentUserId(), category))

    @PutMapping
    @Operation(summary = "설정 저장 (upsert)")
    fun save(@Valid @RequestBody request: PreferenceRequest): ApiResponse<PreferenceResponse> =
        ApiResponse.ok(preferenceService.savePreference(currentUserId(), currentTenantId(), request))

    @PutMapping("/batch")
    @Operation(summary = "설정 일괄 저장")
    fun saveBatch(@Valid @RequestBody request: PreferenceBatchRequest): ApiResponse<List<PreferenceResponse>> =
        ApiResponse.ok(preferenceService.saveBatch(currentUserId(), currentTenantId(), request))

    @DeleteMapping("/{category}/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "설정 삭제")
    fun delete(@PathVariable category: PreferenceCategory, @PathVariable key: String) {
        preferenceService.deletePreference(currentUserId(), category, key)
    }

    // ── Grid Preferences ──

    @GetMapping("/grids")
    @Operation(summary = "전체 그리드 설정 조회")
    fun getAllGrids(): ApiResponse<List<GridPreferenceResponse>> =
        ApiResponse.ok(preferenceService.getAllGridPreferences(currentUserId()))

    @GetMapping("/grids/{gridId}")
    @Operation(summary = "특정 그리드 설정 조회")
    fun getGrid(@PathVariable gridId: String): ApiResponse<GridPreferenceResponse?> =
        ApiResponse.ok(preferenceService.getGridPreference(currentUserId(), gridId))

    @PutMapping("/grids")
    @Operation(summary = "그리드 설정 저장 (upsert)")
    fun saveGrid(@Valid @RequestBody request: GridPreferenceRequest): ApiResponse<GridPreferenceResponse> =
        ApiResponse.ok(preferenceService.saveGridPreference(currentUserId(), currentTenantId(), request))

    @DeleteMapping("/grids/{gridId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "그리드 설정 초기화")
    fun deleteGrid(@PathVariable gridId: String) {
        preferenceService.deleteGridPreference(currentUserId(), gridId)
    }

    private fun currentUserId(): String = TenantContext.getUserId()
        ?: throw IllegalStateException("User not authenticated")

    private fun currentTenantId(): String = TenantContext.getTenantId()
}
