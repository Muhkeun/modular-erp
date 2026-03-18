package com.modularerp.masterdata.repository

import com.modularerp.masterdata.domain.BomHeader
import com.modularerp.masterdata.domain.BomStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface BomRepository : JpaRepository<BomHeader, Long> {

    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<BomHeader>

    /** Find the active (RELEASED) BOM for a product */
    @Query("""
        SELECT b FROM BomHeader b
        WHERE b.tenantId = :tenantId AND b.productCode = :productCode
        AND b.plantCode = :plantCode AND b.status = 'RELEASED' AND b.active = true
        ORDER BY b.revision DESC
    """)
    fun findActiveBom(tenantId: String, productCode: String, plantCode: String): Optional<BomHeader>

    @Query("""
        SELECT b FROM BomHeader b WHERE b.tenantId = :tenantId AND b.active = true
        AND (:productCode IS NULL OR b.productCode LIKE %:productCode%)
        AND (:status IS NULL OR b.status = :status)
        ORDER BY b.productCode, b.revision DESC
    """)
    fun search(tenantId: String, productCode: String?, status: BomStatus?, pageable: Pageable): Page<BomHeader>
}
