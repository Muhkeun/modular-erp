package com.modularerp.audit.repository

import com.modularerp.audit.domain.AuditAction
import com.modularerp.audit.domain.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.tenantId = :tenantId
        AND (:userId IS NULL OR a.userId = :userId)
        AND (:action IS NULL OR a.action = :action)
        AND (:entityType IS NULL OR a.entityType = :entityType)
        AND (:from IS NULL OR a.createdAt >= :from)
        AND (:to IS NULL OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
    """)
    fun search(
        tenantId: String,
        userId: String?,
        action: AuditAction?,
        entityType: String?,
        from: Instant?,
        to: Instant?,
        pageable: Pageable
    ): Page<AuditLog>

    fun findByTenantIdAndEntityTypeAndEntityId(
        tenantId: String,
        entityType: String,
        entityId: String
    ): List<AuditLog>
}
