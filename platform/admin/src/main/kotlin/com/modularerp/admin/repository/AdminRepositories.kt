package com.modularerp.admin.repository

import com.modularerp.admin.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByTenantIdAndCode(tenantId: String, code: String): Role?
    fun findAllByTenantId(tenantId: String): List<Role>
    fun findAllByTenantIdAndCodeIn(tenantId: String, codes: List<String>): List<Role>
}

@Repository
interface MenuProfileRepository : JpaRepository<MenuProfile, Long> {
    fun findByTenantIdAndCode(tenantId: String, code: String): MenuProfile?
    fun findAllByTenantId(tenantId: String): List<MenuProfile>
}

@Repository
interface SystemCodeRepository : JpaRepository<SystemCode, Long> {
    fun findByTenantIdAndGroupCode(tenantId: String, groupCode: String): SystemCode?
    fun findAllByTenantId(tenantId: String): List<SystemCode>
}

@Repository
interface OrganizationRepository : JpaRepository<Organization, Long> {
    fun findAllByTenantId(tenantId: String): List<Organization>
    fun findByTenantIdAndCode(tenantId: String, code: String): Organization?

    @Query("SELECT o FROM Organization o WHERE o.tenantId = :tenantId AND o.parent IS NULL ORDER BY o.sortOrder")
    fun findRootsByTenantId(tenantId: String): List<Organization>
}
