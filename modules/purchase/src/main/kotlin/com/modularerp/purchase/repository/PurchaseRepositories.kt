package com.modularerp.purchase.repository

import com.modularerp.purchase.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface PurchaseRequestRepository : JpaRepository<PurchaseRequest, Long> {

    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<PurchaseRequest>

    fun findByTenantIdAndDocumentNo(tenantId: String, documentNo: String): Optional<PurchaseRequest>

    @Query("""
        SELECT pr FROM PurchaseRequest pr
        WHERE pr.tenantId = :tenantId AND pr.active = true
        AND (:status IS NULL OR pr.status = :status)
        AND (:companyCode IS NULL OR pr.companyCode = :companyCode)
        AND (:documentNo IS NULL OR pr.documentNo LIKE %:documentNo%)
        ORDER BY pr.createdAt DESC
    """)
    fun search(tenantId: String, status: PrStatus?, companyCode: String?,
               documentNo: String?, pageable: Pageable): Page<PurchaseRequest>
}

interface PurchaseOrderRepository : JpaRepository<PurchaseOrder, Long> {

    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<PurchaseOrder>

    fun findByTenantIdAndDocumentNo(tenantId: String, documentNo: String): Optional<PurchaseOrder>

    @Query("""
        SELECT po FROM PurchaseOrder po
        WHERE po.tenantId = :tenantId AND po.active = true
        AND (:status IS NULL OR po.status = :status)
        AND (:vendorCode IS NULL OR po.vendorCode = :vendorCode)
        AND (:documentNo IS NULL OR po.documentNo LIKE %:documentNo%)
        ORDER BY po.createdAt DESC
    """)
    fun search(tenantId: String, status: PoStatus?, vendorCode: String?,
               documentNo: String?, pageable: Pageable): Page<PurchaseOrder>
}

interface RfqRepository : JpaRepository<RequestForQuotation, Long> {

    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<RequestForQuotation>

    @Query("""
        SELECT r FROM RequestForQuotation r
        WHERE r.tenantId = :tenantId AND r.active = true
        AND (:status IS NULL OR r.status = :status)
        ORDER BY r.createdAt DESC
    """)
    fun search(tenantId: String, status: RfqStatus?, pageable: Pageable): Page<RequestForQuotation>
}
