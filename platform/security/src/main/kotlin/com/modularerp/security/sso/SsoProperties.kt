package com.modularerp.security.sso

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "modular-erp.security.sso")
data class SsoProperties(
    val enabled: Boolean = false,
    val provider: String = "none",
    val clientId: String = "",
    val clientSecret: String = "",
    val issuerUri: String = "",
    val redirectUri: String = "",
    val allowedDomains: List<String> = emptyList()
)
