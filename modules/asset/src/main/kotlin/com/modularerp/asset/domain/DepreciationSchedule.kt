package com.modularerp.asset.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "depreciation_schedules")
class DepreciationSchedule(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    val asset: Asset,

    @Column(nullable = false)
    var periodYear: Int,

    @Column(nullable = false)
    var periodMonth: Int,

    @Column(nullable = false, precision = 19, scale = 4)
    var depreciationAmount: BigDecimal,

    @Column(nullable = false, precision = 19, scale = 4)
    var accumulatedAmount: BigDecimal,

    @Column(nullable = false, precision = 19, scale = 4)
    var bookValueAfter: BigDecimal,

    @Column(nullable = false)
    var posted: Boolean = false,

    var journalEntryId: Long? = null

) : TenantEntity()
