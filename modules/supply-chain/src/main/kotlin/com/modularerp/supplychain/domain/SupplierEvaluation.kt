package com.modularerp.supplychain.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "supplier_evaluations")
class SupplierEvaluation(

    @Column(nullable = false, length = 50)
    var vendorCode: String,

    @Column(nullable = false, length = 200)
    var vendorName: String,

    @Column(nullable = false, length = 10)
    var evaluationPeriod: String,  // "202603"

    @Column(nullable = false, precision = 5, scale = 2)
    var qualityScore: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 2)
    var deliveryScore: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 2)
    var priceScore: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 5, scale = 2)
    var serviceScore: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    var grade: SupplierGrade = SupplierGrade.C,

    var evaluationDate: LocalDate = LocalDate.now(),

    @Column(length = 1000)
    var remarks: String? = null

) : TenantEntity() {

    val totalScore: BigDecimal
        get() = qualityScore.multiply(BigDecimal("0.30"))
            .add(deliveryScore.multiply(BigDecimal("0.25")))
            .add(priceScore.multiply(BigDecimal("0.25")))
            .add(serviceScore.multiply(BigDecimal("0.20")))

    fun calculateGrade() {
        val score = totalScore
        grade = when {
            score >= BigDecimal("90") -> SupplierGrade.A
            score >= BigDecimal("80") -> SupplierGrade.B
            score >= BigDecimal("70") -> SupplierGrade.C
            score >= BigDecimal("60") -> SupplierGrade.D
            else -> SupplierGrade.F
        }
    }
}

enum class SupplierGrade { A, B, C, D, F }
