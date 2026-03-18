package com.modularerp.account.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 분개전표(Journal Entry) — 회계의 기본 기록 단위.
 *
 * 모든 재무 거래(입고, 송장, 지급 등)는 분개전표를 통해 회계 처리된다.
 * 복식부기 원칙에 따라 차변 합계와 대변 합계가 반드시 일치해야 한다.
 *
 * 상태 흐름: DRAFT → POSTED → REVERSED
 *
 * 핵심 비즈니스 규칙:
 * - 전기(post) 시 차변 = 대변 균형 검증 (불균형 시 거부)
 * - 최소 2개 행(차변 1 + 대변 1) 필요
 * - 전기 취소(reverse) 시 역분개 — 원 전표는 REVERSED 상태로 전환
 * - referenceDocNo/Type으로 원 거래 전표(GR, Invoice 등)와 연결
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

    /** 전기 — 회계 장부에 반영. 차대 균형 + 최소 2행 검증 */
    fun post() {
        check(status == JeStatus.DRAFT) { "Can only post from DRAFT" }
        check(isBalanced) { "Journal entry must be balanced (Debit=$totalDebit, Credit=$totalCredit)" }
        check(lines.size >= 2) { "At least two lines required" }
        status = JeStatus.POSTED
    }

    /** 역분개 — 전기된 전표를 취소. 별도 역분개 전표 생성 필요 */
    fun reverse() {
        check(status == JeStatus.POSTED) { "Can only reverse POSTED entries" }
        status = JeStatus.REVERSED
    }
}

/**
 * 분개전표 행 — 개별 차변 또는 대변 기록.
 * 계정코드(accountCode)와 코스트센터(costCenter)로 귀속을 지정한다.
 */
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

/** 분개 유형: 수동, 입고, 출고, 송장, 지급 */
enum class JournalEntryType { MANUAL, GOODS_RECEIPT, GOODS_ISSUE, INVOICE, PAYMENT }
/** 분개 상태: 작성중 → 전기 → 역분개 */
enum class JeStatus { DRAFT, POSTED, REVERSED }
