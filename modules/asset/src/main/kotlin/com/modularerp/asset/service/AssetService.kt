package com.modularerp.asset.service

import com.modularerp.asset.domain.*
import com.modularerp.asset.dto.*
import com.modularerp.asset.repository.*
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
@Transactional(readOnly = true)
class AssetService(
    private val assetRepository: AssetRepository,
    private val depreciationRepository: DepreciationScheduleRepository,
    private val disposalRepository: AssetDisposalRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    fun getById(id: Long): AssetResponse = findAsset(id).toResponse()

    fun search(status: AssetStatus?, category: AssetCategory?, name: String?, pageable: Pageable): Page<AssetResponse> =
        assetRepository.search(TenantContext.getTenantId(), status, category, name, pageable).map { it.toResponse() }

    @Transactional
    fun registerAsset(request: CreateAssetRequest): AssetResponse {
        val tenantId = TenantContext.getTenantId()
        val assetNo = docNumberGenerator.next("FA", "FA")
        val asset = Asset(
            assetNo = assetNo, name = request.name, description = request.description,
            category = request.category, acquisitionDate = request.acquisitionDate,
            acquisitionCost = request.acquisitionCost, usefulLifeMonths = request.usefulLifeMonths,
            depreciationMethod = request.depreciationMethod, salvageValue = request.salvageValue,
            location = request.location, department = request.department,
            responsiblePerson = request.responsiblePerson, serialNumber = request.serialNumber,
            currency = request.currency
        ).apply { assignTenant(tenantId) }
        return assetRepository.save(asset).toResponse()
    }

    @Transactional
    fun updateAsset(id: Long, request: UpdateAssetRequest): AssetResponse {
        val asset = findAsset(id)
        request.name?.let { asset.name = it }
        request.description?.let { asset.description = it }
        request.location?.let { asset.location = it }
        request.department?.let { asset.department = it }
        request.responsiblePerson?.let { asset.responsiblePerson = it }
        return assetRepository.save(asset).toResponse()
    }

    @Transactional
    fun activateAsset(id: Long): AssetResponse {
        val asset = findAsset(id)
        asset.activateAsset()
        return assetRepository.save(asset).toResponse()
    }

    @Transactional
    fun disposeAsset(id: Long, request: DisposeAssetRequest): AssetDisposalResponse {
        val tenantId = TenantContext.getTenantId()
        val asset = findAsset(id)
        val bookValue = asset.bookValue
        val gainLoss = request.disposalAmount.subtract(bookValue)

        val disposal = AssetDisposal(
            asset = asset, disposalDate = request.disposalDate,
            disposalType = request.disposalType, disposalAmount = request.disposalAmount,
            bookValueAtDisposal = bookValue, gainLoss = gainLoss, reason = request.reason
        ).apply { assignTenant(tenantId) }

        if (request.disposalType == DisposalType.SCRAP) asset.scrap() else asset.dispose()
        assetRepository.save(asset)
        return disposalRepository.save(disposal).toResponse()
    }

    /**
     * Calculate monthly depreciation for a single asset (straight-line).
     */
    fun calculateDepreciation(asset: Asset): BigDecimal {
        if (asset.status != AssetStatus.ACTIVE) return BigDecimal.ZERO
        val depreciableAmount = asset.acquisitionCost.subtract(asset.salvageValue)
        return when (asset.depreciationMethod) {
            DepreciationMethod.STRAIGHT_LINE ->
                depreciableAmount.divide(BigDecimal(asset.usefulLifeMonths), 4, RoundingMode.HALF_UP)
            DepreciationMethod.DECLINING_BALANCE -> {
                val rate = BigDecimal(2).divide(BigDecimal(asset.usefulLifeMonths), 10, RoundingMode.HALF_UP)
                asset.bookValue.multiply(rate).setScale(4, RoundingMode.HALF_UP)
            }
            DepreciationMethod.SUM_OF_YEARS -> {
                // Simplified: use straight-line as fallback
                depreciableAmount.divide(BigDecimal(asset.usefulLifeMonths), 4, RoundingMode.HALF_UP)
            }
        }
    }

    /**
     * Run depreciation for all active assets for a given year/month.
     */
    @Transactional
    fun runDepreciation(year: Int, month: Int): List<DepreciationScheduleResponse> {
        val tenantId = TenantContext.getTenantId()
        val activeAssets = assetRepository.findAllActive(tenantId)
        val results = mutableListOf<DepreciationSchedule>()

        for (asset in activeAssets) {
            val amount = calculateDepreciation(asset)
            if (amount.compareTo(BigDecimal.ZERO) <= 0) continue
            // Don't depreciate below salvage value
            val maxDepreciation = asset.bookValue.subtract(asset.salvageValue)
            val actualAmount = amount.min(maxDepreciation)
            if (actualAmount.compareTo(BigDecimal.ZERO) <= 0) continue

            asset.accumulatedDepreciation = asset.accumulatedDepreciation.add(actualAmount)
            assetRepository.save(asset)

            val schedule = DepreciationSchedule(
                asset = asset, periodYear = year, periodMonth = month,
                depreciationAmount = actualAmount,
                accumulatedAmount = asset.accumulatedDepreciation,
                bookValueAfter = asset.bookValue
            ).apply { assignTenant(tenantId) }
            results.add(depreciationRepository.save(schedule))
        }
        return results.map { it.toResponse() }
    }

    fun getDepreciationSchedule(assetId: Long): List<DepreciationScheduleResponse> =
        depreciationRepository.findByAsset(TenantContext.getTenantId(), assetId).map { it.toResponse() }

    fun getAssetSummary(): AssetSummaryResponse {
        val assets = assetRepository.findAllActive(TenantContext.getTenantId())
        val byCategory = assets.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.bookValue } }
        return AssetSummaryResponse(
            totalAssets = assets.size,
            totalAcquisitionCost = assets.sumOf { it.acquisitionCost },
            totalAccumulatedDepreciation = assets.sumOf { it.accumulatedDepreciation },
            totalBookValue = assets.sumOf { it.bookValue },
            byCategory = byCategory
        )
    }

    private fun findAsset(id: Long): Asset =
        assetRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("Asset", id) }
}
