package com.modularerp.admin.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

/**
 * 필드 레벨 권한.
 * 특정 역할에 대해 특정 리소스의 필드별 접근 수준 정의.
 *
 * 예: VIEWER 역할은 PurchaseOrder의 unitPrice 필드를 HIDDEN으로 설정.
 */
@Entity
@Table(
    name = "field_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "role_code", "resource", "field_name"])]
)
class FieldPermission(

    @Column(name = "role_code", nullable = false, length = 50)
    val roleCode: String,

    /** 리소스 식별자: "items", "purchase-orders" 등 */
    @Column(nullable = false, length = 100)
    val resource: String,

    /** 필드명: "unitPrice", "costPrice", "margin" 등 */
    @Column(name = "field_name", nullable = false, length = 100)
    val fieldName: String,

    /** 접근 수준 */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 20)
    var accessLevel: FieldAccessLevel

) : TenantEntity() {

    fun updateAccessLevel(level: FieldAccessLevel) {
        this.accessLevel = level
    }
}

enum class FieldAccessLevel {
    FULL,       // 읽기/쓰기 가능
    READONLY,   // 읽기만 가능
    MASKED,     // 마스킹 표시 (예: *****)
    HIDDEN      // 완전 숨김
}
