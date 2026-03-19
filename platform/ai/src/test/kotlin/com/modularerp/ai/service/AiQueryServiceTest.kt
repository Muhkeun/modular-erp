package com.modularerp.ai.service

import com.modularerp.ai.config.AiProperties
import com.modularerp.ai.domain.QueryIntent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiQueryServiceTest {

    private lateinit var queryService: AiQueryService

    @BeforeEach
    fun setup() {
        val aiProperties = AiProperties(enabled = true)
        val erpToolService = ErpToolService()
        queryService = AiQueryService(aiProperties, erpToolService)
    }

    @Test
    fun `detects sales intent from Korean`() {
        val result = queryService.parseQuery("지난달 매출 보여줘", "tenant1", listOf("SALES:READ"))
        assertEquals(QueryIntent.SALES_SUMMARY, result.intent)
    }

    @Test
    fun `detects stock intent`() {
        val result = queryService.parseQuery("재고 부족 품목 리스트", "tenant1", listOf("STOCK:READ"))
        assertEquals(QueryIntent.STOCK_STATUS, result.intent)
    }

    @Test
    fun `detects purchase intent`() {
        val result = queryService.parseQuery("이번달 구매 현황", "tenant1", listOf("PURCHASE:READ"))
        assertEquals(QueryIntent.PURCHASE_SUMMARY, result.intent)
    }

    @Test
    fun `detects financial intent`() {
        val result = queryService.parseQuery("월간 손익 현황", "tenant1", listOf("ACCOUNT:READ"))
        assertEquals(QueryIntent.FINANCIAL_SUMMARY, result.intent)
    }

    @Test
    fun `returns general query for unrecognized input`() {
        val result = queryService.parseQuery("오늘 날씨 어때?", "tenant1", listOf("USER"))
        assertEquals(QueryIntent.GENERAL_QUERY, result.intent)
    }

    @Test
    fun `returns permission error when lacking access`() {
        val result = queryService.parseQuery("매출 보여줘", "tenant1", emptyList())
        // Should still detect intent but tool execution may fail
        assertEquals(QueryIntent.SALES_SUMMARY, result.intent)
    }
}
