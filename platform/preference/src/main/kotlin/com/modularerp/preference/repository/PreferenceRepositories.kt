package com.modularerp.preference.repository

import com.modularerp.preference.domain.PreferenceCategory
import com.modularerp.preference.domain.UserGridPreference
import com.modularerp.preference.domain.UserPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserPreferenceRepository : JpaRepository<UserPreference, Long> {

    fun findByUserIdAndCategory(userId: String, category: PreferenceCategory): List<UserPreference>

    fun findByUserIdAndCategoryAndKey(userId: String, category: PreferenceCategory, key: String): UserPreference?

    fun findAllByUserId(userId: String): List<UserPreference>

    fun deleteByUserIdAndCategoryAndKey(userId: String, category: PreferenceCategory, key: String)
}

@Repository
interface UserGridPreferenceRepository : JpaRepository<UserGridPreference, Long> {

    fun findByUserIdAndGridId(userId: String, gridId: String): UserGridPreference?

    fun findAllByUserId(userId: String): List<UserGridPreference>

    fun deleteByUserIdAndGridId(userId: String, gridId: String)
}
