package com.modularerp.admin.service

import com.modularerp.admin.domain.ApiKey
import com.modularerp.admin.domain.ApiKeyStatus
import com.modularerp.admin.dto.*
import com.modularerp.admin.repository.ApiKeyRepository
import com.modularerp.core.exception.BusinessException
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
@Transactional(readOnly = true)
class ApiKeyService(
    private val apiKeyRepo: ApiKeyRepository
) {

    fun getAll(): List<ApiKeyResponse> =
        apiKeyRepo.findAllByTenantId(TenantContext.getTenantId()).map(ApiKeyResponse::from)

    /**
     * API 키 생성. 원본 키는 이 응답에서만 제공됨.
     */
    @Transactional
    fun create(request: CreateApiKeyRequest): ApiKeyCreateResponse {
        val rawKey = generateRawKey()
        val keyHash = sha256(rawKey)
        val keyPrefix = rawKey.take(8)

        val apiKey = ApiKey(
            name = request.name,
            keyHash = keyHash,
            keyPrefix = keyPrefix,
            description = request.description,
            allowedResources = request.allowedResources,
            rateLimit = request.rateLimit,
            expiresAt = request.expiresAt
        ).apply { assignTenant(TenantContext.getTenantId()) }

        val saved = apiKeyRepo.save(apiKey)
        return ApiKeyCreateResponse(
            id = saved.id,
            name = saved.name,
            rawKey = rawKey,
            keyPrefix = keyPrefix,
            expiresAt = saved.expiresAt
        )
    }

    @Transactional
    fun revoke(id: Long) {
        val key = apiKeyRepo.findById(id).orElseThrow { BusinessException("KEY_NOT_FOUND", "API key not found") }
        key.revoke()
    }

    @Transactional
    fun update(id: Long, request: UpdateApiKeyRequest): ApiKeyResponse {
        val key = apiKeyRepo.findById(id).orElseThrow { BusinessException("KEY_NOT_FOUND", "API key not found") }
        key.name = request.name
        key.description = request.description
        key.allowedResources = request.allowedResources
        key.rateLimit = request.rateLimit
        return ApiKeyResponse.from(apiKeyRepo.save(key))
    }

    /**
     * API 키 인증. raw key를 해시 비교.
     */
    @Transactional
    fun authenticate(rawKey: String): ApiKey? {
        val hash = sha256(rawKey)
        val key = apiKeyRepo.findByKeyHash(hash) ?: return null
        if (!key.isValid()) return null
        key.markUsed()
        return key
    }

    private fun generateRawKey(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return "mek_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
