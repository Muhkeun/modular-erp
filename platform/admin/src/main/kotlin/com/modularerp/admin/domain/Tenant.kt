package com.modularerp.admin.domain

import com.modularerp.core.domain.BaseEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * 테넌트 (조직/회사) 정보.
 * 멀티테넌트 관리 콘솔에서 사용.
 */
@Entity
@Table(name = "tenants")
class Tenant(

    @Column(name = "tenant_id", nullable = false, unique = true, length = 50)
    val tenantId: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var plan: TenantPlan = TenantPlan.FREE,

    /** 최대 사용자 수 */
    @Column(name = "max_users", nullable = false)
    var maxUsers: Int = 10,

    /** 최대 저장 용량 (MB) */
    @Column(name = "max_storage_mb", nullable = false)
    var maxStorageMb: Long = 1024,

    /** 커스텀 설정 (JSON) */
    @Column(name = "settings", columnDefinition = "TEXT")
    var settings: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TenantStatus = TenantStatus.ACTIVE,

    @Column(name = "activated_at")
    var activatedAt: Instant? = Instant.now()

) : BaseEntity() {

    fun suspend() { this.status = TenantStatus.SUSPENDED }
    fun activateTenant() { this.status = TenantStatus.ACTIVE; this.activatedAt = Instant.now() }
    fun update(name: String, description: String?, maxUsers: Int, maxStorageMb: Long) {
        this.name = name
        this.description = description
        this.maxUsers = maxUsers
        this.maxStorageMb = maxStorageMb
    }
}

enum class TenantPlan {
    FREE, STARTER, PROFESSIONAL, ENTERPRISE
}

enum class TenantStatus {
    ACTIVE, SUSPENDED, DEACTIVATED
}
