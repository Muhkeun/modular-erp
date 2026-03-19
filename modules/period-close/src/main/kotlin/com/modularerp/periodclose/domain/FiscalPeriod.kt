package com.modularerp.periodclose.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "fiscal_periods")
class FiscalPeriod(

    @Column(nullable = false)
    var fiscalYear: Int,

    @Column(nullable = false)
    var period: Int,

    @Column(nullable = false, length = 20)
    var periodName: String,

    @Column(nullable = false)
    var startDate: LocalDate,

    @Column(nullable = false)
    var endDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: FiscalPeriodStatus = FiscalPeriodStatus.OPEN,

    @Column(length = 100)
    var closedBy: String? = null,

    var closedAt: LocalDateTime? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "fiscalPeriod", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sequence ASC")
    val tasks: MutableList<PeriodCloseTask> = mutableListOf()

    fun softClose(userId: String) {
        check(status == FiscalPeriodStatus.OPEN) { "Can only soft-close from OPEN" }
        status = FiscalPeriodStatus.SOFT_CLOSE
        closedBy = userId
        closedAt = LocalDateTime.now()
    }

    fun hardClose(userId: String) {
        check(status == FiscalPeriodStatus.SOFT_CLOSE) { "Can only hard-close from SOFT_CLOSE" }
        status = FiscalPeriodStatus.HARD_CLOSE
        closedBy = userId
        closedAt = LocalDateTime.now()
    }

    fun reopen() {
        check(status == FiscalPeriodStatus.SOFT_CLOSE) { "Can only reopen from SOFT_CLOSE" }
        status = FiscalPeriodStatus.OPEN
        closedBy = null
        closedAt = null
    }
}

enum class FiscalPeriodStatus { OPEN, SOFT_CLOSE, HARD_CLOSE }
