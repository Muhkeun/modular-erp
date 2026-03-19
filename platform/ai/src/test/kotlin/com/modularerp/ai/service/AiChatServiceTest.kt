package com.modularerp.ai.service

import com.modularerp.ai.config.AiProperties
import com.modularerp.ai.config.EmbeddingProperties
import com.modularerp.ai.config.RagProperties
import com.modularerp.ai.domain.AiConversation
import com.modularerp.ai.domain.ConversationStatus
import com.modularerp.ai.repository.AiConversationRepository
import com.modularerp.ai.repository.AiMessageRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class AiChatServiceTest {

    private lateinit var aiProperties: AiProperties
    private lateinit var conversationRepository: AiConversationRepository
    private lateinit var messageRepository: AiMessageRepository
    private lateinit var embeddingService: EmbeddingService
    private lateinit var erpToolService: ErpToolService
    private lateinit var aiQueryService: AiQueryService
    private lateinit var aiReportService: AiReportService
    private lateinit var chatService: AiChatService

    @BeforeEach
    fun setup() {
        aiProperties = AiProperties(
            enabled = true,
            apiKey = "",  // mock mode
            embedding = EmbeddingProperties(),
            rag = RagProperties()
        )
        conversationRepository = mock()
        messageRepository = mock()
        embeddingService = mock()
        erpToolService = ErpToolService()
        aiQueryService = AiQueryService(aiProperties, erpToolService)
        aiReportService = mock()

        chatService = AiChatService(
            aiProperties, conversationRepository, messageRepository,
            embeddingService, erpToolService, aiQueryService, aiReportService
        )
    }

    @Test
    fun `chat throws when AI is disabled`() {
        val disabledProps = AiProperties(enabled = false)
        val service = AiChatService(
            disabledProps, conversationRepository, messageRepository,
            embeddingService, erpToolService, aiQueryService, aiReportService
        )

        assertThrows<AiDisabledException> {
            service.chat("user1", "tenant1", null, "hello")
        }
    }

    @Test
    fun `chat creates conversation and saves messages in mock mode`() {
        val savedConversation = AiConversation(sessionId = "test-session", userId = "user1")
        savedConversation.assignTenant("tenant1")

        whenever(conversationRepository.save(any<AiConversation>())).thenReturn(savedConversation)

        val response = chatService.chat("user1", "tenant1", null, "도움말")

        assertNotNull(response)
        assertTrue(response.message.contains("ERP AI 어시스턴트"))
        verify(conversationRepository, atLeastOnce()).save(any())
    }

    @Test
    fun `chat resumes existing conversation`() {
        val existing = AiConversation(sessionId = "existing-session", userId = "user1")
        existing.assignTenant("tenant1")

        whenever(conversationRepository.findBySessionId("existing-session")).thenReturn(existing)
        whenever(conversationRepository.save(any<AiConversation>())).thenReturn(existing)

        val response = chatService.chat("user1", "tenant1", "existing-session", "hello")

        assertNotNull(response)
        assertEquals("existing-session", response.sessionId)
    }

    @Test
    fun `getConversations returns paginated results`() {
        val conv = AiConversation(sessionId = "s1", userId = "user1")
        conv.assignTenant("t1")
        val page = PageImpl(listOf(conv))

        whenever(conversationRepository.findByUserIdOrderByLastMessageAtDesc(eq("user1"), any()))
            .thenReturn(page)

        val result = chatService.getConversations("user1", PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `archiveConversation throws for wrong user`() {
        val conv = AiConversation(sessionId = "s1", userId = "user1")
        whenever(conversationRepository.findBySessionId("s1")).thenReturn(conv)

        assertThrows<SecurityException> {
            chatService.archiveConversation("s1", "other-user")
        }
    }

    @Test
    fun `archiveConversation succeeds for correct user`() {
        val conv = AiConversation(sessionId = "s1", userId = "user1")
        whenever(conversationRepository.findBySessionId("s1")).thenReturn(conv)
        whenever(conversationRepository.save(any<AiConversation>())).thenReturn(conv)

        chatService.archiveConversation("s1", "user1")

        assertEquals(ConversationStatus.ARCHIVED, conv.status)
        verify(conversationRepository).save(conv)
    }
}
