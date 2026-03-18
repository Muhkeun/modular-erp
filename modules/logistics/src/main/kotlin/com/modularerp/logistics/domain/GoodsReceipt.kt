package com.modularerp.logistics.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 입고전표(GR) — 발주서(PO) 기반의 자재 입고를 기록하는 물류 전표.
 *
 * 공급업체로부터 자재를 수령하면 GR을 생성하고,
 * 확정(confirm) 시 재고(StockSummary)가 증가하고 PO 입고수량이 갱신된다.
 *
 * 상태 흐름: DRAFT → CONFIRMED / CANCELLED
 *
 * 핵심 비즈니스 규칙:
 * - 확정 시 품목별 재고 자동 증가 (재고가 없으면 신규 생성)
 * - PO 연결(poDocumentNo)로 발주 대비 입고 진행률 추적
 * - 취소 시 확정 전/후 모두 가능 (확정 후 취소 시 재고 차감 필요)
 */
@Entity
@Table(name = "goods_receipts")
class GoodsReceipt(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 20)
    var storageLocation: String,

    @Column(length = 30)
    var poDocumentNo: String? = null,

    @Column(nullable = false, length = 50)
    var vendorCode: String,

    @Column(nullable = false, length = 200)
    var vendorName: String,

    @Column(nullable = false)
    var receiptDate: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: GrStatus = GrStatus.DRAFT,

    @Column(length = 500)
    var remark: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "goodsReceipt", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("lineNo ASC")
    val lines: MutableList<GoodsReceiptLine> = mutableListOf()

    fun addLine(
        itemCode: String, itemName: String, quantity: BigDecimal,
        unitOfMeasure: String, unitPrice: BigDecimal = BigDecimal.ZERO,
        poLineNo: Int? = null, storageLocation: String? = null
    ): GoodsReceiptLine {
        val lineNo = (lines.maxOfOrNull { it.lineNo } ?: 0) + 1
        val line = GoodsReceiptLine(
            goodsReceipt = this, lineNo = lineNo,
            itemCode = itemCode, itemName = itemName,
            quantity = quantity, unitOfMeasure = unitOfMeasure,
            unitPrice = unitPrice, poLineNo = poLineNo,
            storageLocation = storageLocation ?: this.storageLocation
        )
        lines.add(line)
        return line
    }

    /** 확정 — 재고 증가 처리의 트리거. 최소 1개 품목이 있어야 함 */
    fun confirm() {
        check(status == GrStatus.DRAFT) { "Can only confirm from DRAFT" }
        check(lines.isNotEmpty()) { "At least one line required" }
        status = GrStatus.CONFIRMED
    }

    fun cancel() {
        check(status in listOf(GrStatus.DRAFT, GrStatus.CONFIRMED))
        status = GrStatus.CANCELLED
    }
}

/**
 * 입고전표 품목 행 — 입고되는 개별 품목의 수량/단가/저장위치.
 * 품질검사 결과(qualityInspectionPassed)를 기록할 수 있다.
 */
@Entity
@Table(name = "goods_receipt_lines")
class GoodsReceiptLine(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    val goodsReceipt: GoodsReceipt,

    val lineNo: Int,

    @Column(nullable = false, length = 50)
    var itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    @Column(nullable = false, precision = 15, scale = 4)
    var quantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String,

    @Column(nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, length = 20)
    var storageLocation: String,

    var poLineNo: Int? = null,

    var qualityInspectionPassed: Boolean? = null

) : TenantEntity() {

    val totalPrice: BigDecimal get() = quantity.multiply(unitPrice)
}

/** 입고전표 상태: 작성중 → 확정/취소 */
enum class GrStatus { DRAFT, CONFIRMED, CANCELLED }
