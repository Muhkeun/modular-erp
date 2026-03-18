package com.modularerp.masterdata.dto

import com.modularerp.masterdata.domain.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.LocalDate

data class CreateBomRequest(
    @field:NotBlank val productCode: String,
    @field:NotBlank val productName: String,
    @field:NotBlank val plantCode: String,
    val revision: String = "001",
    val baseQuantity: BigDecimal = BigDecimal.ONE,
    val baseUnit: String = "EA",
    val validFrom: LocalDate = LocalDate.now(),
    val validTo: LocalDate? = null,
    val description: String? = null,
    @field:NotEmpty val components: List<BomComponentInput>
)

data class BomComponentInput(
    val itemCode: String,
    val itemName: String,
    val quantity: BigDecimal,
    val unitOfMeasure: String = "EA",
    val scrapRate: BigDecimal = BigDecimal.ZERO,
    val phantom: Boolean = false,
    val sortOrder: Int? = null,
    val operationNo: Int? = null,
    val remark: String? = null
)

data class BomResponse(
    val id: Long, val productCode: String, val productName: String,
    val plantCode: String, val revision: String, val status: BomStatus,
    val baseQuantity: BigDecimal, val baseUnit: String,
    val validFrom: LocalDate, val validTo: LocalDate?,
    val description: String?, val components: List<BomComponentResponse>
)

data class BomComponentResponse(
    val id: Long, val itemCode: String, val itemName: String,
    val quantity: BigDecimal, val grossQuantity: BigDecimal,
    val unitOfMeasure: String, val scrapRate: BigDecimal,
    val phantom: Boolean, val sortOrder: Int, val operationNo: Int?, val remark: String?
)

fun BomHeader.toResponse() = BomResponse(
    id = id, productCode = productCode, productName = productName,
    plantCode = plantCode, revision = revision, status = status,
    baseQuantity = baseQuantity, baseUnit = baseUnit,
    validFrom = validFrom, validTo = validTo, description = description,
    components = components.map {
        BomComponentResponse(it.id, it.itemCode, it.itemName, it.quantity, it.grossQuantity,
            it.unitOfMeasure, it.scrapRate, it.phantom, it.sortOrder, it.operationNo, it.remark)
    }
)
