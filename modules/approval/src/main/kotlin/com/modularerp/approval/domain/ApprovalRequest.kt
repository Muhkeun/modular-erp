package com.modularerp.approval.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "approval_requests")
class ApprovalRequest(
    @Column(nullable = false, length = 30)
    val documentType: String,

    @Column(nullable = false)
    val documentId: Long,

    @Column(length = 30)
    var documentNo: String? = null,

    @Column(nullable = false)
    val requestedBy: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ApprovalStatus = ApprovalStatus.DRAFT

) : TenantEntity() {

    @OneToMany(mappedBy = "request", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    val steps: MutableList<ApprovalStep> = mutableListOf()

    @OneToMany(mappedBy = "approvalRequest", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("commentAt ASC")
    val comments: MutableList<ApprovalComment> = mutableListOf()

    var completedAt: Instant? = null
        private set

    fun addStep(approverRole: String, approverId: String? = null, stepOrder: Int? = null) {
        val order = stepOrder ?: (steps.size + 1)
        steps.add(ApprovalStep(
            stepOrder = order,
            approverRole = approverRole,
            approverId = approverId,
            request = this
        ))
    }

    fun submit() {
        check(status == ApprovalStatus.DRAFT) { "Can only submit from DRAFT status" }
        check(steps.isNotEmpty()) { "At least one approval step required" }
        status = ApprovalStatus.PENDING
        // Activate the first step
        steps.sortedBy { it.stepOrder }.firstOrNull()?.activateStep()
    }

    fun approve(approverId: String, comment: String? = null) {
        check(status == ApprovalStatus.PENDING) { "Not in PENDING status" }
        val currentStep = currentPendingStep() ?: return
        currentStep.approve(approverId, comment)

        if (steps.all { it.decision == ApprovalDecision.APPROVED }) {
            status = ApprovalStatus.APPROVED
            completedAt = Instant.now()
        } else {
            // Activate next step
            steps.sortedBy { it.stepOrder }
                .firstOrNull { it.decision == null && it.stepStatus != StepStatus.ACTIVE }
                ?.activateStep()
        }
    }

    fun reject(approverId: String, comment: String? = null) {
        check(status == ApprovalStatus.PENDING) { "Not in PENDING status" }
        val currentStep = currentPendingStep() ?: return
        currentStep.reject(approverId, comment)
        status = ApprovalStatus.REJECTED
        completedAt = Instant.now()
    }

    fun returnToSubmitter(approverId: String, comment: String? = null) {
        check(status == ApprovalStatus.PENDING) { "Not in PENDING status" }
        val currentStep = currentPendingStep() ?: return
        currentStep.returnStep(approverId, comment)
        status = ApprovalStatus.RETURNED
        completedAt = Instant.now()
    }

    fun cancel() {
        check(status in listOf(ApprovalStatus.DRAFT, ApprovalStatus.PENDING))
        status = ApprovalStatus.CANCELLED
        completedAt = Instant.now()
    }

    fun addComment(commentBy: String, commentText: String, stepSequence: Int? = null) {
        comments.add(ApprovalComment(
            approvalRequest = this,
            stepSequence = stepSequence,
            commentBy = commentBy,
            comment = commentText
        ))
    }

    fun currentPendingStep(): ApprovalStep? =
        steps.sortedBy { it.stepOrder }.firstOrNull { it.stepStatus == StepStatus.ACTIVE && it.decision == null }
}

enum class ApprovalStatus {
    DRAFT, PENDING, APPROVED, REJECTED, RETURNED, CANCELLED
}
