package com.modularerp.account.repository

import com.modularerp.account.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface JournalEntryRepository : JpaRepository<JournalEntry, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<JournalEntry>

    @Query("""
        SELECT je FROM JournalEntry je WHERE je.tenantId = :tenantId AND je.active = true
        AND (:status IS NULL OR je.status = :status)
        AND (:entryType IS NULL OR je.entryType = :entryType)
        AND (:documentNo IS NULL OR je.documentNo LIKE %:documentNo%)
        ORDER BY je.postingDate DESC
    """)
    fun search(tenantId: String, status: JeStatus?, entryType: JournalEntryType?,
               documentNo: String?, pageable: Pageable): Page<JournalEntry>
}

interface AccountMasterRepository : JpaRepository<AccountMaster, Long> {
    fun findByTenantIdAndCode(tenantId: String, code: String): Optional<AccountMaster>

    @Query("SELECT a FROM AccountMaster a WHERE a.tenantId = :tenantId AND a.active = true ORDER BY a.code")
    fun findAllActive(tenantId: String, pageable: Pageable): Page<AccountMaster>
}
