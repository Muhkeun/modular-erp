package com.modularerp.audit.dto

import com.modularerp.audit.domain.AuditAction
import com.modularerp.audit.domain.AuditLog
import java.time.Instant

data class AuditLogResponse(
    val id: Long,
    val userId: String,
    val action: AuditAction,
    val entityType: String,
    val entityId: String?,
    val oldValue: String?,
    val newValue: String?,
    val ipAddress: String?,
    val metadata: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(entity: AuditLog) = AuditLogResponse(
            id = entity.id,
            userId = entity.userId,
            action = entity.action,
            entityType = entity.entityType,
            entityId = entity.entityId,
            oldValue = entity.oldValue,
            newValue = entity.newValue,
            ipAddress = entity.ipAddress,
            metadata = entity.metadata,
            createdAt = entity.createdAt
        )
    }
}
