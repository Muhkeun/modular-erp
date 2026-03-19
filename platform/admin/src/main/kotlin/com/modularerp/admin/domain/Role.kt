package com.modularerp.admin.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

/**
 * RBAC 역할 정의.
 * Role → Permission → Resource 3단 구조.
 */
@Entity
@Table(
    name = "roles",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "role_code"])]
)
class Role(

    @Column(name = "role_code", nullable = false, length = 50)
    val code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    /** 시스템 역할은 삭제/수정 불가 (ADMIN, USER 등) */
    @Column(name = "is_system", nullable = false)
    val isSystem: Boolean = false,

    @OneToMany(mappedBy = "role", cascade = [CascadeType.ALL], orphanRemoval = true)
    val permissions: MutableSet<Permission> = mutableSetOf()

) : TenantEntity() {

    fun addPermission(resource: String, actions: Set<ActionType>) {
        val existing = permissions.find { it.resource == resource }
        if (existing != null) {
            existing.actions.addAll(actions)
        } else {
            permissions.add(Permission(role = this, resource = resource, actions = actions.toMutableSet()))
        }
    }

    fun removePermission(resource: String) {
        permissions.removeIf { it.resource == resource }
    }

    fun update(name: String, description: String?) {
        this.name = name
        this.description = description
    }
}
