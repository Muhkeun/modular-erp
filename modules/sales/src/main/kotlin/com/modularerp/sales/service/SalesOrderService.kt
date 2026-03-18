package com.modularerp.sales.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.sales.domain.*
import com.modularerp.sales.dto.*
import com.modularerp.sales.repository.SalesOrderRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SalesOrderService(
    private val soRepository: SalesOrderRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    fun getById(id: Long): SoResponse = findSo(id).toResponse()

    fun search(status: SoStatus?, customerCode: String?, documentNo: String?, pageable: Pageable): Page<SoResponse> =
        soRepository.search(TenantContext.getTenantId(), status, customerCode, documentNo, pageable).map { it.toResponse() }

    @Transactional
    fun create(request: CreateSoRequest): SoResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("SO", "SO")

        val so = SalesOrder(
            documentNo = docNo, companyCode = request.companyCode, plantCode = request.plantCode,
            customerCode = request.customerCode, customerName = request.customerName,
            deliveryDate = request.deliveryDate, currencyCode = request.currencyCode,
            paymentTerms = request.paymentTerms, shippingAddress = request.shippingAddress, remark = request.remark
        ).apply { assignTenant(tenantId) }

        request.lines.forEach { line ->
            so.addLine(line.itemCode, line.itemName, line.quantity, line.unitOfMeasure,
                line.unitPrice, line.taxRate, line.specification).assignTenant(tenantId)
        }

        return soRepository.save(so).toResponse()
    }

    @Transactional fun submit(id: Long) = findSo(id).also { it.submit() }.let { soRepository.save(it).toResponse() }
    @Transactional fun approve(id: Long) = findSo(id).also { it.approve() }.let { soRepository.save(it).toResponse() }
    @Transactional fun reject(id: Long) = findSo(id).also { it.reject() }.let { soRepository.save(it).toResponse() }

    private fun findSo(id: Long): SalesOrder =
        soRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("SalesOrder", id) }
}
