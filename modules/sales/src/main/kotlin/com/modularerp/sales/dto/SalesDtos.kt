package com.modularerp.sales.dto

import com.modularerp.sales.domain.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.LocalDate

data class CreateSoRequest(
    @field:NotBlank val companyCode: String,
    @field:NotBlank val plantCode: String,
    @field:NotBlank val customerCode: String,
    @field:NotBlank val customerName: String,
    val deliveryDate: LocalDate? = null,
    val currencyCode: String = "KRW",
    val paymentTerms: String? = null,
    val shippingAddress: String? = null,
    val remark: String? = null,
    @field:NotEmpty val lines: List<SoLineInput>
)

data class SoLineInput(
    val itemCode: String, val itemName: String, val quantity: BigDecimal,
    val unitOfMeasure: String = "EA", val unitPrice: BigDecimal,
    val taxRate: BigDecimal = BigDecimal("0.10"), val specification: String? = null
)

data class SoResponse(
    val id: Long, val documentNo: String, val companyCode: String, val plantCode: String,
    val customerCode: String, val customerName: String, val orderDate: LocalDate,
    val deliveryDate: LocalDate?, val status: SoStatus, val currencyCode: String,
    val totalAmount: BigDecimal, val taxAmount: BigDecimal, val grandTotal: BigDecimal,
    val paymentTerms: String?, val shippingAddress: String?, val remark: String?,
    val lines: List<SoLineResponse>
)

data class SoLineResponse(
    val id: Long, val lineNo: Int, val itemCode: String, val itemName: String,
    val quantity: BigDecimal, val unitOfMeasure: String, val unitPrice: BigDecimal,
    val taxRate: BigDecimal, val totalPrice: BigDecimal, val taxAmount: BigDecimal,
    val shippedQuantity: BigDecimal, val openQuantity: BigDecimal, val specification: String?
)

fun SalesOrder.toResponse() = SoResponse(
    id = id, documentNo = documentNo, companyCode = companyCode, plantCode = plantCode,
    customerCode = customerCode, customerName = customerName, orderDate = orderDate,
    deliveryDate = deliveryDate, status = status, currencyCode = currencyCode,
    totalAmount = totalAmount, taxAmount = taxAmount, grandTotal = grandTotal,
    paymentTerms = paymentTerms, shippingAddress = shippingAddress, remark = remark,
    lines = lines.map {
        SoLineResponse(it.id, it.lineNo, it.itemCode, it.itemName, it.quantity, it.unitOfMeasure,
            it.unitPrice, it.taxRate, it.totalPrice, it.taxAmount, it.shippedQuantity, it.openQuantity, it.specification)
    }
)
