package com.modularerp.currency.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "exchange_rates")
class ExchangeRate(

    @Column(nullable = false, length = 3)
    var fromCurrency: String = "",

    @Column(nullable = false, length = 3)
    var toCurrency: String = "",

    @Column(nullable = false)
    var rateDate: LocalDate = LocalDate.now(),

    @Column(nullable = false, precision = 15, scale = 6)
    var exchangeRate: BigDecimal = BigDecimal.ONE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var rateType: RateType = RateType.SPOT,

    @Column(length = 50)
    var source: String? = null

) : TenantEntity()

enum class RateType { SPOT, AVERAGE, CLOSING }
