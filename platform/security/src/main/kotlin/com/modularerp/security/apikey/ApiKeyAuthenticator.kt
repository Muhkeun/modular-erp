package com.modularerp.security.apikey

/**
 * Interface for API key authentication.
 * Implemented in the admin module to avoid circular dependency.
 */
interface ApiKeyAuthenticator {

    /**
     * Authenticate with the raw API key string.
     * @return ApiKeyPrincipal if valid, null otherwise
     */
    fun authenticate(rawKey: String): ApiKeyPrincipal?
}

data class ApiKeyPrincipal(
    val id: Long,
    val name: String,
    val tenantId: String,
    val allowedResources: List<String>,
    val rateLimit: Int?
)
