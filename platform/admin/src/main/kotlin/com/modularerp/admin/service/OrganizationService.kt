package com.modularerp.admin.service

import com.modularerp.admin.domain.Organization
import com.modularerp.admin.dto.*
import com.modularerp.admin.repository.OrganizationRepository
import com.modularerp.core.exception.BusinessException
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrganizationService(
    private val orgRepo: OrganizationRepository
) {

    fun getTree(): List<OrganizationResponse> =
        orgRepo.findRootsByTenantId(TenantContext.getTenantId())
            .filter { it.active }
            .map(OrganizationResponse::from)

    fun getAll(): List<OrganizationResponse> =
        orgRepo.findAllByTenantId(TenantContext.getTenantId())
            .filter { it.active }
            .map(OrganizationResponse::from)

    @Transactional
    fun create(request: CreateOrganizationRequest): OrganizationResponse {
        val tenantId = TenantContext.getTenantId()
        val parent = request.parentId?.let {
            orgRepo.findById(it).orElseThrow { BusinessException("ORG_NOT_FOUND", "Parent org not found") }
        }

        val org = Organization(
            code = request.code,
            name = request.name,
            orgType = request.orgType,
            parent = parent,
            sortOrder = request.sortOrder,
            description = request.description
        ).apply { assignTenant(tenantId) }

        return OrganizationResponse.from(orgRepo.save(org))
    }

    @Transactional
    fun update(id: Long, request: UpdateOrganizationRequest): OrganizationResponse {
        val org = orgRepo.findById(id).orElseThrow { BusinessException("ORG_NOT_FOUND", "Organization not found") }
        org.update(request.name, request.sortOrder, request.description)
        return OrganizationResponse.from(orgRepo.save(org))
    }

    @Transactional
    fun delete(id: Long) {
        val org = orgRepo.findById(id).orElseThrow { BusinessException("ORG_NOT_FOUND", "Organization not found") }
        org.deactivate()
    }
}
