package com.modularerp.account.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Journal Entry — the fundamental accounting record.
 * Every financial transaction (GR, Invoice, Payment) creates journal entries.
 */
@Entity
@Table(name = "journal_entries")
class JournalEntry(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(nullable = false)
    var postingDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var documentDate: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var entryType: JournalEntryType = JournalEntryType.MANUAL,

    @Column(length = 30)
    var referenceDocNo: String? = null,

    @Column(length = 20)
    var referenceDocType: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: JeStatus = JeStatus.DRAFT,

    @Column(length = 500)
    var description: String? = null,

    @Column(length = 3)
    var currencyCode: String = "KRW"

) : TenantEntity() {

    @OneToMany(mappedBy = "journalEntry", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("lineNo ASC")
    val lines: MutableList<JournalEntryLine> = mutableListOf()

    val totalDebit: BigDecimal get() = lines.filter { it.debitAmount > BigDecimal.ZERO }.sumOf { it.debitAmount }
    val totalCredit: BigDecimal get() = lines.filter { it.creditAmount > BigDecimal.ZERO }.sumOf { it.creditAmount }
    val isBalanced: Boolean get() = totalDebit.compareTo(totalCredit) == 0

    fun addDebitLine(accountCode: String, accountName: String, amount: BigDecimal,
                     costCenter: String? = null, description: String? = null): JournalEntryLine {
        val lineNo = (lines.maxOfOrNull { it.lineNo } ?: 0) + 1
        val line = JournalEntryLine(
            journalEntry = this, lineNo = lineNo, accountCode = accountCode, accountName = accountName,
            debitAmount = amount, creditAmount = BigDecimal.ZERO, costCenter = costCenter, description = description
        )
        lines.add(line)
        return line
    }

    fun addCreditLine(accountCode: String, accountName: String, amount: BigDecimal,
                      costCenter: String? = null, description: String? = null): JournalEntryLine {
        val lineNo = (lines.maxOfOrNull { it.lineNo } ?: 0) + 1
        val line = JournalEntryLine(
            journalEntry = this, lineNo = lineNo, accountCode = accountCode, accountName = accountName,
            debitAmount = BigDecimal.ZERO, creditAmount = amount, costCenter = costCenter, description = description
        )
        lines.add(line)
        return line
    }

    fun post() {
        check(status == JeStatus.DRAFT) { "Can only post from DRAFT" }
        check(isBalanced) { "Journal entry must be balanced (Debit=$totalDebit, Credit=$totalCredit)" }
        check(lines.size >= 2) { "At least two lines required" }
        status = JeStatus.POSTED
    }

    fun reverse() {
        check(status == JeStatus.POSTED) { "Can only reverse POSTED entries" }
        status = JeStatus.REVERSED
    }
}

@Entity
@Table(name = "journal_entry_lines")
class JournalEntryLine(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    val journalEntry: JournalEntry,

    val lineNo: Int,

    @Column(nullable = false, length = 20)
    var accountCode: String,

    @Column(nullable = false, length = 200)
    var accountName: String,

    @Column(nullable = false, precision = 19, scale = 4)
    var debitAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 19, scale = 4)
    var creditAmount: BigDecimal = BigDecimal.ZERO,

    @Column(length = 20)
    var costCenter: String? = null,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity()

enum class JournalEntryType { MANUAL, GOODS_RECEIPT, GOODS_ISSUE, INVOICE, PAYMENT }
enum class JeStatus { DRAFT, POSTED, REVERSED }
