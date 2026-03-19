package com.modularerp.ai.service

import com.modularerp.ai.config.AiProperties
import com.modularerp.ai.domain.*
import com.modularerp.ai.dto.*
import com.modularerp.ai.repository.AiConversationRepository
import com.modularerp.ai.repository.AiMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Main AI chat service.
 * Orchestrates conversation management, context building, LLM calls, and response parsing.
 */
@Service
class AiChatService(
    private val aiProperties: AiProperties,
    private val conversationRepository: AiConversationRepository,
    private val messageRepository: AiMessageRepository,
    private val embeddingService: EmbeddingService,
    private val erpToolService: ErpToolService,
    private val aiQueryService: AiQueryService,
    private val aiReportService: AiReportService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun chat(
        userId: String,
        tenantId: String,
        sessionId: String?,
        message: String,
        userPermissions: List<String> = emptyList(),
        userRoles: List<String> = emptyList()
    ): AiChatResponse {
        if (!aiProperties.enabled) {
            throw AiDisabledException("AI features are not enabled")
        }

        if (aiProperties.apiKey.isBlank()) {
            // Use mock AI mode
            return handleMockMode(userId, tenantId, sessionId, message, userPermissions)
        }

        // Production mode with real LLM
        return handleLlmMode(userId, tenantId, sessionId, message, userPermissions, userRoles)
    }

    fun getConversations(userId: String, pageable: Pageable): Page<ConversationResponse> {
        return conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId, pageable)
            .map { it.toResponse() }
    }

    fun getMessages(sessionId: String): List<MessageResponse> {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .map { it.toResponse() }
    }

    @Transactional
    fun archiveConversation(sessionId: String, userId: String) {
        val conversation = conversationRepository.findBySessionId(sessionId)
            ?: throw IllegalArgumentException("Conversation not found: $sessionId")
        if (conversation.userId != userId) {
            throw SecurityException("Not authorized to archive this conversation")
        }
        conversation.archive()
        conversationRepository.save(conversation)
    }

    // ---- Private ----

    private fun handleMockMode(
        userId: String,
        tenantId: String,
        sessionId: String?,
        message: String,
        userPermissions: List<String>
    ): AiChatResponse {
        val conversation = getOrCreateConversation(userId, tenantId, sessionId)

        // Save user message
        val userMessage = AiMessage(role = MessageRole.USER, content = message)
        userMessage.assignTenant(tenantId)
        conversation.addMessage(userMessage)

        // Determine intent and generate mock response
        val response = generateMockResponse(message, tenantId, userPermissions)

        // Save assistant message
        val assistantMessage = AiMessage(
            role = MessageRole.ASSISTANT,
            content = response.message
        )
        assistantMessage.assignTenant(tenantId)
        conversation.addMessage(assistantMessage)

        conversationRepository.save(conversation)

        return response.copy(sessionId = conversation.sessionId)
    }

    private fun handleLlmMode(
        userId: String,
        tenantId: String,
        sessionId: String?,
        message: String,
        userPermissions: List<String>,
        userRoles: List<String>
    ): AiChatResponse {
        val conversation = getOrCreateConversation(userId, tenantId, sessionId)

        // Save user message
        val userMessage = AiMessage(role = MessageRole.USER, content = message)
        userMessage.assignTenant(tenantId)
        conversation.addMessage(userMessage)

        // Build context
        val systemPrompt = buildSystemPrompt(userId, tenantId, userPermissions, userRoles)
        val history = buildConversationHistory(conversation)

        // RAG: search for relevant documents
        val ragContext = if (aiProperties.rag.enabled) {
            val docs = embeddingService.searchSimilar(message, tenantId)
            if (docs.isNotEmpty()) {
                "\n\n--- 관련 참고 자료 ---\n" + docs.joinToString("\n---\n") { it.content }
            } else ""
        } else ""

        // Build full prompt for LLM
        val fullPrompt = """
            |$systemPrompt
            |$ragContext
            |
            |$history
            |User: $message
        """.trimMargin()

        // Call LLM via LangChain4j (placeholder - actual integration depends on runtime)
        val llmResponse = callLlm(fullPrompt)

        // Parse response for artifacts and actions
        val artifacts = mutableListOf<ArtifactInfo>()
        val suggestedActions = mutableListOf<SuggestedAction>()

        // Check if response indicates report generation
        if (llmResponse.contains("[REPORT:") || message.contains("보고서") || message.contains("report")) {
            try {
                val reportResult = aiReportService.generateReport(
                    userId, tenantId, message, userPermissions = userPermissions
                )
                artifacts.add(
                    ArtifactInfo(
                        type = reportResult.format,
                        filename = reportResult.filename,
                        downloadUrl = "/api/v1/ai/download/${reportResult.fileId}"
                    )
                )
            } catch (e: Exception) {
                log.warn("Report generation failed: {}", e.message)
            }
        }

        // Save assistant message
        val assistantMessage = AiMessage(
            role = MessageRole.ASSISTANT,
            content = llmResponse
        )
        assistantMessage.assignTenant(tenantId)
        conversation.addMessage(assistantMessage)

        conversationRepository.save(conversation)

        return AiChatResponse(
            sessionId = conversation.sessionId,
            message = llmResponse,
            artifacts = artifacts.ifEmpty { null },
            suggestedActions = suggestedActions.ifEmpty { null }
        )
    }

    private fun callLlm(prompt: String): String {
        // LangChain4j Anthropic integration
        // In production, this would use:
        // val model = AnthropicChatModel.builder()
        //     .apiKey(aiProperties.apiKey)
        //     .modelName(aiProperties.model)
        //     .maxTokens(aiProperties.maxTokens)
        //     .temperature(aiProperties.temperature)
        //     .build()
        // return model.generate(prompt)

        log.info("LLM call would be made with model={}, maxTokens={}", aiProperties.model, aiProperties.maxTokens)
        return "LLM 응답이 여기에 생성됩니다. API 키가 설정되면 실제 AI 응답을 제공합니다."
    }

    private fun generateMockResponse(
        message: String,
        tenantId: String,
        userPermissions: List<String>
    ): AiChatResponse {
        val lowerMessage = message.lowercase()

        val (responseText, artifacts, actions) = when {
            lowerMessage.containsAny("매출", "판매", "sales") -> Triple(
                "매출 관련 데이터를 조회합니다. 현재 MVP 모드에서는 실제 데이터 조회가 제한됩니다.\n\n" +
                    "AI가 활성화되면 다음과 같은 분석이 가능합니다:\n" +
                    "- 기간별 매출 추이 분석\n- 거래처별 매출 비교\n- 매출 예측",
                null,
                listOf(SuggestedAction("매출 보고서 생성", "report", mapOf("type" to "sales")))
            )
            lowerMessage.containsAny("재고", "stock", "inventory") -> Triple(
                "재고 현황을 조회합니다.\n\n" +
                    "현재 MVP 모드입니다. AI 모드에서는 다음이 가능합니다:\n" +
                    "- 재고 부족 품목 자동 알림\n- 적정 재고량 제안\n- 발주 자동 생성",
                null,
                listOf(
                    SuggestedAction("재고 현황 보고서", "report", mapOf("type" to "stock")),
                    SuggestedAction("부족 품목 조회", "query", mapOf("intent" to "STOCK_STATUS"))
                )
            )
            lowerMessage.containsAny("보고서", "report", "리포트") -> {
                try {
                    val result = aiReportService.generateReport(
                        "mock-user", tenantId, message, userPermissions = userPermissions
                    )
                    Triple(
                        result.summary,
                        listOf(ArtifactInfo(result.format, result.filename, "/api/v1/ai/download/${result.fileId}")),
                        null
                    )
                } catch (e: Exception) {
                    Triple("보고서 생성 중 오류가 발생했습니다: ${e.message}", null, null)
                }
            }
            lowerMessage.containsAny("도움", "help", "뭐 할 수 있") -> Triple(
                """
                |안녕하세요! ERP AI 어시스턴트입니다. 다음과 같은 도움을 드릴 수 있습니다:
                |
                |📊 **데이터 조회**
                |- "지난달 매출 보여줘"
                |- "재고 부족 품목 알려줘"
                |- "미결 발주 현황"
                |
                |📋 **보고서 생성**
                |- "월간 매출 보고서 만들어줘"
                |- "거래처별 미수금 현황 정리해줘"
                |
                |⚡ **업무 실행**
                |- "A품목 100개 구매요청 생성해줘"
                |- "B거래처 주문 상태 확인해줘"
                |
                |현재 MVP 모드로 동작 중입니다. AI API 키를 설정하면 더 정확한 응답을 제공합니다.
                """.trimMargin(),
                null,
                listOf(
                    SuggestedAction("매출 현황", "query", mapOf("intent" to "SALES_SUMMARY")),
                    SuggestedAction("재고 현황", "query", mapOf("intent" to "STOCK_STATUS")),
                    SuggestedAction("구매 현황", "query", mapOf("intent" to "PURCHASE_SUMMARY"))
                )
            )
            else -> Triple(
                "요청을 이해했습니다. 현재 MVP 모드에서는 제한된 패턴만 지원합니다.\n\n" +
                    "'도움말'을 입력하면 사용 가능한 기능을 확인할 수 있습니다.",
                null,
                listOf(SuggestedAction("도움말 보기", "chat", mapOf("message" to "도움")))
            )
        }

        return AiChatResponse(
            sessionId = "",
            message = responseText,
            artifacts = artifacts,
            suggestedActions = actions
        )
    }

    private fun getOrCreateConversation(userId: String, tenantId: String, sessionId: String?): AiConversation {
        if (sessionId != null) {
            val existing = conversationRepository.findBySessionId(sessionId)
            if (existing != null) return existing
        }

        val newSessionId = sessionId ?: UUID.randomUUID().toString()
        val conversation = AiConversation(
            sessionId = newSessionId,
            userId = userId
        )
        conversation.assignTenant(tenantId)
        return conversationRepository.save(conversation)
    }

    private fun buildSystemPrompt(
        userId: String,
        tenantId: String,
        permissions: List<String>,
        roles: List<String>
    ): String {
        val toolDefs = erpToolService.getToolDefinitions(permissions)
        return """
            |You are an ERP AI assistant for a Korean manufacturing/distribution company.
            |Respond in Korean unless the user writes in English.
            |
            |Current context:
            |- User: $userId
            |- Tenant: $tenantId
            |- Roles: ${roles.joinToString(", ")}
            |- Permissions: ${permissions.joinToString(", ")}
            |
            |Available ERP modules: Master Data, Sales, Purchase, Logistics, Production,
            |Accounting, HR, Quality, Planning, Budget, Asset, CRM
            |
            |Available tools:
            |$toolDefs
            |
            |Instructions:
            |1. When the user asks for data, use the appropriate tool.
            |2. When the user asks for a report, generate structured data and offer to create Excel/PDF.
            |3. For write operations (create/update), confirm with the user before executing.
            |4. Never expose internal SQL, API errors, or system details.
            |5. Respect user permissions - do not return data the user cannot access.
            |6. For structured responses (tool calls), use JSON format.
        """.trimMargin()
    }

    private fun buildConversationHistory(conversation: AiConversation): String {
        val recentMessages = conversation.messages.takeLast(20)
        return recentMessages.joinToString("\n") { msg ->
            "${msg.role.name}: ${msg.content}"
        }
    }

    private fun AiConversation.toResponse() = ConversationResponse(
        id = id,
        sessionId = sessionId,
        title = title,
        status = status,
        messageCount = messageCount,
        lastMessageAt = lastMessageAt,
        createdAt = createdAt
    )

    private fun AiMessage.toResponse() = MessageResponse(
        id = id,
        role = role,
        content = content,
        tokenCount = tokenCount,
        metadata = metadata,
        messageCreatedAt = messageCreatedAt
    )

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}

class AiDisabledException(message: String) : RuntimeException(message)
