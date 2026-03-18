package com.modularerp.production.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.masterdata.repository.BomRepository
import com.modularerp.masterdata.service.BomService
import com.modularerp.production.domain.*
import com.modularerp.production.dto.*
import com.modularerp.production.repository.*
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * 작업지시(WO) 서비스 — 생산 실행의 핵심 서비스.
 *
 * 주요 업무 흐름:
 * 1. WO 생성 시 Routing에서 공정, BOM에서 소요자재를 자동 복사(autoPopulate)
 * 2. 생산 실적 보고 — 공정별/WO 전체 양품·불량 수량 기록
 * 3. 자재 불출 — 생산현장에 자재 출고 기록
 * 4. 완료 시 미완료 공정을 일괄 완료 처리
 */
@Service
@Transactional(readOnly = true)
class WorkOrderService(
    private val woRepository: WorkOrderRepository,
    private val routingRepository: RoutingRepository,
    private val bomService: BomService,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    fun getById(id: Long): WoResponse = findWo(id).toResponse()

    fun search(status: WoStatus?, plantCode: String?, productCode: String?,
               documentNo: String?, pageable: Pageable): Page<WoResponse> =
        woRepository.search(TenantContext.getTenantId(), status, plantCode, productCode, documentNo, pageable)
            .map { it.toResponse() }

    /**
     * WO 생성 — autoPopulate=true 시 Routing에서 공정, BOM 전개로 소요자재를 자동 구성.
     * MRP 실행 결과 또는 수주(SO) 기반으로 생성된다.
     */
    @Transactional
    fun create(request: CreateWorkOrderRequest): WoResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("WO", "WO")

        val wo = WorkOrder(
            documentNo = docNo, companyCode = request.companyCode, plantCode = request.plantCode,
            productCode = request.productCode, productName = request.productName,
            plannedQuantity = request.plannedQuantity, unitOfMeasure = request.unitOfMeasure,
            orderType = request.orderType, priority = request.priority,
            salesOrderNo = request.salesOrderNo,
            plannedStartDate = request.plannedStartDate, plannedEndDate = request.plannedEndDate,
            remark = request.remark
        ).apply { assignTenant(tenantId) }

        if (request.autoPopulate) {
            populateFromRouting(tenantId, wo)
            populateFromBom(tenantId, wo)
        }

        return woRepository.save(wo).toResponse()
    }

    @Transactional
    fun release(id: Long): WoResponse {
        val wo = findWo(id)
        wo.release()
        return woRepository.save(wo).toResponse()
    }

    @Transactional
    fun start(id: Long): WoResponse {
        val wo = findWo(id)
        wo.start()
        return woRepository.save(wo).toResponse()
    }

    /**
     * 생산 실적 보고 — 양품/불량 수량을 WO와 해당 공정에 반영.
     * 공정번호(operationNo) 지정 시 해당 공정의 실적도 함께 갱신한다.
     */
    @Transactional
    fun reportProduction(id: Long, request: ReportProductionRequest): WoResponse {
        val wo = findWo(id)

        // Update specific operation if provided
        if (request.operationNo != null) {
            val op = wo.operations.find { it.operationNo == request.operationNo }
                ?: throw EntityNotFoundException("WorkOrderOperation", request.operationNo)
            if (op.status == OpStatus.PENDING) op.start()
            op.reportProgress(request.goodQuantity, request.scrapQuantity,
                request.actualSetupTime, request.actualRunTime)
        }

        wo.reportProduction(request.goodQuantity, request.scrapQuantity)
        return woRepository.save(wo).toResponse()
    }

    /**
     * 자재 불출 — 창고에서 생산현장으로 자재를 출고 기록.
     * WO 소요자재의 실제 소비량을 갱신한다.
     */
    @Transactional
    fun issueMaterial(id: Long, request: IssueMaterialRequest): WoResponse {
        val wo = findWo(id)
        val mat = wo.materialRequirements.find { it.itemCode == request.itemCode }
            ?: throw EntityNotFoundException("WorkOrderMaterial", request.itemCode)
        mat.issue(request.quantity)
        return woRepository.save(wo).toResponse()
    }

    @Transactional
    fun complete(id: Long): WoResponse {
        val wo = findWo(id)
        wo.complete()
        // 진행 중인 공정을 일괄 완료 처리
        wo.operations.filter { it.status == OpStatus.IN_PROGRESS }.forEach { it.complete() }
        return woRepository.save(wo).toResponse()
    }

    @Transactional
    fun close(id: Long): WoResponse {
        val wo = findWo(id)
        wo.close()
        return woRepository.save(wo).toResponse()
    }

    /** Routing에서 공정 복사 — RELEASED 상태의 최신 Routing에서 공정 정보를 가져옴 */
    private fun populateFromRouting(tenantId: String, wo: WorkOrder) {
        val routing = routingRepository.findActiveRouting(tenantId, wo.productCode, wo.plantCode).orElse(null) ?: return
        wo.routingRevision = routing.revision
        routing.operations.forEach { op ->
            wo.addOperation(op.operationNo, op.operationName, op.workCenterCode,
                op.setupTime, op.runTimePerUnit).assignTenant(tenantId)
        }
    }

    /** BOM 전개로 소요자재 생성 — 팬텀(phantom) 품목은 건너뛰고 실제 자재만 등록 */
    private fun populateFromBom(tenantId: String, wo: WorkOrder) {
        val exploded = bomService.explode(wo.productCode, wo.plantCode, wo.plannedQuantity)
        exploded.filter { !it.phantom }.forEach { line ->
            wo.addMaterial(line.itemCode, line.itemName, line.requiredQuantity,
                line.unitOfMeasure, line.operationNo).assignTenant(tenantId)
        }
    }

    private fun findWo(id: Long): WorkOrder =
        woRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("WorkOrder", id) }
}
