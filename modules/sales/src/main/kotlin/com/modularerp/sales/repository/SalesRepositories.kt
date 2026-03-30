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
        AND (:applyCompanyScope = false OR so.companyCode IN :companyCodes)
        AND (:applyPlantScope = false OR so.plantCode IN :plantCodes)
        AND (:createdBy IS NULL OR so.createdBy = :createdBy)
        ORDER BY so.createdAt DESC
    """)
    fun search(tenantId: String, status: SoStatus?, customerCode: String?,
               documentNo: String?,
               applyCompanyScope: Boolean, companyCodes: Collection<String>,
               applyPlantScope: Boolean, plantCodes: Collection<String>,
               createdBy: String?,
               pageable: Pageable): Page<SalesOrder>
}
