package com.modularerp.purchase.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 발주서(PO) — 공급업체에 보내는 공식 구매 주문서.
 *
 * 승인된 구매요청(PR)에서 전환하거나 직접 생성할 수 있다.
 * 승인 후 공급업체에 발송(SENT)되며, 입고(GR) 완료 시 자동 완료 처리된다.
 *
 * 상태 흐름: DRAFT → SUBMITTED → APPROVED → SENT → COMPLETED
 *                              ↘ REJECTED
 *
 * 핵심 비즈니스 규칙:
 * - 품목별 입고수량(receivedQuantity)을 추적하여 부분입고 지원
 * - 세금 계산: 품목별 단가 × 수량 × 세율(기본 10%)
 * - PR과의 추적성: prDocumentNo/prLineNo로 원래 요청과 연결
 */
@Entity
@Table(name = "purchase_orders")
class PurchaseOrder(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 50)
    var vendorCode: String,

    @Column(nullable = false, length = 200)
    var vendorName: String,

    @Column(nullable = false)
    var orderDate: LocalDate = LocalDate.now(),

    var deliveryDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PoStatus = PoStatus.DRAFT,

    @Column(length = 3)
    var currencyCode: String = "KRW",

    @Column(length = 500)
    var paymentTerms: String? = null,

    @Column(length = 500)
    var deliveryTerms: String? = null,

    @Column(length = 1000)
    var remark: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "purchaseOrder", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("lineNo ASC")
    val lines: MutableList<PurchaseOrderLine> = mutableListOf()

    val totalAmount: BigDecimal
        get() = lines.sumOf { it.totalPrice }

    val taxAmount: BigDecimal
        get() = lines.sumOf { it.taxAmount }

    val grandTotal: BigDecimal
        get() = totalAmount.add(taxAmount)

    fun addLine(
        itemCode: String, itemName: String, quantity: BigDecimal,
        unitOfMeasure: String, unitPrice: BigDecimal,
        taxRate: BigDecimal = BigDecimal("0.10"),
        prDocumentNo: String? = null, prLineNo: Int? = null,
        specification: String? = null
    ): PurchaseOrderLine {
        val lineNo = (lines.maxOfOrNull { it.lineNo } ?: 0) + 1
        val line = PurchaseOrderLine(
            purchaseOrder = this, lineNo = lineNo,
            itemCode = itemCode, itemName = itemName,
            quantity = quantity, unitOfMeasure = unitOfMeasure,
            unitPrice = unitPrice, taxRate = taxRate,
            prDocumentNo = prDocumentNo, prLineNo = prLineNo,
            specification = specification
        )
        lines.add(line)
        return line
    }

    /** 제출 — 결재 요청 */
    fun submit() {
        check(status == PoStatus.DRAFT) { "Can only submit from DRAFT" }
        check(lines.isNotEmpty()) { "At least one line required" }
        status = PoStatus.SUBMITTED
    }

    /** 승인 — 이벤트 발행으로 물류 모듈에 입고 준비 알림 */
    fun approve() {
        check(status == PoStatus.SUBMITTED) { "Can only approve from SUBMITTED" }
        status = PoStatus.APPROVED
    }

    fun reject() {
        check(status == PoStatus.SUBMITTED) { "Can only reject from SUBMITTED" }
        status = PoStatus.REJECTED
    }

    /** 발송 — 공급업체에 발주서 전달 */
    fun send() {
        check(status == PoStatus.APPROVED) { "Can only send from APPROVED" }
        status = PoStatus.SENT
    }

    /** 완료 — 모든 품목 입고 완료 시 마감 */
    fun complete() {
        status = PoStatus.COMPLETED
    }
}

/**
 * 발주서 품목 행 — PO의 개별 발주 품목.
 * 입고수량(receivedQuantity)으로 부분입고를 추적하며,
 * 미입고 잔량(openQuantity)이 0이면 해당 품목은 입고 완료.
 */
@Entity
@Table(name = "purchase_order_lines")
class PurchaseOrderLine(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    val purchaseOrder: PurchaseOrder,

    @Column(nullable = false)
    val lineNo: Int,

    @Column(nullable = false, length = 50)
    var itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    @Column(nullable = false, precision = 15, scale = 4)
    var quantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String = "EA",

    @Column(nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 4)
    var taxRate: BigDecimal = BigDecimal("0.10"),

    @Column(length = 200)
    var specification: String? = null,

    /** 구매요청(PR) 추적 — 어떤 PR에서 전환되었는지 역추적용 */
    @Column(length = 30)
    var prDocumentNo: String? = null,
    var prLineNo: Int? = null,

    /** 누적 입고수량 — GR 확정 시 증가, 발주수량 대비 입고 진행률 추적 */
    @Column(nullable = false, precision = 15, scale = 4)
    var receivedQuantity: BigDecimal = BigDecimal.ZERO

) : TenantEntity() {

    val totalPrice: BigDecimal
        get() = quantity.multiply(unitPrice)

    val taxAmount: BigDecimal
        get() = totalPrice.multiply(taxRate)

    val openQuantity: BigDecimal
        get() = quantity.subtract(receivedQuantity)

    /** 입고 처리 — 미입고 잔량을 초과하는 입고는 불가 (과입고 방지) */
    fun receive(qty: BigDecimal) {
        require(qty <= openQuantity) { "Cannot receive more than open quantity ($openQuantity)" }
        receivedQuantity = receivedQuantity.add(qty)
    }

    val isFullyReceived: Boolean
        get() = receivedQuantity >= quantity
}

/** 발주서 상태: 작성중 → 제출 → 승인/반려 → 발송 → 완료/취소 */
enum class PoStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED, SENT, COMPLETED, CANCELLED
}
