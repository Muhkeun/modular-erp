package com.modularerp.masterdata.domain

import com.modularerp.i18n.domain.TranslationId
import jakarta.persistence.*

@Entity
@Table(name = "item_translations")
class ItemTranslation(
    @EmbeddedId
    val id: TranslationId,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", insertable = false, updatable = false)
    val item: Item? = null
)
