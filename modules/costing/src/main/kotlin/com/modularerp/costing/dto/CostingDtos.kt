package com.modularerp.costing.dto

import com.modularerp.costing.domain.*
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

// ── CostCenter DTOs ──

data class CreateCostCenterRequest(
    @field:NotBlank val costCenterCode: String,
    @field:NotBlank val costCenterName: String,
    val parentCode: String? = null,
    val departmentCode: String? = null,
    val managerName: String? = null,
    val status: CostCenterStatus = CostCenterStatus.ACTIVE
)

data class CostCenterResponse(
    val id: Long, val costCenterCode: String, val costCenterName: String,
    val parentCode: String?, val departmentCode: String?, val managerName: String?,
    val status: CostCenterStatus
)

fun CostCenter.toResponse() = CostCenterResponse(
    id = id, costCenterCode = costCenterCode, costCenterName = costCenterName,
    parentCode = parentCode, departmentCode = departmentCode,
    managerName = managerName, status = status
)

// ── StandardCost DTOs ──

data class CreateStandardCostRequest(
    @field:NotBlank val itemCode: String,
    val costType: CostType = CostType.MATERIAL,
    val standardRate: BigDecimal = BigDecimal.ZERO,
    val effectiveFrom: LocalDate = LocalDate.now(),
    val effectiveTo: LocalDate? = null,
    val currency: String = "KRW",
    val notes: String? = null
)

data class StandardCostResponse(
    val id: Long, val itemCode: String, val costType: CostType,
    val standardRate: BigDecimal, val effectiveFrom: LocalDate, val effectiveTo: LocalDate?,
    val currency: String, val notes: String?
)

fun StandardCost.toResponse() = StandardCostResponse(
    id = id, itemCode = itemCode, costType = costType, standardRate = standardRate,
    effectiveFrom = effectiveFrom, effectiveTo = effectiveTo,
    currency = currency, notes = notes
)

// ── CostAllocation DTOs ──

data class CreateCostAllocationRequest(
    val allocationDate: LocalDate = LocalDate.now(),
    @field:NotBlank val fromCostCenter: String,
    @field:NotBlank val toCostCenter: String,
    val allocationType: AllocationType = AllocationType.DIRECT,
    val amount: BigDecimal = BigDecimal.ZERO,
    val allocationBasis: String? = null,
    val percentage: BigDecimal? = null,
    val description: String? = null,
    val fiscalYear: Int,
    val period: Int
)

data class CostAllocationResponse(
    val id: Long, val documentNo: String, val allocationDate: LocalDate,
    val fromCostCenter: String, val toCostCenter: String, val allocationType: AllocationType,
    val amount: BigDecimal, val allocationBasis: String?, val percentage: BigDecimal?,
    val description: String?, val status: CostAllocationStatus,
    val fiscalYear: Int, val period: Int
)

fun CostAllocation.toResponse() = CostAllocationResponse(
    id = id, documentNo = documentNo, allocationDate = allocationDate,
    fromCostCenter = fromCostCenter, toCostCenter = toCostCenter,
    allocationType = allocationType, amount = amount, allocationBasis = allocationBasis,
    percentage = percentage, description = description, status = status,
    fiscalYear = fiscalYear, period = period
)

// ── ProductCost DTOs ──

data class CalculateProductCostRequest(
    @field:NotBlank val itemCode: String,
    val fiscalYear: Int,
    val period: Int,
    val quantity: BigDecimal = BigDecimal.ONE
)

data class ProductCostResponse(
    val id: Long, val itemCode: String, val fiscalYear: Int, val period: Int,
    val materialCost: BigDecimal, val laborCost: BigDecimal, val overheadCost: BigDecimal,
    val totalCost: BigDecimal, val unitCost: BigDecimal, val quantity: BigDecimal,
    val currency: String, val calculated: Boolean, val calculatedAt: LocalDateTime?
)

fun ProductCost.toResponse() = ProductCostResponse(
    id = id, itemCode = itemCode, fiscalYear = fiscalYear, period = period,
    materialCost = materialCost, laborCost = laborCost, overheadCost = overheadCost,
    totalCost = totalCost, unitCost = unitCost, quantity = quantity,
    currency = currency, calculated = calculated, calculatedAt = calculatedAt
)

// ── Variance ──

data class VarianceResponse(
    val itemCode: String, val costType: CostType,
    val standardRate: BigDecimal, val actualRate: BigDecimal,
    val variance: BigDecimal, val variancePercentage: BigDecimal
)

data class CostCenterSummaryResponse(
    val costCenterCode: String, val costCenterName: String,
    val totalAllocated: BigDecimal, val totalReceived: BigDecimal
)
