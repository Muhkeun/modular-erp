package com.modularerp.admin.service

import com.modularerp.admin.domain.MenuProfile
import com.modularerp.admin.domain.MenuProfileItem
import com.modularerp.admin.dto.*
import com.modularerp.admin.repository.MenuProfileRepository
import com.modularerp.core.exception.BusinessException
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MenuProfileService(
    private val menuProfileRepo: MenuProfileRepository
) {

    fun getAll(): List<MenuProfileResponse> =
        menuProfileRepo.findAllByTenantId(TenantContext.getTenantId()).map(MenuProfileResponse::from)

    fun getByCode(code: String): MenuProfileResponse {
        val profile = findByCode(code)
        return MenuProfileResponse.from(profile)
    }

    @Transactional
    fun create(request: CreateMenuProfileRequest): MenuProfileResponse {
        val tenantId = TenantContext.getTenantId()
        if (menuProfileRepo.findByTenantIdAndCode(tenantId, request.code) != null) {
            throw BusinessException("PROFILE_DUPLICATE", "Profile code '${request.code}' already exists")
        }

        val profile = MenuProfile(
            code = request.code,
            name = request.name,
            description = request.description
        ).apply { assignTenant(tenantId) }

        val items = request.menuItems.map {
            MenuProfileItem(profile = profile, menuCode = it.menuCode, sortOrder = it.sortOrder, visible = it.visible)
        }
        profile.setMenuItems(items)

        return MenuProfileResponse.from(menuProfileRepo.save(profile))
    }

    @Transactional
    fun update(code: String, request: UpdateMenuProfileRequest): MenuProfileResponse {
        val profile = findByCode(code)
        profile.update(request.name, request.description)

        val items = request.menuItems.map {
            MenuProfileItem(profile = profile, menuCode = it.menuCode, sortOrder = it.sortOrder, visible = it.visible)
        }
        profile.setMenuItems(items)

        return MenuProfileResponse.from(menuProfileRepo.save(profile))
    }

    @Transactional
    fun delete(code: String) {
        val profile = findByCode(code)
        profile.deactivate()
    }

    private fun findByCode(code: String): MenuProfile =
        menuProfileRepo.findByTenantIdAndCode(TenantContext.getTenantId(), code)
            ?: throw BusinessException("PROFILE_NOT_FOUND", "Menu profile '$code' not found")
}
