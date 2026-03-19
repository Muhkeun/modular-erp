package com.modularerp.security.sso

import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.web.bind.annotation.*

data class SsoProviderInfo(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val authorizeUrl: String?
)

data class SsoCallbackRequest(
    val code: String,
    val state: String? = null
)

data class SsoCallbackResponse(
    val token: String?,
    val error: String?
)

@RestController
@RequestMapping("/api/v1/auth/sso")
@Tag(name = "SSO Authentication", description = "Single Sign-On endpoints")
@EnableConfigurationProperties(SsoProperties::class)
class SsoController(
    private val ssoProperties: SsoProperties
) {

    companion object {
        private val SUPPORTED_PROVIDERS = listOf(
            SsoProviderInfo("google", "Google", false, null),
            SsoProviderInfo("azure-ad", "Microsoft Azure AD", false, null),
            SsoProviderInfo("okta", "Okta", false, null),
            SsoProviderInfo("saml", "SAML 2.0", false, null)
        )
    }

    @GetMapping("/providers")
    fun getProviders(): ApiResponse<List<SsoProviderInfo>> {
        val providers = SUPPORTED_PROVIDERS.map { provider ->
            if (ssoProperties.enabled && ssoProperties.provider == provider.id) {
                provider.copy(
                    enabled = true,
                    authorizeUrl = "/api/v1/auth/sso/authorize/${provider.id}"
                )
            } else {
                provider
            }
        }
        return ApiResponse.ok(providers)
    }

    @GetMapping("/authorize/{provider}")
    fun authorize(@PathVariable provider: String): ApiResponse<Map<String, String>> {
        if (!ssoProperties.enabled) {
            return ApiResponse.error("SSO_NOT_CONFIGURED", "SSO is not enabled. Configure modular-erp.security.sso in application.yml")
        }
        if (ssoProperties.provider != provider) {
            return ApiResponse.error("PROVIDER_NOT_CONFIGURED", "SSO provider '$provider' is not configured. Current provider: ${ssoProperties.provider}")
        }
        if (ssoProperties.clientId.isBlank() || ssoProperties.issuerUri.isBlank()) {
            return ApiResponse.error("SSO_INCOMPLETE", "SSO provider '$provider' configuration is incomplete. Set clientId and issuerUri.")
        }

        // Build OAuth2 authorization URL based on provider
        val authUrl = buildAuthorizationUrl(provider)
        return ApiResponse.ok(mapOf("redirectUrl" to authUrl))
    }

    @PostMapping("/callback/{provider}")
    fun callback(
        @PathVariable provider: String,
        @RequestBody request: SsoCallbackRequest
    ): ApiResponse<SsoCallbackResponse> {
        if (!ssoProperties.enabled) {
            return ApiResponse.error("SSO_NOT_CONFIGURED", "SSO is not enabled")
        }
        if (ssoProperties.provider != provider) {
            return ApiResponse.error("PROVIDER_NOT_CONFIGURED", "SSO provider '$provider' is not configured")
        }

        // TODO: Implement actual OAuth2 token exchange when SSO is configured
        // 1. Exchange authorization code for tokens using provider's token endpoint
        // 2. Validate ID token / fetch user info
        // 3. Check allowedDomains if configured
        // 4. Create or update user in UserRepository
        // 5. Generate JWT using JwtProvider
        return ApiResponse.error(
            "SSO_NOT_IMPLEMENTED",
            "SSO callback handling for '$provider' is prepared but requires OAuth2 client configuration. " +
            "Set clientId, clientSecret, and issuerUri in application.yml to activate."
        )
    }

    private fun buildAuthorizationUrl(provider: String): String {
        val baseUrl = when (provider) {
            "google" -> "https://accounts.google.com/o/oauth2/v2/auth"
            "azure-ad" -> "${ssoProperties.issuerUri}/oauth2/v2.0/authorize"
            "okta" -> "${ssoProperties.issuerUri}/v1/authorize"
            else -> ssoProperties.issuerUri
        }
        return "$baseUrl?client_id=${ssoProperties.clientId}&response_type=code&redirect_uri=${ssoProperties.redirectUri}&scope=openid+email+profile"
    }
}
