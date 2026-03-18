package com.modularerp.purchase.dto

import com.modularerp.purchase.domain.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.LocalDate

// === Purchase Request DTOs ===

data class CreatePrRequest(
    @field:NotBlank val companyCode: String,
    @field:NotBlank val plantCode: String,
    val departmentCode: String? = null,
    val prType: PrType = PrType.STANDARD,
    val deliveryDate: LocalDate? = null,
    val description: String? = null,
    @field:NotEmpty val lines: List<PrLineInput>
)

data class PrLineInput(
    val itemCode: String,
    val itemName: String,
    val quantity: BigDecimal,
    val unitOfMeasure: String = "EA",
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val specification: String? = null,
    val remark: String? = null
)

data class PrResponse(
    val id: Long,
    val documentNo: String,
    val companyCode: String,
    val plantCode: String,
    val departmentCode: String?,
    val requestDate: LocalDate,
    val deliveryDate: LocalDate?,
    val status: PrStatus,
    val prType: PrType,
    val description: String?,
    val requestedBy: String?,
    val totalAmount: BigDecimal,
    val lines: List<PrLineResponse>
)

data class PrLineResponse(
    val id: Long,
    val lineNo: Int,
    val itemCode: String,
    val itemName: String,
    val quantity: BigDecimal,
    val unitOfMeasure: String,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val openQuantity: BigDecimal,
    val specification: String?,
    val remark: String?
)

fun PurchaseRequest.toResponse() = PrResponse(
    id = id, documentNo = documentNo, companyCode = companyCode, plantCode = plantCode,
    departmentCode = departmentCode, requestDate = requestDate, deliveryDate = deliveryDate,
    status = status, prType = prType, description = description, requestedBy = requestedBy,
    totalAmount = totalAmount,
    lines = lines.map { it.toResponse() }
)

fun PurchaseRequestLine.toResponse() = PrLineResponse(
    id = id, lineNo = lineNo, itemCode = itemCode, itemName = itemName,
    quantity = quantity, unitOfMeasure = unitOfMeasure, unitPrice = unitPrice,
    totalPrice = totalPrice, openQuantity = openQuantity,
    specification = specification, remark = remark
)

// === Purchase Order DTOs ===

data class CreatePoRequest(
    @field:NotBlank val companyCode: String,
    @field:NotBlank val plantCode: String,
    @field:NotBlank val vendorCode: String,
    @field:NotBlank val vendorName: String,
    val deliveryDate: LocalDate? = null,
    val currencyCode: String = "KRW",
    val paymentTerms: String? = null,
    val deliveryTerms: String? = null,
    val remark: String? = null,
    @field:NotEmpty val lines: List<PoLineInput>
)

data class CreatePoFromPrRequest(
    @field:NotBlank val vendorCode: String,
    @field:NotBlank val vendorName: String,
    val deliveryDate: LocalDate? = null,
    val currencyCode: String = "KRW",
    val paymentTerms: String? = null
)

data class PoLineInput(
    val itemCode: String,
    val itemName: String,
    val quantity: BigDecimal,
    val unitOfMeasure: String = "EA",
    val unitPrice: BigDecimal,
    val taxRate: BigDecimal = BigDecimal("0.10"),
    val specification: String? = null,
    val prDocumentNo: String? = null,
    val prLineNo: Int? = null
)

data class PoResponse(
    val id: Long,
    val documentNo: String,
    val companyCode: String,
    val plantCode: String,
    val vendorCode: String,
    val vendorName: String,
    val orderDate: LocalDate,
    val deliveryDate: LocalDate?,
    val status: PoStatus,
    val currencyCode: String,
    val totalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val grandTotal: BigDecimal,
    val paymentTerms: String?,
    val remark: String?,
    val lines: List<PoLineResponse>
)

data class PoLineResponse(
    val id: Long,
    val lineNo: Int,
    val itemCode: String,
    val itemName: String,
    val quantity: BigDecimal,
    val unitOfMeasure: String,
    val unitPrice: BigDecimal,
    val taxRate: BigDecimal,
    val totalPrice: BigDecimal,
    val taxAmount: BigDecimal,
    val receivedQuantity: BigDecimal,
    val openQuantity: BigDecimal,
    val specification: String?,
    val prDocumentNo: String?,
    val prLineNo: Int?
)

fun PurchaseOrder.toResponse() = PoResponse(
    id = id, documentNo = documentNo, companyCode = companyCode, plantCode = plantCode,
    vendorCode = vendorCode, vendorName = vendorName, orderDate = orderDate,
    deliveryDate = deliveryDate, status = status, currencyCode = currencyCode,
    totalAmount = totalAmount, taxAmount = taxAmount, grandTotal = grandTotal,
    paymentTerms = paymentTerms, remark = remark,
    lines = lines.map { it.toResponse() }
)

fun PurchaseOrderLine.toResponse() = PoLineResponse(
    id = id, lineNo = lineNo, itemCode = itemCode, itemName = itemName,
    quantity = quantity, unitOfMeasure = unitOfMeasure, unitPrice = unitPrice,
    taxRate = taxRate, totalPrice = totalPrice, taxAmount = taxAmount,
    receivedQuantity = receivedQuantity, openQuantity = openQuantity,
    specification = specification, prDocumentNo = prDocumentNo, prLineNo = prLineNo
)
