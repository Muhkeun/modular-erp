package com.modularerp.approval.controller

import com.modularerp.approval.dto.*
import com.modularerp.approval.service.ApprovalService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/approvals")
@Tag(name = "Approvals", description = "결재 처리")
class ApprovalController(
    private val approvalService: ApprovalService
) {

    @GetMapping("/my-pending")
    @Operation(summary = "결재 대기함 - My pending approvals")
    fun getMyPending(): ApiResponse<List<ApprovalRequestResponse>> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        val tenantId = TenantContext.getTenantId()
        return ApiResponse.ok(approvalService.getMyPendingApprovals(userId, tenantId))
    }

    @GetMapping("/my-submitted")
    @Operation(summary = "상신함 - My submitted requests")
    fun getMySubmitted(): ApiResponse<List<ApprovalRequestResponse>> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        val tenantId = TenantContext.getTenantId()
        return ApiResponse.ok(approvalService.getMySubmittedRequests(userId, tenantId))
    }

    @GetMapping("/{id}")
    @Operation(summary = "결재 요청 상세")
    fun getById(@PathVariable id: Long): ApiResponse<ApprovalRequestResponse> =
        ApiResponse.ok(approvalService.getById(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "결재 상신 - Submit for approval")
    fun submit(@Valid @RequestBody request: SubmitApprovalRequest): ApiResponse<Long> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        val tenantId = TenantContext.getTenantId()
        val id = approvalService.submitForApproval(
            request.documentType, request.documentId, request.documentNo, userId, tenantId
        )
        return ApiResponse.ok(id)
    }

    @PostMapping("/{id}/steps/{stepId}/approve")
    @Operation(summary = "결재 승인")
    fun approveStep(
        @PathVariable id: Long,
        @PathVariable stepId: Long,
        @RequestBody(required = false) request: ProcessApprovalRequest?
    ): ApiResponse<ApprovalRequestResponse> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        return ApiResponse.ok(approvalService.processApproval(id, stepId, "APPROVE", request?.comment, userId))
    }

    @PostMapping("/{id}/steps/{stepId}/reject")
    @Operation(summary = "결재 반려")
    fun rejectStep(
        @PathVariable id: Long,
        @PathVariable stepId: Long,
        @RequestBody(required = false) request: ProcessApprovalRequest?
    ): ApiResponse<ApprovalRequestResponse> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        return ApiResponse.ok(approvalService.processApproval(id, stepId, "REJECT", request?.comment, userId))
    }

    @PostMapping("/{id}/steps/{stepId}/return")
    @Operation(summary = "결재 반송 (반려 후 재작성)")
    fun returnStep(
        @PathVariable id: Long,
        @PathVariable stepId: Long,
        @RequestBody(required = false) request: ProcessApprovalRequest?
    ): ApiResponse<ApprovalRequestResponse> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        return ApiResponse.ok(approvalService.processApproval(id, stepId, "RETURN", request?.comment, userId))
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "코멘트 추가")
    fun addComment(
        @PathVariable id: Long,
        @Valid @RequestBody request: AddCommentRequest
    ): ApiResponse<ApprovalRequestResponse> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        return ApiResponse.ok(approvalService.addComment(id, userId, request.comment, request.stepSequence))
    }

    // ── Delegations ──

    @PostMapping("/delegations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "결재 위임 생성")
    fun createDelegation(@Valid @RequestBody request: CreateDelegationRequest): ApiResponse<DelegationResponse> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        val tenantId = TenantContext.getTenantId()
        return ApiResponse.ok(approvalService.createDelegation(
            userId, request.toUserId, request.startDate, request.endDate, request.reason, tenantId
        ))
    }

    @GetMapping("/delegations")
    @Operation(summary = "내 결재 위임 목록")
    fun getMyDelegations(): ApiResponse<List<DelegationResponse>> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        val tenantId = TenantContext.getTenantId()
        return ApiResponse.ok(approvalService.getMyDelegations(userId, tenantId))
    }

    @DeleteMapping("/delegations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "결재 위임 비활성화")
    fun deactivateDelegation(@PathVariable id: Long) {
        approvalService.deactivateDelegation(id)
    }

    // ── Dashboard ──

    @GetMapping("/dashboard")
    @Operation(summary = "결재 대시보드")
    fun getDashboard(): ApiResponse<ApprovalDashboardResponse> {
        val userId = TenantContext.getUserId() ?: "anonymous"
        val tenantId = TenantContext.getTenantId()
        return ApiResponse.ok(approvalService.getDashboard(userId, tenantId))
    }
}
