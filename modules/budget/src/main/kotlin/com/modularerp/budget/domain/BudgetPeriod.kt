package com.modularerp.budget.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "budget_periods")
class BudgetPeriod(

    @Column(nullable = false)
    var fiscalYear: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var periodType: BudgetPeriodType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: BudgetPeriodStatus = BudgetPeriodStatus.DRAFT,

    @Column(nullable = false)
    var startDate: LocalDate,

    @Column(nullable = false)
    var endDate: LocalDate,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "budgetPeriod", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<BudgetItem> = mutableListOf()

    fun approve() {
        check(status == BudgetPeriodStatus.DRAFT) { "Can only approve from DRAFT" }
        status = BudgetPeriodStatus.APPROVED
    }

    fun activatePeriod() {
        check(status == BudgetPeriodStatus.APPROVED) { "Can only activate from APPROVED" }
        status = BudgetPeriodStatus.ACTIVE
    }

    fun close() {
        check(status == BudgetPeriodStatus.ACTIVE) { "Can only close from ACTIVE" }
        status = BudgetPeriodStatus.CLOSED
    }
}

enum class BudgetPeriodType { ANNUAL, QUARTERLY, MONTHLY }
enum class BudgetPeriodStatus { DRAFT, APPROVED, ACTIVE, CLOSED }
