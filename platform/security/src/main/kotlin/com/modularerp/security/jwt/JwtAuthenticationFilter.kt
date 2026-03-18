package com.modularerp.security.jwt

import com.modularerp.security.tenant.TenantContext
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(0)
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)
        if (token != null) {
            try {
                val claims = jwtProvider.validateAndParse(token)
                val userId = jwtProvider.getUserId(claims)
                val tenantId = jwtProvider.getTenantId(claims)
                val roles = jwtProvider.getRoles(claims)
                val locale = jwtProvider.getLocale(claims)

                TenantContext.setTenantId(tenantId)
                TenantContext.setUserId(userId)
                TenantContext.setLocale(locale)

                request.setAttribute("tenantId", tenantId)

                val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
                val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
                SecurityContextHolder.getContext().authentication = auth
            } catch (_: Exception) {
                // Invalid token — proceed unauthenticated
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith("Bearer ")) header.substring(7) else null
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !path.startsWith("/api/")
    }
}
