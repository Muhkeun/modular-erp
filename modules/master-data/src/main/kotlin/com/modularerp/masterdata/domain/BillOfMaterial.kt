package com.modularerp.masterdata.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 자재명세서(BOM) — 제품의 구성 부품/원자재 구조를 정의.
 *
 * 다단계 BOM을 지원하여 하위 구성품이 자체 BOM을 가질 수 있으며,
 * 팬텀 품목(phantom)은 가상 조립품으로 전개 시 하위 품목으로 치환된다.
 * 리비전(revision) + 유효기간(validFrom/To)으로 버전을 관리한다.
 *
 * 핵심 비즈니스 규칙:
 * - 기준수량(baseQuantity) 대비 구성품 수량을 정의 (예: 완제품 10개당 볼트 20개)
 * - 스크랩률(scrapRate)로 예상 손실을 반영한 총소요량(grossQuantity) 자동 계산
 * - RELEASED 상태의 BOM만 생산(WO)과 MRP에서 사용 가능
 */
@Entity
@Table(
    name = "bom_headers",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "product_code", "revision"])]
)
class BomHeader(

    @Column(nullable = false, length = 50)
    val productCode: String,

    @Column(nullable = false, length = 200)
    var productName: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 10)
    var revision: String = "001",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: BomStatus = BomStatus.DRAFT,

    @Column(nullable = false, precision = 15, scale = 4)
    var baseQuantity: BigDecimal = BigDecimal.ONE,

    @Column(nullable = false, length = 10)
    var baseUnit: String = "EA",

    var validFrom: LocalDate = LocalDate.now(),

    var validTo: LocalDate? = null,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "bomHeader", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val components: MutableList<BomComponent> = mutableListOf()

    fun addComponent(
        itemCode: String, itemName: String, quantity: BigDecimal,
        unitOfMeasure: String = "EA", scrapRate: BigDecimal = BigDecimal.ZERO,
        phantom: Boolean = false, sortOrder: Int? = null,
        operationNo: Int? = null, remark: String? = null
    ): BomComponent {
        val order = sortOrder ?: ((components.maxOfOrNull { it.sortOrder } ?: 0) + 10)
        val component = BomComponent(
            bomHeader = this, itemCode = itemCode, itemName = itemName,
            quantity = quantity, unitOfMeasure = unitOfMeasure,
            scrapRate = scrapRate, phantom = phantom,
            sortOrder = order, operationNo = operationNo, remark = remark
        )
        components.add(component)
        return component
    }

    /** 확정 — 생산/MRP에서 사용 가능 상태로 전환. 구성품이 1개 이상이어야 함 */
    fun release() {
        check(status == BomStatus.DRAFT) { "Can only release from DRAFT" }
        check(components.isNotEmpty()) { "BOM must have at least one component" }
        status = BomStatus.RELEASED
    }

    /** 폐기 — 구버전 BOM을 비활성화. 신규 리비전 등록 후 이전 버전에 적용 */
    fun obsolete() {
        status = BomStatus.OBSOLETE
    }
}

/**
 * BOM 구성품 — 상위 제품(BomHeader)에 필요한 개별 자재/부품.
 *
 * 스크랩률(scrapRate)을 적용한 총소요량(grossQuantity)으로
 * 예상 손실을 포함한 실제 필요수량을 산출한다.
 */
@Entity
@Table(name = "bom_components")
class BomComponent(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_header_id", nullable = false)
    val bomHeader: BomHeader,

    @Column(nullable = false, length = 50)
    var itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    /** 기준수량당 소요량 — 상위 제품의 baseQuantity 대비 필요한 수량 */
    @Column(nullable = false, precision = 15, scale = 6)
    var quantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String = "EA",

    /** 스크랩률 (0.05 = 5%) — 예상 손실을 반영한 추가 자재 비율 */
    @Column(nullable = false, precision = 5, scale = 4)
    var scrapRate: BigDecimal = BigDecimal.ZERO,

    /** 팬텀 여부 — true이면 가상 조립품으로, BOM 전개 시 하위 구성품으로 치환 */
    var phantom: Boolean = false,

    var sortOrder: Int = 0,

    /** 소비 공정번호 — Routing의 어떤 공정에서 이 자재를 사용하는지 연결 */
    var operationNo: Int? = null,

    @Column(length = 500)
    var remark: String? = null

) : TenantEntity() {

    /** 총소요량 = 소요량 × (1 + 스크랩률) — 손실분을 포함한 실제 필요수량 */
    val grossQuantity: BigDecimal
        get() = quantity.multiply(BigDecimal.ONE.add(scrapRate))
}

/** BOM 상태: 작성중 → 확정(사용가능) → 폐기(구버전) */
enum class BomStatus { DRAFT, RELEASED, OBSOLETE }
