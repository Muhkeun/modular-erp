package com.modularerp.asset.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "asset_disposals")
class AssetDisposal(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    val asset: Asset,

    @Column(nullable = false)
    var disposalDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var disposalType: DisposalType,

    @Column(nullable = false, precision = 19, scale = 4)
    var disposalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 19, scale = 4)
    var bookValueAtDisposal: BigDecimal,

    @Column(nullable = false, precision = 19, scale = 4)
    var gainLoss: BigDecimal,

    @Column(length = 500)
    var reason: String? = null,

    @Column(length = 100)
    var approvedBy: String? = null

) : TenantEntity()

enum class DisposalType { SALE, SCRAP, DONATION, TRANSFER }
