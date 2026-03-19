package com.modularerp.costing.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "product_costs")
class ProductCost(

    @Column(nullable = false, length = 50)
    var itemCode: String = "",

    @Column(nullable = false)
    var fiscalYear: Int = 0,

    @Column(nullable = false)
    var period: Int = 0,

    @Column(nullable = false, precision = 19, scale = 4)
    var materialCost: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 19, scale = 4)
    var laborCost: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 19, scale = 4)
    var overheadCost: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 19, scale = 4)
    var totalCost: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 19, scale = 4)
    var unitCost: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 4)
    var quantity: BigDecimal = BigDecimal.ONE,

    @Column(nullable = false, length = 3)
    var currency: String = "KRW",

    @Column(nullable = false)
    var calculated: Boolean = false,

    var calculatedAt: LocalDateTime? = null

) : TenantEntity() {

    fun calculate() {
        totalCost = materialCost.add(laborCost).add(overheadCost)
        unitCost = if (quantity > BigDecimal.ZERO) totalCost.divide(quantity, 4, java.math.RoundingMode.HALF_UP)
                   else BigDecimal.ZERO
        calculated = true
        calculatedAt = LocalDateTime.now()
    }
}
