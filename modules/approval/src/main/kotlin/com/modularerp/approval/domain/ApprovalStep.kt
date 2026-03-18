package com.modularerp.approval.domain

import com.modularerp.core.domain.BaseEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "approval_steps")
class ApprovalStep(
    @Column(nullable = false)
    val stepOrder: Int,

    @Column(nullable = false, length = 50)
    val approverRole: String,

    @Column(length = 50)
    val approverId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: ApprovalRequest? = null

) : BaseEntity() {

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var decision: ApprovalDecision? = null
        private set

    var decidedBy: String? = null
        private set

    var decidedAt: Instant? = null
        private set

    @Column(length = 500)
    var comment: String? = null
        private set

    fun approve(approverId: String, comment: String? = null) {
        this.decision = ApprovalDecision.APPROVED
        this.decidedBy = approverId
        this.decidedAt = Instant.now()
        this.comment = comment
    }

    fun reject(approverId: String, comment: String? = null) {
        this.decision = ApprovalDecision.REJECTED
        this.decidedBy = approverId
        this.decidedAt = Instant.now()
        this.comment = comment
    }
}

enum class ApprovalDecision {
    APPROVED, REJECTED
}
