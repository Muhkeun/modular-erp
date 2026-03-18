package com.modularerp.purchase.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Purchase Order (PO) — a formal order sent to a vendor.
 * Can be created from an approved PR or directly.
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

    fun submit() {
        check(status == PoStatus.DRAFT) { "Can only submit from DRAFT" }
        check(lines.isNotEmpty()) { "At least one line required" }
        status = PoStatus.SUBMITTED
    }

    fun approve() {
        check(status == PoStatus.SUBMITTED) { "Can only approve from SUBMITTED" }
        status = PoStatus.APPROVED
    }

    fun reject() {
        check(status == PoStatus.SUBMITTED) { "Can only reject from SUBMITTED" }
        status = PoStatus.REJECTED
    }

    fun send() {
        check(status == PoStatus.APPROVED) { "Can only send from APPROVED" }
        status = PoStatus.SENT
    }

    fun complete() {
        status = PoStatus.COMPLETED
    }
}

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

    /** Traceability back to PR */
    @Column(length = 30)
    var prDocumentNo: String? = null,
    var prLineNo: Int? = null,

    /** Received quantity tracking */
    @Column(nullable = false, precision = 15, scale = 4)
    var receivedQuantity: BigDecimal = BigDecimal.ZERO

) : TenantEntity() {

    val totalPrice: BigDecimal
        get() = quantity.multiply(unitPrice)

    val taxAmount: BigDecimal
        get() = totalPrice.multiply(taxRate)

    val openQuantity: BigDecimal
        get() = quantity.subtract(receivedQuantity)

    fun receive(qty: BigDecimal) {
        require(qty <= openQuantity) { "Cannot receive more than open quantity ($openQuantity)" }
        receivedQuantity = receivedQuantity.add(qty)
    }

    val isFullyReceived: Boolean
        get() = receivedQuantity >= quantity
}

enum class PoStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED, SENT, COMPLETED, CANCELLED
}
