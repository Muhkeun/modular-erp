package com.modularerp.costing.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "cost_allocations")
class CostAllocation(

    @Column(nullable = false, length = 30)
    var documentNo: String = "",

    @Column(nullable = false)
    var allocationDate: LocalDate = LocalDate.now(),

    @Column(nullable = false, length = 30)
    var fromCostCenter: String = "",

    @Column(nullable = false, length = 30)
    var toCostCenter: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var allocationType: AllocationType = AllocationType.DIRECT,

    @Column(nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO,

    @Column(length = 200)
    var allocationBasis: String? = null,

    @Column(precision = 5, scale = 2)
    var percentage: BigDecimal? = null,

    @Column(length = 500)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CostAllocationStatus = CostAllocationStatus.DRAFT,

    @Column(nullable = false)
    var fiscalYear: Int = 0,

    @Column(nullable = false)
    var period: Int = 0

) : TenantEntity() {

    fun post() {
        check(status == CostAllocationStatus.DRAFT) { "Only DRAFT allocations can be posted" }
        status = CostAllocationStatus.POSTED
    }
}

enum class AllocationType { DIRECT, ACTIVITY_BASED, PERCENTAGE }
enum class CostAllocationStatus { DRAFT, POSTED }
