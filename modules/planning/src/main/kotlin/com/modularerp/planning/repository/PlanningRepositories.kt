package com.modularerp.planning.repository

import com.modularerp.planning.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.Optional

interface MrpRunRepository : JpaRepository<MrpRun, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<MrpRun>

    @Query("SELECT m FROM MrpRun m WHERE m.tenantId = :tenantId AND m.active = true ORDER BY m.createdAt DESC")
    fun findRecent(tenantId: String, pageable: Pageable): Page<MrpRun>
}

interface ProductionScheduleRepository : JpaRepository<ProductionSchedule, Long> {
    @Query("""
        SELECT ps FROM ProductionSchedule ps WHERE ps.tenantId = :tenantId AND ps.active = true
        AND ps.plantCode = :plantCode
        AND ps.scheduleDate BETWEEN :fromDate AND :toDate
        AND (:workCenterCode IS NULL OR ps.workCenterCode = :workCenterCode)
        ORDER BY ps.scheduleDate, ps.sequenceNo
    """)
    fun findSchedule(tenantId: String, plantCode: String, fromDate: LocalDate, toDate: LocalDate,
                     workCenterCode: String?): List<ProductionSchedule>
}

interface CapacityPlanRepository : JpaRepository<CapacityPlan, Long> {
    @Query("""
        SELECT cp FROM CapacityPlan cp WHERE cp.tenantId = :tenantId AND cp.active = true
        AND cp.plantCode = :plantCode
        AND cp.planDate BETWEEN :fromDate AND :toDate
        ORDER BY cp.workCenterCode, cp.planDate
    """)
    fun findCapacity(tenantId: String, plantCode: String, fromDate: LocalDate, toDate: LocalDate): List<CapacityPlan>

    fun findByTenantIdAndWorkCenterCodeAndPlanDate(tenantId: String, workCenterCode: String, planDate: LocalDate): Optional<CapacityPlan>
}
