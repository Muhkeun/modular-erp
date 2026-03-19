package com.modularerp.admin.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * API Key 관리.
 * 외부 시스템 연동을 위한 API 키 발급/폐기.
 */
@Entity
@Table(name = "api_keys")
class ApiKey(

    @Column(nullable = false, length = 100)
    var name: String,

    /** SHA-256 해시 저장 (원본은 발급 시 한 번만 노출) */
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    val keyHash: String,

    /** 키 앞 8자만 표시용 */
    @Column(name = "key_prefix", nullable = false, length = 8)
    val keyPrefix: String,

    @Column(length = 500)
    var description: String? = null,

    /** 허용 리소스 (콤마 구분). null이면 전체 접근 */
    @Column(name = "allowed_resources", length = 2000)
    var allowedResources: String? = null,

    /** 분당 요청 제한. null이면 무제한 */
    @Column(name = "rate_limit")
    var rateLimit: Int? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ApiKeyStatus = ApiKeyStatus.ACTIVE

) : TenantEntity() {

    fun revoke() {
        this.status = ApiKeyStatus.REVOKED
    }

    fun isValid(): Boolean {
        if (status != ApiKeyStatus.ACTIVE) return false
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) return false
        return true
    }

    fun markUsed() {
        this.lastUsedAt = Instant.now()
    }

    fun getAllowedResourceList(): List<String> =
        allowedResources?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}

enum class ApiKeyStatus {
    ACTIVE, REVOKED, EXPIRED
}
