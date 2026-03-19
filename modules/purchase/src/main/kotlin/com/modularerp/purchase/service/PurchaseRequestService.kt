package com.modularerp.purchase.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.core.port.ApprovalPort
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.purchase.domain.*
import com.modularerp.purchase.dto.*
import com.modularerp.purchase.repository.PurchaseRequestRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PurchaseRequestService(
    private val prRepository: PurchaseRequestRepository,
    private val docNumberGenerator: DocumentNumberGenerator,
    private val approvalPort: ApprovalPort? = null
) {

    fun getById(id: Long): PrResponse {
        val pr = findPr(id)
        return pr.toResponse()
    }

    fun search(status: PrStatus?, companyCode: String?, documentNo: String?, pageable: Pageable): Page<PrResponse> {
        val tenantId = TenantContext.getTenantId()
        return prRepository.search(tenantId, status, companyCode, documentNo, pageable).map { it.toResponse() }
    }

    @Transactional
    fun create(request: CreatePrRequest): PrResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("PR", "PR")

        val pr = PurchaseRequest(
            documentNo = docNo,
            companyCode = request.companyCode,
            plantCode = request.plantCode,
            departmentCode = request.departmentCode,
            prType = request.prType,
            deliveryDate = request.deliveryDate,
            description = request.description,
            requestedBy = TenantContext.getUserId()
        ).apply { assignTenant(tenantId) }

        request.lines.forEach { line ->
            pr.addLine(
                itemCode = line.itemCode, itemName = line.itemName,
                quantity = line.quantity, unitOfMeasure = line.unitOfMeasure,
                unitPrice = line.unitPrice, specification = line.specification, remark = line.remark
            ).assignTenant(tenantId)
        }

        return prRepository.save(pr).toResponse()
    }

    @Transactional
    fun submit(id: Long): PrResponse {
        val pr = findPr(id)
        pr.submit()
        val saved = prRepository.save(pr)

        // Submit for workflow approval if ApprovalPort is available
        try {
            approvalPort?.submitForApproval(
                documentType = "PR",
                documentId = saved.id,
                documentNo = saved.documentNo,
                submittedBy = TenantContext.getUserId() ?: "anonymous",
                tenantId = TenantContext.getTenantId()
            )
        } catch (_: Exception) {
            // If no workflow is configured, continue without approval workflow
        }

        return saved.toResponse()
    }

    @Transactional
    fun approve(id: Long): PrResponse {
        val pr = findPr(id)
        pr.approve()
        return prRepository.save(pr).toResponse()
    }

    @Transactional
    fun reject(id: Long): PrResponse {
        val pr = findPr(id)
        pr.reject()
        return prRepository.save(pr).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        val pr = findPr(id)
        pr.deactivate()
        prRepository.save(pr)
    }

    private fun findPr(id: Long): PurchaseRequest {
        val tenantId = TenantContext.getTenantId()
        return prRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow { EntityNotFoundException("PurchaseRequest", id) }
    }
}
