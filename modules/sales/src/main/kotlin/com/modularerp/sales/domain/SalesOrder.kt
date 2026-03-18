package com.modularerp.sales.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 수주(SO) — 고객으로부터 받은 판매 주문서.
 *
 * 영업 프로세스의 핵심 전표로, 승인 후 출하(ship)와 매출 인식의 기준이 된다.
 * 주문생산(MTO) 방식에서는 WO 생성의 트리거가 된다.
 *
 * 상태 흐름: DRAFT → SUBMITTED → APPROVED → SHIPPED → COMPLETED
 *                              ↘ REJECTED
 *
 * 핵심 비즈니스 규칙:
 * - 품목별 출하수량(shippedQuantity)으로 부분 출하 추적
 * - 미출하 잔량(openQuantity) = 주문수량 - 출하수량
 * - 세금 계산: 품목 단가 × 수량 × 세율(기본 10%)
 */
@Entity
@Table(name = "sales_orders")
class SalesOrder(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 50)
    var customerCode: String,

    @Column(nullable = false, length = 200)
    var customerName: String,

    @Column(nullable = false)
    var orderDate: LocalDate = LocalDate.now(),

    var deliveryDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SoStatus = SoStatus.DRAFT,

    @Column(length = 3)
    var currencyCode: String = "KRW",

    @Column(length = 500)
    var paymentTerms: String? = null,

    @Column(length = 500)
    var shippingAddress: String? = null,

    @Column(length = 1000)
    var remark: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "salesOrder", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("lineNo ASC")
    val lines: MutableList<SalesOrderLine> = mutableListOf()

    val totalAmount: BigDecimal get() = lines.sumOf { it.totalPrice }
    val taxAmount: BigDecimal get() = lines.sumOf { it.taxAmount }
    val grandTotal: BigDecimal get() = totalAmount.add(taxAmount)

    fun addLine(
        itemCode: String, itemName: String, quantity: BigDecimal,
        unitOfMeasure: String, unitPrice: BigDecimal,
        taxRate: BigDecimal = BigDecimal("0.10"), specification: String? = null
    ): SalesOrderLine {
        val lineNo = (lines.maxOfOrNull { it.lineNo } ?: 0) + 1
        val line = SalesOrderLine(
            salesOrder = this, lineNo = lineNo, itemCode = itemCode, itemName = itemName,
            quantity = quantity, unitOfMeasure = unitOfMeasure, unitPrice = unitPrice,
            taxRate = taxRate, specification = specification
        )
        lines.add(line)
        return line
    }

    /** 제출 — 결재 요청 */
    fun submit() { check(status == SoStatus.DRAFT); status = SoStatus.SUBMITTED }
    /** 승인 — 출하 및 생산 진행 가능 */
    fun approve() { check(status == SoStatus.SUBMITTED); status = SoStatus.APPROVED }
    /** 반려 */
    fun reject() { check(status == SoStatus.SUBMITTED); status = SoStatus.REJECTED }
    /** 출하 — 물류에서 GI 처리 후 상태 전환 */
    fun ship() { check(status == SoStatus.APPROVED); status = SoStatus.SHIPPED }
    /** 완료 — 대금 수령 후 최종 마감 */
    fun complete() { status = SoStatus.COMPLETED }
}

/**
 * 수주 품목 행 — SO의 개별 판매 품목.
 * 출하수량(shippedQuantity)으로 부분 출하를 추적하며,
 * 미출하 잔량(openQuantity)이 0이면 해당 품목은 출하 완료.
 */
@Entity
@Table(name = "sales_order_lines")
class SalesOrderLine(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    val salesOrder: SalesOrder,

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
    var unitPrice: BigDecimal,
    @Column(nullable = false, precision = 5, scale = 4)
    var taxRate: BigDecimal = BigDecimal("0.10"),
    @Column(length = 200)
    var specification: String? = null,

    @Column(nullable = false, precision = 15, scale = 4)
    var shippedQuantity: BigDecimal = BigDecimal.ZERO

) : TenantEntity() {
    val totalPrice: BigDecimal get() = quantity.multiply(unitPrice)
    val taxAmount: BigDecimal get() = totalPrice.multiply(taxRate)
    val openQuantity: BigDecimal get() = quantity.subtract(shippedQuantity)
}

/** 수주 상태: 작성중 → 제출 → 승인/반려 → 출하 → 완료/취소 */
enum class SoStatus { DRAFT, SUBMITTED, APPROVED, REJECTED, SHIPPED, COMPLETED, CANCELLED }
