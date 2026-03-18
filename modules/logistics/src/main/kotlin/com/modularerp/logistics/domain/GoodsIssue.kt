package com.modularerp.logistics.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 출고전표(GI) — 재고 감소를 수반하는 자재 출고를 기록하는 물류 전표.
 *
 * 출고 유형(GiType)에 따라 목적이 구분된다:
 * - SALES: 수주 납품을 위한 출고
 * - PRODUCTION: 생산 자재 불출
 * - TRANSFER: 저장위치 간 재고 이동
 * - SCRAP: 폐기 처리
 * - RETURN: 공급업체 반품
 *
 * 상태 흐름: DRAFT → CONFIRMED / CANCELLED
 *
 * 핵심 비즈니스 규칙:
 * - 확정 시 재고(StockSummary) 자동 차감
 * - 참조문서(referenceDocNo)로 수주/WO 등 원 전표와 연결
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

    /** 확정 — 재고 차감 처리의 트리거 */
    fun confirm() {
        check(status == GiStatus.DRAFT) { "Can only confirm from DRAFT" }
        status = GiStatus.CONFIRMED
    }

    fun cancel() {
        check(status in listOf(GiStatus.DRAFT, GiStatus.CONFIRMED))
        status = GiStatus.CANCELLED
    }
}

/** 출고전표 품목 행 — 출고되는 개별 품목의 수량과 저장위치 */
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

/** 출고 유형: 판매, 이동, 생산불출, 폐기, 반품 */
enum class GiType { SALES, TRANSFER, PRODUCTION, SCRAP, RETURN }
/** 출고전표 상태: 작성중 → 확정/취소 */
enum class GiStatus { DRAFT, CONFIRMED, CANCELLED }
