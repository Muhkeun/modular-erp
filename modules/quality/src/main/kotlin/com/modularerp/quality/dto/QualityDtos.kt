package com.modularerp.quality.dto

import com.modularerp.quality.domain.*
import java.math.BigDecimal
import java.time.LocalDate

data class CreateQiRequest(
    val inspectionType: InspectionType = InspectionType.INCOMING,
    val referenceDocNo: String? = null,
    val itemCode: String, val itemName: String, val plantCode: String,
    val inspectedQuantity: BigDecimal,
    val inspectionDate: LocalDate = LocalDate.now()
)

data class CompleteQiRequest(
    val acceptedQuantity: BigDecimal,
    val rejectedQuantity: BigDecimal,
    val result: QiResult,
    val remarks: String? = null
)

data class QiResponse(
    val id: Long, val documentNo: String, val inspectionType: InspectionType,
    val referenceDocNo: String?, val itemCode: String, val itemName: String, val plantCode: String,
    val inspectedQuantity: BigDecimal, val acceptedQuantity: BigDecimal?, val rejectedQuantity: BigDecimal?,
    val inspectionDate: LocalDate, val status: QiStatus, val result: QiResult?, val remarks: String?
)

fun QualityInspection.toResponse() = QiResponse(
    id = id, documentNo = documentNo, inspectionType = inspectionType,
    referenceDocNo = referenceDocNo, itemCode = itemCode, itemName = itemName, plantCode = plantCode,
    inspectedQuantity = inspectedQuantity, acceptedQuantity = acceptedQuantity,
    rejectedQuantity = rejectedQuantity, inspectionDate = inspectionDate,
    status = status, result = result, remarks = remarks
)
