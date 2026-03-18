package com.modularerp.masterdata.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.masterdata.domain.*
import com.modularerp.masterdata.dto.*
import com.modularerp.masterdata.repository.BomRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Exploded BOM line — result of multi-level BOM explosion.
 */
data class ExplodedBomLine(
    val level: Int,
    val itemCode: String,
    val itemName: String,
    val requiredQuantity: BigDecimal,
    val unitOfMeasure: String,
    val phantom: Boolean,
    val operationNo: Int?
)

@Service
@Transactional(readOnly = true)
class BomService(private val bomRepository: BomRepository) {

    fun getById(id: Long): BomResponse =
        findBom(id).toResponse()

    fun search(productCode: String?, status: BomStatus?, pageable: Pageable): Page<BomResponse> =
        bomRepository.search(TenantContext.getTenantId(), productCode, status, pageable).map { it.toResponse() }

    @Transactional
    fun create(request: CreateBomRequest): BomResponse {
        val tenantId = TenantContext.getTenantId()
        val bom = BomHeader(
            productCode = request.productCode, productName = request.productName,
            plantCode = request.plantCode, revision = request.revision,
            baseQuantity = request.baseQuantity, baseUnit = request.baseUnit,
            validFrom = request.validFrom, validTo = request.validTo, description = request.description
        ).apply { assignTenant(tenantId) }

        request.components.forEach { c ->
            bom.addComponent(c.itemCode, c.itemName, c.quantity, c.unitOfMeasure,
                c.scrapRate, c.phantom, c.sortOrder, c.operationNo, c.remark).assignTenant(tenantId)
        }

        return bomRepository.save(bom).toResponse()
    }

    @Transactional
    fun release(id: Long): BomResponse {
        val bom = findBom(id)
        bom.release()
        return bomRepository.save(bom).toResponse()
    }

    /**
     * Multi-level BOM explosion: recursively expands all components.
     * Phantom items are expanded (their sub-components appear directly).
     * Returns a flat list with level indicator.
     */
    fun explode(productCode: String, plantCode: String, quantity: BigDecimal): List<ExplodedBomLine> {
        val tenantId = TenantContext.getTenantId()
        val result = mutableListOf<ExplodedBomLine>()
        explodeRecursive(tenantId, productCode, plantCode, quantity, 0, result, mutableSetOf())
        return result
    }

    private fun explodeRecursive(
        tenantId: String, productCode: String, plantCode: String,
        parentQty: BigDecimal, level: Int,
        result: MutableList<ExplodedBomLine>, visited: MutableSet<String>
    ) {
        // Circular reference protection
        if (productCode in visited) return
        visited.add(productCode)

        val bom = bomRepository.findActiveBom(tenantId, productCode, plantCode).orElse(null) ?: return
        val factor = parentQty.divide(bom.baseQuantity, 6, java.math.RoundingMode.HALF_UP)

        for (comp in bom.components) {
            val requiredQty = comp.grossQuantity.multiply(factor)

            if (comp.phantom) {
                // Phantom: don't list the phantom itself, explode its sub-BOM
                explodeRecursive(tenantId, comp.itemCode, plantCode, requiredQty, level, result, visited)
            } else {
                result.add(ExplodedBomLine(
                    level = level + 1, itemCode = comp.itemCode, itemName = comp.itemName,
                    requiredQuantity = requiredQty, unitOfMeasure = comp.unitOfMeasure,
                    phantom = false, operationNo = comp.operationNo
                ))
                // If this component also has a BOM, explode it
                explodeRecursive(tenantId, comp.itemCode, plantCode, requiredQty, level + 1, result, visited)
            }
        }

        visited.remove(productCode)
    }

    private fun findBom(id: Long): BomHeader =
        bomRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("BOM", id) }
}
