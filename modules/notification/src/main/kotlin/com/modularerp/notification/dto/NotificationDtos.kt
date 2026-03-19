package com.modularerp.notification.dto

import com.modularerp.notification.domain.*
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

// --- Notification ---

data class SendNotificationRequest(
    val channel: NotificationChannel = NotificationChannel.IN_APP,
    @field:NotBlank val recipientId: String,
    val recipientEmail: String? = null,
    @field:NotBlank val subject: String,
    @field:NotBlank val body: String,
    val referenceType: String? = null,
    val referenceId: Long? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

data class SendByTemplateRequest(
    @field:NotBlank val templateCode: String,
    @field:NotBlank val recipientId: String,
    val recipientEmail: String? = null,
    val variables: Map<String, String> = emptyMap(),
    val referenceType: String? = null,
    val referenceId: Long? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

data class NotificationResponse(
    val id: Long,
    val templateCode: String?,
    val channel: NotificationChannel,
    val recipientId: String,
    val recipientEmail: String?,
    val subject: String,
    val body: String,
    val status: NotificationStatus,
    val sentAt: LocalDateTime?,
    val readAt: LocalDateTime?,
    val referenceType: String?,
    val referenceId: Long?,
    val errorMessage: String?,
    val priority: NotificationPriority
)

fun Notification.toResponse() = NotificationResponse(
    id = id, templateCode = templateCode, channel = channel,
    recipientId = recipientId, recipientEmail = recipientEmail,
    subject = subject, body = body, status = status,
    sentAt = sentAt, readAt = readAt,
    referenceType = referenceType, referenceId = referenceId,
    errorMessage = errorMessage, priority = priority
)

// --- Template ---

data class CreateTemplateRequest(
    @field:NotBlank val templateCode: String,
    @field:NotBlank val templateName: String,
    val channel: NotificationChannel,
    @field:NotBlank val eventType: String,
    @field:NotBlank val subject: String,
    @field:NotBlank val body: String,
    val enabled: Boolean = true,
    val language: String = "ko"
)

data class UpdateTemplateRequest(
    val templateName: String? = null,
    val subject: String? = null,
    val body: String? = null,
    val enabled: Boolean? = null
)

data class TemplateResponse(
    val id: Long,
    val templateCode: String,
    val templateName: String,
    val channel: NotificationChannel,
    val eventType: String,
    val subject: String,
    val body: String,
    val enabled: Boolean,
    val language: String
)

fun NotificationTemplate.toResponse() = TemplateResponse(
    id = id, templateCode = templateCode, templateName = templateName,
    channel = channel, eventType = eventType, subject = subject,
    body = body, enabled = enabled, language = language
)

// --- Preference ---

data class UpdatePreferenceRequest(
    @field:NotBlank val eventType: String,
    val channelInApp: Boolean = true,
    val channelEmail: Boolean = false,
    val channelSms: Boolean = false,
    val channelPush: Boolean = false
)

data class PreferenceResponse(
    val id: Long,
    val userId: String,
    val eventType: String,
    val channelInApp: Boolean,
    val channelEmail: Boolean,
    val channelSms: Boolean,
    val channelPush: Boolean
)

fun NotificationPreference.toResponse() = PreferenceResponse(
    id = id, userId = userId, eventType = eventType,
    channelInApp = channelInApp, channelEmail = channelEmail,
    channelSms = channelSms, channelPush = channelPush
)
