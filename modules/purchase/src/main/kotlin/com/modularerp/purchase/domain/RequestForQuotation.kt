package com.modularerp.purchase.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 견적요청(RFQ) — 공급업체에 가격 견적을 요청하는 문서.
 *
 * 구매요청(PR) 승인 후 복수의 공급업체에 견적을 요청하고,
 * 가격/납기를 비교한 뒤 최적 업체를 낙찰(award)하여 발주서(PO)를 생성한다.
 *
 * 상태 흐름: DRAFT → PUBLISHED → CLOSED → AWARDED
 *
 * 핵심 비즈니스 규칙:
 * - 최소 1개 품목 + 1개 공급업체가 있어야 발행(publish) 가능
 * - 견적 마감(close) 후 낙찰(award) 처리 — 1개 업체만 낙찰 가능
 * - PR 행과 연결하여 요청 원본 추적 가능
 */
@Entity
@Table(name = "rfqs")
class RequestForQuotation(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false)
    var issueDate: LocalDate = LocalDate.now(),

    var dueDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: RfqStatus = RfqStatus.DRAFT,

    @Column(length = 1000)
    var description: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "rfq", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("lineNo ASC")
    val lines: MutableList<RfqLine> = mutableListOf()

    @OneToMany(mappedBy = "rfq", cascade = [CascadeType.ALL], orphanRemoval = true)
    val vendors: MutableSet<RfqVendor> = mutableSetOf()

    fun addLine(itemCode: String, itemName: String, quantity: BigDecimal, unitOfMeasure: String,
                specification: String? = null, prDocumentNo: String? = null, prLineNo: Int? = null): RfqLine {
        val lineNo = (lines.maxOfOrNull { it.lineNo } ?: 0) + 1
        val line = RfqLine(rfq = this, lineNo = lineNo, itemCode = itemCode, itemName = itemName,
            quantity = quantity, unitOfMeasure = unitOfMeasure, specification = specification,
            prDocumentNo = prDocumentNo, prLineNo = prLineNo)
        lines.add(line)
        return line
    }

    fun addVendor(vendorCode: String, vendorName: String): RfqVendor {
        val vendor = RfqVendor(rfq = this, vendorCode = vendorCode, vendorName = vendorName)
        vendors.add(vendor)
        return vendor
    }

    /** 발행 — 공급업체에 견적 요청 발송. 품목과 업체가 모두 등록되어야 함 */
    fun publish() {
        check(status == RfqStatus.DRAFT) { "Can only publish from DRAFT" }
        check(lines.isNotEmpty()) { "At least one line required" }
        check(vendors.isNotEmpty()) { "At least one vendor required" }
        status = RfqStatus.PUBLISHED
    }

    /** 마감 — 견적 접수 기간 종료. 이후 낙찰 처리 가능 */
    fun close() {
        check(status == RfqStatus.PUBLISHED) { "Can only close from PUBLISHED" }
        status = RfqStatus.CLOSED
    }

    /** 낙찰 — 최적 공급업체를 선정. 해당 업체의 견적 기준으로 PO 생성 가능 */
    fun award(vendorCode: String) {
        check(status == RfqStatus.CLOSED) { "Can only award from CLOSED" }
        vendors.forEach { v ->
            v.awarded = v.vendorCode == vendorCode
        }
        status = RfqStatus.AWARDED
    }
}

/** 견적요청 품목 행 — 견적을 요청할 개별 품목. PR 행과 연결 가능 */
@Entity
@Table(name = "rfq_lines")
class RfqLine(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    val rfq: RequestForQuotation,

    @Column(nullable = false)
    val lineNo: Int,

    @Column(nullable = false, length = 50)
    var itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    @Column(nullable = false, precision = 15, scale = 4)
    var quantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String,

    @Column(length = 200)
    var specification: String? = null,

    @Column(length = 30)
    var prDocumentNo: String? = null,
    var prLineNo: Int? = null

) : TenantEntity()

/**
 * 견적 참여 공급업체 — RFQ에 초대된 공급업체.
 * 각 업체가 품목별 단가/납기를 제출(submitQuotation)하면 비교 평가 가능.
 */
@Entity
@Table(name = "rfq_vendors")
class RfqVendor(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    val rfq: RequestForQuotation,

    @Column(nullable = false, length = 50)
    val vendorCode: String,

    @Column(nullable = false, length = 200)
    var vendorName: String,

    var awarded: Boolean = false,

    var respondedAt: LocalDateTime? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "rfqVendor", cascade = [CascadeType.ALL], orphanRemoval = true)
    val quotationLines: MutableList<QuotationLine> = mutableListOf()

    /** 견적 제출 — 공급업체가 품목별 단가와 납기를 회신. 동일 행 재제출 시 덮어쓰기 */
    fun submitQuotation(lineNo: Int, unitPrice: BigDecimal, leadTimeDays: Int? = null, remark: String? = null) {
        quotationLines.removeIf { it.rfqLineNo == lineNo }
        quotationLines.add(QuotationLine(
            rfqVendor = this, rfqLineNo = lineNo,
            unitPrice = unitPrice, leadTimeDays = leadTimeDays, remark = remark
        ))
        respondedAt = LocalDateTime.now()
    }
}

/** 견적 응답 행 — 공급업체가 제출한 품목별 단가/납기 정보 */
@Entity
@Table(name = "quotation_lines")
class QuotationLine(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_vendor_id", nullable = false)
    val rfqVendor: RfqVendor,

    @Column(nullable = false)
    val rfqLineNo: Int,

    @Column(nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal,

    var leadTimeDays: Int? = null,

    @Column(length = 500)
    var remark: String? = null

) : TenantEntity()

/** 견적요청 상태: 작성중 → 발행 → 마감 → 낙찰/취소 */
enum class RfqStatus {
    DRAFT, PUBLISHED, CLOSED, AWARDED, CANCELLED
}
