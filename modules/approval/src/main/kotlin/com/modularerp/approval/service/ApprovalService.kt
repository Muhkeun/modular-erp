package com.modularerp.approval.service

import com.modularerp.approval.domain.*
import com.modularerp.approval.dto.*
import com.modularerp.approval.repository.*
import com.modularerp.core.exception.BusinessException
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.core.port.DocumentApprovalCallback
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class ApprovalService(
    private val approvalRequestRepo: ApprovalRequestRepository,
    private val approvalStepRepo: ApprovalStepRepository,
    private val delegationRepo: ApprovalDelegationRepository,
    private val workflowRepo: WorkflowDefinitionRepository,
    private val callbacks: List<DocumentApprovalCallback> = emptyList()
) {

    /**
     * Submit a document for approval.
     * Finds the active workflow for the document type, creates an approval request
     * with steps from the workflow, and sets the first step to ACTIVE.
     */
    @Transactional
    fun submitForApproval(
        documentType: String,
        documentId: Long,
        documentNo: String,
        submittedBy: String,
        tenantId: String
    ): Long {
        // Check if there's already an active approval for this document
        val existing = approvalRequestRepo.findByTenantIdAndDocumentTypeAndDocumentId(tenantId, documentType, documentId)
        if (existing != null && existing.status == ApprovalStatus.PENDING) {
            throw BusinessException("APPROVAL_ALREADY_PENDING", "An approval request is already pending for this document")
        }

        // Find active workflow for this document type; if none configured, skip approval workflow
        val workflow = workflowRepo.findByTenantIdAndDocumentTypeAndIsCurrentTrue(tenantId, documentType)
            ?: return -1L

        val request = ApprovalRequest(
            documentType = documentType,
            documentId = documentId,
            documentNo = documentNo,
            requestedBy = submittedBy
        ).apply { assignTenant(tenantId) }

        // Create steps from workflow definition
        workflow.steps.sortedBy { it.stepOrder }.forEach { wfStep ->
            request.addStep(
                approverRole = wfStep.approverValue,
                approverId = if (wfStep.approverType == ApproverType.SPECIFIC_USER) wfStep.approverValue else null,
                stepOrder = wfStep.stepOrder
            )
        }

        request.submit()
        val saved = approvalRequestRepo.save(request)
        return saved.id
    }

    /**
     * Process an approval action on a specific step.
     */
    @Transactional
    fun processApproval(
        approvalRequestId: Long,
        stepId: Long,
        action: String,
        comment: String?,
        actionBy: String
    ): ApprovalRequestResponse {
        val tenantId = TenantContext.getTenantId()
        val request = approvalRequestRepo.findByTenantIdAndId(tenantId, approvalRequestId)
            ?: throw EntityNotFoundException("ApprovalRequest", approvalRequestId)

        val step = request.steps.find { it.id == stepId }
            ?: throw EntityNotFoundException("ApprovalStep", stepId)

        // Check if user is authorized (direct or via delegation)
        val effectiveUser = resolveEffectiveApprover(actionBy, step, tenantId)

        when (action.uppercase()) {
            "APPROVE" -> {
                request.approve(effectiveUser, comment)
                if (comment != null) {
                    request.addComment(effectiveUser, comment, step.stepOrder)
                }
                // If fully approved, callback to source document
                if (request.status == ApprovalStatus.APPROVED) {
                    notifyCallbacks(request.documentType, request.documentId, true, tenantId)
                }
            }
            "REJECT" -> {
                request.reject(effectiveUser, comment)
                if (comment != null) {
                    request.addComment(effectiveUser, comment, step.stepOrder)
                }
                notifyCallbacks(request.documentType, request.documentId, false, tenantId)
            }
            "RETURN" -> {
                request.returnToSubmitter(effectiveUser, comment)
                if (comment != null) {
                    request.addComment(effectiveUser, comment, step.stepOrder)
                }
                notifyCallbacks(request.documentType, request.documentId, false, tenantId)
            }
            else -> throw BusinessException("INVALID_ACTION", "Invalid approval action: $action")
        }

        return ApprovalRequestResponse.from(approvalRequestRepo.save(request))
    }

    /**
     * Get pending approvals for a user (including delegated ones).
     */
    fun getMyPendingApprovals(userId: String, tenantId: String): List<ApprovalRequestResponse> {
        // Get user's effective roles (simplified: use userId as role for now)
        val roles = getUserRoles(userId, tenantId)
        val requests = approvalRequestRepo.findPendingForUser(tenantId, userId, roles)
        return requests.map(ApprovalRequestResponse::from)
    }

    /**
     * Get requests submitted by a user.
     */
    fun getMySubmittedRequests(userId: String, tenantId: String): List<ApprovalRequestResponse> {
        val page = approvalRequestRepo.findSubmittedByUser(tenantId, userId, PageRequest.of(0, 50))
        return page.content.map(ApprovalRequestResponse::from)
    }

    /**
     * Get a single approval request by ID.
     */
    fun getById(id: Long): ApprovalRequestResponse {
        val tenantId = TenantContext.getTenantId()
        val request = approvalRequestRepo.findByTenantIdAndId(tenantId, id)
            ?: throw EntityNotFoundException("ApprovalRequest", id)
        return ApprovalRequestResponse.from(request)
    }

    /**
     * Get approval status for a document.
     */
    fun getApprovalStatus(documentType: String, documentId: Long, tenantId: String): String? {
        val request = approvalRequestRepo.findByTenantIdAndDocumentTypeAndDocumentId(tenantId, documentType, documentId)
        return request?.status?.name
    }

    /**
     * Create a delegation record.
     */
    @Transactional
    fun createDelegation(
        fromUserId: String,
        toUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        reason: String?,
        tenantId: String
    ): DelegationResponse {
        require(!startDate.isAfter(endDate)) { "Start date must not be after end date" }

        val delegation = ApprovalDelegation(
            fromUserId = fromUserId,
            toUserId = toUserId,
            startDate = startDate,
            endDate = endDate,
            reason = reason
        ).apply { assignTenant(tenantId) }

        return DelegationResponse.from(delegationRepo.save(delegation))
    }

    /**
     * List delegations for a user.
     */
    fun getMyDelegations(userId: String, tenantId: String): List<DelegationResponse> {
        return delegationRepo.findAllByTenantIdAndFromUserId(tenantId, userId)
            .map(DelegationResponse::from)
    }

    /**
     * Deactivate a delegation.
     */
    @Transactional
    fun deactivateDelegation(delegationId: Long) {
        val delegation = delegationRepo.findById(delegationId)
            .orElseThrow { EntityNotFoundException("ApprovalDelegation", delegationId) }
        delegation.deactivateDelegation()
        delegationRepo.save(delegation)
    }

    /**
     * Get dashboard data: pending count, submitted count, recent actions.
     */
    fun getDashboard(userId: String, tenantId: String): ApprovalDashboardResponse {
        val roles = getUserRoles(userId, tenantId)
        val pendingCount = approvalRequestRepo.countPendingForUser(tenantId, userId, roles)
        val submitted = approvalRequestRepo.findSubmittedByUser(tenantId, userId, PageRequest.of(0, 5))
        return ApprovalDashboardResponse(
            pendingCount = pendingCount,
            submittedCount = submitted.totalElements,
            recentActions = submitted.content.map(ApprovalRequestResponse::from)
        )
    }

    /**
     * Add a comment to an approval request.
     */
    @Transactional
    fun addComment(requestId: Long, commentBy: String, commentText: String, stepSequence: Int?): ApprovalRequestResponse {
        val tenantId = TenantContext.getTenantId()
        val request = approvalRequestRepo.findByTenantIdAndId(tenantId, requestId)
            ?: throw EntityNotFoundException("ApprovalRequest", requestId)
        request.addComment(commentBy, commentText, stepSequence)
        request.comments.last().assignTenant(tenantId)
        return ApprovalRequestResponse.from(approvalRequestRepo.save(request))
    }

    // ── Private helpers ──

    private fun resolveEffectiveApprover(userId: String, step: ApprovalStep, tenantId: String): String {
        // Check if the user is the direct approver
        if (step.approverId == userId || step.approverRole == userId) {
            return userId
        }
        // Check delegations
        val delegations = delegationRepo.findAllByTenantIdAndToUserIdAndDelegationActiveTrue(tenantId, userId)
        val today = LocalDate.now()
        val validDelegation = delegations.find { d ->
            d.isEffective(today) &&
            (step.approverId == d.fromUserId || step.approverRole == d.fromUserId)
        }
        if (validDelegation != null) {
            return userId // delegated user acts on behalf
        }
        // For role-based approval, allow if user has the role
        // (simplified: accept any authenticated user for now)
        return userId
    }

    private fun getUserRoles(userId: String, tenantId: String): List<String> {
        // Simplified: return userId as a role. In production, query user-role mapping.
        return listOf(userId, "ROLE_USER")
    }

    private fun notifyCallbacks(documentType: String, documentId: Long, approved: Boolean, tenantId: String) {
        callbacks.filter { it.supportedDocumentTypes().contains(documentType) }
            .forEach { it.onApprovalCompleted(documentType, documentId, approved, tenantId) }
    }
}
