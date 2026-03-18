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

@Service
@Transactional(readOnly = true)
class GoodsIssueService(
    private val giRepository: GoodsIssueRepository,
    private val stockRepository: StockSummaryRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    fun getById(id: Long): GiResponse =
        findGi(id).toResponse()

    fun search(status: GiStatus?, documentNo: String?, pageable: Pageable): Page<GiResponse> =
        giRepository.search(TenantContext.getTenantId(), status, documentNo, pageable).map { it.toResponse() }

    @Transactional
    fun create(request: CreateGiRequest): GiResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("GI", "GI")

        val gi = GoodsIssue(
            documentNo = docNo, companyCode = request.companyCode, plantCode = request.plantCode,
            storageLocation = request.storageLocation, issueType = request.issueType,
            referenceDocNo = request.referenceDocNo, issueDate = request.issueDate, remark = request.remark
        ).apply { assignTenant(tenantId) }

        request.lines.forEach { line ->
            gi.addLine(line.itemCode, line.itemName, line.quantity, line.unitOfMeasure, line.storageLocation)
                .assignTenant(tenantId)
        }

        return giRepository.save(gi).toResponse()
    }

    @Transactional
    fun confirm(id: Long): GiResponse {
        val gi = findGi(id)
        gi.confirm()

        val tenantId = gi.tenantId
        gi.lines.forEach { line ->
            val stock = stockRepository.findByTenantIdAndItemCodeAndPlantCodeAndStorageLocation(
                tenantId, line.itemCode, gi.plantCode, line.storageLocation
            ).orElseThrow { EntityNotFoundException("Stock", "${line.itemCode}@${line.storageLocation}") }

            stock.issueStock(line.quantity)
            stockRepository.save(stock)
        }

        return giRepository.save(gi).toResponse()
    }

    private fun findGi(id: Long): GoodsIssue =
        giRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("GoodsIssue", id) }
}
