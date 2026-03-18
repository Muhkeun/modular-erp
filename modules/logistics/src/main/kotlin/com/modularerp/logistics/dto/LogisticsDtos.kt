package com.modularerp.logistics.dto

import com.modularerp.logistics.domain.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.LocalDate

// === GR DTOs ===
data class CreateGrRequest(
    @field:NotBlank val companyCode: String,
    @field:NotBlank val plantCode: String,
    @field:NotBlank val storageLocation: String,
    val poDocumentNo: String? = null,
    @field:NotBlank val vendorCode: String,
    @field:NotBlank val vendorName: String,
    val receiptDate: LocalDate = LocalDate.now(),
    val remark: String? = null,
    @field:NotEmpty val lines: List<GrLineInput>
)

data class GrLineInput(
    val itemCode: String, val itemName: String,
    val quantity: BigDecimal, val unitOfMeasure: String = "EA",
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val poLineNo: Int? = null, val storageLocation: String? = null
)

data class GrResponse(
    val id: Long, val documentNo: String, val companyCode: String, val plantCode: String,
    val storageLocation: String, val poDocumentNo: String?, val vendorCode: String, val vendorName: String,
    val receiptDate: LocalDate, val status: GrStatus, val remark: String?,
    val lines: List<GrLineResponse>
)

data class GrLineResponse(
    val id: Long, val lineNo: Int, val itemCode: String, val itemName: String,
    val quantity: BigDecimal, val unitOfMeasure: String, val unitPrice: BigDecimal,
    val totalPrice: BigDecimal, val storageLocation: String, val poLineNo: Int?
)

fun GoodsReceipt.toResponse() = GrResponse(
    id = id, documentNo = documentNo, companyCode = companyCode, plantCode = plantCode,
    storageLocation = storageLocation, poDocumentNo = poDocumentNo, vendorCode = vendorCode,
    vendorName = vendorName, receiptDate = receiptDate, status = status, remark = remark,
    lines = lines.map { GrLineResponse(it.id, it.lineNo, it.itemCode, it.itemName, it.quantity, it.unitOfMeasure, it.unitPrice, it.totalPrice, it.storageLocation, it.poLineNo) }
)

// === GI DTOs ===
data class CreateGiRequest(
    @field:NotBlank val companyCode: String,
    @field:NotBlank val plantCode: String,
    @field:NotBlank val storageLocation: String,
    val issueType: GiType = GiType.SALES,
    val referenceDocNo: String? = null,
    val issueDate: LocalDate = LocalDate.now(),
    val remark: String? = null,
    @field:NotEmpty val lines: List<GiLineInput>
)

data class GiLineInput(
    val itemCode: String, val itemName: String,
    val quantity: BigDecimal, val unitOfMeasure: String = "EA",
    val storageLocation: String? = null
)

data class GiResponse(
    val id: Long, val documentNo: String, val companyCode: String, val plantCode: String,
    val storageLocation: String, val issueType: GiType, val referenceDocNo: String?,
    val issueDate: LocalDate, val status: GiStatus, val remark: String?,
    val lines: List<GiLineResponse>
)

data class GiLineResponse(
    val id: Long, val lineNo: Int, val itemCode: String, val itemName: String,
    val quantity: BigDecimal, val unitOfMeasure: String, val storageLocation: String
)

fun GoodsIssue.toResponse() = GiResponse(
    id = id, documentNo = documentNo, companyCode = companyCode, plantCode = plantCode,
    storageLocation = storageLocation, issueType = issueType, referenceDocNo = referenceDocNo,
    issueDate = issueDate, status = status, remark = remark,
    lines = lines.map { GiLineResponse(it.id, it.lineNo, it.itemCode, it.itemName, it.quantity, it.unitOfMeasure, it.storageLocation) }
)

// === Stock DTOs ===
data class StockResponse(
    val id: Long, val itemCode: String, val itemName: String,
    val plantCode: String, val storageLocation: String, val unitOfMeasure: String,
    val quantityOnHand: BigDecimal, val quantityReserved: BigDecimal,
    val availableQuantity: BigDecimal, val totalValue: BigDecimal
)

fun StockSummary.toResponse() = StockResponse(
    id = id, itemCode = itemCode, itemName = itemName, plantCode = plantCode,
    storageLocation = storageLocation, unitOfMeasure = unitOfMeasure,
    quantityOnHand = quantityOnHand, quantityReserved = quantityReserved,
    availableQuantity = availableQuantity, totalValue = totalValue
)
