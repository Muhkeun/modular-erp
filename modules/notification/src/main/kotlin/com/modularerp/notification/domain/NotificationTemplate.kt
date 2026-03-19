package com.modularerp.notification.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

@Entity
@Table(name = "notification_templates")
class NotificationTemplate(

    @Column(nullable = false, length = 50)
    var templateCode: String,

    @Column(nullable = false, length = 200)
    var templateName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var channel: NotificationChannel,

    @Column(nullable = false, length = 50)
    var eventType: String,

    @Column(nullable = false, length = 500)
    var subject: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var body: String,

    var enabled: Boolean = true,

    @Column(nullable = false, length = 10)
    var language: String = "ko"

) : TenantEntity()

enum class NotificationChannel { IN_APP, EMAIL, SMS, PUSH }
