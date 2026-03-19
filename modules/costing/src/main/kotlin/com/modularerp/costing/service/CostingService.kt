package com.modularerp.costing.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.costing.domain.*
import com.modularerp.costing.dto.*
import com.modularerp.costing.repository.*
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CostingService(
    private val costCenterRepository: CostCenterRepository,
    private val standardCostRepository: StandardCostRepository,
    private val costAllocationRepository: CostAllocationRepository,
    private val productCostRepository: ProductCostRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    // ── CostCenter ──

    fun getCostCenterById(id: Long): CostCenterResponse = findCostCenter(id).toResponse()

    fun searchCostCenters(status: CostCenterStatus?, costCenterCode: String?,
                          pageable: Pageable): Page<CostCenterResponse> =
        costCenterRepository.search(TenantContext.getTenantId(), status, costCenterCode, pageable)
            .map { it.toResponse() }

    @Transactional
    fun createCostCenter(request: CreateCostCenterRequest): CostCenterResponse {
        val tenantId = TenantContext.getTenantId()
        val cc = CostCenter(
            costCenterCode = request.costCenterCode, costCenterName = request.costCenterName,
            parentCode = request.parentCode, departmentCode = request.departmentCode,
            managerName = request.managerName, status = request.status
        ).apply { assignTenant(tenantId) }
        return costCenterRepository.save(cc).toResponse()
    }

    @Transactional
    fun updateCostCenter(id: Long, request: CreateCostCenterRequest): CostCenterResponse {
        val cc = findCostCenter(id)
        cc.costCenterName = request.costCenterName
        cc.parentCode = request.parentCode
        cc.departmentCode = request.departmentCode
        cc.managerName = request.managerName
        cc.status = request.status
        return costCenterRepository.save(cc).toResponse()
    }

    @Transactional
    fun deleteCostCenter(id: Long) { findCostCenter(id).deactivate() }

    // ── StandardCost ──

    fun getStandardCostById(id: Long): StandardCostResponse = findStandardCost(id).toResponse()

    fun searchStandardCosts(itemCode: String?, costType: CostType?,
                            pageable: Pageable): Page<StandardCostResponse> =
        standardCostRepository.search(TenantContext.getTenantId(), itemCode, costType, pageable)
            .map { it.toResponse() }

    @Transactional
    fun createStandardCost(request: CreateStandardCostRequest): StandardCostResponse {
        val tenantId = TenantContext.getTenantId()
        val sc = StandardCost(
            itemCode = request.itemCode, costType = request.costType,
            standardRate = request.standardRate, effectiveFrom = request.effectiveFrom,
            effectiveTo = request.effectiveTo, currency = request.currency, notes = request.notes
        ).apply { assignTenant(tenantId) }
        return standardCostRepository.save(sc).toResponse()
    }

    @Transactional
    fun updateStandardCost(id: Long, request: CreateStandardCostRequest): StandardCostResponse {
        val sc = findStandardCost(id)
        sc.costType = request.costType
        sc.standardRate = request.standardRate
        sc.effectiveFrom = request.effectiveFrom
        sc.effectiveTo = request.effectiveTo
        sc.currency = request.currency
        sc.notes = request.notes
        return standardCostRepository.save(sc).toResponse()
    }

    // ── ProductCost ──

    fun searchProductCosts(itemCode: String?, fiscalYear: Int?,
                           pageable: Pageable): Page<ProductCostResponse> =
        productCostRepository.search(TenantContext.getTenantId(), itemCode, fiscalYear, pageable)
            .map { it.toResponse() }

    @Transactional
    fun calculateProductCost(request: CalculateProductCostRequest): ProductCostResponse {
        val tenantId = TenantContext.getTenantId()
        val effectiveDate = LocalDate.now()
        val standards = standardCostRepository.findEffective(tenantId, request.itemCode, effectiveDate)

        val materialCost = standards.filter { it.costType == CostType.MATERIAL }
            .sumOf { it.standardRate.multiply(request.quantity) }
        val laborCost = standards.filter { it.costType == CostType.LABOR }
            .sumOf { it.standardRate.multiply(request.quantity) }
        val overheadCost = standards.filter { it.costType == CostType.OVERHEAD || it.costType == CostType.SUBCONTRACTING }
            .sumOf { it.standardRate.multiply(request.quantity) }

        val pc = ProductCost(
            itemCode = request.itemCode, fiscalYear = request.fiscalYear,
            period = request.period, materialCost = materialCost,
            laborCost = laborCost, overheadCost = overheadCost,
            quantity = request.quantity
        ).apply {
            assignTenant(tenantId)
            calculate()
        }

        return productCostRepository.save(pc).toResponse()
    }

    // ── CostAllocation ──

    fun searchAllocations(status: CostAllocationStatus?, fiscalYear: Int?,
                          pageable: Pageable): Page<CostAllocationResponse> =
        costAllocationRepository.search(TenantContext.getTenantId(), status, fiscalYear, pageable)
            .map { it.toResponse() }

    @Transactional
    fun createAllocation(request: CreateCostAllocationRequest): CostAllocationResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("CA", "CA")
        val ca = CostAllocation(
            documentNo = docNo, allocationDate = request.allocationDate,
            fromCostCenter = request.fromCostCenter, toCostCenter = request.toCostCenter,
            allocationType = request.allocationType, amount = request.amount,
            allocationBasis = request.allocationBasis, percentage = request.percentage,
            description = request.description, fiscalYear = request.fiscalYear, period = request.period
        ).apply { assignTenant(tenantId) }
        return costAllocationRepository.save(ca).toResponse()
    }

    @Transactional
    fun postAllocation(id: Long): CostAllocationResponse {
        val ca = findAllocation(id)
        ca.post()
        return costAllocationRepository.save(ca).toResponse()
    }

    fun getVarianceAnalysis(itemCode: String): List<VarianceResponse> {
        val tenantId = TenantContext.getTenantId()
        val standards = standardCostRepository.findEffective(tenantId, itemCode, LocalDate.now())
        // Return standard rates as baseline; actual rates would come from production data
        return standards.map { sc ->
            VarianceResponse(
                itemCode = sc.itemCode, costType = sc.costType,
                standardRate = sc.standardRate, actualRate = sc.standardRate,
                variance = BigDecimal.ZERO, variancePercentage = BigDecimal.ZERO
            )
        }
    }

    // ── Private helpers ──

    private fun findCostCenter(id: Long): CostCenter =
        costCenterRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("CostCenter", id) }

    private fun findStandardCost(id: Long): StandardCost =
        standardCostRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("StandardCost", id) }

    private fun findAllocation(id: Long): CostAllocation =
        costAllocationRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("CostAllocation", id) }
}
