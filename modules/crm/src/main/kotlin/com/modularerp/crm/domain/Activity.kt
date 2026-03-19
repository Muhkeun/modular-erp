package com.modularerp.crm.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "crm_activities")
class Activity(

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var activityType: ActivityType = ActivityType.NOTE,

    @Column(nullable = false, length = 200)
    var subject: String = "",

    @Column(length = 1000)
    var description: String? = null,

    @Column(nullable = false)
    var activityDate: LocalDateTime = LocalDateTime.now(),

    var dueDate: LocalDateTime? = null,

    @Column(nullable = false)
    var completed: Boolean = false,

    var completedAt: LocalDateTime? = null,

    @Column(length = 30)
    var referenceType: String? = null,

    var referenceId: Long? = null,

    @Column(length = 100)
    var assignedTo: String? = null

) : TenantEntity() {

    fun complete() {
        completed = true
        completedAt = LocalDateTime.now()
    }
}

enum class ActivityType { CALL, EMAIL, MEETING, NOTE, TASK }
