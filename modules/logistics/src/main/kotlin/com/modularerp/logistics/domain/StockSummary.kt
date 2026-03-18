package com.modularerp.logistics.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Stock Summary — aggregated stock by item + plant + storage location.
 * Updated by GR (increase) and GI (decrease) confirmations.
 */
@Entity
@Table(
    name = "stock_summaries",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "item_code", "plant_code", "storage_location"])]
)
class StockSummary(

    @Column(nullable = false, length = 50)
    val itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    @Column(nullable = false, length = 20)
    val plantCode: String,

    @Column(nullable = false, length = 20)
    val storageLocation: String,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String = "EA",

    @Column(nullable = false, precision = 15, scale = 4)
    var quantityOnHand: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 15, scale = 4)
    var quantityReserved: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 19, scale = 4)
    var totalValue: BigDecimal = BigDecimal.ZERO

) : TenantEntity() {

    val availableQuantity: BigDecimal
        get() = quantityOnHand.subtract(quantityReserved)

    fun receiveStock(quantity: BigDecimal, unitPrice: BigDecimal) {
        quantityOnHand = quantityOnHand.add(quantity)
        totalValue = totalValue.add(quantity.multiply(unitPrice))
    }

    fun issueStock(quantity: BigDecimal) {
        require(quantity <= availableQuantity) { "Insufficient stock: available=$availableQuantity, requested=$quantity" }
        val avgPrice = if (quantityOnHand > BigDecimal.ZERO) totalValue.divide(quantityOnHand, 4, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO
        quantityOnHand = quantityOnHand.subtract(quantity)
        totalValue = totalValue.subtract(quantity.multiply(avgPrice))
    }

    fun reserve(quantity: BigDecimal) {
        require(quantity <= availableQuantity) { "Insufficient available stock" }
        quantityReserved = quantityReserved.add(quantity)
    }

    fun unreserve(quantity: BigDecimal) {
        quantityReserved = quantityReserved.subtract(quantity).coerceAtLeast(BigDecimal.ZERO)
    }
}
