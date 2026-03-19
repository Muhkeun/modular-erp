package com.modularerp.notification.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notifications")
class Notification(

    @Column(length = 50)
    var templateCode: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var channel: NotificationChannel,

    @Column(nullable = false, length = 100)
    var recipientId: String,

    @Column(length = 200)
    var recipientEmail: String? = null,

    @Column(nullable = false, length = 500)
    var subject: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var body: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: NotificationStatus = NotificationStatus.PENDING,

    var sentAt: LocalDateTime? = null,

    var readAt: LocalDateTime? = null,

    @Column(length = 50)
    var referenceType: String? = null,

    var referenceId: Long? = null,

    @Column(length = 2000)
    var errorMessage: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var priority: NotificationPriority = NotificationPriority.NORMAL

) : TenantEntity() {

    fun markAsRead() {
        check(status != NotificationStatus.FAILED) { "Cannot mark failed notification as read" }
        status = NotificationStatus.READ
        readAt = LocalDateTime.now()
    }

    fun markAsSent() {
        check(status == NotificationStatus.PENDING) { "Can only send PENDING notifications" }
        status = NotificationStatus.SENT
        sentAt = LocalDateTime.now()
    }

    fun markAsFailed(error: String) {
        status = NotificationStatus.FAILED
        errorMessage = error
    }
}

enum class NotificationStatus { PENDING, SENT, READ, FAILED }
enum class NotificationPriority { LOW, NORMAL, HIGH, URGENT }
