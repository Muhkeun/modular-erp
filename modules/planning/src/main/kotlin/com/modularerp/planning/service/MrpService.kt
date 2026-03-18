package com.modularerp.planning.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.logistics.repository.StockSummaryRepository
import com.modularerp.masterdata.service.BomService
import com.modularerp.planning.domain.*
import com.modularerp.planning.dto.*
import com.modularerp.planning.repository.*
import com.modularerp.production.repository.WorkOrderRepository
import com.modularerp.production.domain.WoStatus
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * MRP Engine — the core of manufacturing planning.
 *
 * Process:
 * 1. Collect demand (open work orders + sales orders)
 * 2. Explode BOMs to get component requirements (gross requirements)
 * 3. Net against current stock and open POs (net requirements)
 * 4. Generate planned orders: PURCHASE for bought items, PRODUCE for made items
 */
@Service
@Transactional(readOnly = true)
class MrpService(
    private val mrpRunRepository: MrpRunRepository,
    private val stockRepository: StockSummaryRepository,
    private val workOrderRepository: WorkOrderRepository,
    private val bomService: BomService
) {

    fun getById(id: Long): MrpRunResponse =
        mrpRunRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("MrpRun", id) }.toResponse()

    fun findRecent(pageable: Pageable): Page<MrpRunResponse> =
        mrpRunRepository.findRecent(TenantContext.getTenantId(), pageable).map { it.toResponse() }

    /**
     * Execute MRP for a plant.
     */
    @Transactional
    fun runMrp(request: RunMrpRequest): MrpRunResponse {
        val tenantId = TenantContext.getTenantId()
        val horizonEnd = LocalDate.now().plusDays(request.planningHorizonDays.toLong())

        val mrpRun = MrpRun(
            plantCode = request.plantCode,
            planningHorizonDays = request.planningHorizonDays,
            executedBy = TenantContext.getUserId()
        ).apply { assignTenant(tenantId) }

        // 1. Collect demand from open work orders
        val demand = mutableMapOf<String, DemandEntry>()
        val openWos = workOrderRepository.search(
            tenantId, WoStatus.RELEASED, request.plantCode, null, null, PageRequest.of(0, 1000)
        ).content

        for (wo in openWos) {
            for (mat in wo.materialRequirements) {
                val shortage = mat.shortageQuantity
                if (shortage > BigDecimal.ZERO) {
                    demand.merge(mat.itemCode, DemandEntry(mat.itemCode, mat.itemName, shortage, mat.unitOfMeasure, wo.plannedStartDate)) { a, b ->
                        a.copy(grossRequirement = a.grossRequirement.add(b.grossRequirement))
                    }
                }
            }
        }

        // 2. For each demanded item, net against stock and open orders
        for ((itemCode, entry) in demand) {
            val stock = stockRepository.findByTenantIdAndItemCodeAndPlantCodeAndStorageLocation(
                tenantId, itemCode, request.plantCode, "MAIN"
            ).map { it.availableQuantity }.orElse(BigDecimal.ZERO)

            val netReq = entry.grossRequirement.subtract(stock).coerceAtLeast(BigDecimal.ZERO)

            // Determine action: check if item has its own BOM (= produce) or not (= purchase)
            val hasBom = bomService.explode(itemCode, request.plantCode, BigDecimal.ONE).isNotEmpty()
            val actionType = if (hasBom) MrpActionType.PRODUCE else MrpActionType.PURCHASE

            mrpRun.addResult(
                itemCode = itemCode, itemName = entry.itemName,
                grossRequirement = entry.grossRequirement, onHandStock = stock,
                scheduledReceipts = BigDecimal.ZERO, netRequirement = netReq,
                plannedOrderQty = if (netReq > BigDecimal.ZERO) netReq else BigDecimal.ZERO,
                unitOfMeasure = entry.unitOfMeasure,
                actionType = if (netReq > BigDecimal.ZERO) actionType else MrpActionType.NONE,
                requiredDate = entry.requiredDate
            ).assignTenant(tenantId)
        }

        mrpRun.execute()
        return mrpRunRepository.save(mrpRun).toResponse()
    }

    private data class DemandEntry(
        val itemCode: String, val itemName: String,
        val grossRequirement: BigDecimal, val unitOfMeasure: String,
        val requiredDate: LocalDate?
    )
}
