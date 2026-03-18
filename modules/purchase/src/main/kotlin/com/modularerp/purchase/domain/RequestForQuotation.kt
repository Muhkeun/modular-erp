package com.modularerp.purchase.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Request for Quotation (RFQ) — sent to vendors to get price quotes.
 * Links back to PR lines and forward to PO creation.
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

    fun publish() {
        check(status == RfqStatus.DRAFT) { "Can only publish from DRAFT" }
        check(lines.isNotEmpty()) { "At least one line required" }
        check(vendors.isNotEmpty()) { "At least one vendor required" }
        status = RfqStatus.PUBLISHED
    }

    fun close() {
        check(status == RfqStatus.PUBLISHED) { "Can only close from PUBLISHED" }
        status = RfqStatus.CLOSED
    }

    fun award(vendorCode: String) {
        check(status == RfqStatus.CLOSED) { "Can only award from CLOSED" }
        vendors.forEach { v ->
            v.awarded = v.vendorCode == vendorCode
        }
        status = RfqStatus.AWARDED
    }
}

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

    fun submitQuotation(lineNo: Int, unitPrice: BigDecimal, leadTimeDays: Int? = null, remark: String? = null) {
        quotationLines.removeIf { it.rfqLineNo == lineNo }
        quotationLines.add(QuotationLine(
            rfqVendor = this, rfqLineNo = lineNo,
            unitPrice = unitPrice, leadTimeDays = leadTimeDays, remark = remark
        ))
        respondedAt = LocalDateTime.now()
    }
}

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

enum class RfqStatus {
    DRAFT, PUBLISHED, CLOSED, AWARDED, CANCELLED
}
