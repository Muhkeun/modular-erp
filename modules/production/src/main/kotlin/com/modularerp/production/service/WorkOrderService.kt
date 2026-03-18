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
     * Create a Work Order, optionally auto-populating operations from Routing
     * and material requirements from BOM explosion.
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
     * Report production output — updates WO and operation quantities.
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
     * Issue material to work order — records actual consumption.
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
        // Auto-complete remaining operations
        wo.operations.filter { it.status == OpStatus.IN_PROGRESS }.forEach { it.complete() }
        return woRepository.save(wo).toResponse()
    }

    @Transactional
    fun close(id: Long): WoResponse {
        val wo = findWo(id)
        wo.close()
        return woRepository.save(wo).toResponse()
    }

    private fun populateFromRouting(tenantId: String, wo: WorkOrder) {
        val routing = routingRepository.findActiveRouting(tenantId, wo.productCode, wo.plantCode).orElse(null) ?: return
        wo.routingRevision = routing.revision
        routing.operations.forEach { op ->
            wo.addOperation(op.operationNo, op.operationName, op.workCenterCode,
                op.setupTime, op.runTimePerUnit).assignTenant(tenantId)
        }
    }

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
