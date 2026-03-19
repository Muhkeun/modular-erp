package com.modularerp.ai.dto

import com.modularerp.ai.domain.ConversationStatus
import com.modularerp.ai.domain.MessageRole
import com.modularerp.ai.domain.QueryIntent
import java.time.LocalDateTime

// === Request DTOs ===

data class AiChatRequest(
    val sessionId: String? = null,
    val message: String
)

data class AiQueryRequest(
    val query: String
)

data class AiReportRequest(
    val description: String,
    val format: String = "excel" // excel, pdf
)

// === Response DTOs ===

data class AiChatResponse(
    val sessionId: String,
    val message: String,
    val artifacts: List<ArtifactInfo>? = null,
    val suggestedActions: List<SuggestedAction>? = null
)

data class ArtifactInfo(
    val type: String, // excel, pdf
    val filename: String,
    val downloadUrl: String
)

data class SuggestedAction(
    val label: String,
    val action: String,
    val params: Map<String, Any>? = null
)

data class ConversationResponse(
    val id: Long,
    val sessionId: String,
    val title: String?,
    val status: ConversationStatus,
    val messageCount: Int,
    val lastMessageAt: LocalDateTime?,
    val createdAt: java.time.Instant
)

data class MessageResponse(
    val id: Long,
    val role: MessageRole,
    val content: String,
    val tokenCount: Int?,
    val metadata: String?,
    val messageCreatedAt: LocalDateTime
)

data class QueryResult(
    val intent: QueryIntent,
    val description: String,
    val data: List<Map<String, Any?>>? = null,
    val summary: String? = null,
    val error: String? = null
)

data class ReportResult(
    val filename: String,
    val format: String,
    val fileId: String,
    val fileBytes: ByteArray,
    val summary: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReportResult) return false
        return fileId == other.fileId
    }

    override fun hashCode(): Int = fileId.hashCode()
}

// === WebSocket DTOs ===

data class WsAiMessage(
    val type: WsMessageType,
    val content: String
)

enum class WsMessageType {
    MESSAGE, THINKING, TOOL_CALL, ARTIFACT, DONE
}

// === Tool DTOs ===

data class ErpTool(
    val name: String,
    val description: String,
    val parameters: Map<String, ToolParameter>,
    val requiresPermission: String
)

data class ToolParameter(
    val type: String,
    val description: String,
    val required: Boolean = false,
    val enumValues: List<String>? = null
)
