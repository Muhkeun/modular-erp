package com.modularerp.periodclose.dto

import com.modularerp.periodclose.domain.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

// --- Fiscal Period DTOs ---

data class GenerateFiscalYearRequest(
    @field:NotNull val fiscalYear: Int
)

data class FiscalPeriodResponse(
    val id: Long,
    val fiscalYear: Int,
    val period: Int,
    val periodName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: FiscalPeriodStatus,
    val closedBy: String?,
    val closedAt: LocalDateTime?
)

fun FiscalPeriod.toResponse() = FiscalPeriodResponse(
    id = id, fiscalYear = fiscalYear, period = period, periodName = periodName,
    startDate = startDate, endDate = endDate, status = status,
    closedBy = closedBy, closedAt = closedAt
)

// --- Period Close Task DTOs ---

data class PeriodCloseTaskResponse(
    val id: Long,
    val fiscalPeriodId: Long,
    val taskType: CloseTaskType,
    val taskName: String,
    val sequence: Int,
    val status: CloseTaskStatus,
    val executedBy: String?,
    val executedAt: LocalDateTime?,
    val errorMessage: String?,
    val notes: String?
)

fun PeriodCloseTask.toResponse() = PeriodCloseTaskResponse(
    id = id, fiscalPeriodId = fiscalPeriod.id, taskType = taskType,
    taskName = taskName, sequence = sequence, status = status,
    executedBy = executedBy, executedAt = executedAt,
    errorMessage = errorMessage, notes = notes
)

// --- Closing Entry DTOs ---

data class CreateClosingEntryRequest(
    @field:NotNull val fiscalPeriodId: Long,
    @field:NotNull val entryType: ClosingEntryType,
    @field:NotBlank val description: String,
    @field:NotBlank val debitAccount: String,
    @field:NotBlank val creditAccount: String,
    @field:NotNull val amount: BigDecimal,
    val reversalDate: LocalDate? = null
)

data class ClosingEntryResponse(
    val id: Long,
    val fiscalPeriodId: Long,
    val documentNo: String,
    val entryType: ClosingEntryType,
    val description: String,
    val debitAccount: String,
    val creditAccount: String,
    val amount: BigDecimal,
    val posted: Boolean,
    val reversalDate: LocalDate?
)

fun ClosingEntry.toResponse() = ClosingEntryResponse(
    id = id, fiscalPeriodId = fiscalPeriod.id, documentNo = documentNo,
    entryType = entryType, description = description,
    debitAccount = debitAccount, creditAccount = creditAccount,
    amount = amount, posted = posted, reversalDate = reversalDate
)
