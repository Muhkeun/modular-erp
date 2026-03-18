package com.modularerp.sales.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

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

    fun submit() { check(status == SoStatus.DRAFT); status = SoStatus.SUBMITTED }
    fun approve() { check(status == SoStatus.SUBMITTED); status = SoStatus.APPROVED }
    fun reject() { check(status == SoStatus.SUBMITTED); status = SoStatus.REJECTED }
    fun ship() { check(status == SoStatus.APPROVED); status = SoStatus.SHIPPED }
    fun complete() { status = SoStatus.COMPLETED }
}

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

enum class SoStatus { DRAFT, SUBMITTED, APPROVED, REJECTED, SHIPPED, COMPLETED, CANCELLED }
