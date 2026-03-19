package com.modularerp.admin.controller

import com.modularerp.admin.dto.*
import com.modularerp.admin.service.ApiKeyService
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/api-keys")
@Tag(name = "Admin - API Keys", description = "외부 연동 API 키 관리")
@PreAuthorize("hasRole('ADMIN')")
class ApiKeyController(
    private val apiKeyService: ApiKeyService
) {

    @GetMapping
    @Operation(summary = "전체 API 키 목록")
    fun getAll(): ApiResponse<List<ApiKeyResponse>> =
        ApiResponse.ok(apiKeyService.getAll())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "API 키 생성 (원본 키는 이 응답에서만 제공)")
    fun create(@Valid @RequestBody request: CreateApiKeyRequest): ApiResponse<ApiKeyCreateResponse> =
        ApiResponse.ok(apiKeyService.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "API 키 수정")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateApiKeyRequest): ApiResponse<ApiKeyResponse> =
        ApiResponse.ok(apiKeyService.update(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "API 키 폐기")
    fun revoke(@PathVariable id: Long) = apiKeyService.revoke(id)
}
