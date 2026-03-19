package com.modularerp.approval.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "approval_comments")
class ApprovalComment(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    val approvalRequest: ApprovalRequest,

    @Column(name = "step_sequence")
    val stepSequence: Int? = null,

    @Column(name = "comment_by", nullable = false, length = 50)
    val commentBy: String,

    @Column(nullable = false, length = 2000)
    val comment: String,

    @Column(name = "comment_at", nullable = false)
    val commentAt: LocalDateTime = LocalDateTime.now()

) : TenantEntity()
