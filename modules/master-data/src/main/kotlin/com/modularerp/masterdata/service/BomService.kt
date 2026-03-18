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
 * BOM 전개 결과 행 — 다단계 BOM 전개(explosion)의 평탄화된 결과.
 * level은 BOM 계층 깊이, phantom은 팬텀 품목 여부를 나타낸다.
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

/**
 * BOM 서비스 — 자재명세서 관리 및 다단계 BOM 전개(explosion).
 *
 * 주요 기능:
 * 1. BOM 생성/확정 — 제품의 구성품 구조를 등록하고 생산에 사용 가능하게 확정
 * 2. BOM 전개 — 재귀적으로 하위 구성품을 풀어 평탄한 자재 목록으로 변환
 *    - 팬텀 품목은 건너뛰고 실제 자재만 목록에 포함
 *    - 순환 참조 방지(visited set)로 무한 루프 차단
 *    - 기준수량(baseQuantity) 비율로 소요량을 환산
 */
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
     * 다단계 BOM 전개 — 제품의 모든 구성품을 재귀적으로 풀어 평탄한 리스트로 반환.
     * 팬텀 품목은 자체가 아닌 하위 구성품이 직접 노출된다.
     * WO 소요자재 생성, MRP 순소요량 계산 등에 사용된다.
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
        // 순환 참조 방지 — A→B→A 같은 무한 루프 차단
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
