package com.modularerp.admin.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

/**
 * 데이터 범위 권한 (Row-Level Security).
 * 역할별로 조회 가능한 데이터 범위를 조직/부서/사업장 단위로 제한.
 *
 * scopeType + scopeValues 조합으로 필터링:
 * - ORGANIZATION: 특정 조직 코드 목록
 * - DEPARTMENT: 특정 부서 코드 목록
 * - PLANT: 특정 공장 코드 목록
 * - OWN: 본인 데이터만
 * - ALL: 전체 (제한 없음)
 */
@Entity
@Table(
    name = "data_scopes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "role_code", "resource", "scope_type"])]
)
class DataScope(

    @Column(name = "role_code", nullable = false, length = 50)
    val roleCode: String,

    /** 대상 리소스: "purchase-orders", "sales-orders" 등. "*"은 전체 */
    @Column(nullable = false, length = 100)
    val resource: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    val scopeType: DataScopeType,

    /**
     * 범위 값 (콤마 구분).
     * - ORGANIZATION: "ORG001,ORG002"
     * - PLANT: "P001,P002"
     * - OWN/ALL: null
     */
    @Column(name = "scope_values", length = 2000)
    var scopeValues: String? = null

) : TenantEntity() {

    fun getValueList(): List<String> =
        scopeValues?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun updateValues(values: String?) {
        this.scopeValues = values
    }
}

enum class DataScopeType {
    ALL,            // 전체 데이터 접근
    OWN,            // 본인 생성 데이터만
    ORGANIZATION,   // 지정 조직 데이터
    DEPARTMENT,     // 지정 부서 데이터
    PLANT           // 지정 공장 데이터
}
