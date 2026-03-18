package com.modularerp.logistics.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.logistics.domain.*
import com.modularerp.logistics.dto.*
import com.modularerp.logistics.repository.*
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 입고(GR) 서비스 — 자재 입고와 재고 증가를 처리.
 *
 * 주요 업무 흐름:
 * 1. PO 기반 GR 생성 — 발주서 연결로 입고 추적
 * 2. 확정(confirm) 시 품목별 재고(StockSummary) 자동 증가
 * 3. 해당 저장위치에 재고가 없으면 신규 StockSummary 레코드 생성
 */
@Service
@Transactional(readOnly = true)
class GoodsReceiptService(
    private val grRepository: GoodsReceiptRepository,
    private val stockRepository: StockSummaryRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    fun getById(id: Long): GrResponse =
        findGr(id).toResponse()

    fun search(status: GrStatus?, documentNo: String?, pageable: Pageable): Page<GrResponse> =
        grRepository.search(TenantContext.getTenantId(), status, documentNo, pageable).map { it.toResponse() }

    @Transactional
    fun create(request: CreateGrRequest): GrResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("GR", "GR")

        val gr = GoodsReceipt(
            documentNo = docNo, companyCode = request.companyCode, plantCode = request.plantCode,
            storageLocation = request.storageLocation, poDocumentNo = request.poDocumentNo,
            vendorCode = request.vendorCode, vendorName = request.vendorName,
            receiptDate = request.receiptDate, remark = request.remark
        ).apply { assignTenant(tenantId) }

        request.lines.forEach { line ->
            gr.addLine(line.itemCode, line.itemName, line.quantity, line.unitOfMeasure,
                line.unitPrice, line.poLineNo, line.storageLocation).assignTenant(tenantId)
        }

        return grRepository.save(gr).toResponse()
    }

    @Transactional
    fun confirm(id: Long): GrResponse {
        val gr = findGr(id)
        gr.confirm()

        // 품목별 재고 증가 — 저장위치에 재고가 없으면 신규 생성
        val tenantId = gr.tenantId
        gr.lines.forEach { line ->
            val stock = stockRepository.findByTenantIdAndItemCodeAndPlantCodeAndStorageLocation(
                tenantId, line.itemCode, gr.plantCode, line.storageLocation
            ).orElseGet {
                StockSummary(
                    itemCode = line.itemCode, itemName = line.itemName,
                    plantCode = gr.plantCode, storageLocation = line.storageLocation,
                    unitOfMeasure = line.unitOfMeasure
                ).apply { assignTenant(tenantId) }
            }
            stock.receiveStock(line.quantity, line.unitPrice)
            stockRepository.save(stock)
        }

        return grRepository.save(gr).toResponse()
    }

    private fun findGr(id: Long): GoodsReceipt =
        grRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("GoodsReceipt", id) }
}
