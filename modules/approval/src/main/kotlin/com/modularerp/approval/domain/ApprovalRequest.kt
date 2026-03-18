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

    @Column(nullable = false)
    val requestedBy: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ApprovalStatus = ApprovalStatus.DRAFT

) : TenantEntity() {

    @OneToMany(mappedBy = "request", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    val steps: MutableList<ApprovalStep> = mutableListOf()

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
    }

    fun approve(approverId: String, comment: String? = null) {
        check(status == ApprovalStatus.PENDING) { "Not in PENDING status" }
        val currentStep = currentPendingStep() ?: return
        currentStep.approve(approverId, comment)

        if (steps.all { it.decision == ApprovalDecision.APPROVED }) {
            status = ApprovalStatus.APPROVED
            completedAt = Instant.now()
        }
    }

    fun reject(approverId: String, comment: String? = null) {
        check(status == ApprovalStatus.PENDING) { "Not in PENDING status" }
        val currentStep = currentPendingStep() ?: return
        currentStep.reject(approverId, comment)
        status = ApprovalStatus.REJECTED
        completedAt = Instant.now()
    }

    fun cancel() {
        check(status in listOf(ApprovalStatus.DRAFT, ApprovalStatus.PENDING))
        status = ApprovalStatus.CANCELLED
        completedAt = Instant.now()
    }

    private fun currentPendingStep(): ApprovalStep? =
        steps.firstOrNull { it.decision == null }
}

enum class ApprovalStatus {
    DRAFT, PENDING, APPROVED, REJECTED, CANCELLED
}
