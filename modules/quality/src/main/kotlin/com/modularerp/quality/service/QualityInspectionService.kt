package com.modularerp.quality.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.quality.domain.*
import com.modularerp.quality.dto.*
import com.modularerp.quality.repository.QualityInspectionRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QualityInspectionService(
    private val qiRepository: QualityInspectionRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {
    fun getById(id: Long) = find(id).toResponse()

    fun search(status: QiStatus?, type: InspectionType?, pageable: Pageable): Page<QiResponse> =
        qiRepository.search(TenantContext.getTenantId(), status, type, pageable).map { it.toResponse() }

    @Transactional
    fun create(req: CreateQiRequest): QiResponse {
        val tenantId = TenantContext.getTenantId()
        val qi = QualityInspection(
            documentNo = docNumberGenerator.next("QI", "QI"),
            inspectionType = req.inspectionType, referenceDocNo = req.referenceDocNo,
            itemCode = req.itemCode, itemName = req.itemName, plantCode = req.plantCode,
            inspectedQuantity = req.inspectedQuantity, inspectionDate = req.inspectionDate,
            inspectorId = TenantContext.getUserId()
        ).apply { assignTenant(tenantId) }
        return qiRepository.save(qi).toResponse()
    }

    @Transactional
    fun complete(id: Long, req: CompleteQiRequest): QiResponse {
        val qi = find(id)
        qi.complete(req.acceptedQuantity, req.rejectedQuantity, req.result, req.remarks)
        return qiRepository.save(qi).toResponse()
    }

    private fun find(id: Long) = qiRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
        .orElseThrow { EntityNotFoundException("QualityInspection", id) }
}
