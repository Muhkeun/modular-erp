package com.modularerp.security.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "modular-erp.security.rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = true,
    val defaultRpm: Int = 100,
    val unauthenticatedRpm: Int = 30
)
