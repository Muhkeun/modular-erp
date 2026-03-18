package com.modularerp.supplychain.repository

import com.modularerp.supplychain.domain.SupplierEvaluation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface SupplierEvaluationRepository : JpaRepository<SupplierEvaluation, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<SupplierEvaluation>

    @Query("""
        SELECT se FROM SupplierEvaluation se WHERE se.tenantId = :tenantId AND se.active = true
        AND (:vendorCode IS NULL OR se.vendorCode = :vendorCode)
        AND (:period IS NULL OR se.evaluationPeriod = :period)
        ORDER BY se.evaluationDate DESC
    """)
    fun search(tenantId: String, vendorCode: String?, period: String?, pageable: Pageable): Page<SupplierEvaluation>
}
