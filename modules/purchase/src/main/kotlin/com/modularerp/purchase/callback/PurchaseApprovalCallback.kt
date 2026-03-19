package com.modularerp.purchase.callback

import com.modularerp.core.port.DocumentApprovalCallback
import com.modularerp.purchase.repository.PurchaseRequestRepository
import com.modularerp.purchase.repository.PurchaseOrderRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PurchaseApprovalCallback(
    private val prRepository: PurchaseRequestRepository,
    private val poRepository: PurchaseOrderRepository
) : DocumentApprovalCallback {

    override fun supportedDocumentTypes(): Set<String> = setOf("PR", "PO")

    @Transactional
    override fun onApprovalCompleted(documentType: String, documentId: Long, approved: Boolean, tenantId: String) {
        when (documentType) {
            "PR" -> {
                val pr = prRepository.findByTenantIdAndId(tenantId, documentId).orElse(null) ?: return
                if (approved) pr.approve() else pr.reject()
                prRepository.save(pr)
            }
            "PO" -> {
                val po = poRepository.findByTenantIdAndId(tenantId, documentId).orElse(null) ?: return
                if (approved) po.approve() else po.reject()
                poRepository.save(po)
            }
        }
    }
}
