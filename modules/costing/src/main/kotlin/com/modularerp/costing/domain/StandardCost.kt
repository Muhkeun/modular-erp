package com.modularerp.costing.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "standard_costs")
class StandardCost(

    @Column(nullable = false, length = 50)
    var itemCode: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var costType: CostType = CostType.MATERIAL,

    @Column(nullable = false, precision = 19, scale = 4)
    var standardRate: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var effectiveFrom: LocalDate = LocalDate.now(),

    var effectiveTo: LocalDate? = null,

    @Column(nullable = false, length = 3)
    var currency: String = "KRW",

    @Column(length = 500)
    var notes: String? = null

) : TenantEntity()

enum class CostType { MATERIAL, LABOR, OVERHEAD, SUBCONTRACTING }
