package com.modularerp.supplychain.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.security.tenant.TenantContext
import com.modularerp.supplychain.domain.SupplierEvaluation
import com.modularerp.supplychain.dto.*
import com.modularerp.supplychain.repository.SupplierEvaluationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SupplierEvaluationService(private val repo: SupplierEvaluationRepository) {

    fun getById(id: Long) = repo.findByTenantIdAndId(TenantContext.getTenantId(), id)
        .orElseThrow { EntityNotFoundException("SupplierEvaluation", id) }.toResponse()

    fun search(vendorCode: String?, period: String?, pageable: Pageable): Page<EvaluationResponse> =
        repo.search(TenantContext.getTenantId(), vendorCode, period, pageable).map { it.toResponse() }

    @Transactional
    fun create(req: CreateEvaluationRequest): EvaluationResponse {
        val eval = SupplierEvaluation(
            vendorCode = req.vendorCode, vendorName = req.vendorName, evaluationPeriod = req.evaluationPeriod,
            qualityScore = req.qualityScore, deliveryScore = req.deliveryScore,
            priceScore = req.priceScore, serviceScore = req.serviceScore, remarks = req.remarks
        ).apply {
            assignTenant(TenantContext.getTenantId())
            calculateGrade()
        }
        return repo.save(eval).toResponse()
    }
}
