package com.modularerp.audit.controller

import com.modularerp.audit.domain.AuditAction
import com.modularerp.audit.dto.AuditLogResponse
import com.modularerp.audit.service.AuditService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Admin - Audit Logs", description = "감사 로그 조회")
@PreAuthorize("hasRole('ADMIN')")
class AuditLogController(
    private val auditService: AuditService
) {

    @GetMapping
    @Operation(summary = "감사 로그 검색")
    fun search(
        @RequestParam(required = false) userId: String?,
        @RequestParam(required = false) action: AuditAction?,
        @RequestParam(required = false) entityType: String?,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @PageableDefault(size = 50) pageable: Pageable
    ): ApiResponse<List<AuditLogResponse>> {
        val page = auditService.search(userId, action, entityType, from, to, pageable)
        return ApiResponse.ok(
            page.content,
            PageMeta(page.number, page.size, page.totalElements, page.totalPages)
        )
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "특정 엔티티 변경 이력 조회")
    fun getEntityHistory(
        @PathVariable entityType: String,
        @PathVariable entityId: String
    ): ApiResponse<List<AuditLogResponse>> =
        ApiResponse.ok(auditService.getEntityHistory(entityType, entityId))
}
