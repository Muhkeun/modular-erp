package com.modularerp.ai.controller

import com.modularerp.ai.config.AiProperties
import com.modularerp.ai.dto.*
import com.modularerp.ai.service.AiChatService
import com.modularerp.ai.service.AiDisabledException
import com.modularerp.ai.service.AiQueryService
import com.modularerp.ai.service.AiReportService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.*
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI", description = "AI/LLM chat, query, and report generation")
class AiController(
    private val aiProperties: AiProperties,
    private val aiChatService: AiChatService,
    private val aiQueryService: AiQueryService,
    private val aiReportService: AiReportService
) {

    @PostMapping("/chat")
    @Operation(summary = "Send a chat message to AI assistant")
    fun chat(
        @RequestBody request: AiChatRequest,
        authentication: Authentication
    ): ApiResponse<AiChatResponse> {
        checkEnabled()
        val response = aiChatService.chat(
            userId = authentication.name,
            tenantId = extractTenantId(authentication),
            sessionId = request.sessionId,
            message = request.message,
            userPermissions = extractPermissions(authentication),
            userRoles = extractRoles(authentication)
        )
        return ApiResponse.ok(response)
    }

    @GetMapping("/conversations")
    @Operation(summary = "List user's conversations")
    fun listConversations(
        @PageableDefault(size = 20) pageable: Pageable,
        authentication: Authentication
    ): ApiResponse<List<ConversationResponse>> {
        checkEnabled()
        val page = aiChatService.getConversations(authentication.name, pageable)
        return ApiResponse.ok(
            page.content,
            PageMeta(page.number, page.size, page.totalElements, page.totalPages)
        )
    }

    @GetMapping("/conversations/{sessionId}/messages")
    @Operation(summary = "Get conversation history")
    fun getMessages(
        @PathVariable sessionId: String
    ): ApiResponse<List<MessageResponse>> {
        checkEnabled()
        return ApiResponse.ok(aiChatService.getMessages(sessionId))
    }

    @DeleteMapping("/conversations/{sessionId}")
    @Operation(summary = "Archive a conversation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun archiveConversation(
        @PathVariable sessionId: String,
        authentication: Authentication
    ) {
        checkEnabled()
        aiChatService.archiveConversation(sessionId, authentication.name)
    }

    @PostMapping("/query")
    @Operation(summary = "Execute a natural language query (no conversation context)")
    fun query(
        @RequestBody request: AiQueryRequest,
        authentication: Authentication
    ): ApiResponse<QueryResult> {
        checkEnabled()
        val result = aiQueryService.parseQuery(
            naturalLanguageQuery = request.query,
            tenantId = extractTenantId(authentication),
            userPermissions = extractPermissions(authentication)
        )
        return ApiResponse.ok(result)
    }

    @PostMapping("/report")
    @Operation(summary = "Generate a report from natural language description")
    fun generateReport(
        @RequestBody request: AiReportRequest,
        authentication: Authentication
    ): ApiResponse<Map<String, Any>> {
        checkEnabled()
        val result = aiReportService.generateReport(
            userId = authentication.name,
            tenantId = extractTenantId(authentication),
            request = request.description,
            format = request.format,
            userPermissions = extractPermissions(authentication)
        )
        return ApiResponse.ok(
            mapOf(
                "fileId" to result.fileId,
                "filename" to result.filename,
                "format" to result.format,
                "downloadUrl" to "/api/v1/ai/download/${result.fileId}",
                "summary" to result.summary
            )
        )
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "Download a generated file")
    fun downloadFile(@PathVariable fileId: String): ResponseEntity<ByteArray> {
        val file = aiReportService.getFile(fileId)
            ?: return ResponseEntity.notFound().build()

        val (bytes, filename) = file
        val contentType = if (filename.endsWith(".pdf"))
            MediaType.APPLICATION_PDF
        else
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .body(bytes)
    }

    // ---- Helpers ----

    private fun checkEnabled() {
        if (!aiProperties.enabled) {
            throw AiDisabledException("AI features are not enabled")
        }
    }

    private fun extractTenantId(auth: Authentication): String {
        // Extract from security context - implementation depends on auth setup
        return "default"
    }

    private fun extractPermissions(auth: Authentication): List<String> {
        return auth.authorities?.map { it.authority } ?: emptyList()
    }

    private fun extractRoles(auth: Authentication): List<String> {
        return auth.authorities
            ?.map { it.authority }
            ?.filter { it.startsWith("ROLE_") }
            ?: emptyList()
    }

    @ExceptionHandler(AiDisabledException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleAiDisabled(ex: AiDisabledException): ApiResponse<Nothing> {
        return ApiResponse.error("AI_DISABLED", ex.message ?: "AI features are not enabled")
    }
}
