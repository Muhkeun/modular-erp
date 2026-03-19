package com.modularerp.currency.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

@Entity
@Table(name = "currencies")
class Currency(

    @Column(nullable = false, length = 3)
    var currencyCode: String = "",

    @Column(nullable = false, length = 100)
    var currencyName: String = "",

    @Column(nullable = false, length = 5)
    var symbol: String = "",

    @Column(nullable = false)
    var decimalPlaces: Int = 2,

    @Column(nullable = false)
    var isBaseCurrency: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CurrencyStatus = CurrencyStatus.ACTIVE

) : TenantEntity()

enum class CurrencyStatus { ACTIVE, INACTIVE }
