package com.modularerp.account.dto

import com.modularerp.account.domain.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.LocalDate

data class CreateJeRequest(
    @field:NotBlank val companyCode: String,
    val postingDate: LocalDate = LocalDate.now(),
    val entryType: JournalEntryType = JournalEntryType.MANUAL,
    val referenceDocNo: String? = null,
    val referenceDocType: String? = null,
    val description: String? = null,
    val currencyCode: String = "KRW",
    @field:NotEmpty val lines: List<JeLineInput>
)

data class JeLineInput(
    val accountCode: String, val accountName: String,
    val debitAmount: BigDecimal = BigDecimal.ZERO,
    val creditAmount: BigDecimal = BigDecimal.ZERO,
    val costCenter: String? = null, val description: String? = null
)

data class JeResponse(
    val id: Long, val documentNo: String, val companyCode: String,
    val postingDate: LocalDate, val documentDate: LocalDate,
    val entryType: JournalEntryType, val status: JeStatus,
    val referenceDocNo: String?, val description: String?, val currencyCode: String,
    val totalDebit: BigDecimal, val totalCredit: BigDecimal, val isBalanced: Boolean,
    val lines: List<JeLineResponse>
)

data class JeLineResponse(
    val id: Long, val lineNo: Int, val accountCode: String, val accountName: String,
    val debitAmount: BigDecimal, val creditAmount: BigDecimal,
    val costCenter: String?, val description: String?
)

fun JournalEntry.toResponse() = JeResponse(
    id = id, documentNo = documentNo, companyCode = companyCode,
    postingDate = postingDate, documentDate = documentDate,
    entryType = entryType, status = status, referenceDocNo = referenceDocNo,
    description = description, currencyCode = currencyCode,
    totalDebit = totalDebit, totalCredit = totalCredit, isBalanced = isBalanced,
    lines = lines.map {
        JeLineResponse(it.id, it.lineNo, it.accountCode, it.accountName,
            it.debitAmount, it.creditAmount, it.costCenter, it.description)
    }
)
