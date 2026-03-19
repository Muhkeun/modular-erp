package com.modularerp.web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Logs HTTP requests with method, URI, status, duration, userId, tenantId, and correlationId.
 * Skips static resources and health check endpoints.
 */
@Component
@Order(-9)
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger("com.modularerp.web.RequestLog")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val start = System.currentTimeMillis()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - start
            val userId = SecurityContextHolder.getContext().authentication?.principal?.toString() ?: "-"
            val tenantId = request.getAttribute("tenantId") as? String ?: "-"
            val correlationId = MDC.get(CorrelationIdFilter.MDC_KEY) ?: "-"

            log.info(
                "method={} uri={} status={} duration={}ms user={} tenant={} correlationId={}",
                request.method,
                request.requestURI,
                response.status,
                duration,
                userId,
                tenantId,
                correlationId
            )
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/actuator/") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.endsWith(".js") ||
                path.endsWith(".css") ||
                path.endsWith(".html") ||
                path.endsWith(".ico") ||
                path.endsWith(".png") ||
                path.endsWith(".map")
    }
}
