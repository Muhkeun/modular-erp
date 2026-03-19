package com.modularerp.preference.dto

import com.modularerp.preference.domain.PreferenceCategory
import com.modularerp.preference.domain.UserGridPreference
import com.modularerp.preference.domain.UserPreference
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// ── UserPreference DTOs ──

data class PreferenceRequest(
    val category: PreferenceCategory,
    @field:NotBlank val key: String,
    @field:NotBlank @field:Size(max = 2000) val value: String
)

data class PreferenceBatchRequest(
    val preferences: List<PreferenceRequest>
)

data class PreferenceResponse(
    val id: Long,
    val category: PreferenceCategory,
    val key: String,
    val value: String
) {
    companion object {
        fun from(entity: UserPreference) = PreferenceResponse(
            id = entity.id,
            category = entity.category,
            key = entity.key,
            value = entity.value
        )
    }
}

// ── UserGridPreference DTOs ──

data class GridPreferenceRequest(
    @field:NotBlank val gridId: String,
    val columnState: String? = null,
    val sortModel: String? = null,
    val filterModel: String? = null,
    val pageSize: Int? = null
)

data class GridPreferenceResponse(
    val id: Long,
    val gridId: String,
    val columnState: String?,
    val sortModel: String?,
    val filterModel: String?,
    val pageSize: Int
) {
    companion object {
        fun from(entity: UserGridPreference) = GridPreferenceResponse(
            id = entity.id,
            gridId = entity.gridId,
            columnState = entity.columnState,
            sortModel = entity.sortModel,
            filterModel = entity.filterModel,
            pageSize = entity.pageSize
        )
    }
}
