package com.modularerp.ai.websocket

import com.modularerp.ai.config.AiProperties
import com.modularerp.ai.dto.WsAiMessage
import com.modularerp.ai.dto.WsMessageType
import com.modularerp.ai.service.AiChatService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Controller
import java.security.Principal

/**
 * WebSocket handler for streaming AI responses.
 * Uses STOMP messaging over SockJS for real-time token-by-token streaming.
 */
@Controller
@ConditionalOnProperty(prefix = "modular-erp.ai", name = ["enabled"], havingValue = "true")
class AiWebSocketHandler(
    private val aiProperties: AiProperties,
    private val aiChatService: AiChatService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/ai/chat")
    fun handleChatMessage(@Payload payload: Map<String, String>, principal: Principal?) {
        val userId = principal?.name ?: "anonymous"
        val sessionId = payload["sessionId"]
        val message = payload["message"] ?: return

        if (!aiProperties.enabled) {
            sendToUser(userId, sessionId, WsAiMessage(WsMessageType.MESSAGE, "AI features are not enabled"))
            sendToUser(userId, sessionId, WsAiMessage(WsMessageType.DONE, ""))
            return
        }

        try {
            // Send thinking indicator
            sendToUser(userId, sessionId, WsAiMessage(WsMessageType.THINKING, "Processing..."))

            // Get response (in production, this would stream tokens)
            val response = aiChatService.chat(
                userId = userId,
                tenantId = "default",
                sessionId = sessionId,
                message = message
            )

            // Send response
            sendToUser(userId, sessionId, WsAiMessage(WsMessageType.MESSAGE, response.message))

            // Send artifacts if any
            response.artifacts?.forEach { artifact ->
                sendToUser(userId, sessionId, WsAiMessage(
                    WsMessageType.ARTIFACT,
                    """{"type":"${artifact.type}","filename":"${artifact.filename}","downloadUrl":"${artifact.downloadUrl}"}"""
                ))
            }

            // Send done signal
            sendToUser(userId, sessionId, WsAiMessage(WsMessageType.DONE, ""))

        } catch (e: Exception) {
            log.error("WebSocket AI chat error", e)
            sendToUser(userId, sessionId, WsAiMessage(WsMessageType.MESSAGE, "오류가 발생했습니다: ${e.message}"))
            sendToUser(userId, sessionId, WsAiMessage(WsMessageType.DONE, ""))
        }
    }

    private fun sendToUser(userId: String, sessionId: String?, message: WsAiMessage) {
        val destination = if (sessionId != null) "/topic/ai/$sessionId" else "/topic/ai/$userId"
        messagingTemplate.convertAndSend(destination, message)
    }
}
