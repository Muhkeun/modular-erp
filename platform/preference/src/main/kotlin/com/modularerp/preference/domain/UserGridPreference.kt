package com.modularerp.preference.domain

import com.modularerp.core.domain.BaseEntity
import jakarta.persistence.*

/**
 * 사용자별 그리드 설정.
 * 화면(gridId)별로 컬럼 순서, 너비, 표시 여부, 정렬, 필터 상태를 저장.
 */
@Entity
@Table(
    name = "user_grid_preferences",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "grid_id"])]
)
class UserGridPreference(

    @Column(name = "user_id", nullable = false, length = 50)
    val userId: String,

    @Column(name = "tenant_id", nullable = false, length = 50)
    val tenantId: String,

    @Column(name = "grid_id", nullable = false, length = 100)
    val gridId: String,

    @Column(name = "column_state", columnDefinition = "TEXT")
    var columnState: String? = null,

    @Column(name = "sort_model", columnDefinition = "TEXT")
    var sortModel: String? = null,

    @Column(name = "filter_model", columnDefinition = "TEXT")
    var filterModel: String? = null,

    @Column(name = "page_size")
    var pageSize: Int = 20

) : BaseEntity() {

    fun update(columnState: String?, sortModel: String?, filterModel: String?, pageSize: Int?) {
        columnState?.let { this.columnState = it }
        sortModel?.let { this.sortModel = it }
        filterModel?.let { this.filterModel = it }
        pageSize?.let { this.pageSize = it }
    }
}
