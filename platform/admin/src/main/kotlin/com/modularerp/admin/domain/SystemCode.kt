package com.modularerp.admin.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

/**
 * 시스템 코드 그룹.
 * 공통 코드 관리: 결재유형, 결제조건, 품목유형, 단위 등
 */
@Entity
@Table(
    name = "system_codes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "group_code"])]
)
class SystemCode(

    @Column(name = "group_code", nullable = false, length = 50)
    val groupCode: String,

    @Column(name = "group_name", nullable = false, length = 100)
    var groupName: String,

    @Column(length = 500)
    var description: String? = null,

    /** 시스템 코드는 수정 불가 */
    @Column(name = "is_system", nullable = false)
    val isSystem: Boolean = false,

    @OneToMany(mappedBy = "systemCode", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    val items: MutableList<SystemCodeItem> = mutableListOf()

) : TenantEntity() {

    fun addItem(code: String, name: String, sortOrder: Int = 0, extra: String? = null): SystemCodeItem {
        val item = SystemCodeItem(
            systemCode = this,
            code = code,
            name = name,
            sortOrder = sortOrder,
            extra = extra
        )
        items.add(item)
        return item
    }

    fun update(groupName: String, description: String?) {
        this.groupName = groupName
        this.description = description
    }
}

@Entity
@Table(name = "system_code_items")
class SystemCodeItem(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_code_id", nullable = false)
    val systemCode: SystemCode,

    @Column(nullable = false, length = 50)
    val code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    /** 추가 속성 (JSON) */
    @Column(length = 2000)
    var extra: String? = null

) : com.modularerp.core.domain.BaseEntity() {

    fun update(name: String, sortOrder: Int, extra: String?) {
        this.name = name
        this.sortOrder = sortOrder
        this.extra = extra
    }
}
