package com.modularerp.logistics.repository

import com.modularerp.logistics.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface GoodsReceiptRepository : JpaRepository<GoodsReceipt, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<GoodsReceipt>

    @Query("""
        SELECT gr FROM GoodsReceipt gr WHERE gr.tenantId = :tenantId AND gr.active = true
        AND (:status IS NULL OR gr.status = :status)
        AND (:documentNo IS NULL OR gr.documentNo LIKE %:documentNo%)
        ORDER BY gr.createdAt DESC
    """)
    fun search(tenantId: String, status: GrStatus?, documentNo: String?, pageable: Pageable): Page<GoodsReceipt>
}

interface GoodsIssueRepository : JpaRepository<GoodsIssue, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<GoodsIssue>

    @Query("""
        SELECT gi FROM GoodsIssue gi WHERE gi.tenantId = :tenantId AND gi.active = true
        AND (:status IS NULL OR gi.status = :status)
        AND (:documentNo IS NULL OR gi.documentNo LIKE %:documentNo%)
        ORDER BY gi.createdAt DESC
    """)
    fun search(tenantId: String, status: GiStatus?, documentNo: String?, pageable: Pageable): Page<GoodsIssue>
}

interface StockSummaryRepository : JpaRepository<StockSummary, Long> {

    fun findByTenantIdAndItemCodeAndPlantCodeAndStorageLocation(
        tenantId: String, itemCode: String, plantCode: String, storageLocation: String
    ): Optional<StockSummary>

    @Query("""
        SELECT s FROM StockSummary s WHERE s.tenantId = :tenantId AND s.active = true
        AND (:plantCode IS NULL OR s.plantCode = :plantCode)
        AND (:itemCode IS NULL OR s.itemCode LIKE %:itemCode%)
        ORDER BY s.itemCode
    """)
    fun search(tenantId: String, plantCode: String?, itemCode: String?, pageable: Pageable): Page<StockSummary>
}
