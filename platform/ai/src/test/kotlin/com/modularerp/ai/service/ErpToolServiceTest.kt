package com.modularerp.ai.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ErpToolServiceTest {

    private lateinit var toolService: ErpToolService

    @BeforeEach
    fun setup() {
        toolService = ErpToolService()
    }

    @Test
    fun `getAvailableTools filters by permission`() {
        val tools = toolService.getAvailableTools(listOf("SALES:READ"))
        assertTrue(tools.any { it.name == "get_sales_orders" })
        assertFalse(tools.any { it.name == "create_purchase_request" })
    }

    @Test
    fun `getAvailableTools returns all for admin`() {
        val tools = toolService.getAvailableTools(listOf("ADMIN"))
        assertTrue(tools.size >= 5)
    }

    @Test
    fun `executeTool throws for unknown tool`() {
        assertThrows<IllegalArgumentException> {
            toolService.executeTool("nonexistent", emptyMap(), "t1", "u1", listOf("ADMIN"))
        }
    }

    @Test
    fun `executeTool throws for insufficient permissions`() {
        assertThrows<SecurityException> {
            toolService.executeTool("create_purchase_request", emptyMap(), "t1", "u1", listOf("SALES:READ"))
        }
    }

    @Test
    fun `executeTool succeeds with correct permissions`() {
        val result = toolService.executeTool(
            "search_items", mapOf("keyword" to "test"), "t1", "u1", listOf("ITEM:READ")
        )
        assertNotNull(result)
        @Suppress("UNCHECKED_CAST")
        val map = result as Map<String, Any>
        assertEquals(true, map["success"])
    }

    @Test
    fun `getToolDefinitions returns formatted string`() {
        val defs = toolService.getToolDefinitions(listOf("ADMIN"))
        assertTrue(defs.contains("search_items"))
        assertTrue(defs.contains("get_sales_orders"))
    }
}
