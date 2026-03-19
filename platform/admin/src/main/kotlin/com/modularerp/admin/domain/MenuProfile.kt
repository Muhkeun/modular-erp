package com.modularerp.admin.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

/**
 * 메뉴 프로필: 역할/그룹별로 접근 가능한 메뉴 항목 정의.
 */
@Entity
@Table(
    name = "menu_profiles",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "profile_code"])]
)
class MenuProfile(

    @Column(name = "profile_code", nullable = false, length = 50)
    val code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], orphanRemoval = true)
    val menuItems: MutableList<MenuProfileItem> = mutableListOf()

) : TenantEntity() {

    fun setMenuItems(items: List<MenuProfileItem>) {
        this.menuItems.clear()
        this.menuItems.addAll(items)
    }

    fun update(name: String, description: String?) {
        this.name = name
        this.description = description
    }
}

@Entity
@Table(name = "menu_profile_items")
class MenuProfileItem(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: MenuProfile,

    /** 메뉴 코드: e.g. "master-data", "master-data.items", "purchase.po" */
    @Column(name = "menu_code", nullable = false, length = 100)
    val menuCode: String,

    /** 표시 순서 */
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    /** 표시 여부 */
    @Column(nullable = false)
    var visible: Boolean = true

) : com.modularerp.core.domain.BaseEntity()
