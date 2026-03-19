package com.modularerp.audit.service

import com.modularerp.audit.domain.AuditAction
import com.modularerp.audit.domain.AuditLog
import com.modularerp.audit.dto.AuditLogResponse
import com.modularerp.audit.repository.AuditLogRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuditService(
    private val auditLogRepo: AuditLogRepository
) {

    /**
     * 감사 로그 비동기 기록 (비즈니스 트랜잭션과 분리).
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun log(
        action: AuditAction,
        entityType: String,
        entityId: String? = null,
        oldValue: String? = null,
        newValue: String? = null,
        ipAddress: String? = null,
        metadata: String? = null
    ) {
        val log = AuditLog(
            tenantId = TenantContext.getTenantId(),
            userId = TenantContext.getUserId() ?: "SYSTEM",
            action = action,
            entityType = entityType,
            entityId = entityId,
            oldValue = oldValue,
            newValue = newValue,
            ipAddress = ipAddress,
            metadata = metadata
        )
        auditLogRepo.save(log)
    }

    @Transactional(readOnly = true)
    fun search(
        userId: String?,
        action: AuditAction?,
        entityType: String?,
        from: Instant?,
        to: Instant?,
        pageable: Pageable
    ): Page<AuditLogResponse> {
        return auditLogRepo.search(
            TenantContext.getTenantId(), userId, action, entityType, from, to, pageable
        ).map(AuditLogResponse::from)
    }

    @Transactional(readOnly = true)
    fun getEntityHistory(entityType: String, entityId: String): List<AuditLogResponse> {
        return auditLogRepo.findByTenantIdAndEntityTypeAndEntityId(
            TenantContext.getTenantId(), entityType, entityId
        ).map(AuditLogResponse::from)
    }
}
