package com.modularerp.approval.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "approval_delegations")
class ApprovalDelegation(

    @Column(name = "from_user_id", nullable = false, length = 50)
    val fromUserId: String,

    @Column(name = "to_user_id", nullable = false, length = 50)
    val toUserId: String,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Column(name = "delegation_active", nullable = false)
    var delegationActive: Boolean = true,

    @Column(length = 500)
    var reason: String? = null

) : TenantEntity() {

    fun deactivateDelegation() {
        this.delegationActive = false
    }

    fun isEffective(date: LocalDate = LocalDate.now()): Boolean =
        delegationActive && !date.isBefore(startDate) && !date.isAfter(endDate)
}
