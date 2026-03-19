package com.modularerp.ai.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.modularerp.ai.config.AiProperties
import com.modularerp.ai.dto.AiChatRequest
import com.modularerp.ai.dto.AiChatResponse
import com.modularerp.ai.dto.AiQueryRequest
import com.modularerp.ai.dto.QueryResult
import com.modularerp.ai.domain.QueryIntent
import com.modularerp.ai.service.AiChatService
import com.modularerp.ai.service.AiQueryService
import com.modularerp.ai.service.AiReportService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.bean.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AiController::class)
class AiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var aiProperties: AiProperties

    @MockBean
    private lateinit var aiChatService: AiChatService

    @MockBean
    private lateinit var aiQueryService: AiQueryService

    @MockBean
    private lateinit var aiReportService: AiReportService

    @Test
    @WithMockUser
    fun `chat returns 503 when AI is disabled`() {
        whenever(aiProperties.enabled).thenReturn(false)

        mockMvc.perform(
            post("/api/v1/ai/chat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(AiChatRequest(message = "hello")))
        ).andExpect(status().isServiceUnavailable)
    }

    @Test
    @WithMockUser
    fun `chat returns response when AI is enabled`() {
        whenever(aiProperties.enabled).thenReturn(true)
        whenever(aiChatService.chat(any(), any(), anyOrNull(), any(), any(), any()))
            .thenReturn(AiChatResponse(sessionId = "s1", message = "Hello!"))

        mockMvc.perform(
            post("/api/v1/ai/chat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(AiChatRequest(message = "hello")))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.sessionId").value("s1"))
            .andExpect(jsonPath("$.data.message").value("Hello!"))
    }

    @Test
    @WithMockUser
    fun `query endpoint works`() {
        whenever(aiProperties.enabled).thenReturn(true)
        whenever(aiQueryService.parseQuery(any(), any(), any()))
            .thenReturn(QueryResult(intent = QueryIntent.SALES_SUMMARY, description = "Sales query"))

        mockMvc.perform(
            post("/api/v1/ai/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(AiQueryRequest(query = "매출")))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.intent").value("SALES_SUMMARY"))
    }
}
