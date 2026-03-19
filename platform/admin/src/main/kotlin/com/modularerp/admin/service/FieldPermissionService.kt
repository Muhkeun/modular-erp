package com.modularerp.admin.service

import com.modularerp.admin.domain.FieldAccessLevel
import com.modularerp.admin.domain.FieldPermission
import com.modularerp.admin.dto.*
import com.modularerp.admin.repository.FieldPermissionRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FieldPermissionService(
    private val fieldPermRepo: FieldPermissionRepository
) {

    fun getByRoleAndResource(roleCode: String, resource: String): List<FieldPermissionResponse> =
        fieldPermRepo.findAllByTenantIdAndRoleCodeAndResource(
            TenantContext.getTenantId(), roleCode, resource
        ).map(FieldPermissionResponse::from)

    /**
     * 사용자의 역할 목록에 대해 특정 리소스의 필드 권한을 통합.
     * 여러 역할 중 가장 넓은 접근 수준 적용 (FULL > READONLY > MASKED > HIDDEN).
     */
    fun getMergedFieldPermissions(roleCodes: List<String>, resource: String): Map<String, FieldAccessLevel> {
        val perms = fieldPermRepo.findAllByTenantIdAndRoleCodeInAndResource(
            TenantContext.getTenantId(), roleCodes, resource
        )
        val merged = mutableMapOf<String, FieldAccessLevel>()
        perms.forEach { perm ->
            val current = merged[perm.fieldName]
            if (current == null || perm.accessLevel.ordinal < current.ordinal) {
                merged[perm.fieldName] = perm.accessLevel
            }
        }
        return merged
    }

    @Transactional
    fun saveForRole(roleCode: String, resource: String, fields: List<FieldPermissionRequest>) {
        val tenantId = TenantContext.getTenantId()
        fieldPermRepo.deleteAllByTenantIdAndRoleCodeAndResource(tenantId, roleCode, resource)

        fields.filter { it.accessLevel != FieldAccessLevel.FULL }.forEach { req ->
            fieldPermRepo.save(
                FieldPermission(
                    roleCode = roleCode,
                    resource = resource,
                    fieldName = req.fieldName,
                    accessLevel = req.accessLevel
                ).apply { assignTenant(tenantId) }
            )
        }
    }
}
