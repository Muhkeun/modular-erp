package com.modularerp.admin.domain

import com.modularerp.core.domain.BaseEntity
import jakarta.persistence.*

/**
 * 역할에 부여된 리소스별 권한.
 * resource: "items", "purchase-orders", "journal-entries" 등
 * actions: READ, CREATE, UPDATE, DELETE, EXPORT, APPROVE
 */
@Entity
@Table(name = "permissions")
class Permission(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    val role: Role,

    /** 리소스 식별자 (API 경로 또는 메뉴 코드) */
    @Column(nullable = false, length = 100)
    val resource: String,

    /** 허용 액션 목록 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "permission_actions", joinColumns = [JoinColumn(name = "permission_id")])
    @Column(name = "action")
    @Enumerated(EnumType.STRING)
    val actions: MutableSet<ActionType> = mutableSetOf()

) : BaseEntity()

enum class ActionType {
    READ, CREATE, UPDATE, DELETE, EXPORT, IMPORT, APPROVE
}
