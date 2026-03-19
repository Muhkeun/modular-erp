package com.modularerp.core.port

/**
 * Cross-module approval port.
 * Allows any document module to submit documents for approval
 * without depending on the approval module directly.
 */
interface ApprovalPort {
    fun submitForApproval(
        documentType: String,
        documentId: Long,
        documentNo: String,
        submittedBy: String,
        tenantId: String
    ): Long

    fun getApprovalStatus(documentType: String, documentId: Long, tenantId: String): String?
}

/**
 * Callback interface for document modules to receive approval results.
 * Each module registers a callback bean for its document types.
 */
interface DocumentApprovalCallback {
    /** Document types this callback handles (e.g., "PR", "PO", "SO") */
    fun supportedDocumentTypes(): Set<String>

    /** Called when approval is completed (approved or rejected) */
    fun onApprovalCompleted(documentType: String, documentId: Long, approved: Boolean, tenantId: String)
}
