package com.modularerp.core.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.math.BigDecimal
import java.util.Currency

@Embeddable
data class Money(
    @Column(name = "amount", precision = 19, scale = 4)
    val amount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "currency_code", length = 3)
    val currencyCode: String = "KRW"
) {
    val currency: Currency get() = Currency.getInstance(currencyCode)

    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot add different currencies" }
        return copy(amount = amount.add(other.amount))
    }

    operator fun minus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot subtract different currencies" }
        return copy(amount = amount.subtract(other.amount))
    }

    operator fun times(quantity: BigDecimal): Money =
        copy(amount = amount.multiply(quantity))

    companion object {
        fun zero(currencyCode: String = "KRW") = Money(BigDecimal.ZERO, currencyCode)
    }
}

@Embeddable
data class Quantity(
    @Column(name = "qty", precision = 15, scale = 4)
    val value: BigDecimal = BigDecimal.ZERO,

    @Column(name = "unit", length = 10)
    val unit: String = "EA"
) {
    operator fun plus(other: Quantity): Quantity {
        require(unit == other.unit) { "Cannot add different units" }
        return copy(value = value.add(other.value))
    }

    operator fun minus(other: Quantity): Quantity {
        require(unit == other.unit) { "Cannot subtract different units" }
        return copy(value = value.subtract(other.value))
    }
}
