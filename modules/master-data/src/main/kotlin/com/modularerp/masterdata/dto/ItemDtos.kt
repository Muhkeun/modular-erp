package com.modularerp.masterdata.dto

import com.modularerp.masterdata.domain.Item
import com.modularerp.masterdata.domain.ItemType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateItemRequest(
    @field:NotBlank @field:Size(max = 50)
    val code: String,
    val itemType: ItemType = ItemType.MATERIAL,
    val itemGroup: String? = null,
    val unitOfMeasure: String = "EA",
    val specification: String? = null,
    val weight: BigDecimal? = null,
    val volume: BigDecimal? = null,
    val makerName: String? = null,
    val makerItemNo: String? = null,
    val qualityInspectionRequired: Boolean = false,
    val phantomBom: Boolean = false,
    val translations: List<TranslationInput> = emptyList()
)

data class UpdateItemRequest(
    val itemType: ItemType? = null,
    val itemGroup: String? = null,
    val unitOfMeasure: String? = null,
    val specification: String? = null,
    val weight: BigDecimal? = null,
    val volume: BigDecimal? = null,
    val makerName: String? = null,
    val makerItemNo: String? = null,
    val qualityInspectionRequired: Boolean? = null,
    val phantomBom: Boolean? = null,
    val translations: List<TranslationInput>? = null
)

data class TranslationInput(
    val locale: String,
    val name: String,
    val description: String? = null
)

data class ItemResponse(
    val id: Long,
    val code: String,
    val name: String,
    val itemType: ItemType,
    val itemGroup: String?,
    val unitOfMeasure: String,
    val specification: String?,
    val weight: BigDecimal?,
    val volume: BigDecimal?,
    val makerName: String?,
    val makerItemNo: String?,
    val qualityInspectionRequired: Boolean,
    val phantomBom: Boolean,
    val active: Boolean,
    val translations: List<TranslationOutput>
)

data class TranslationOutput(
    val locale: String,
    val name: String,
    val description: String?
)

fun Item.toResponse(locale: String = "ko") = ItemResponse(
    id = id,
    code = code,
    name = getName(locale),
    itemType = itemType,
    itemGroup = itemGroup,
    unitOfMeasure = unitOfMeasure,
    specification = specification,
    weight = weight,
    volume = volume,
    makerName = makerName,
    makerItemNo = makerItemNo,
    qualityInspectionRequired = qualityInspectionRequired,
    phantomBom = phantomBom,
    active = active,
    translations = translations.map { TranslationOutput(it.id.locale, it.name, it.description) }
)
