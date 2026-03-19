package com.modularerp.admin.service

import com.modularerp.admin.domain.DataScope
import com.modularerp.admin.domain.DataScopeType
import com.modularerp.admin.dto.*
import com.modularerp.admin.repository.DataScopeRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DataScopeService(
    private val dataScopeRepo: DataScopeRepository
) {

    fun getByRole(roleCode: String): List<DataScopeResponse> =
        dataScopeRepo.findAllByTenantIdAndRoleCode(
            TenantContext.getTenantId(), roleCode
        ).map(DataScopeResponse::from)

    /**
     * 사용자의 역할 목록에 대해 특정 리소스의 데이터 범위를 통합.
     * ALL이 하나라도 있으면 전체 접근. 아니면 모든 범위 병합.
     */
    fun getMergedDataScope(roleCodes: List<String>, resource: String): ResolvedDataScope {
        val scopes = dataScopeRepo.findAllByTenantIdAndRoleCodeInAndResource(
            TenantContext.getTenantId(), roleCodes, resource
        )

        if (scopes.isEmpty()) return ResolvedDataScope(DataScopeType.ALL, emptyList())
        if (scopes.any { it.scopeType == DataScopeType.ALL }) return ResolvedDataScope(DataScopeType.ALL, emptyList())

        // OWN은 최소 범위
        val nonOwn = scopes.filter { it.scopeType != DataScopeType.OWN }
        if (nonOwn.isEmpty()) return ResolvedDataScope(DataScopeType.OWN, emptyList())

        // 나머지 범위 병합 (같은 타입의 값들 합치기)
        val mergedValues = nonOwn.flatMap { it.getValueList() }.distinct()
        return ResolvedDataScope(nonOwn.first().scopeType, mergedValues)
    }

    @Transactional
    fun saveForRole(roleCode: String, resource: String, scopes: List<DataScopeRequest>) {
        val tenantId = TenantContext.getTenantId()
        dataScopeRepo.deleteAllByTenantIdAndRoleCodeAndResource(tenantId, roleCode, resource)

        scopes.forEach { req ->
            dataScopeRepo.save(
                DataScope(
                    roleCode = roleCode,
                    resource = resource,
                    scopeType = req.scopeType,
                    scopeValues = req.scopeValues
                ).apply { assignTenant(tenantId) }
            )
        }
    }
}

data class ResolvedDataScope(
    val type: DataScopeType,
    val values: List<String>
)
