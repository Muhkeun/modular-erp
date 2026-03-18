package com.modularerp.security.tenant

import jakarta.persistence.EntityManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.hibernate.Session
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 테넌트 필터 — 모든 HTTP 요청에서 테넌트 ID를 추출하고 데이터 격리를 설정.
 *
 * 동작 방식:
 * 1. X-Tenant-Id 헤더 또는 요청 속성에서 테넌트 ID를 추출
 * 2. TenantContext(ThreadLocal)에 테넌트 ID를 저장하여 서비스 계층에서 참조
 * 3. Hibernate 세션에 tenantFilter를 활성화하여 모든 쿼리에 tenant_id 조건 자동 추가
 * 4. 요청 완료 후 TenantContext를 정리하여 메모리 누수 방지
 *
 * 인증/문서/모니터링 경로는 테넌트 필터를 건너뛴다.
 */
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
        // API 경로만 필터 적용, 나머지는 모두 스킵
        return !path.startsWith("/api/") || path.startsWith("/api/v1/auth/")
    }
}
