package com.modularerp.budget.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "budget_items")
class BudgetItem(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_period_id", nullable = false)
    val budgetPeriod: BudgetPeriod,

    @Column(nullable = false, length = 20)
    var accountCode: String,

    @Column(nullable = false, length = 200)
    var accountName: String,

    @Column(length = 20)
    var departmentCode: String? = null,

    @Column(length = 20)
    var plantCode: String? = null,

    @Column(nullable = false, precision = 19, scale = 4)
    var budgetAmount: BigDecimal,

    @Column(nullable = false, precision = 19, scale = 4)
    var revisedAmount: BigDecimal,

    @Column(nullable = false, precision = 19, scale = 4)
    var actualAmount: BigDecimal = BigDecimal.ZERO,

    @Column(length = 3)
    var currency: String = "KRW",

    @Column(length = 500)
    var notes: String? = null

) : TenantEntity() {

    val remainingAmount: BigDecimal
        get() = revisedAmount.subtract(actualAmount)
}
