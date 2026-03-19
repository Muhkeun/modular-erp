package com.modularerp.costing.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

@Entity
@Table(name = "cost_centers")
class CostCenter(

    @Column(nullable = false, length = 30)
    var costCenterCode: String = "",

    @Column(nullable = false, length = 200)
    var costCenterName: String = "",

    @Column(length = 30)
    var parentCode: String? = null,

    @Column(length = 30)
    var departmentCode: String? = null,

    @Column(length = 100)
    var managerName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CostCenterStatus = CostCenterStatus.ACTIVE

) : TenantEntity()

enum class CostCenterStatus { ACTIVE, INACTIVE }
