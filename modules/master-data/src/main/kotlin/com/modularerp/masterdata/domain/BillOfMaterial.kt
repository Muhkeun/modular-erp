package com.modularerp.masterdata.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Bill of Materials (BOM) — defines the component structure of a product.
 * Supports multi-level BOM (components can themselves have BOMs),
 * phantom items (pass-through assemblies), and version/revision tracking.
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

    fun release() {
        check(status == BomStatus.DRAFT) { "Can only release from DRAFT" }
        check(components.isNotEmpty()) { "BOM must have at least one component" }
        status = BomStatus.RELEASED
    }

    fun obsolete() {
        status = BomStatus.OBSOLETE
    }
}

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

    /** Quantity required per base quantity of parent */
    @Column(nullable = false, precision = 15, scale = 6)
    var quantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String = "EA",

    /** Scrap rate (0.05 = 5%) — extra material needed for expected waste */
    @Column(nullable = false, precision = 5, scale = 4)
    var scrapRate: BigDecimal = BigDecimal.ZERO,

    /** Phantom = virtual sub-assembly; its components are consumed directly */
    var phantom: Boolean = false,

    var sortOrder: Int = 0,

    /** Links to routing operation where this component is consumed */
    var operationNo: Int? = null,

    @Column(length = 500)
    var remark: String? = null

) : TenantEntity() {

    /** Net quantity including scrap allowance */
    val grossQuantity: BigDecimal
        get() = quantity.multiply(BigDecimal.ONE.add(scrapRate))
}

enum class BomStatus { DRAFT, RELEASED, OBSOLETE }
