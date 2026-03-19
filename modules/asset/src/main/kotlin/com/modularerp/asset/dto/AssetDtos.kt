package com.modularerp.asset.dto

import com.modularerp.asset.domain.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

// --- Asset DTOs ---

data class CreateAssetRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    @field:NotNull val category: AssetCategory,
    @field:NotNull val acquisitionDate: LocalDate,
    @field:NotNull val acquisitionCost: BigDecimal,
    @field:NotNull val usefulLifeMonths: Int,
    val depreciationMethod: DepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
    val salvageValue: BigDecimal = BigDecimal.ZERO,
    val location: String? = null,
    val department: String? = null,
    val responsiblePerson: String? = null,
    val serialNumber: String? = null,
    val currency: String = "KRW"
)

data class UpdateAssetRequest(
    val name: String? = null,
    val description: String? = null,
    val location: String? = null,
    val department: String? = null,
    val responsiblePerson: String? = null
)

data class AssetResponse(
    val id: Long,
    val assetNo: String,
    val name: String,
    val description: String?,
    val category: AssetCategory,
    val status: AssetStatus,
    val acquisitionDate: LocalDate,
    val acquisitionCost: BigDecimal,
    val usefulLifeMonths: Int,
    val depreciationMethod: DepreciationMethod,
    val salvageValue: BigDecimal,
    val accumulatedDepreciation: BigDecimal,
    val bookValue: BigDecimal,
    val location: String?,
    val department: String?,
    val responsiblePerson: String?,
    val serialNumber: String?,
    val currency: String
)

fun Asset.toResponse() = AssetResponse(
    id = id, assetNo = assetNo, name = name, description = description,
    category = category, status = status, acquisitionDate = acquisitionDate,
    acquisitionCost = acquisitionCost, usefulLifeMonths = usefulLifeMonths,
    depreciationMethod = depreciationMethod, salvageValue = salvageValue,
    accumulatedDepreciation = accumulatedDepreciation, bookValue = bookValue,
    location = location, department = department, responsiblePerson = responsiblePerson,
    serialNumber = serialNumber, currency = currency
)

// --- Depreciation Schedule DTOs ---

data class DepreciationScheduleResponse(
    val id: Long,
    val assetId: Long,
    val periodYear: Int,
    val periodMonth: Int,
    val depreciationAmount: BigDecimal,
    val accumulatedAmount: BigDecimal,
    val bookValueAfter: BigDecimal,
    val posted: Boolean,
    val journalEntryId: Long?
)

fun DepreciationSchedule.toResponse() = DepreciationScheduleResponse(
    id = id, assetId = asset.id, periodYear = periodYear, periodMonth = periodMonth,
    depreciationAmount = depreciationAmount, accumulatedAmount = accumulatedAmount,
    bookValueAfter = bookValueAfter, posted = posted, journalEntryId = journalEntryId
)

// --- Disposal DTOs ---

data class DisposeAssetRequest(
    @field:NotNull val disposalDate: LocalDate,
    @field:NotNull val disposalType: DisposalType,
    val disposalAmount: BigDecimal = BigDecimal.ZERO,
    val reason: String? = null
)

data class AssetDisposalResponse(
    val id: Long,
    val assetId: Long,
    val disposalDate: LocalDate,
    val disposalType: DisposalType,
    val disposalAmount: BigDecimal,
    val bookValueAtDisposal: BigDecimal,
    val gainLoss: BigDecimal,
    val reason: String?,
    val approvedBy: String?
)

fun AssetDisposal.toResponse() = AssetDisposalResponse(
    id = id, assetId = asset.id, disposalDate = disposalDate,
    disposalType = disposalType, disposalAmount = disposalAmount,
    bookValueAtDisposal = bookValueAtDisposal, gainLoss = gainLoss,
    reason = reason, approvedBy = approvedBy
)

// --- Run Depreciation Request ---

data class RunDepreciationRequest(
    @field:NotNull val year: Int,
    @field:NotNull val month: Int
)

// --- Asset Summary ---

data class AssetSummaryResponse(
    val totalAssets: Int,
    val totalAcquisitionCost: BigDecimal,
    val totalAccumulatedDepreciation: BigDecimal,
    val totalBookValue: BigDecimal,
    val byCategory: Map<AssetCategory, BigDecimal>
)
