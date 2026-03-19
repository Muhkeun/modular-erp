package com.modularerp.currency.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "currency_revaluations")
class CurrencyRevaluation(

    @Column(nullable = false, length = 30)
    var documentNo: String = "",

    @Column(nullable = false)
    var revaluationDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var fiscalYear: Int = 0,

    @Column(nullable = false)
    var period: Int = 0,

    @Column(nullable = false, length = 3)
    var fromCurrency: String = "",

    @Column(nullable = false, length = 3)
    var toCurrency: String = "",

    @Column(nullable = false, precision = 15, scale = 6)
    var originalRate: BigDecimal = BigDecimal.ONE,

    @Column(nullable = false, precision = 15, scale = 6)
    var revaluationRate: BigDecimal = BigDecimal.ONE,

    @Column(nullable = false, precision = 19, scale = 4)
    var unrealizedGainLoss: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: RevaluationStatus = RevaluationStatus.DRAFT,

    @Column(length = 100)
    var postedBy: String? = null,

    var postedAt: LocalDateTime? = null

) : TenantEntity() {

    fun post(userId: String) {
        check(status == RevaluationStatus.DRAFT) { "Only DRAFT revaluations can be posted" }
        status = RevaluationStatus.POSTED
        postedBy = userId
        postedAt = LocalDateTime.now()
    }

    fun reverse() {
        check(status == RevaluationStatus.POSTED) { "Only POSTED revaluations can be reversed" }
        status = RevaluationStatus.REVERSED
    }
}

enum class RevaluationStatus { DRAFT, POSTED, REVERSED }
