package com.modularerp.planning.dto

import com.modularerp.planning.domain.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class RunMrpRequest(
    val plantCode: String,
    val planningHorizonDays: Int = 30
)

data class MrpRunResponse(
    val id: Long, val plantCode: String, val planningHorizonDays: Int,
    val status: MrpStatus, val executedAt: LocalDateTime?, val executedBy: String?,
    val results: List<MrpResultResponse>
)

data class MrpResultResponse(
    val id: Long, val itemCode: String, val itemName: String,
    val grossRequirement: BigDecimal, val onHandStock: BigDecimal,
    val scheduledReceipts: BigDecimal, val netRequirement: BigDecimal,
    val plannedOrderQty: BigDecimal, val unitOfMeasure: String,
    val actionType: MrpActionType, val requiredDate: LocalDate?,
    val generatedDocNo: String?
)

fun MrpRun.toResponse() = MrpRunResponse(
    id = id, plantCode = plantCode, planningHorizonDays = planningHorizonDays,
    status = status, executedAt = executedAt, executedBy = executedBy,
    results = results.map {
        MrpResultResponse(it.id, it.itemCode, it.itemName, it.grossRequirement, it.onHandStock,
            it.scheduledReceipts, it.netRequirement, it.plannedOrderQty, it.unitOfMeasure,
            it.actionType, it.requiredDate, it.generatedDocNo)
    }
)

data class CapacityPlanResponse(
    val id: Long, val plantCode: String, val workCenterCode: String, val workCenterName: String,
    val planDate: LocalDate, val availableHours: BigDecimal,
    val plannedLoadHours: BigDecimal, val actualHours: BigDecimal,
    val remainingCapacity: BigDecimal, val utilizationRate: BigDecimal, val isOverloaded: Boolean
)

fun CapacityPlan.toResponse() = CapacityPlanResponse(
    id = id, plantCode = plantCode, workCenterCode = workCenterCode, workCenterName = workCenterName,
    planDate = planDate, availableHours = availableHours, plannedLoadHours = plannedLoadHours,
    actualHours = actualHours, remainingCapacity = remainingCapacity,
    utilizationRate = utilizationRate, isOverloaded = isOverloaded
)

data class ScheduleResponse(
    val id: Long, val plantCode: String, val workCenterCode: String,
    val scheduleDate: LocalDate, val workOrderNo: String?,
    val productCode: String, val productName: String,
    val plannedQuantity: BigDecimal, val plannedHours: BigDecimal,
    val status: ScheduleStatus, val sequenceNo: Int
)

fun ProductionSchedule.toResponse() = ScheduleResponse(
    id = id, plantCode = plantCode, workCenterCode = workCenterCode,
    scheduleDate = scheduleDate, workOrderNo = workOrderNo,
    productCode = productCode, productName = productName,
    plannedQuantity = plannedQuantity, plannedHours = plannedHours,
    status = status, sequenceNo = sequenceNo
)
