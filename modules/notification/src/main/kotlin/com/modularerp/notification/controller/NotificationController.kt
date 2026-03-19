package com.modularerp.notification.controller

import com.modularerp.notification.domain.*
import com.modularerp.notification.dto.*
import com.modularerp.notification.service.NotificationService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
class NotificationController(private val notificationService: NotificationService) {

    @GetMapping
    fun getUserNotifications(
        @RequestParam recipientId: String,
        @RequestParam(required = false) channel: NotificationChannel?,
        @RequestParam(required = false) status: NotificationStatus?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<NotificationResponse>> {
        val page = notificationService.getUserNotifications(recipientId, channel, status, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(@RequestParam recipientId: String) =
        ApiResponse.ok(mapOf("count" to notificationService.getUnreadCount(recipientId)))

    @PutMapping("/{id}/read")
    fun markAsRead(@PathVariable id: Long) = ApiResponse.ok(notificationService.markAsRead(id))

    @PutMapping("/read-all")
    fun markAllAsRead(@RequestParam recipientId: String): ApiResponse<Any> {
        notificationService.markAllAsRead(recipientId)
        return ApiResponse.ok("All notifications marked as read")
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ApiResponse<Any> {
        notificationService.deleteNotification(id)
        return ApiResponse.ok("Notification deleted")
    }

    // --- Templates ---

    @GetMapping("/templates")
    fun getTemplates(@PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<TemplateResponse>> {
        val page = notificationService.getTemplates(pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTemplate(@Valid @RequestBody req: CreateTemplateRequest) =
        ApiResponse.ok(notificationService.createTemplate(req))

    @PutMapping("/templates/{id}")
    fun updateTemplate(@PathVariable id: Long, @Valid @RequestBody req: UpdateTemplateRequest) =
        ApiResponse.ok(notificationService.updateTemplate(id, req))

    // --- Preferences ---

    @GetMapping("/preferences")
    fun getPreferences(@RequestParam userId: String) =
        ApiResponse.ok(notificationService.getPreferences(userId))

    @PutMapping("/preferences")
    fun updatePreference(@RequestParam userId: String, @Valid @RequestBody req: UpdatePreferenceRequest) =
        ApiResponse.ok(notificationService.updatePreference(userId, req))
}
