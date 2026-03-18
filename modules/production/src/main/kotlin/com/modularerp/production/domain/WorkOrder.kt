package com.modularerp.production.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Work Order (WO) — a production order to manufacture a specific quantity.
 * Links to BOM (for material) and Routing (for operations).
 * Tracks material consumption, operation progress, and output.
 */
@Entity
@Table(name = "work_orders")
class WorkOrder(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 50)
    var productCode: String,

    @Column(nullable = false, length = 200)
    var productName: String,

    /** Planned production quantity */
    @Column(nullable = false, precision = 15, scale = 4)
    var plannedQuantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String = "EA",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: WoStatus = WoStatus.PLANNED,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var orderType: WoType = WoType.STANDARD,

    /** Reference to sales order if make-to-order */
    @Column(length = 30)
    var salesOrderNo: String? = null,

    var plannedStartDate: LocalDate? = null,
    var plannedEndDate: LocalDate? = null,
    var actualStartDate: LocalDateTime? = null,
    var actualEndDate: LocalDateTime? = null,

    @Column(length = 10)
    var bomRevision: String? = null,

    @Column(length = 10)
    var routingRevision: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var priority: WoPriority = WoPriority.NORMAL,

    @Column(length = 1000)
    var remark: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "workOrder", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("operationNo ASC")
    val operations: MutableList<WorkOrderOperation> = mutableListOf()

    @OneToMany(mappedBy = "workOrder", cascade = [CascadeType.ALL], orphanRemoval = true)
    val materialRequirements: MutableList<WorkOrderMaterial> = mutableListOf()

    /** Completed (good) quantity from all operations' results */
    @Column(nullable = false, precision = 15, scale = 4)
    var completedQuantity: BigDecimal = BigDecimal.ZERO
        protected set

    @Column(nullable = false, precision = 15, scale = 4)
    var scrapQuantity: BigDecimal = BigDecimal.ZERO
        protected set

    val remainingQuantity: BigDecimal
        get() = plannedQuantity.subtract(completedQuantity)

    val yieldRate: BigDecimal
        get() {
            val total = completedQuantity.add(scrapQuantity)
            return if (total > BigDecimal.ZERO) completedQuantity.divide(total, 4, java.math.RoundingMode.HALF_UP)
            else BigDecimal.ZERO
        }

    fun addOperation(
        operationNo: Int, operationName: String, workCenterCode: String,
        setupTime: BigDecimal, runTimePerUnit: BigDecimal
    ): WorkOrderOperation {
        val op = WorkOrderOperation(
            workOrder = this, operationNo = operationNo, operationName = operationName,
            workCenterCode = workCenterCode, setupTime = setupTime, runTimePerUnit = runTimePerUnit
        )
        operations.add(op)
        return op
    }

    fun addMaterial(
        itemCode: String, itemName: String, requiredQuantity: BigDecimal,
        unitOfMeasure: String, operationNo: Int? = null
    ): WorkOrderMaterial {
        val mat = WorkOrderMaterial(
            workOrder = this, itemCode = itemCode, itemName = itemName,
            requiredQuantity = requiredQuantity, unitOfMeasure = unitOfMeasure, operationNo = operationNo
        )
        materialRequirements.add(mat)
        return mat
    }

    fun release() {
        check(status == WoStatus.PLANNED) { "Can only release from PLANNED" }
        status = WoStatus.RELEASED
    }

    fun start() {
        check(status == WoStatus.RELEASED) { "Can only start from RELEASED" }
        status = WoStatus.IN_PROGRESS
        actualStartDate = LocalDateTime.now()
    }

    fun reportProduction(goodQty: BigDecimal, scrapQty: BigDecimal = BigDecimal.ZERO) {
        check(status == WoStatus.IN_PROGRESS) { "Must be IN_PROGRESS to report" }
        completedQuantity = completedQuantity.add(goodQty)
        scrapQuantity = scrapQuantity.add(scrapQty)
    }

    fun complete() {
        check(status == WoStatus.IN_PROGRESS) { "Can only complete from IN_PROGRESS" }
        status = WoStatus.COMPLETED
        actualEndDate = LocalDateTime.now()
    }

    fun close() {
        check(status == WoStatus.COMPLETED) { "Can only close from COMPLETED" }
        status = WoStatus.CLOSED
    }
}

@Entity
@Table(name = "work_order_operations")
class WorkOrderOperation(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    val workOrder: WorkOrder,

    val operationNo: Int,

    @Column(nullable = false, length = 200)
    var operationName: String,

    @Column(nullable = false, length = 20)
    var workCenterCode: String,

    @Column(nullable = false, precision = 10, scale = 4)
    var setupTime: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 10, scale = 6)
    var runTimePerUnit: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OpStatus = OpStatus.PENDING,

    /** Actual time spent */
    @Column(precision = 10, scale = 4)
    var actualSetupTime: BigDecimal? = null,

    @Column(precision = 10, scale = 4)
    var actualRunTime: BigDecimal? = null,

    @Column(precision = 15, scale = 4)
    var completedQuantity: BigDecimal = BigDecimal.ZERO,

    @Column(precision = 15, scale = 4)
    var scrapQuantity: BigDecimal = BigDecimal.ZERO,

    var startedAt: LocalDateTime? = null,
    var completedAt: LocalDateTime? = null

) : TenantEntity() {

    fun start() {
        check(status == OpStatus.PENDING) { "Operation already started" }
        status = OpStatus.IN_PROGRESS
        startedAt = LocalDateTime.now()
    }

    fun reportProgress(goodQty: BigDecimal, scrapQty: BigDecimal = BigDecimal.ZERO,
                       actualSetup: BigDecimal? = null, actualRun: BigDecimal? = null) {
        check(status == OpStatus.IN_PROGRESS) { "Operation not in progress" }
        completedQuantity = completedQuantity.add(goodQty)
        scrapQuantity = scrapQuantity.add(scrapQty)
        actualSetup?.let { actualSetupTime = it }
        actualRun?.let { actualRunTime = it }
    }

    fun complete() {
        check(status == OpStatus.IN_PROGRESS)
        status = OpStatus.COMPLETED
        completedAt = LocalDateTime.now()
    }
}

@Entity
@Table(name = "work_order_materials")
class WorkOrderMaterial(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    val workOrder: WorkOrder,

    @Column(nullable = false, length = 50)
    var itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    @Column(nullable = false, precision = 15, scale = 4)
    var requiredQuantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String,

    /** Which operation consumes this material */
    var operationNo: Int? = null,

    /** Actually issued quantity */
    @Column(nullable = false, precision = 15, scale = 4)
    var issuedQuantity: BigDecimal = BigDecimal.ZERO

) : TenantEntity() {

    val shortageQuantity: BigDecimal
        get() = requiredQuantity.subtract(issuedQuantity).coerceAtLeast(BigDecimal.ZERO)

    fun issue(qty: BigDecimal) {
        issuedQuantity = issuedQuantity.add(qty)
    }
}

enum class WoStatus { PLANNED, RELEASED, IN_PROGRESS, COMPLETED, CLOSED, CANCELLED }
enum class WoType { STANDARD, REWORK, PROTOTYPE, MAINTENANCE }
enum class WoPriority { LOW, NORMAL, HIGH, URGENT }
enum class OpStatus { PENDING, IN_PROGRESS, COMPLETED }
