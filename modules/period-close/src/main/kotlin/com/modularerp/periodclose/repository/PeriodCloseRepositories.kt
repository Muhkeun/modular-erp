package com.modularerp.periodclose.repository

import com.modularerp.periodclose.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface FiscalPeriodRepository : JpaRepository<FiscalPeriod, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<FiscalPeriod>

    @Query("""
        SELECT fp FROM FiscalPeriod fp WHERE fp.tenantId = :tenantId AND fp.active = true
        AND (:fiscalYear IS NULL OR fp.fiscalYear = :fiscalYear)
        AND (:status IS NULL OR fp.status = :status)
        ORDER BY fp.fiscalYear DESC, fp.period ASC
    """)
    fun search(tenantId: String, fiscalYear: Int?, status: FiscalPeriodStatus?, pageable: Pageable): Page<FiscalPeriod>

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.tenantId = :tenantId AND fp.fiscalYear = :fiscalYear AND fp.active = true ORDER BY fp.period")
    fun findByFiscalYear(tenantId: String, fiscalYear: Int): List<FiscalPeriod>
}

interface PeriodCloseTaskRepository : JpaRepository<PeriodCloseTask, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<PeriodCloseTask>

    @Query("""
        SELECT t FROM PeriodCloseTask t WHERE t.tenantId = :tenantId AND t.active = true
        AND t.fiscalPeriod.id = :periodId
        ORDER BY t.sequence ASC
    """)
    fun findByPeriod(tenantId: String, periodId: Long): List<PeriodCloseTask>
}

interface ClosingEntryRepository : JpaRepository<ClosingEntry, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<ClosingEntry>

    @Query("""
        SELECT ce FROM ClosingEntry ce WHERE ce.tenantId = :tenantId AND ce.active = true
        AND ce.fiscalPeriod.id = :periodId
        ORDER BY ce.documentNo ASC
    """)
    fun findByPeriod(tenantId: String, periodId: Long, pageable: Pageable): Page<ClosingEntry>
}
