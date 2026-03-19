package com.modularerp.admin.dto

import com.modularerp.admin.domain.*
import jakarta.validation.constraints.NotBlank

// ── Role DTOs ──

data class CreateRoleRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val name: String,
    val description: String? = null,
    val permissions: List<PermissionRequest> = emptyList()
)

data class UpdateRoleRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    val permissions: List<PermissionRequest> = emptyList()
)

data class PermissionRequest(
    @field:NotBlank val resource: String,
    val actions: Set<ActionType>
)

data class RoleResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val isSystem: Boolean,
    val permissions: List<PermissionResponse>
) {
    companion object {
        fun from(entity: Role) = RoleResponse(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            description = entity.description,
            isSystem = entity.isSystem,
            permissions = entity.permissions.map(PermissionResponse::from)
        )
    }
}

data class PermissionResponse(
    val id: Long,
    val resource: String,
    val actions: Set<ActionType>
) {
    companion object {
        fun from(entity: Permission) = PermissionResponse(
            id = entity.id,
            resource = entity.resource,
            actions = entity.actions.toSet()
        )
    }
}

// ── MenuProfile DTOs ──

data class CreateMenuProfileRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val name: String,
    val description: String? = null,
    val menuItems: List<MenuProfileItemRequest> = emptyList()
)

data class UpdateMenuProfileRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    val menuItems: List<MenuProfileItemRequest> = emptyList()
)

data class MenuProfileItemRequest(
    @field:NotBlank val menuCode: String,
    val sortOrder: Int = 0,
    val visible: Boolean = true
)

data class MenuProfileResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val menuItems: List<MenuProfileItemResponse>
) {
    companion object {
        fun from(entity: MenuProfile) = MenuProfileResponse(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            description = entity.description,
            menuItems = entity.menuItems.map(MenuProfileItemResponse::from)
        )
    }
}

data class MenuProfileItemResponse(
    val id: Long,
    val menuCode: String,
    val sortOrder: Int,
    val visible: Boolean
) {
    companion object {
        fun from(entity: MenuProfileItem) = MenuProfileItemResponse(
            id = entity.id,
            menuCode = entity.menuCode,
            sortOrder = entity.sortOrder,
            visible = entity.visible
        )
    }
}

// ── SystemCode DTOs ──

data class CreateSystemCodeRequest(
    @field:NotBlank val groupCode: String,
    @field:NotBlank val groupName: String,
    val description: String? = null,
    val items: List<SystemCodeItemRequest> = emptyList()
)

data class UpdateSystemCodeRequest(
    @field:NotBlank val groupName: String,
    val description: String? = null,
    val items: List<SystemCodeItemRequest> = emptyList()
)

data class SystemCodeItemRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val name: String,
    val sortOrder: Int = 0,
    val extra: String? = null
)

data class SystemCodeResponse(
    val id: Long,
    val groupCode: String,
    val groupName: String,
    val description: String?,
    val isSystem: Boolean,
    val items: List<SystemCodeItemResponse>
) {
    companion object {
        fun from(entity: SystemCode) = SystemCodeResponse(
            id = entity.id,
            groupCode = entity.groupCode,
            groupName = entity.groupName,
            description = entity.description,
            isSystem = entity.isSystem,
            items = entity.items.map(SystemCodeItemResponse::from)
        )
    }
}

data class SystemCodeItemResponse(
    val id: Long,
    val code: String,
    val name: String,
    val sortOrder: Int,
    val extra: String?
) {
    companion object {
        fun from(entity: SystemCodeItem) = SystemCodeItemResponse(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            sortOrder = entity.sortOrder,
            extra = entity.extra
        )
    }
}

// ── Organization DTOs ──

data class CreateOrganizationRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val name: String,
    val orgType: OrgType,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val description: String? = null
)

data class UpdateOrganizationRequest(
    @field:NotBlank val name: String,
    val sortOrder: Int = 0,
    val description: String? = null
)

data class OrganizationResponse(
    val id: Long,
    val code: String,
    val name: String,
    val orgType: OrgType,
    val parentId: Long?,
    val sortOrder: Int,
    val description: String?,
    val children: List<OrganizationResponse>
) {
    companion object {
        fun from(entity: Organization): OrganizationResponse = OrganizationResponse(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            orgType = entity.orgType,
            parentId = entity.parent?.id,
            sortOrder = entity.sortOrder,
            description = entity.description,
            children = entity.children.filter { it.active }.map { from(it) }
        )
    }
}
