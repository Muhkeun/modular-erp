package com.modularerp.document.repository

import com.modularerp.document.domain.DocumentSequence
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface DocumentSequenceRepository : JpaRepository<DocumentSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ds FROM DocumentSequence ds WHERE ds.tenantId = :tenantId AND ds.documentType = :documentType AND ds.period = :period")
    fun findForUpdate(tenantId: String, documentType: String, period: String): Optional<DocumentSequence>
}
