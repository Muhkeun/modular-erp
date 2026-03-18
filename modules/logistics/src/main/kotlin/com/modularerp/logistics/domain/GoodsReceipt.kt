package com.modularerp.logistics.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Goods Receipt (GR) — records inbound material from a PO or return.
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

enum class GrStatus { DRAFT, CONFIRMED, CANCELLED }
