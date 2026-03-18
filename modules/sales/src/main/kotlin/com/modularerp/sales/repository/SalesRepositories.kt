package com.modularerp.sales.repository

import com.modularerp.sales.domain.SalesOrder
import com.modularerp.sales.domain.SoStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface SalesOrderRepository : JpaRepository<SalesOrder, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<SalesOrder>

    @Query("""
        SELECT so FROM SalesOrder so WHERE so.tenantId = :tenantId AND so.active = true
        AND (:status IS NULL OR so.status = :status)
        AND (:customerCode IS NULL OR so.customerCode = :customerCode)
        AND (:documentNo IS NULL OR so.documentNo LIKE %:documentNo%)
        ORDER BY so.createdAt DESC
    """)
    fun search(tenantId: String, status: SoStatus?, customerCode: String?,
               documentNo: String?, pageable: Pageable): Page<SalesOrder>
}
