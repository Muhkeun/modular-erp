package com.modularerp.periodclose.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "closing_entries")
class ClosingEntry(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    val fiscalPeriod: FiscalPeriod,

    @Column(nullable = false, length = 30)
    var documentNo: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var entryType: ClosingEntryType,

    @Column(nullable = false, length = 500)
    var description: String,

    @Column(nullable = false, length = 20)
    var debitAccount: String,

    @Column(nullable = false, length = 20)
    var creditAccount: String,

    @Column(nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal,

    @Column(nullable = false)
    var posted: Boolean = false,

    var reversalDate: LocalDate? = null

) : TenantEntity() {

    fun post() {
        check(!posted) { "Entry already posted" }
        posted = true
    }
}

enum class ClosingEntryType {
    ACCRUAL, DEFERRAL, DEPRECIATION, REVALUATION, RECLASSIFICATION, CLOSING
}
