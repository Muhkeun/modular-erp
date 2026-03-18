package com.modularerp.production.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Work Center — a production resource (machine, line, station).
 * Defines capacity and cost rates for production scheduling.
 */
@Entity
@Table(name = "work_centers")
class WorkCenter(

    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var centerType: WorkCenterType = WorkCenterType.MACHINE,

    /** Available hours per day */
    @Column(nullable = false, precision = 5, scale = 2)
    var capacityPerDay: BigDecimal = BigDecimal("8.00"),

    /** Number of parallel resources (e.g. 3 identical machines) */
    @Column(nullable = false)
    var resourceCount: Int = 1,

    /** Cost per hour (labor + machine) */
    @Column(nullable = false, precision = 19, scale = 4)
    var costPerHour: BigDecimal = BigDecimal.ZERO,

    /** Setup cost per operation */
    @Column(nullable = false, precision = 19, scale = 4)
    var setupCost: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: WorkCenterStatus = WorkCenterStatus.ACTIVE,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity() {

    /** Total daily capacity in hours */
    val totalDailyCapacity: BigDecimal
        get() = capacityPerDay.multiply(BigDecimal(resourceCount))
}

enum class WorkCenterType { MACHINE, LABOR, ASSEMBLY_LINE, INSPECTION }
enum class WorkCenterStatus { ACTIVE, MAINTENANCE, INACTIVE }
