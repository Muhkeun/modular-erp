package com.modularerp.admin.service

import com.modularerp.admin.domain.Tenant
import com.modularerp.admin.domain.TenantStatus
import com.modularerp.admin.dto.*
import com.modularerp.admin.repository.TenantRepository
import com.modularerp.core.exception.BusinessException
import com.modularerp.security.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TenantManagementService(
    private val tenantRepo: TenantRepository,
    private val userRepo: UserRepository
) {

    fun getAll(): List<TenantResponse> =
        tenantRepo.findAll().filter { it.active }.map { toResponse(it) }

    fun getById(tenantId: String): TenantResponse {
        val tenant = findTenant(tenantId)
        return toResponse(tenant)
    }

    @Transactional
    fun create(request: CreateTenantRequest): TenantResponse {
        if (tenantRepo.findByTenantId(request.tenantId) != null) {
            throw BusinessException("TENANT_DUPLICATE", "Tenant '${request.tenantId}' already exists")
        }
        val tenant = Tenant(
            tenantId = request.tenantId,
            name = request.name,
            description = request.description,
            plan = request.plan,
            maxUsers = request.maxUsers,
            maxStorageMb = request.maxStorageMb
        )
        return toResponse(tenantRepo.save(tenant))
    }

    @Transactional
    fun update(tenantId: String, request: UpdateTenantRequest): TenantResponse {
        val tenant = findTenant(tenantId)
        tenant.update(request.name, request.description, request.maxUsers, request.maxStorageMb)
        return toResponse(tenantRepo.save(tenant))
    }

    @Transactional
    fun suspend(tenantId: String) {
        val tenant = findTenant(tenantId)
        tenant.suspend()
    }

    @Transactional
    fun activate(tenantId: String) {
        val tenant = findTenant(tenantId)
        tenant.activateTenant()
    }

    private fun findTenant(tenantId: String): Tenant =
        tenantRepo.findByTenantId(tenantId)
            ?: throw BusinessException("TENANT_NOT_FOUND", "Tenant '$tenantId' not found")

    private fun toResponse(tenant: Tenant): TenantResponse {
        val userCount = userRepo.countByTenantId(tenant.tenantId)
        return TenantResponse.from(tenant, userCount)
    }
}
