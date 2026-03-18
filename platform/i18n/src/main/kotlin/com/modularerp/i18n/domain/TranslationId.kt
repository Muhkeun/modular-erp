package com.modularerp.i18n.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class TranslationId(
    @Column(name = "entity_id")
    val entityId: Long = 0,

    @Column(name = "locale", length = 5)
    val locale: String = "ko"
) : Serializable
