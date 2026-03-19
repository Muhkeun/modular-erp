package com.modularerp.ai

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiPermissionCheckerTest {

    private lateinit var checker: AiPermissionChecker

    private val sampleTools = listOf(
        ErpToolRegistry.ToolDefinition(
            name = "search_items",
            description = "Search items",
            descriptionKo = "품목 검색",
            parameters = emptyList(),
            requiredPermission = "MASTER_DATA:READ",
            category = "query"
        ),
        ErpToolRegistry.ToolDefinition(
            name = "get_sales_orders",
            description = "Get sales orders",
            descriptionKo = "판매주문 조회",
            parameters = emptyList(),
            requiredPermission = "SALES:READ",
            category = "query"
        ),
        ErpToolRegistry.ToolDefinition(
            name = "generate_excel_report",
            description = "Generate Excel report",
            descriptionKo = "Excel 보고서 생성",
            parameters = emptyList(),
            requiredPermission = "REPORT:EXPORT",
            category = "report"
        )
    )

    @BeforeEach
    fun setUp() {
        checker = AiPermissionChecker()
    }

    @Test
    fun `wildcard permission grants access to all tools`() {
        val filtered = checker.filterToolsByPermission(sampleTools, listOf("*"))
        assertEquals(3, filtered.size)
    }

    @Test
    fun `exact permission match grants access`() {
        val filtered = checker.filterToolsByPermission(sampleTools, listOf("SALES:READ"))
        assertEquals(1, filtered.size)
        assertEquals("get_sales_orders", filtered[0].name)
    }

    @Test
    fun `resource wildcard grants access to all actions on resource`() {
        val filtered = checker.filterToolsByPermission(sampleTools, listOf("SALES:*"))
        assertEquals(1, filtered.size)
        assertEquals("get_sales_orders", filtered[0].name)
    }

    @Test
    fun `no matching permission returns empty list`() {
        val filtered = checker.filterToolsByPermission(sampleTools, listOf("HR:READ"))
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `multiple permissions filter correctly`() {
        val filtered = checker.filterToolsByPermission(
            sampleTools,
            listOf("MASTER_DATA:READ", "REPORT:EXPORT")
        )
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.name == "search_items" })
        assertTrue(filtered.any { it.name == "generate_excel_report" })
    }

    @Test
    fun `canExecuteTool returns true for permitted tool`() {
        assertTrue(checker.canExecuteTool("search_items", sampleTools, listOf("MASTER_DATA:READ")))
    }

    @Test
    fun `canExecuteTool returns false for unpermitted tool`() {
        assertFalse(checker.canExecuteTool("search_items", sampleTools, listOf("SALES:READ")))
    }

    @Test
    fun `canExecuteTool returns false for unknown tool`() {
        assertFalse(checker.canExecuteTool("nonexistent_tool", sampleTools, listOf("*")))
    }

    @Test
    fun `resource-level permission matches any action`() {
        val filtered = checker.filterToolsByPermission(sampleTools, listOf("MASTER_DATA:WRITE"))
        // MASTER_DATA:WRITE starts with "MASTER_DATA:" so it matches
        assertEquals(1, filtered.size)
        assertEquals("search_items", filtered[0].name)
    }
}
