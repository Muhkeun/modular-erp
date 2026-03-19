package com.modularerp.security.ratelimit

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Rate limiting filter. Runs after authentication filters so that
 * authenticated users get their per-user limit applied.
 *
 * Priority: API key limit > authenticated user limit > IP-based limit.
 * The API key rate limit is set via request attribute by ApiKeyAuthenticationFilter.
 */
@Component
@Order(5)
@EnableConfigurationProperties(RateLimitProperties::class)
class RateLimitFilter(
    private val properties: RateLimitProperties,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)
    private val rateLimiter = RateLimiter()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!properties.enabled) {
            filterChain.doFilter(request, response)
            return
        }

        val (key, limit) = resolveKeyAndLimit(request)
        val result = rateLimiter.tryConsume(key, limit)

        response.setIntHeader("X-RateLimit-Limit", limit)
        response.setIntHeader("X-RateLimit-Remaining", result.remaining)

        if (!result.allowed) {
            log.warn("Rate limit exceeded for key={}", key)
            response.setIntHeader("Retry-After", result.retryAfterSeconds)
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            objectMapper.writeValue(
                response.writer,
                mapOf(
                    "success" to false,
                    "error" to mapOf(
                        "code" to "RATE_LIMIT_EXCEEDED",
                        "message" to "Too many requests. Retry after ${result.retryAfterSeconds} seconds."
                    )
                )
            )
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveKeyAndLimit(request: HttpServletRequest): Pair<String, Int> {
        // API key rate limit (set by ApiKeyAuthenticationFilter)
        val apiKeyLimit = request.getAttribute("apiKeyRateLimit") as? Int
        val apiKeyId = request.getAttribute("apiKeyId") as? String
        if (apiKeyId != null && apiKeyLimit != null) {
            return "apikey:$apiKeyId" to apiKeyLimit
        }

        // Authenticated user
        val auth = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
        if (auth != null && auth.isAuthenticated && auth.principal != "anonymousUser") {
            return "user:${auth.principal}" to properties.defaultRpm
        }

        // Unauthenticated — by IP
        val ip = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr
        return "ip:$ip" to properties.unauthenticatedRpm
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !path.startsWith("/api/") ||
                path.startsWith("/actuator/") ||
                path == "/api/v1/auth/login" ||
                path == "/api/v1/auth/register"
    }

    @Scheduled(fixedRate = 120_000)
    fun cleanupStaleEntries() {
        rateLimiter.cleanup()
    }
}
