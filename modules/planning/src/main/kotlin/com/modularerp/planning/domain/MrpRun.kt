package com.modularerp.planning.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * MRP Run — a batch execution of Material Requirements Planning.
 * Explodes BOM for demand, checks stock, generates planned orders (PR/WO).
 */
@Entity
@Table(name = "mrp_runs")
class MrpRun(

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false)
    var planningHorizonDays: Int = 30,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MrpStatus = MrpStatus.PLANNED,

    var executedAt: LocalDateTime? = null,

    @Column(length = 50)
    var executedBy: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "mrpRun", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("itemCode ASC")
    val results: MutableList<MrpResult> = mutableListOf()

    fun addResult(
        itemCode: String, itemName: String,
        grossRequirement: BigDecimal, onHandStock: BigDecimal,
        scheduledReceipts: BigDecimal, netRequirement: BigDecimal,
        plannedOrderQty: BigDecimal, unitOfMeasure: String,
        actionType: MrpActionType, requiredDate: LocalDate?
    ): MrpResult {
        val result = MrpResult(
            mrpRun = this, itemCode = itemCode, itemName = itemName,
            grossRequirement = grossRequirement, onHandStock = onHandStock,
            scheduledReceipts = scheduledReceipts, netRequirement = netRequirement,
            plannedOrderQty = plannedOrderQty, unitOfMeasure = unitOfMeasure,
            actionType = actionType, requiredDate = requiredDate
        )
        results.add(result)
        return result
    }

    fun execute() {
        status = MrpStatus.EXECUTED
        executedAt = LocalDateTime.now()
    }
}

@Entity
@Table(name = "mrp_results")
class MrpResult(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mrp_run_id", nullable = false)
    val mrpRun: MrpRun,

    @Column(nullable = false, length = 50)
    val itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    /** Total demand from WOs, SOs, forecasts */
    @Column(nullable = false, precision = 15, scale = 4)
    var grossRequirement: BigDecimal,

    /** Current stock on hand */
    @Column(nullable = false, precision = 15, scale = 4)
    var onHandStock: BigDecimal,

    /** Open POs + In-transit */
    @Column(nullable = false, precision = 15, scale = 4)
    var scheduledReceipts: BigDecimal,

    /** = Gross - OnHand - Scheduled (if > 0) */
    @Column(nullable = false, precision = 15, scale = 4)
    var netRequirement: BigDecimal,

    /** Quantity to order/produce */
    @Column(nullable = false, precision = 15, scale = 4)
    var plannedOrderQty: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var actionType: MrpActionType,

    var requiredDate: LocalDate? = null,

    /** Generated document number (PR or WO) */
    @Column(length = 30)
    var generatedDocNo: String? = null

) : TenantEntity()

/**
 * Production Schedule — calendar view of planned work orders.
 */
@Entity
@Table(name = "production_schedules")
class ProductionSchedule(

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 20)
    var workCenterCode: String,

    @Column(nullable = false)
    var scheduleDate: LocalDate,

    @Column(length = 30)
    var workOrderNo: String? = null,

    @Column(nullable = false, length = 50)
    var productCode: String,

    @Column(nullable = false, length = 200)
    var productName: String,

    @Column(nullable = false, precision = 15, scale = 4)
    var plannedQuantity: BigDecimal,

    /** Planned hours for this schedule slot */
    @Column(nullable = false, precision = 10, scale = 4)
    var plannedHours: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ScheduleStatus = ScheduleStatus.PLANNED,

    var sequenceNo: Int = 0

) : TenantEntity()

/**
 * Capacity Plan — tracks load vs capacity per work center per day.
 */
@Entity
@Table(name = "capacity_plans")
class CapacityPlan(

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 20)
    var workCenterCode: String,

    @Column(nullable = false, length = 200)
    var workCenterName: String,

    @Column(nullable = false)
    var planDate: LocalDate,

    /** Available capacity in hours */
    @Column(nullable = false, precision = 10, scale = 4)
    var availableHours: BigDecimal,

    /** Planned load in hours */
    @Column(nullable = false, precision = 10, scale = 4)
    var plannedLoadHours: BigDecimal = BigDecimal.ZERO,

    /** Actual hours consumed */
    @Column(nullable = false, precision = 10, scale = 4)
    var actualHours: BigDecimal = BigDecimal.ZERO

) : TenantEntity() {

    val remainingCapacity: BigDecimal
        get() = availableHours.subtract(plannedLoadHours)

    val utilizationRate: BigDecimal
        get() = if (availableHours > BigDecimal.ZERO)
            plannedLoadHours.divide(availableHours, 4, java.math.RoundingMode.HALF_UP)
        else BigDecimal.ZERO

    val isOverloaded: Boolean
        get() = plannedLoadHours > availableHours

    fun addLoad(hours: BigDecimal) {
        plannedLoadHours = plannedLoadHours.add(hours)
    }
}

enum class MrpStatus { PLANNED, EXECUTED, CONFIRMED }
enum class MrpActionType { PURCHASE, PRODUCE, NONE }
enum class ScheduleStatus { PLANNED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED }
