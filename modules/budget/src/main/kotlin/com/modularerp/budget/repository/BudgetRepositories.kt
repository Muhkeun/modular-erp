package com.modularerp.budget.repository

import com.modularerp.budget.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface BudgetPeriodRepository : JpaRepository<BudgetPeriod, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<BudgetPeriod>

    @Query("""
        SELECT bp FROM BudgetPeriod bp WHERE bp.tenantId = :tenantId AND bp.active = true
        AND (:status IS NULL OR bp.status = :status)
        AND (:fiscalYear IS NULL OR bp.fiscalYear = :fiscalYear)
        ORDER BY bp.fiscalYear DESC, bp.startDate ASC
    """)
    fun search(tenantId: String, status: BudgetPeriodStatus?, fiscalYear: Int?, pageable: Pageable): Page<BudgetPeriod>
}

interface BudgetItemRepository : JpaRepository<BudgetItem, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<BudgetItem>

    @Query("""
        SELECT bi FROM BudgetItem bi WHERE bi.tenantId = :tenantId AND bi.active = true
        AND bi.budgetPeriod.id = :periodId
        ORDER BY bi.accountCode ASC
    """)
    fun findByPeriod(tenantId: String, periodId: Long, pageable: Pageable): Page<BudgetItem>

    @Query("""
        SELECT bi FROM BudgetItem bi WHERE bi.tenantId = :tenantId AND bi.active = true
        AND bi.accountCode = :accountCode AND bi.budgetPeriod.status = 'ACTIVE'
    """)
    fun findActiveByAccountCode(tenantId: String, accountCode: String): List<BudgetItem>
}

interface BudgetTransferRepository : JpaRepository<BudgetTransfer, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<BudgetTransfer>

    @Query("""
        SELECT bt FROM BudgetTransfer bt WHERE bt.tenantId = :tenantId AND bt.active = true
        AND (:status IS NULL OR bt.status = :status)
        ORDER BY bt.transferDate DESC
    """)
    fun search(tenantId: String, status: BudgetTransferStatus?, pageable: Pageable): Page<BudgetTransfer>
}
