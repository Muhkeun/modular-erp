package com.modularerp.admin.repository

import com.modularerp.admin.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FieldPermissionRepository : JpaRepository<FieldPermission, Long> {
    fun findAllByTenantIdAndRoleCode(tenantId: String, roleCode: String): List<FieldPermission>
    fun findAllByTenantIdAndRoleCodeAndResource(tenantId: String, roleCode: String, resource: String): List<FieldPermission>
    fun findAllByTenantIdAndRoleCodeInAndResource(tenantId: String, roleCodes: List<String>, resource: String): List<FieldPermission>
    fun findByTenantIdAndRoleCodeAndResourceAndFieldName(tenantId: String, roleCode: String, resource: String, fieldName: String): FieldPermission?
    fun deleteAllByTenantIdAndRoleCodeAndResource(tenantId: String, roleCode: String, resource: String)
}

@Repository
interface DataScopeRepository : JpaRepository<DataScope, Long> {
    fun findAllByTenantIdAndRoleCode(tenantId: String, roleCode: String): List<DataScope>
    fun findAllByTenantIdAndRoleCodeInAndResource(tenantId: String, roleCodes: List<String>, resource: String): List<DataScope>
    fun findByTenantIdAndRoleCodeAndResourceAndScopeType(tenantId: String, roleCode: String, resource: String, scopeType: DataScopeType): DataScope?
    fun deleteAllByTenantIdAndRoleCodeAndResource(tenantId: String, roleCode: String, resource: String)
}

@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, Long> {
    fun findAllByTenantId(tenantId: String): List<ApiKey>
    fun findByKeyHash(keyHash: String): ApiKey?
}

@Repository
interface TenantRepository : JpaRepository<Tenant, Long> {
    fun findByTenantId(tenantId: String): Tenant?
    fun findAllByStatus(status: TenantStatus): List<Tenant>
}
