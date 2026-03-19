package com.modularerp.notification.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

@Entity
@Table(name = "notification_preferences")
class NotificationPreference(

    @Column(nullable = false, length = 100)
    var userId: String,

    @Column(nullable = false, length = 50)
    var eventType: String,

    var channelInApp: Boolean = true,

    var channelEmail: Boolean = false,

    var channelSms: Boolean = false,

    var channelPush: Boolean = false

) : TenantEntity()
