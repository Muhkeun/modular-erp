package com.modularerp.security.apikey

import com.modularerp.security.tenant.TenantContext
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authenticates requests using the X-API-Key header.
 * Runs before JwtAuthenticationFilter so API key takes precedence if present.
 */
@Component
@Order(-1)
class ApiKeyAuthenticationFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Autowired(required = false)
    private var apiKeyAuthenticator: ApiKeyAuthenticator? = null

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val apiKey = request.getHeader("X-API-Key")
        if (apiKey != null && apiKeyAuthenticator != null) {
            val principal = apiKeyAuthenticator!!.authenticate(apiKey)
            if (principal != null) {
                TenantContext.setTenantId(principal.tenantId)
                TenantContext.setUserId("apikey:${principal.name}")
                request.setAttribute("tenantId", principal.tenantId)
                request.setAttribute("apiKeyId", principal.id.toString())
                request.setAttribute("apiKeyRateLimit", principal.rateLimit ?: 100)

                val authorities = listOf(SimpleGrantedAuthority("ROLE_API_KEY"))
                val auth = UsernamePasswordAuthenticationToken(
                    "apikey:${principal.id}", null, authorities
                )
                SecurityContextHolder.getContext().authentication = auth

                log.debug("API key authenticated: name={}, tenant={}", principal.name, principal.tenantId)
            } else {
                log.warn("Invalid API key attempted")
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.contentType = "application/json"
                response.writer.write("""{"success":false,"error":{"code":"INVALID_API_KEY","message":"Invalid or expired API key"}}""")
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !request.requestURI.startsWith("/api/") || request.getHeader("X-API-Key") == null
    }
}
