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

/** Event published when a PO is approved — logistics module listens for GR creation */
class PurchaseOrderApprovedEvent(
    tenantId: String,
    val poId: Long,
    val documentNo: String,
    val vendorCode: String
) : DomainEvent(tenantId = tenantId)

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

        // Consume PR open quantities if linked
        consumePrQuantities(tenantId, po)

        return poRepository.save(po).toResponse()
    }

    /** Create PO directly from an approved PR */
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
