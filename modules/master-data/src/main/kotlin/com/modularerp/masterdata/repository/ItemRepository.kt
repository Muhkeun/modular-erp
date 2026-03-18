package com.modularerp.masterdata.repository

import com.modularerp.masterdata.domain.Item
import com.modularerp.masterdata.domain.ItemType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface ItemRepository : JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    fun findByTenantIdAndCode(tenantId: String, code: String): Optional<Item>

    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Item>

    @Query("""
        SELECT i FROM Item i
        LEFT JOIN FETCH i.translations t
        WHERE i.tenantId = :tenantId
        AND i.active = true
        AND (:code IS NULL OR i.code LIKE %:code%)
        AND (:itemType IS NULL OR i.itemType = :itemType)
        AND (:itemGroup IS NULL OR i.itemGroup = :itemGroup)
    """)
    fun search(
        tenantId: String,
        code: String?,
        itemType: ItemType?,
        itemGroup: String?,
        pageable: Pageable
    ): Page<Item>

    fun existsByTenantIdAndCode(tenantId: String, code: String): Boolean
}
