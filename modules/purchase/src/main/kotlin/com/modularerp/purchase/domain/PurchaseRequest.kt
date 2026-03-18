package com.modularerp.purchase.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 구매요청(PR) — 구매 프로세스의 시작점.
 *
 * 현업 부서에서 자재/서비스 구매가 필요할 때 작성하며,
 * 결재 승인 후 견적요청(RFQ) 또는 발주서(PO)로 전환된다.
 *
 * 상태 흐름: DRAFT → SUBMITTED → APPROVED → CLOSED
 *                              ↘ REJECTED
 *
 * 핵심 비즈니스 규칙:
 * - 품목별 미전환 수량(openQuantity)을 추적하여 중복 발주를 방지
 * - PR 1건에서 여러 PO로 분할 전환 가능 (부분 전환)
 */
@Entity
@Table(name = "purchase_requests")
class PurchaseRequest(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(length = 20)
    var departmentCode: String? = null,

    @Column(nullable = false)
    var requestDate: LocalDate = LocalDate.now(),

    var deliveryDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PrStatus = PrStatus.DRAFT,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var prType: PrType = PrType.STANDARD,

    @Column(length = 500)
    var description: String? = null,

    @Column(length = 50)
    var requestedBy: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "purchaseRequest", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("lineNo ASC")
    val lines: MutableList<PurchaseRequestLine> = mutableListOf()

    val totalAmount: BigDecimal
        get() = lines.sumOf { it.totalPrice }

    /** 품목 행 추가 — 요청할 자재/서비스 품목을 행 단위로 등록 */
    fun addLine(
        itemCode: String, itemName: String, quantity: BigDecimal,
        unitOfMeasure: String, unitPrice: BigDecimal,
        specification: String? = null, remark: String? = null
    ): PurchaseRequestLine {
        val lineNo = (lines.maxOfOrNull { it.lineNo } ?: 0) + 1
        val line = PurchaseRequestLine(
            purchaseRequest = this, lineNo = lineNo,
            itemCode = itemCode, itemName = itemName,
            quantity = quantity, unitOfMeasure = unitOfMeasure,
            unitPrice = unitPrice, specification = specification, remark = remark
        )
        lines.add(line)
        return line
    }

    /** 제출 — 결재 요청. 최소 1개 품목이 있어야 제출 가능 */
    fun submit() {
        check(status == PrStatus.DRAFT) { "Can only submit from DRAFT" }
        check(lines.isNotEmpty()) { "At least one line required" }
        status = PrStatus.SUBMITTED
    }

    /** 승인 — 결재 완료. 이후 PO/RFQ 전환 가능 */
    fun approve() {
        check(status == PrStatus.SUBMITTED) { "Can only approve from SUBMITTED" }
        status = PrStatus.APPROVED
    }

    /** 반려 — 요청 거부 */
    fun reject() {
        check(status == PrStatus.SUBMITTED) { "Can only reject from SUBMITTED" }
        status = PrStatus.REJECTED
    }

    /** 마감 — 모든 품목이 PO로 전환 완료되었을 때 마감 처리 */
    fun close() {
        check(status == PrStatus.APPROVED) { "Can only close from APPROVED" }
        status = PrStatus.CLOSED
    }
}

/**
 * 구매요청 품목 행 — PR의 개별 요청 품목.
 * openQuantity로 PO 미전환 잔량을 관리하여 부분 전환을 지원한다.
 */
@Entity
@Table(name = "purchase_request_lines")
class PurchaseRequestLine(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    val purchaseRequest: PurchaseRequest,

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

    @Column(length = 200)
    var specification: String? = null,

    @Column(length = 500)
    var remark: String? = null,

    /** PO 미전환 잔량 — 이 수량이 0이면 해당 행은 완전히 발주 전환된 상태 */
    @Column(nullable = false, precision = 15, scale = 4)
    var openQuantity: BigDecimal = quantity

) : TenantEntity() {

    val totalPrice: BigDecimal
        get() = quantity.multiply(unitPrice)

    /** PO 전환 시 미전환 잔량을 차감 — 발주 수량이 잔량을 초과할 수 없음 */
    fun consumeQuantity(qty: BigDecimal) {
        require(qty <= openQuantity) { "Cannot consume more than open quantity" }
        openQuantity = openQuantity.subtract(qty)
    }
}

/** 구매요청 상태: 작성중 → 제출 → 승인/반려 → 마감 */
enum class PrStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED, CLOSED
}

/** 구매요청 유형: 일반, 긴급, 프로젝트, 투자 */
enum class PrType {
    STANDARD, URGENT, PROJECT, INVESTMENT
}
