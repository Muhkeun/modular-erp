package com.modularerp.supplychain.dto

import com.modularerp.supplychain.domain.*
import java.math.BigDecimal
import java.time.LocalDate

data class CreateEvaluationRequest(
    val vendorCode: String, val vendorName: String, val evaluationPeriod: String,
    val qualityScore: BigDecimal, val deliveryScore: BigDecimal,
    val priceScore: BigDecimal, val serviceScore: BigDecimal, val remarks: String? = null
)

data class EvaluationResponse(
    val id: Long, val vendorCode: String, val vendorName: String, val evaluationPeriod: String,
    val qualityScore: BigDecimal, val deliveryScore: BigDecimal,
    val priceScore: BigDecimal, val serviceScore: BigDecimal,
    val totalScore: BigDecimal, val grade: SupplierGrade,
    val evaluationDate: LocalDate, val remarks: String?
)

fun SupplierEvaluation.toResponse() = EvaluationResponse(
    id = id, vendorCode = vendorCode, vendorName = vendorName, evaluationPeriod = evaluationPeriod,
    qualityScore = qualityScore, deliveryScore = deliveryScore,
    priceScore = priceScore, serviceScore = serviceScore,
    totalScore = totalScore, grade = grade, evaluationDate = evaluationDate, remarks = remarks
)
