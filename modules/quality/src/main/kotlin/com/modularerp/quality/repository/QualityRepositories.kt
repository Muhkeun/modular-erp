package com.modularerp.quality.repository

import com.modularerp.quality.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface QualityInspectionRepository : JpaRepository<QualityInspection, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<QualityInspection>

    @Query("""
        SELECT qi FROM QualityInspection qi WHERE qi.tenantId = :tenantId AND qi.active = true
        AND (:status IS NULL OR qi.status = :status)
        AND (:inspectionType IS NULL OR qi.inspectionType = :inspectionType)
        ORDER BY qi.createdAt DESC
    """)
    fun search(tenantId: String, status: QiStatus?, inspectionType: InspectionType?, pageable: Pageable): Page<QualityInspection>
}
