package com.modularerp.production.repository

import com.modularerp.production.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface WorkCenterRepository : JpaRepository<WorkCenter, Long> {
    fun findByTenantIdAndCode(tenantId: String, code: String): Optional<WorkCenter>

    @Query("""
        SELECT wc FROM WorkCenter wc WHERE wc.tenantId = :tenantId AND wc.active = true
        AND (:plantCode IS NULL OR wc.plantCode = :plantCode)
        AND (:applyPlantScope = false OR wc.plantCode IN :plantCodes)
        ORDER BY wc.code
    """)
    fun search(
        tenantId: String,
        plantCode: String?,
        applyPlantScope: Boolean,
        plantCodes: Collection<String>,
        pageable: Pageable
    ): Page<WorkCenter>
}

interface RoutingRepository : JpaRepository<Routing, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Routing>

    @Query("""
        SELECT r FROM Routing r WHERE r.tenantId = :tenantId AND r.productCode = :productCode
        AND r.plantCode = :plantCode AND r.status = 'RELEASED' AND r.active = true
        ORDER BY r.revision DESC
    """)
    fun findActiveRouting(tenantId: String, productCode: String, plantCode: String): Optional<Routing>

    @Query("""
        SELECT r FROM Routing r WHERE r.tenantId = :tenantId AND r.active = true
        AND (:productCode IS NULL OR r.productCode LIKE %:productCode%)
        AND (:applyPlantScope = false OR r.plantCode IN :plantCodes)
        ORDER BY r.productCode
    """)
    fun search(
        tenantId: String,
        productCode: String?,
        applyPlantScope: Boolean,
        plantCodes: Collection<String>,
        pageable: Pageable
    ): Page<Routing>
}

interface WorkOrderRepository : JpaRepository<WorkOrder, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<WorkOrder>

    @Query("""
        SELECT wo FROM WorkOrder wo WHERE wo.tenantId = :tenantId AND wo.active = true
        AND (:status IS NULL OR wo.status = :status)
        AND (:plantCode IS NULL OR wo.plantCode = :plantCode)
        AND (:productCode IS NULL OR wo.productCode = :productCode)
        AND (:documentNo IS NULL OR wo.documentNo LIKE %:documentNo%)
        AND (:applyCompanyScope = false OR wo.companyCode IN :companyCodes)
        AND (:applyPlantScope = false OR wo.plantCode IN :plantCodes)
        AND (:createdBy IS NULL OR wo.createdBy = :createdBy)
        ORDER BY wo.createdAt DESC
    """)
    fun search(tenantId: String, status: WoStatus?, plantCode: String?,
               productCode: String?, documentNo: String?,
               applyCompanyScope: Boolean, companyCodes: Collection<String>,
               applyPlantScope: Boolean, plantCodes: Collection<String>,
               createdBy: String?,
               pageable: Pageable): Page<WorkOrder>
}
