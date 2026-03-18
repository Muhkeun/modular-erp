package com.modularerp.logistics.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * 재고 요약(Stock Summary) — 품목 + 공장 + 저장위치 단위의 집계 재고.
 *
 * 입고(GR) 확정 시 증가, 출고(GI) 확정 시 감소한다.
 * MRP, 가용재고 확인, 재고 평가 등에 사용되는 핵심 재고 엔티티.
 *
 * 핵심 비즈니스 규칙:
 * - 가용재고(available) = 보유수량(onHand) - 예약수량(reserved)
 * - 출고 시 이동평균법으로 재고금액 차감: 단가 = 총금액 / 보유수량
 * - 예약(reserve)은 실제 출고 전 재고를 확보하는 소프트 잠금
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

    /** 입고 — 보유수량과 재고금액을 증가시킨다 */
    fun receiveStock(quantity: BigDecimal, unitPrice: BigDecimal) {
        quantityOnHand = quantityOnHand.add(quantity)
        totalValue = totalValue.add(quantity.multiply(unitPrice))
    }

    /** 출고 — 이동평균 단가로 재고금액 차감. 가용재고 부족 시 거부 */
    fun issueStock(quantity: BigDecimal) {
        require(quantity <= availableQuantity) { "Insufficient stock: available=$availableQuantity, requested=$quantity" }
        val avgPrice = if (quantityOnHand > BigDecimal.ZERO) totalValue.divide(quantityOnHand, 4, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO
        quantityOnHand = quantityOnHand.subtract(quantity)
        totalValue = totalValue.subtract(quantity.multiply(avgPrice))
    }

    /** 예약 — 수주/WO에 의한 재고 확보. 실제 출고 전까지 다른 용도로 사용 불가 */
    fun reserve(quantity: BigDecimal) {
        require(quantity <= availableQuantity) { "Insufficient available stock" }
        quantityReserved = quantityReserved.add(quantity)
    }

    /** 예약 해제 — 취소 등으로 확보된 재고를 다시 가용 상태로 전환 */
    fun unreserve(quantity: BigDecimal) {
        quantityReserved = quantityReserved.subtract(quantity).coerceAtLeast(BigDecimal.ZERO)
    }
}
