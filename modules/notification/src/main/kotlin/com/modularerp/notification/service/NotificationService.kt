package com.modularerp.notification.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.notification.domain.*
import com.modularerp.notification.dto.*
import com.modularerp.notification.repository.*
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val preferenceRepository: NotificationPreferenceRepository
) {

    @Transactional
    fun sendNotification(request: SendNotificationRequest): NotificationResponse {
        val tenantId = TenantContext.getTenantId()
        val notification = Notification(
            channel = request.channel, recipientId = request.recipientId,
            recipientEmail = request.recipientEmail, subject = request.subject,
            body = request.body, referenceType = request.referenceType,
            referenceId = request.referenceId, priority = request.priority
        ).apply { assignTenant(tenantId) }

        // For IN_APP, mark as sent immediately
        if (request.channel == NotificationChannel.IN_APP) {
            notification.markAsSent()
        }

        return notificationRepository.save(notification).toResponse()
    }

    @Transactional
    fun sendByTemplate(request: SendByTemplateRequest): NotificationResponse {
        val tenantId = TenantContext.getTenantId()
        val template = templateRepository.findByTenantIdAndTemplateCode(tenantId, request.templateCode)
            .orElseThrow { EntityNotFoundException("NotificationTemplate", request.templateCode) }

        check(template.enabled) { "Template is disabled" }

        val resolvedSubject = processTemplate(template.subject, request.variables)
        val resolvedBody = processTemplate(template.body, request.variables)

        val notification = Notification(
            templateCode = template.templateCode, channel = template.channel,
            recipientId = request.recipientId, recipientEmail = request.recipientEmail,
            subject = resolvedSubject, body = resolvedBody,
            referenceType = request.referenceType, referenceId = request.referenceId,
            priority = request.priority
        ).apply { assignTenant(tenantId) }

        if (template.channel == NotificationChannel.IN_APP) {
            notification.markAsSent()
        }

        return notificationRepository.save(notification).toResponse()
    }

    @Transactional
    fun markAsRead(id: Long): NotificationResponse {
        val notification = findNotification(id)
        notification.markAsRead()
        return notificationRepository.save(notification).toResponse()
    }

    @Transactional
    fun markAllAsRead(recipientId: String) {
        notificationRepository.markAllAsRead(TenantContext.getTenantId(), recipientId)
    }

    fun getUnreadCount(recipientId: String): Long =
        notificationRepository.countUnread(TenantContext.getTenantId(), recipientId)

    fun getUserNotifications(recipientId: String, channel: NotificationChannel?,
                             status: NotificationStatus?, pageable: Pageable): Page<NotificationResponse> =
        notificationRepository.findByRecipient(TenantContext.getTenantId(), recipientId, channel, status, pageable)
            .map { it.toResponse() }

    @Transactional
    fun deleteNotification(id: Long) {
        val notification = findNotification(id)
        notification.deactivate()
        notificationRepository.save(notification)
    }

    // --- Template management ---

    fun getTemplates(pageable: Pageable): Page<TemplateResponse> =
        templateRepository.findAllActive(TenantContext.getTenantId(), pageable).map { it.toResponse() }

    @Transactional
    fun createTemplate(request: CreateTemplateRequest): TemplateResponse {
        val tenantId = TenantContext.getTenantId()
        val template = NotificationTemplate(
            templateCode = request.templateCode, templateName = request.templateName,
            channel = request.channel, eventType = request.eventType,
            subject = request.subject, body = request.body,
            enabled = request.enabled, language = request.language
        ).apply { assignTenant(tenantId) }
        return templateRepository.save(template).toResponse()
    }

    @Transactional
    fun updateTemplate(id: Long, request: UpdateTemplateRequest): TemplateResponse {
        val template = findTemplate(id)
        request.templateName?.let { template.templateName = it }
        request.subject?.let { template.subject = it }
        request.body?.let { template.body = it }
        request.enabled?.let { template.enabled = it }
        return templateRepository.save(template).toResponse()
    }

    // --- Preference management ---

    fun getPreferences(userId: String): List<PreferenceResponse> =
        preferenceRepository.findByTenantIdAndUserId(TenantContext.getTenantId(), userId)
            .map { it.toResponse() }

    @Transactional
    fun updatePreference(userId: String, request: UpdatePreferenceRequest): PreferenceResponse {
        val tenantId = TenantContext.getTenantId()
        val pref = preferenceRepository.findByTenantIdAndUserIdAndEventType(tenantId, userId, request.eventType)
            .orElseGet {
                NotificationPreference(userId = userId, eventType = request.eventType)
                    .apply { assignTenant(tenantId) }
            }
        pref.channelInApp = request.channelInApp
        pref.channelEmail = request.channelEmail
        pref.channelSms = request.channelSms
        pref.channelPush = request.channelPush
        return preferenceRepository.save(pref).toResponse()
    }

    fun processTemplate(template: String, variables: Map<String, String>): String {
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace("\${$key}", value)
        }
        return result
    }

    private fun findNotification(id: Long): Notification =
        notificationRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("Notification", id) }

    private fun findTemplate(id: Long): NotificationTemplate =
        templateRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("NotificationTemplate", id) }
}
