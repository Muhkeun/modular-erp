package com.modularerp.admin.service

import com.modularerp.admin.domain.Permission
import com.modularerp.admin.domain.Role
import com.modularerp.admin.dto.*
import com.modularerp.admin.repository.RoleRepository
import com.modularerp.core.exception.BusinessException
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RoleService(
    private val roleRepo: RoleRepository
) {

    fun getAll(): List<RoleResponse> =
        roleRepo.findAllByTenantId(TenantContext.getTenantId()).map(RoleResponse::from)

    fun getByCode(code: String): RoleResponse {
        val role = findByCode(code)
        return RoleResponse.from(role)
    }

    @Transactional
    fun create(request: CreateRoleRequest): RoleResponse {
        val tenantId = TenantContext.getTenantId()
        if (roleRepo.findByTenantIdAndCode(tenantId, request.code) != null) {
            throw BusinessException("ROLE_DUPLICATE", "Role code '${request.code}' already exists")
        }

        val role = Role(
            code = request.code,
            name = request.name,
            description = request.description
        ).apply { assignTenant(tenantId) }

        request.permissions.forEach { perm ->
            role.addPermission(perm.resource, perm.actions)
        }

        return RoleResponse.from(roleRepo.save(role))
    }

    @Transactional
    fun update(code: String, request: UpdateRoleRequest): RoleResponse {
        val role = findByCode(code)
        if (role.isSystem) throw BusinessException("ROLE_SYSTEM", "Cannot modify system role")

        role.update(request.name, request.description)
        role.permissions.clear()
        request.permissions.forEach { perm ->
            role.addPermission(perm.resource, perm.actions)
        }

        return RoleResponse.from(roleRepo.save(role))
    }

    @Transactional
    fun delete(code: String) {
        val role = findByCode(code)
        if (role.isSystem) throw BusinessException("ROLE_SYSTEM", "Cannot delete system role")
        role.deactivate()
    }

    /**
     * 사용자의 역할 목록으로부터 통합 Permission 맵 조회.
     */
    fun getPermissionsForRoles(roleCodes: List<String>): Map<String, Set<String>> {
        val tenantId = TenantContext.getTenantId()
        val roles = roleRepo.findAllByTenantIdAndCodeIn(tenantId, roleCodes)
        val merged = mutableMapOf<String, MutableSet<String>>()
        roles.flatMap { it.permissions }.forEach { perm ->
            merged.getOrPut(perm.resource) { mutableSetOf() }
                .addAll(perm.actions.map { it.name })
        }
        return merged
    }

    private fun findByCode(code: String): Role =
        roleRepo.findByTenantIdAndCode(TenantContext.getTenantId(), code)
            ?: throw BusinessException("ROLE_NOT_FOUND", "Role '$code' not found")
}
