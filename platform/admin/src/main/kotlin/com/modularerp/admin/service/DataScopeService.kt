package com.modularerp.admin.service

import com.modularerp.admin.domain.DataScope
import com.modularerp.admin.domain.DataScopeType
import com.modularerp.admin.domain.OrgType
import com.modularerp.admin.dto.*
import com.modularerp.admin.repository.OrganizationRepository
import com.modularerp.admin.repository.DataScopeRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DataScopeService(
    private val dataScopeRepo: DataScopeRepository,
    private val organizationRepo: OrganizationRepository
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

    fun resolveSearchFilter(roleCodes: List<String>, resource: String, userId: String?): DataScopeSearchFilter {
        val resolved = getMergedDataScope(roleCodes, resource)

        return when (resolved.type) {
            DataScopeType.ALL -> DataScopeSearchFilter()
            DataScopeType.OWN ->
                if (userId.isNullOrBlank()) DataScopeSearchFilter(denyAll = true)
                else DataScopeSearchFilter(ownUserId = userId)
            DataScopeType.ORGANIZATION ->
                expandOrganizationScope(resolved.values)
            DataScopeType.DEPARTMENT ->
                if (resolved.values.isEmpty()) DataScopeSearchFilter(denyAll = true)
                else DataScopeSearchFilter(departmentCodes = resolved.values)
            DataScopeType.PLANT ->
                if (resolved.values.isEmpty()) DataScopeSearchFilter(denyAll = true)
                else DataScopeSearchFilter(plantCodes = resolved.values)
        }
    }

    /**
     * ORGANIZATION scope는 조직 트리를 내려가며 문서 필드로 투영 가능한 코드(company/plant/department)만 추출한다.
     * OPERATING_UNIT처럼 직접 대응 필드가 없는 노드는 하위 조직을 통해서만 권한을 확장한다.
     */
    private fun expandOrganizationScope(orgCodes: List<String>): DataScopeSearchFilter {
        if (orgCodes.isEmpty()) return DataScopeSearchFilter(denyAll = true)

        val organizations = organizationRepo.findAllByTenantId(TenantContext.getTenantId()).filter { it.active }
        if (organizations.isEmpty()) return DataScopeSearchFilter(denyAll = true)

        val organizationsByCode = organizations.associateBy { it.code }
        val childrenByParentId = organizations.groupBy { it.parent?.id }
        val expanded = linkedSetOf<com.modularerp.admin.domain.Organization>()

        fun collect(node: com.modularerp.admin.domain.Organization) {
            if (!expanded.add(node)) return
            childrenByParentId[node.id]
                .orEmpty()
                .filter { it.active }
                .sortedBy { it.sortOrder }
                .forEach(::collect)
        }

        orgCodes.mapNotNull(organizationsByCode::get).forEach(::collect)
        if (expanded.isEmpty()) return DataScopeSearchFilter(denyAll = true)

        return DataScopeSearchFilter(
            companyCodes = expanded.filter { it.orgType == OrgType.COMPANY }.map { it.code }.distinct(),
            departmentCodes = expanded.filter { it.orgType == OrgType.DEPARTMENT }.map { it.code }.distinct(),
            plantCodes = expanded.filter { it.orgType == OrgType.PLANT }.map { it.code }.distinct(),
        )
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
