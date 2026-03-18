package com.modularerp.core.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.ParamDef

/**
 * 멀티테넌트 엔티티 — 테넌트(회사/조직) 격리를 지원하는 엔티티 기반 클래스.
 *
 * Hibernate @Filter를 사용하여 모든 조회 쿼리에 tenant_id 조건을 자동 추가한다.
 * 이를 통해 테넌트 간 데이터가 완전히 격리되며,
 * 개별 쿼리에서 테넌트 조건을 명시할 필요가 없다.
 *
 * 모든 비즈니스 도메인 엔티티(PR, PO, WO, SO 등)는 이 클래스를 상속한다.
 */
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = [ParamDef(name = "tenantId", type = String::class)])
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
abstract class TenantEntity : BaseEntity() {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    var tenantId: String = ""
        protected set

    fun assignTenant(tenantId: String) {
        this.tenantId = tenantId
    }
}
