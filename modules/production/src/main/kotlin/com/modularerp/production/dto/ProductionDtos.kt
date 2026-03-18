package com.modularerp.production.dto

import com.modularerp.production.domain.*
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

// === Work Center ===
data class CreateWorkCenterRequest(
    @field:NotBlank val code: String, @field:NotBlank val name: String,
    @field:NotBlank val plantCode: String, val centerType: WorkCenterType = WorkCenterType.MACHINE,
    val capacityPerDay: BigDecimal = BigDecimal("8.00"), val resourceCount: Int = 1,
    val costPerHour: BigDecimal = BigDecimal.ZERO, val setupCost: BigDecimal = BigDecimal.ZERO,
    val description: String? = null
)

data class WorkCenterResponse(
    val id: Long, val code: String, val name: String, val plantCode: String,
    val centerType: WorkCenterType, val capacityPerDay: BigDecimal, val resourceCount: Int,
    val totalDailyCapacity: BigDecimal, val costPerHour: BigDecimal, val setupCost: BigDecimal,
    val status: WorkCenterStatus, val description: String?
)

fun WorkCenter.toResponse() = WorkCenterResponse(
    id = id, code = code, name = name, plantCode = plantCode, centerType = centerType,
    capacityPerDay = capacityPerDay, resourceCount = resourceCount,
    totalDailyCapacity = totalDailyCapacity, costPerHour = costPerHour,
    setupCost = setupCost, status = status, description = description
)

// === Routing ===
data class CreateRoutingRequest(
    @field:NotBlank val productCode: String, @field:NotBlank val productName: String,
    @field:NotBlank val plantCode: String, val revision: String = "001",
    val description: String? = null, val operations: List<OperationInput> = emptyList()
)

data class OperationInput(
    val operationNo: Int, val operationName: String, val workCenterCode: String,
    val setupTime: BigDecimal = BigDecimal.ZERO, val runTimePerUnit: BigDecimal,
    val description: String? = null
)

data class RoutingResponse(
    val id: Long, val productCode: String, val productName: String, val plantCode: String,
    val revision: String, val status: RoutingStatus, val totalStandardTime: BigDecimal,
    val description: String?, val operations: List<OperationResponse>
)

data class OperationResponse(
    val id: Long, val operationNo: Int, val operationName: String, val workCenterCode: String,
    val setupTime: BigDecimal, val runTimePerUnit: BigDecimal, val description: String?
)

fun Routing.toResponse() = RoutingResponse(
    id = id, productCode = productCode, productName = productName, plantCode = plantCode,
    revision = revision, status = status, totalStandardTime = totalStandardTime, description = description,
    operations = operations.map { OperationResponse(it.id, it.operationNo, it.operationName, it.workCenterCode, it.setupTime, it.runTimePerUnit, it.description) }
)

// === Work Order ===
data class CreateWorkOrderRequest(
    @field:NotBlank val companyCode: String, @field:NotBlank val plantCode: String,
    @field:NotBlank val productCode: String, @field:NotBlank val productName: String,
    val plannedQuantity: BigDecimal, val unitOfMeasure: String = "EA",
    val orderType: WoType = WoType.STANDARD, val priority: WoPriority = WoPriority.NORMAL,
    val salesOrderNo: String? = null,
    val plannedStartDate: LocalDate? = null, val plannedEndDate: LocalDate? = null,
    val remark: String? = null,
    /** If true, auto-populate operations from routing and materials from BOM */
    val autoPopulate: Boolean = true
)

data class ReportProductionRequest(
    val operationNo: Int? = null,
    val goodQuantity: BigDecimal,
    val scrapQuantity: BigDecimal = BigDecimal.ZERO,
    val actualSetupTime: BigDecimal? = null,
    val actualRunTime: BigDecimal? = null
)

data class IssueMaterialRequest(
    val itemCode: String, val quantity: BigDecimal
)

data class WoResponse(
    val id: Long, val documentNo: String, val companyCode: String, val plantCode: String,
    val productCode: String, val productName: String,
    val plannedQuantity: BigDecimal, val completedQuantity: BigDecimal,
    val scrapQuantity: BigDecimal, val remainingQuantity: BigDecimal,
    val yieldRate: BigDecimal, val unitOfMeasure: String,
    val status: WoStatus, val orderType: WoType, val priority: WoPriority,
    val salesOrderNo: String?,
    val plannedStartDate: LocalDate?, val plannedEndDate: LocalDate?,
    val actualStartDate: LocalDateTime?, val actualEndDate: LocalDateTime?,
    val remark: String?,
    val operations: List<WoOperationResponse>,
    val materials: List<WoMaterialResponse>
)

data class WoOperationResponse(
    val id: Long, val operationNo: Int, val operationName: String, val workCenterCode: String,
    val setupTime: BigDecimal, val runTimePerUnit: BigDecimal,
    val status: OpStatus, val completedQuantity: BigDecimal, val scrapQuantity: BigDecimal,
    val actualSetupTime: BigDecimal?, val actualRunTime: BigDecimal?,
    val startedAt: LocalDateTime?, val completedAt: LocalDateTime?
)

data class WoMaterialResponse(
    val id: Long, val itemCode: String, val itemName: String,
    val requiredQuantity: BigDecimal, val issuedQuantity: BigDecimal,
    val shortageQuantity: BigDecimal, val unitOfMeasure: String, val operationNo: Int?
)

fun WorkOrder.toResponse() = WoResponse(
    id = id, documentNo = documentNo, companyCode = companyCode, plantCode = plantCode,
    productCode = productCode, productName = productName,
    plannedQuantity = plannedQuantity, completedQuantity = completedQuantity,
    scrapQuantity = scrapQuantity, remainingQuantity = remainingQuantity,
    yieldRate = yieldRate, unitOfMeasure = unitOfMeasure,
    status = status, orderType = orderType, priority = priority,
    salesOrderNo = salesOrderNo,
    plannedStartDate = plannedStartDate, plannedEndDate = plannedEndDate,
    actualStartDate = actualStartDate, actualEndDate = actualEndDate, remark = remark,
    operations = operations.map {
        WoOperationResponse(it.id, it.operationNo, it.operationName, it.workCenterCode,
            it.setupTime, it.runTimePerUnit, it.status, it.completedQuantity, it.scrapQuantity,
            it.actualSetupTime, it.actualRunTime, it.startedAt, it.completedAt)
    },
    materials = materialRequirements.map {
        WoMaterialResponse(it.id, it.itemCode, it.itemName, it.requiredQuantity,
            it.issuedQuantity, it.shortageQuantity, it.unitOfMeasure, it.operationNo)
    }
)
