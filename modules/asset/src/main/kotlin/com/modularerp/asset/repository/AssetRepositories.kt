package com.modularerp.asset.repository

import com.modularerp.asset.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface AssetRepository : JpaRepository<Asset, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Asset>

    @Query("""
        SELECT a FROM Asset a WHERE a.tenantId = :tenantId AND a.active = true
        AND (:status IS NULL OR a.status = :status)
        AND (:category IS NULL OR a.category = :category)
        AND (:name IS NULL OR a.name LIKE %:name%)
        ORDER BY a.assetNo ASC
    """)
    fun search(tenantId: String, status: AssetStatus?, category: AssetCategory?,
               name: String?, pageable: Pageable): Page<Asset>

    @Query("SELECT a FROM Asset a WHERE a.tenantId = :tenantId AND a.status = 'ACTIVE' AND a.active = true")
    fun findAllActive(tenantId: String): List<Asset>
}

interface DepreciationScheduleRepository : JpaRepository<DepreciationSchedule, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<DepreciationSchedule>

    @Query("""
        SELECT ds FROM DepreciationSchedule ds WHERE ds.tenantId = :tenantId AND ds.active = true
        AND ds.asset.id = :assetId
        ORDER BY ds.periodYear ASC, ds.periodMonth ASC
    """)
    fun findByAsset(tenantId: String, assetId: Long): List<DepreciationSchedule>

    @Query("""
        SELECT ds FROM DepreciationSchedule ds WHERE ds.tenantId = :tenantId AND ds.active = true
        AND ds.periodYear = :year AND ds.periodMonth = :month AND ds.posted = false
    """)
    fun findUnpostedByPeriod(tenantId: String, year: Int, month: Int): List<DepreciationSchedule>
}

interface AssetDisposalRepository : JpaRepository<AssetDisposal, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<AssetDisposal>

    @Query("""
        SELECT ad FROM AssetDisposal ad WHERE ad.tenantId = :tenantId AND ad.active = true
        ORDER BY ad.disposalDate DESC
    """)
    fun findAll(tenantId: String, pageable: Pageable): Page<AssetDisposal>
}
