package com.modularerp.purchase.service

import com.modularerp.core.event.DomainEvent
import com.modularerp.core.event.DomainEventPublisher
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.purchase.domain.*
import com.modularerp.purchase.dto.*
import com.modularerp.purchase.repository.PurchaseOrderRepository
import com.modularerp.purchase.repository.PurchaseRequestRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** PO 승인 이벤트 — 물류 모듈에서 수신하여 입고(GR) 준비를 시작한다 */
class PurchaseOrderApprovedEvent(
    tenantId: String,
    val poId: Long,
    val documentNo: String,
    val vendorCode: String
) : DomainEvent(tenantId = tenantId)

/**
 * 발주서(PO) 서비스 — 발주 생성/승인/PR 연동을 담당.
 *
 * 주요 업무 흐름:
 * 1. 직접 PO 생성 또는 승인된 PR에서 PO 전환
 * 2. PR → PO 전환 시 PR의 미전환 잔량(openQuantity)을 자동 차감
 * 3. PO 승인 시 PurchaseOrderApprovedEvent를 발행하여 물류 모듈에 알림
 */
@Service
@Transactional(readOnly = true)
class PurchaseOrderService(
    private val poRepository: PurchaseOrderRepository,
    private val prRepository: PurchaseRequestRepository,
    private val docNumberGenerator: DocumentNumberGenerator,
    private val eventPublisher: DomainEventPublisher
) {

    fun getById(id: Long): PoResponse {
        return findPo(id).toResponse()
    }

    fun search(status: PoStatus?, vendorCode: String?, documentNo: String?, pageable: Pageable): Page<PoResponse> {
        val tenantId = TenantContext.getTenantId()
        return poRepository.search(tenantId, status, vendorCode, documentNo, pageable).map { it.toResponse() }
    }

    @Transactional
    fun create(request: CreatePoRequest): PoResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("PO", "PO")

        val po = PurchaseOrder(
            documentNo = docNo,
            companyCode = request.companyCode,
            plantCode = request.plantCode,
            vendorCode = request.vendorCode,
            vendorName = request.vendorName,
            deliveryDate = request.deliveryDate,
            currencyCode = request.currencyCode,
            paymentTerms = request.paymentTerms,
            deliveryTerms = request.deliveryTerms,
            remark = request.remark
        ).apply { assignTenant(tenantId) }

        request.lines.forEach { line ->
            po.addLine(
                itemCode = line.itemCode, itemName = line.itemName,
                quantity = line.quantity, unitOfMeasure = line.unitOfMeasure,
                unitPrice = line.unitPrice, taxRate = line.taxRate,
                prDocumentNo = line.prDocumentNo, prLineNo = line.prLineNo,
                specification = line.specification
            ).assignTenant(tenantId)
        }

        // PR 연결 품목의 미전환 잔량 차감 — 중복 발주 방지
        consumePrQuantities(tenantId, po)

        return poRepository.save(po).toResponse()
    }

    /** PR에서 PO 전환 — 승인된 PR의 미전환 잔량 품목을 일괄 발주. PR 잔량을 자동 차감 */
    @Transactional
    fun createFromPr(prId: Long, request: CreatePoFromPrRequest): PoResponse {
        val tenantId = TenantContext.getTenantId()
        val pr = prRepository.findByTenantIdAndId(tenantId, prId)
            .orElseThrow { EntityNotFoundException("PurchaseRequest", prId) }

        require(pr.status == PrStatus.APPROVED) { "PR must be APPROVED to create PO" }

        val docNo = docNumberGenerator.next("PO", "PO")
        val po = PurchaseOrder(
            documentNo = docNo,
            companyCode = pr.companyCode,
            plantCode = pr.plantCode,
            vendorCode = request.vendorCode,
            vendorName = request.vendorName,
            deliveryDate = request.deliveryDate ?: pr.deliveryDate,
            currencyCode = request.currencyCode,
            paymentTerms = request.paymentTerms
        ).apply { assignTenant(tenantId) }

        pr.lines.filter { it.openQuantity > java.math.BigDecimal.ZERO }.forEach { prLine ->
            po.addLine(
                itemCode = prLine.itemCode, itemName = prLine.itemName,
                quantity = prLine.openQuantity, unitOfMeasure = prLine.unitOfMeasure,
                unitPrice = prLine.unitPrice,
                prDocumentNo = pr.documentNo, prLineNo = prLine.lineNo,
                specification = prLine.specification
            ).assignTenant(tenantId)

            prLine.consumeQuantity(prLine.openQuantity)
        }

        prRepository.save(pr)
        return poRepository.save(po).toResponse()
    }

    @Transactional
    fun submit(id: Long): PoResponse {
        val po = findPo(id)
        po.submit()
        return poRepository.save(po).toResponse()
    }

    @Transactional
    fun approve(id: Long): PoResponse {
        val po = findPo(id)
        po.approve()
        val saved = poRepository.save(po)

        eventPublisher.publish(PurchaseOrderApprovedEvent(
            tenantId = po.tenantId, poId = po.id,
            documentNo = po.documentNo, vendorCode = po.vendorCode
        ))

        return saved.toResponse()
    }

    @Transactional
    fun reject(id: Long): PoResponse {
        val po = findPo(id)
        po.reject()
        return poRepository.save(po).toResponse()
    }

    /** PO 품목에 연결된 PR 행의 미전환 잔량을 차감하여 이중 발주를 방지 */
    private fun consumePrQuantities(tenantId: String, po: PurchaseOrder) {
        po.lines.filter { it.prDocumentNo != null }.groupBy { it.prDocumentNo!! }.forEach { (prDocNo, poLines) ->
            val pr = prRepository.findByTenantIdAndDocumentNo(tenantId, prDocNo).orElse(null) ?: return@forEach
            poLines.forEach { poLine ->
                val prLine = pr.lines.find { it.lineNo == poLine.prLineNo } ?: return@forEach
                prLine.consumeQuantity(poLine.quantity)
            }
            prRepository.save(pr)
        }
    }

    private fun findPo(id: Long): PurchaseOrder {
        val tenantId = TenantContext.getTenantId()
        return poRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow { EntityNotFoundException("PurchaseOrder", id) }
    }
}
