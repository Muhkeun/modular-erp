package com.modularerp.budget.dto

import com.modularerp.budget.domain.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

// --- Budget Period DTOs ---

data class CreateBudgetPeriodRequest(
    @field:NotNull val fiscalYear: Int,
    @field:NotNull val periodType: BudgetPeriodType,
    @field:NotNull val startDate: LocalDate,
    @field:NotNull val endDate: LocalDate,
    val description: String? = null
)

data class UpdateBudgetPeriodRequest(
    val description: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

data class BudgetPeriodResponse(
    val id: Long,
    val fiscalYear: Int,
    val periodType: BudgetPeriodType,
    val status: BudgetPeriodStatus,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val description: String?
)

fun BudgetPeriod.toResponse() = BudgetPeriodResponse(
    id = id, fiscalYear = fiscalYear, periodType = periodType,
    status = status, startDate = startDate, endDate = endDate,
    description = description
)

// --- Budget Item DTOs ---

data class CreateBudgetItemRequest(
    @field:NotNull val budgetPeriodId: Long,
    @field:NotBlank val accountCode: String,
    @field:NotBlank val accountName: String,
    val departmentCode: String? = null,
    val plantCode: String? = null,
    @field:NotNull val budgetAmount: BigDecimal,
    val revisedAmount: BigDecimal? = null,
    val currency: String = "KRW",
    val notes: String? = null
)

data class UpdateBudgetItemRequest(
    val accountName: String? = null,
    val budgetAmount: BigDecimal? = null,
    val revisedAmount: BigDecimal? = null,
    val notes: String? = null
)

data class BudgetItemResponse(
    val id: Long,
    val budgetPeriodId: Long,
    val accountCode: String,
    val accountName: String,
    val departmentCode: String?,
    val plantCode: String?,
    val budgetAmount: BigDecimal,
    val revisedAmount: BigDecimal,
    val actualAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val currency: String,
    val notes: String?
)

fun BudgetItem.toResponse() = BudgetItemResponse(
    id = id, budgetPeriodId = budgetPeriod.id,
    accountCode = accountCode, accountName = accountName,
    departmentCode = departmentCode, plantCode = plantCode,
    budgetAmount = budgetAmount, revisedAmount = revisedAmount,
    actualAmount = actualAmount, remainingAmount = remainingAmount,
    currency = currency, notes = notes
)

// --- Budget Transfer DTOs ---

data class CreateBudgetTransferRequest(
    @field:NotNull val fromBudgetItemId: Long,
    @field:NotNull val toBudgetItemId: Long,
    @field:NotNull val amount: BigDecimal,
    @field:NotBlank val reason: String,
    val transferDate: LocalDate = LocalDate.now()
)

data class BudgetTransferResponse(
    val id: Long,
    val documentNo: String,
    val transferDate: LocalDate,
    val fromBudgetItemId: Long,
    val toBudgetItemId: Long,
    val amount: BigDecimal,
    val reason: String,
    val status: BudgetTransferStatus,
    val approvedBy: String?,
    val approvedAt: LocalDateTime?
)

fun BudgetTransfer.toResponse() = BudgetTransferResponse(
    id = id, documentNo = documentNo, transferDate = transferDate,
    fromBudgetItemId = fromBudgetItem.id, toBudgetItemId = toBudgetItem.id,
    amount = amount, reason = reason, status = status,
    approvedBy = approvedBy, approvedAt = approvedAt
)

// --- Analysis DTO ---

data class BudgetAnalysisResponse(
    val accountCode: String,
    val accountName: String,
    val budgetAmount: BigDecimal,
    val revisedAmount: BigDecimal,
    val actualAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val utilizationRate: BigDecimal
)
