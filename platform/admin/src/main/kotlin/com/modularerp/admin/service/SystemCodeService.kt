package com.modularerp.admin.service

import com.modularerp.admin.domain.SystemCode
import com.modularerp.admin.dto.*
import com.modularerp.admin.repository.SystemCodeRepository
import com.modularerp.core.exception.BusinessException
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SystemCodeService(
    private val systemCodeRepo: SystemCodeRepository
) {

    fun getAll(): List<SystemCodeResponse> =
        systemCodeRepo.findAllByTenantId(TenantContext.getTenantId()).map(SystemCodeResponse::from)

    fun getByGroupCode(groupCode: String): SystemCodeResponse {
        val code = findByGroupCode(groupCode)
        return SystemCodeResponse.from(code)
    }

    @Transactional
    fun create(request: CreateSystemCodeRequest): SystemCodeResponse {
        val tenantId = TenantContext.getTenantId()
        if (systemCodeRepo.findByTenantIdAndGroupCode(tenantId, request.groupCode) != null) {
            throw BusinessException("CODE_DUPLICATE", "Group code '${request.groupCode}' already exists")
        }

        val systemCode = SystemCode(
            groupCode = request.groupCode,
            groupName = request.groupName,
            description = request.description
        ).apply { assignTenant(tenantId) }

        request.items.forEach {
            systemCode.addItem(it.code, it.name, it.sortOrder, it.extra)
        }

        return SystemCodeResponse.from(systemCodeRepo.save(systemCode))
    }

    @Transactional
    fun update(groupCode: String, request: UpdateSystemCodeRequest): SystemCodeResponse {
        val systemCode = findByGroupCode(groupCode)
        if (systemCode.isSystem) throw BusinessException("CODE_SYSTEM", "Cannot modify system code")

        systemCode.update(request.groupName, request.description)
        systemCode.items.clear()
        request.items.forEach {
            systemCode.addItem(it.code, it.name, it.sortOrder, it.extra)
        }

        return SystemCodeResponse.from(systemCodeRepo.save(systemCode))
    }

    @Transactional
    fun delete(groupCode: String) {
        val systemCode = findByGroupCode(groupCode)
        if (systemCode.isSystem) throw BusinessException("CODE_SYSTEM", "Cannot delete system code")
        systemCode.deactivate()
    }

    private fun findByGroupCode(groupCode: String): SystemCode =
        systemCodeRepo.findByTenantIdAndGroupCode(TenantContext.getTenantId(), groupCode)
            ?: throw BusinessException("CODE_NOT_FOUND", "System code '$groupCode' not found")
}
