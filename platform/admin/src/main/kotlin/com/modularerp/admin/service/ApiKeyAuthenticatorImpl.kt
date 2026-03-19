package com.modularerp.admin.service

import com.modularerp.security.apikey.ApiKeyAuthenticator
import com.modularerp.security.apikey.ApiKeyPrincipal
import org.springframework.stereotype.Component

@Component
class ApiKeyAuthenticatorImpl(
    private val apiKeyService: ApiKeyService
) : ApiKeyAuthenticator {

    override fun authenticate(rawKey: String): ApiKeyPrincipal? {
        val apiKey = apiKeyService.authenticate(rawKey) ?: return null
        return ApiKeyPrincipal(
            id = apiKey.id,
            name = apiKey.name,
            tenantId = apiKey.tenantId,
            allowedResources = apiKey.getAllowedResourceList(),
            rateLimit = apiKey.rateLimit
        )
    }
}
