package com.modularerp.audit.domain

import jakarta.persistence.*
import java.time.Instant

/**
 * 감사 로그 엔티티.
 * BaseEntity를 상속하지 않고 독립적으로 관리 (감사 로그 자체에 감사 추적 불필요).
 */
@Entity
@Table(name = "audit_logs", indexes = [
    Index(name = "idx_audit_tenant_time", columnList = "tenant_id, created_at DESC"),
    Index(name = "idx_audit_user", columnList = "user_id, created_at DESC"),
    Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
])
class AuditLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tenant_id", nullable = false, length = 50)
    val tenantId: String,

    @Column(name = "user_id", nullable = false, length = 50)
    val userId: String,

    /** 액션 유형 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val action: AuditAction,

    /** 대상 엔티티 타입: "Item", "PurchaseOrder" 등 */
    @Column(name = "entity_type", nullable = false, length = 100)
    val entityType: String,

    /** 대상 엔티티 ID */
    @Column(name = "entity_id", length = 50)
    val entityId: String? = null,

    /** 변경 전 데이터 (JSON) */
    @Column(name = "old_value", columnDefinition = "TEXT")
    val oldValue: String? = null,

    /** 변경 후 데이터 (JSON) */
    @Column(name = "new_value", columnDefinition = "TEXT")
    val newValue: String? = null,

    /** 요청 IP */
    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    /** 추가 메타데이터 (JSON) */
    @Column(columnDefinition = "TEXT")
    val metadata: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)

enum class AuditAction {
    CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT, IMPORT, APPROVE, REJECT
}
