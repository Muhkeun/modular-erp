package com.modularerp.purchase.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Purchase Request (PR) — the starting point of the procurement process.
 * A user or department requests materials/services to be procured.
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

    fun submit() {
        check(status == PrStatus.DRAFT) { "Can only submit from DRAFT" }
        check(lines.isNotEmpty()) { "At least one line required" }
        status = PrStatus.SUBMITTED
    }

    fun approve() {
        check(status == PrStatus.SUBMITTED) { "Can only approve from SUBMITTED" }
        status = PrStatus.APPROVED
    }

    fun reject() {
        check(status == PrStatus.SUBMITTED) { "Can only reject from SUBMITTED" }
        status = PrStatus.REJECTED
    }

    fun close() {
        check(status == PrStatus.APPROVED) { "Can only close from APPROVED" }
        status = PrStatus.CLOSED
    }
}

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

    /** Remaining qty not yet converted to PO */
    @Column(nullable = false, precision = 15, scale = 4)
    var openQuantity: BigDecimal = quantity

) : TenantEntity() {

    val totalPrice: BigDecimal
        get() = quantity.multiply(unitPrice)

    fun consumeQuantity(qty: BigDecimal) {
        require(qty <= openQuantity) { "Cannot consume more than open quantity" }
        openQuantity = openQuantity.subtract(qty)
    }
}

enum class PrStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED, CLOSED
}

enum class PrType {
    STANDARD, URGENT, PROJECT, INVESTMENT
}
