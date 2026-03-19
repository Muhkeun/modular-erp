package com.modularerp.approval.repository

import com.modularerp.approval.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WorkflowDefinitionRepository : JpaRepository<WorkflowDefinition, Long> {
    fun findAllByTenantId(tenantId: String): List<WorkflowDefinition>
    fun findByTenantIdAndDocumentTypeAndIsCurrentTrue(tenantId: String, documentType: String): WorkflowDefinition?
    fun findAllByTenantIdAndDocumentType(tenantId: String, documentType: String): List<WorkflowDefinition>
}

@Repository
interface ApprovalRequestRepository : JpaRepository<ApprovalRequest, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): ApprovalRequest?

    fun findByTenantIdAndDocumentTypeAndDocumentId(tenantId: String, documentType: String, documentId: Long): ApprovalRequest?

    @Query("""
        SELECT ar FROM ApprovalRequest ar
        JOIN ar.steps s
        WHERE ar.tenantId = :tenantId
          AND ar.status = 'PENDING'
          AND s.stepStatus = 'ACTIVE'
          AND s.decision IS NULL
          AND (s.approverId = :userId OR s.approverRole IN :roles)
        ORDER BY ar.createdAt DESC
    """)
    fun findPendingForUser(
        @Param("tenantId") tenantId: String,
        @Param("userId") userId: String,
        @Param("roles") roles: List<String>
    ): List<ApprovalRequest>

    @Query("""
        SELECT ar FROM ApprovalRequest ar
        WHERE ar.tenantId = :tenantId
          AND ar.requestedBy = :userId
        ORDER BY ar.createdAt DESC
    """)
    fun findSubmittedByUser(
        @Param("tenantId") tenantId: String,
        @Param("userId") userId: String,
        pageable: Pageable
    ): Page<ApprovalRequest>

    @Query("""
        SELECT COUNT(ar) FROM ApprovalRequest ar
        JOIN ar.steps s
        WHERE ar.tenantId = :tenantId
          AND ar.status = 'PENDING'
          AND s.stepStatus = 'ACTIVE'
          AND s.decision IS NULL
          AND (s.approverId = :userId OR s.approverRole IN :roles)
    """)
    fun countPendingForUser(
        @Param("tenantId") tenantId: String,
        @Param("userId") userId: String,
        @Param("roles") roles: List<String>
    ): Long
}

@Repository
interface ApprovalStepRepository : JpaRepository<ApprovalStep, Long>

@Repository
interface ApprovalDelegationRepository : JpaRepository<ApprovalDelegation, Long> {
    fun findAllByTenantIdAndFromUserIdAndDelegationActiveTrue(tenantId: String, fromUserId: String): List<ApprovalDelegation>
    fun findAllByTenantIdAndToUserIdAndDelegationActiveTrue(tenantId: String, toUserId: String): List<ApprovalDelegation>
    fun findAllByTenantIdAndFromUserId(tenantId: String, fromUserId: String): List<ApprovalDelegation>
}

@Repository
interface ApprovalCommentRepository : JpaRepository<ApprovalComment, Long>
