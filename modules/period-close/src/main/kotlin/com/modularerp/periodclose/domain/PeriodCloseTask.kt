package com.modularerp.periodclose.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "period_close_tasks")
class PeriodCloseTask(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    val fiscalPeriod: FiscalPeriod,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var taskType: CloseTaskType,

    @Column(nullable = false, length = 200)
    var taskName: String,

    @Column(nullable = false)
    var sequence: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CloseTaskStatus = CloseTaskStatus.PENDING,

    @Column(length = 100)
    var executedBy: String? = null,

    var executedAt: LocalDateTime? = null,

    @Column(length = 1000)
    var errorMessage: String? = null,

    @Column(length = 500)
    var notes: String? = null

) : TenantEntity() {

    fun start(userId: String) {
        check(status == CloseTaskStatus.PENDING) { "Can only start PENDING tasks" }
        status = CloseTaskStatus.IN_PROGRESS
        executedBy = userId
        executedAt = LocalDateTime.now()
    }

    fun complete() {
        check(status == CloseTaskStatus.IN_PROGRESS) { "Can only complete IN_PROGRESS tasks" }
        status = CloseTaskStatus.COMPLETED
    }

    fun fail(error: String) {
        status = CloseTaskStatus.FAILED
        errorMessage = error
    }

    fun skip() {
        check(status == CloseTaskStatus.PENDING) { "Can only skip PENDING tasks" }
        status = CloseTaskStatus.SKIPPED
    }
}

enum class CloseTaskType {
    AP_CLOSE, AR_CLOSE, INVENTORY_CLOSE, DEPRECIATION_RUN,
    COST_ALLOCATION, EXCHANGE_RATE_REVALUATION, ACCRUAL_POSTING,
    RECONCILIATION, FINANCIAL_REPORT
}

enum class CloseTaskStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED }
