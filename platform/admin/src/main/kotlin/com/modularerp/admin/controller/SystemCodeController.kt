package com.modularerp.admin.controller

import com.modularerp.admin.dto.*
import com.modularerp.admin.service.SystemCodeService
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/system-codes")
@Tag(name = "Admin - System Codes", description = "시스템 공통 코드 관리")
class SystemCodeController(
    private val systemCodeService: SystemCodeService
) {

    @GetMapping
    @Operation(summary = "전체 시스템 코드 목록")
    fun getAll(): ApiResponse<List<SystemCodeResponse>> =
        ApiResponse.ok(systemCodeService.getAll())

    @GetMapping("/{groupCode}")
    @Operation(summary = "시스템 코드 상세 (항목 포함)")
    fun getByGroupCode(@PathVariable groupCode: String): ApiResponse<SystemCodeResponse> =
        ApiResponse.ok(systemCodeService.getByGroupCode(groupCode))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "시스템 코드 생성")
    fun create(@Valid @RequestBody request: CreateSystemCodeRequest): ApiResponse<SystemCodeResponse> =
        ApiResponse.ok(systemCodeService.create(request))

    @PutMapping("/{groupCode}")
    @Operation(summary = "시스템 코드 수정")
    fun update(@PathVariable groupCode: String, @Valid @RequestBody request: UpdateSystemCodeRequest): ApiResponse<SystemCodeResponse> =
        ApiResponse.ok(systemCodeService.update(groupCode, request))

    @DeleteMapping("/{groupCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "시스템 코드 삭제")
    fun delete(@PathVariable groupCode: String) = systemCodeService.delete(groupCode)
}
