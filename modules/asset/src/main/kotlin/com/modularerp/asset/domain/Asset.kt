package com.modularerp.asset.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "assets")
class Asset(

    @Column(nullable = false, length = 30)
    var assetNo: String = "",

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var category: AssetCategory,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AssetStatus = AssetStatus.DRAFT,

    @Column(nullable = false)
    var acquisitionDate: LocalDate,

    @Column(nullable = false, precision = 19, scale = 4)
    var acquisitionCost: BigDecimal,

    @Column(nullable = false)
    var usefulLifeMonths: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var depreciationMethod: DepreciationMethod = DepreciationMethod.STRAIGHT_LINE,

    @Column(nullable = false, precision = 19, scale = 4)
    var salvageValue: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 19, scale = 4)
    var accumulatedDepreciation: BigDecimal = BigDecimal.ZERO,

    @Column(length = 100)
    var location: String? = null,

    @Column(length = 20)
    var department: String? = null,

    @Column(length = 100)
    var responsiblePerson: String? = null,

    @Column(length = 100)
    var serialNumber: String? = null,

    @Column(length = 3)
    var currency: String = "KRW"

) : TenantEntity() {

    val bookValue: BigDecimal
        get() = acquisitionCost.subtract(accumulatedDepreciation)

    fun activateAsset() {
        check(status == AssetStatus.DRAFT) { "Can only activate from DRAFT" }
        status = AssetStatus.ACTIVE
    }

    fun dispose() {
        check(status == AssetStatus.ACTIVE || status == AssetStatus.UNDER_MAINTENANCE) {
            "Can only dispose ACTIVE or UNDER_MAINTENANCE assets"
        }
        status = AssetStatus.DISPOSED
    }

    fun scrap() {
        check(status == AssetStatus.ACTIVE || status == AssetStatus.UNDER_MAINTENANCE) {
            "Can only scrap ACTIVE or UNDER_MAINTENANCE assets"
        }
        status = AssetStatus.SCRAPPED
    }
}

enum class AssetCategory { BUILDING, MACHINERY, VEHICLE, FURNITURE, IT_EQUIPMENT, INTANGIBLE, OTHER }
enum class AssetStatus { DRAFT, ACTIVE, UNDER_MAINTENANCE, DISPOSED, SCRAPPED }
enum class DepreciationMethod { STRAIGHT_LINE, DECLINING_BALANCE, SUM_OF_YEARS }
