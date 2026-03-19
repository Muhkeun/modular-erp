package com.modularerp.notification.repository

import com.modularerp.notification.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Notification>

    @Query("""
        SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.active = true
        AND n.recipientId = :recipientId
        AND (:channel IS NULL OR n.channel = :channel)
        AND (:status IS NULL OR n.status = :status)
        ORDER BY n.createdAt DESC
    """)
    fun findByRecipient(tenantId: String, recipientId: String, channel: NotificationChannel?,
                        status: NotificationStatus?, pageable: Pageable): Page<Notification>

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.status IN ('PENDING','SENT') AND n.active = true")
    fun countUnread(tenantId: String, recipientId: String): Long

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.status IN ('PENDING','SENT')")
    fun markAllAsRead(tenantId: String, recipientId: String)
}

interface NotificationTemplateRepository : JpaRepository<NotificationTemplate, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<NotificationTemplate>
    fun findByTenantIdAndTemplateCode(tenantId: String, templateCode: String): Optional<NotificationTemplate>

    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND t.active = true ORDER BY t.templateCode")
    fun findAllActive(tenantId: String, pageable: Pageable): Page<NotificationTemplate>
}

interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, Long> {
    fun findByTenantIdAndUserId(tenantId: String, userId: String): List<NotificationPreference>
    fun findByTenantIdAndUserIdAndEventType(tenantId: String, userId: String, eventType: String): Optional<NotificationPreference>
}
