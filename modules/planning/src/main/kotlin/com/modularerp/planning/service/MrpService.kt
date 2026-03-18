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
 * MRP 엔진 — 자재소요계획의 핵심 서비스.
 *
 * 실행 프로세스:
 * 1. 수요 수집 — 출고지시(RELEASED) 상태의 WO에서 미충족 자재 소요량 집계
 * 2. 총소요량 산출 — 품목별 수요 합산
 * 3. 순소요량 계산 — 총소요량 - 현재고 - 입고예정
 * 4. 계획 오더 생성 — BOM 보유 품목은 PRODUCE, 미보유는 PURCHASE로 분류
 *
 * 결과는 MrpResult에 품목별로 저장되며, 확정(CONFIRMED) 시 실제 PR/WO로 전환된다.
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
     * MRP 실행 — 지정 공장의 자재소요계획을 일괄 수행.
     * 계획 기간(planningHorizonDays) 내의 수요를 대상으로 순소요량을 계산한다.
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

        // 1단계: 출고지시(RELEASED) WO의 미충족 소요자재에서 수요 수집
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

        // 2단계: 품목별 현재고 차감 → 순소요량 산출 → 조치유형(구매/생산) 결정
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
