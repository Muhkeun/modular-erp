package com.modularerp.admin.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

/**
 * 조직 트리: 회사 → 사업장 → 부서 계층 구조.
 */
@Entity
@Table(name = "organizations")
class Organization(

    @Column(nullable = false, length = 50)
    val code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val orgType: OrgType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Organization? = null,

    @OneToMany(mappedBy = "parent")
    val children: MutableList<Organization> = mutableListOf(),

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity() {

    fun update(name: String, sortOrder: Int, description: String?) {
        this.name = name
        this.sortOrder = sortOrder
        this.description = description
    }
}

enum class OrgType {
    COMPANY,        // 회사
    OPERATING_UNIT, // 사업장
    PLANT,          // 공장/플랜트
    DEPARTMENT      // 부서
}
