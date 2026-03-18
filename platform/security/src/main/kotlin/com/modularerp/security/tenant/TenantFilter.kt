package com.modularerp.security.tenant

import jakarta.persistence.EntityManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.hibernate.Session
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(1)
class TenantFilter(
    private val entityManager: EntityManager
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val tenantId = resolveTenantId(request)
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId)
                enableHibernateFilter(tenantId)
            }
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun resolveTenantId(request: HttpServletRequest): String? {
        return request.getHeader("X-Tenant-Id")
            ?: request.getAttribute("tenantId") as? String
    }

    private fun enableHibernateFilter(tenantId: String) {
        val session = entityManager.unwrap(Session::class.java)
        session.enableFilter("tenantFilter").setParameter("tenantId", tenantId)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/api/v1/auth/")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/actuator")
    }
}
