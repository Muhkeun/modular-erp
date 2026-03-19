package com.modularerp.costing.repository

import com.modularerp.costing.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.Optional

interface CostCenterRepository : JpaRepository<CostCenter, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<CostCenter>

    @Query("""
        SELECT cc FROM CostCenter cc WHERE cc.tenantId = :tenantId AND cc.active = true
        AND (:status IS NULL OR cc.status = :status)
        AND (:costCenterCode IS NULL OR cc.costCenterCode LIKE %:costCenterCode%)
        ORDER BY cc.costCenterCode ASC
    """)
    fun search(tenantId: String, status: CostCenterStatus?, costCenterCode: String?,
               pageable: Pageable): Page<CostCenter>
}

interface StandardCostRepository : JpaRepository<StandardCost, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<StandardCost>

    @Query("""
        SELECT sc FROM StandardCost sc WHERE sc.tenantId = :tenantId AND sc.active = true
        AND (:itemCode IS NULL OR sc.itemCode = :itemCode)
        AND (:costType IS NULL OR sc.costType = :costType)
        ORDER BY sc.effectiveFrom DESC
    """)
    fun search(tenantId: String, itemCode: String?, costType: CostType?,
               pageable: Pageable): Page<StandardCost>

    @Query("""
        SELECT sc FROM StandardCost sc WHERE sc.tenantId = :tenantId AND sc.active = true
        AND sc.itemCode = :itemCode AND sc.effectiveFrom <= :date
        AND (sc.effectiveTo IS NULL OR sc.effectiveTo >= :date)
    """)
    fun findEffective(tenantId: String, itemCode: String, date: LocalDate): List<StandardCost>
}

interface CostAllocationRepository : JpaRepository<CostAllocation, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<CostAllocation>

    @Query("""
        SELECT ca FROM CostAllocation ca WHERE ca.tenantId = :tenantId AND ca.active = true
        AND (:status IS NULL OR ca.status = :status)
        AND (:fiscalYear IS NULL OR ca.fiscalYear = :fiscalYear)
        ORDER BY ca.allocationDate DESC
    """)
    fun search(tenantId: String, status: CostAllocationStatus?, fiscalYear: Int?,
               pageable: Pageable): Page<CostAllocation>
}

interface ProductCostRepository : JpaRepository<ProductCost, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<ProductCost>

    @Query("""
        SELECT pc FROM ProductCost pc WHERE pc.tenantId = :tenantId AND pc.active = true
        AND (:itemCode IS NULL OR pc.itemCode = :itemCode)
        AND (:fiscalYear IS NULL OR pc.fiscalYear = :fiscalYear)
        ORDER BY pc.fiscalYear DESC, pc.period DESC
    """)
    fun search(tenantId: String, itemCode: String?, fiscalYear: Int?,
               pageable: Pageable): Page<ProductCost>
}
