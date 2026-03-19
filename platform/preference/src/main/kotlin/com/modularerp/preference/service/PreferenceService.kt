package com.modularerp.preference.service

import com.modularerp.preference.domain.PreferenceCategory
import com.modularerp.preference.domain.UserGridPreference
import com.modularerp.preference.domain.UserPreference
import com.modularerp.preference.dto.*
import com.modularerp.preference.repository.UserGridPreferenceRepository
import com.modularerp.preference.repository.UserPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PreferenceService(
    private val preferenceRepo: UserPreferenceRepository,
    private val gridPreferenceRepo: UserGridPreferenceRepository
) {

    // ── UserPreference ──

    fun getAllPreferences(userId: String): List<PreferenceResponse> =
        preferenceRepo.findAllByUserId(userId).map(PreferenceResponse::from)

    fun getPreferencesByCategory(userId: String, category: PreferenceCategory): List<PreferenceResponse> =
        preferenceRepo.findByUserIdAndCategory(userId, category).map(PreferenceResponse::from)

    @Transactional
    fun savePreference(userId: String, tenantId: String, request: PreferenceRequest): PreferenceResponse {
        val existing = preferenceRepo.findByUserIdAndCategoryAndKey(userId, request.category, request.key)
        val entity = if (existing != null) {
            existing.updateValue(request.value)
            existing
        } else {
            UserPreference(
                userId = userId,
                tenantId = tenantId,
                category = request.category,
                key = request.key,
                value = request.value
            )
        }
        return PreferenceResponse.from(preferenceRepo.save(entity))
    }

    @Transactional
    fun saveBatch(userId: String, tenantId: String, request: PreferenceBatchRequest): List<PreferenceResponse> =
        request.preferences.map { savePreference(userId, tenantId, it) }

    @Transactional
    fun deletePreference(userId: String, category: PreferenceCategory, key: String) {
        preferenceRepo.deleteByUserIdAndCategoryAndKey(userId, category, key)
    }

    // ── GridPreference ──

    fun getAllGridPreferences(userId: String): List<GridPreferenceResponse> =
        gridPreferenceRepo.findAllByUserId(userId).map(GridPreferenceResponse::from)

    fun getGridPreference(userId: String, gridId: String): GridPreferenceResponse? =
        gridPreferenceRepo.findByUserIdAndGridId(userId, gridId)?.let(GridPreferenceResponse::from)

    @Transactional
    fun saveGridPreference(userId: String, tenantId: String, request: GridPreferenceRequest): GridPreferenceResponse {
        val existing = gridPreferenceRepo.findByUserIdAndGridId(userId, request.gridId)
        val entity = if (existing != null) {
            existing.update(request.columnState, request.sortModel, request.filterModel, request.pageSize)
            existing
        } else {
            UserGridPreference(
                userId = userId,
                tenantId = tenantId,
                gridId = request.gridId,
                columnState = request.columnState,
                sortModel = request.sortModel,
                filterModel = request.filterModel,
                pageSize = request.pageSize ?: 20
            )
        }
        return GridPreferenceResponse.from(gridPreferenceRepo.save(entity))
    }

    @Transactional
    fun deleteGridPreference(userId: String, gridId: String) {
        gridPreferenceRepo.deleteByUserIdAndGridId(userId, gridId)
    }
}
