package com.modularerp.approval.dto

import com.modularerp.approval.domain.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

// ── Request DTOs ──

data class SubmitApprovalRequest(
    val documentType: String,
    val documentId: Long,
    val documentNo: String
)

data class ProcessApprovalRequest(
    val comment: String? = null
)

data class CreateDelegationRequest(
    val toUserId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String? = null
)

data class AddCommentRequest(
    val comment: String,
    val stepSequence: Int? = null
)

// ── Response DTOs ──

data class ApprovalRequestResponse(
    val id: Long,
    val documentType: String,
    val documentId: Long,
    val documentNo: String?,
    val requestedBy: String,
    val status: ApprovalStatus,
    val steps: List<ApprovalStepResponse>,
    val comments: List<ApprovalCommentResponse>,
    val createdAt: Instant?,
    val completedAt: Instant?
) {
    companion object {
        fun from(e: ApprovalRequest) = ApprovalRequestResponse(
            id = e.id,
            documentType = e.documentType,
            documentId = e.documentId,
            documentNo = e.documentNo,
            requestedBy = e.requestedBy,
            status = e.status,
            steps = e.steps.sortedBy { it.stepOrder }.map(ApprovalStepResponse::from),
            comments = e.comments.sortedBy { it.commentAt }.map(ApprovalCommentResponse::from),
            createdAt = e.createdAt,
            completedAt = e.completedAt
        )
    }
}

data class ApprovalStepResponse(
    val id: Long,
    val stepOrder: Int,
    val approverRole: String,
    val approverId: String?,
    val decision: ApprovalDecision?,
    val stepStatus: StepStatus,
    val decidedBy: String?,
    val decidedAt: Instant?,
    val comment: String?
) {
    companion object {
        fun from(e: ApprovalStep) = ApprovalStepResponse(
            id = e.id,
            stepOrder = e.stepOrder,
            approverRole = e.approverRole,
            approverId = e.approverId,
            decision = e.decision,
            stepStatus = e.stepStatus,
            decidedBy = e.decidedBy,
            decidedAt = e.decidedAt,
            comment = e.comment
        )
    }
}

data class ApprovalCommentResponse(
    val id: Long,
    val stepSequence: Int?,
    val commentBy: String,
    val comment: String,
    val commentAt: LocalDateTime
) {
    companion object {
        fun from(e: ApprovalComment) = ApprovalCommentResponse(
            id = e.id,
            stepSequence = e.stepSequence,
            commentBy = e.commentBy,
            comment = e.comment,
            commentAt = e.commentAt
        )
    }
}

data class DelegationResponse(
    val id: Long,
    val fromUserId: String,
    val toUserId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val delegationActive: Boolean,
    val reason: String?,
    val createdAt: Instant?
) {
    companion object {
        fun from(e: ApprovalDelegation) = DelegationResponse(
            id = e.id,
            fromUserId = e.fromUserId,
            toUserId = e.toUserId,
            startDate = e.startDate,
            endDate = e.endDate,
            delegationActive = e.delegationActive,
            reason = e.reason,
            createdAt = e.createdAt
        )
    }
}

data class ApprovalDashboardResponse(
    val pendingCount: Long,
    val submittedCount: Long,
    val recentActions: List<ApprovalRequestResponse>
)
