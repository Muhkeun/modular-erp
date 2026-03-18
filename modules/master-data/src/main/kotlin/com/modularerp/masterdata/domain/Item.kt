package com.modularerp.masterdata.domain

import com.modularerp.core.domain.TenantEntity
import com.modularerp.i18n.domain.TranslationId
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "items",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "code"])]
)
class Item(
    @Column(nullable = false, length = 50)
    val code: String,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var itemType: ItemType = ItemType.MATERIAL,

    @Column(length = 50)
    var itemGroup: String? = null,

    @Column(length = 10, nullable = false)
    var unitOfMeasure: String = "EA",

    @Column(length = 100)
    var specification: String? = null,

    @Column(precision = 12, scale = 4)
    var weight: BigDecimal? = null,

    @Column(precision = 12, scale = 4)
    var volume: BigDecimal? = null,

    @Column(length = 50)
    var makerName: String? = null,

    @Column(length = 50)
    var makerItemNo: String? = null,

    var qualityInspectionRequired: Boolean = false,

    var phantomBom: Boolean = false

) : TenantEntity() {

    @OneToMany(mappedBy = "item", cascade = [CascadeType.ALL], orphanRemoval = true)
    val translations: MutableSet<ItemTranslation> = mutableSetOf()

    fun addTranslation(locale: String, name: String, description: String? = null) {
        translations.removeIf { it.id.locale == locale }
        translations.add(ItemTranslation(
            id = TranslationId(entityId = this.id, locale = locale),
            name = name,
            description = description,
            item = this
        ))
    }

    fun getName(locale: String): String =
        translations.find { it.id.locale == locale }?.name
            ?: translations.firstOrNull()?.name
            ?: code
}

enum class ItemType {
    MATERIAL, PRODUCT, SEMI_PRODUCT, SERVICE, ASSET
}
