package com.modularerp.budget.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "budget_transfers")
class BudgetTransfer(

    @Column(nullable = false, length = 30)
    var documentNo: String = "",

    @Column(nullable = false)
    var transferDate: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_budget_item_id", nullable = false)
    val fromBudgetItem: BudgetItem,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_budget_item_id", nullable = false)
    val toBudgetItem: BudgetItem,

    @Column(nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal,

    @Column(nullable = false, length = 500)
    var reason: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: BudgetTransferStatus = BudgetTransferStatus.DRAFT,

    @Column(length = 100)
    var approvedBy: String? = null,

    var approvedAt: LocalDateTime? = null

) : TenantEntity() {

    fun approve(approver: String) {
        check(status == BudgetTransferStatus.DRAFT) { "Can only approve from DRAFT" }
        status = BudgetTransferStatus.APPROVED
        approvedBy = approver
        approvedAt = LocalDateTime.now()
    }

    fun complete() {
        check(status == BudgetTransferStatus.APPROVED) { "Can only complete from APPROVED" }
        status = BudgetTransferStatus.COMPLETED
    }
}

enum class BudgetTransferStatus { DRAFT, APPROVED, COMPLETED }
