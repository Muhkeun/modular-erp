package com.modularerp.admin.controller

import com.modularerp.admin.dto.*
import com.modularerp.admin.service.OrganizationService
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/organizations")
@Tag(name = "Admin - Organizations", description = "조직 관리")
@PreAuthorize("hasRole('ADMIN')")
class OrganizationController(
    private val orgService: OrganizationService
) {

    @GetMapping("/tree")
    @Operation(summary = "조직 트리 조회")
    fun getTree(): ApiResponse<List<OrganizationResponse>> =
        ApiResponse.ok(orgService.getTree())

    @GetMapping
    @Operation(summary = "전체 조직 플랫 목록")
    fun getAll(): ApiResponse<List<OrganizationResponse>> =
        ApiResponse.ok(orgService.getAll())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "조직 생성")
    fun create(@Valid @RequestBody request: CreateOrganizationRequest): ApiResponse<OrganizationResponse> =
        ApiResponse.ok(orgService.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "조직 수정")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateOrganizationRequest): ApiResponse<OrganizationResponse> =
        ApiResponse.ok(orgService.update(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "조직 삭제 (비활성화)")
    fun delete(@PathVariable id: Long) = orgService.delete(id)
}
