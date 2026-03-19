package com.modularerp.approval.adapter

import com.modularerp.approval.service.ApprovalService
import com.modularerp.core.port.ApprovalPort
import org.springframework.stereotype.Component

@Component
class ApprovalAdapter(
    private val approvalService: ApprovalService
) : ApprovalPort {

    override fun submitForApproval(
        documentType: String,
        documentId: Long,
        documentNo: String,
        submittedBy: String,
        tenantId: String
    ): Long {
        return approvalService.submitForApproval(documentType, documentId, documentNo, submittedBy, tenantId)
    }

    override fun getApprovalStatus(documentType: String, documentId: Long, tenantId: String): String? {
        return approvalService.getApprovalStatus(documentType, documentId, tenantId)
    }
}
