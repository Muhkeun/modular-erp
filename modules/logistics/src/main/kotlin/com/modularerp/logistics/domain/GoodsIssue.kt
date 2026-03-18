package com.modularerp.logistics.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Goods Issue (GI) — records outbound material for sales orders, production, or transfers.
 */
@Entity
@Table(name = "goods_issues")
class GoodsIssue(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 20)
    var storageLocation: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var issueType: GiType = GiType.SALES,

    @Column(length = 30)
    var referenceDocNo: String? = null,

    @Column(nullable = false)
    var issueDate: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: GiStatus = GiStatus.DRAFT,

    @Column(length = 500)
    var remark: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "goodsIssue", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("lineNo ASC")
    val lines: MutableList<GoodsIssueLine> = mutableListOf()

    fun addLine(
        itemCode: String, itemName: String, quantity: BigDecimal,
        unitOfMeasure: String, storageLocation: String? = null
    ): GoodsIssueLine {
        val lineNo = (lines.maxOfOrNull { it.lineNo } ?: 0) + 1
        val line = GoodsIssueLine(
            goodsIssue = this, lineNo = lineNo,
            itemCode = itemCode, itemName = itemName,
            quantity = quantity, unitOfMeasure = unitOfMeasure,
            storageLocation = storageLocation ?: this.storageLocation
        )
        lines.add(line)
        return line
    }

    fun confirm() {
        check(status == GiStatus.DRAFT) { "Can only confirm from DRAFT" }
        status = GiStatus.CONFIRMED
    }

    fun cancel() {
        check(status in listOf(GiStatus.DRAFT, GiStatus.CONFIRMED))
        status = GiStatus.CANCELLED
    }
}

@Entity
@Table(name = "goods_issue_lines")
class GoodsIssueLine(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_issue_id", nullable = false)
    val goodsIssue: GoodsIssue,

    val lineNo: Int,

    @Column(nullable = false, length = 50)
    var itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    @Column(nullable = false, precision = 15, scale = 4)
    var quantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String,

    @Column(nullable = false, length = 20)
    var storageLocation: String

) : TenantEntity()

enum class GiType { SALES, TRANSFER, PRODUCTION, SCRAP, RETURN }
enum class GiStatus { DRAFT, CONFIRMED, CANCELLED }
