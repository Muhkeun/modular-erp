package com.modularerp.preference.domain

import com.modularerp.core.domain.BaseEntity
import jakarta.persistence.*

/**
 * 사용자 개인 설정 (key-value 방식).
 * 테마, 날짜 포맷, 숫자 포맷, 기본 회사/사업장 등 저장.
 */
@Entity
@Table(
    name = "user_preferences",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "category", "preference_key"])]
)
class UserPreference(

    @Column(name = "user_id", nullable = false, length = 50)
    val userId: String,

    @Column(name = "tenant_id", nullable = false, length = 50)
    val tenantId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val category: PreferenceCategory,

    @Column(name = "preference_key", nullable = false, length = 100)
    val key: String,

    @Column(name = "preference_value", nullable = false, length = 4000)
    var value: String

) : BaseEntity() {

    fun updateValue(newValue: String) {
        this.value = newValue
    }
}

enum class PreferenceCategory {
    DISPLAY,    // 테마, 다크모드, 사이드바 상태
    FORMAT,     // 날짜, 숫자, 통화 포맷
    DEFAULT,    // 기본 회사, 사업장, 언어
    DASHBOARD,  // 대시보드 위젯 배치
    GRID        // 그리드 글로벌 설정 (페이지 사이즈 등)
}
