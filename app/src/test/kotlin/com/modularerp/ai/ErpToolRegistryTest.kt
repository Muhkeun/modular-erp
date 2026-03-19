package com.modularerp.ai

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.eq

class ErpToolRegistryTest {

    private val erpDataService: ErpDataService = mock(ErpDataService::class.java)
    private val registry = ErpToolRegistry(erpDataService)

    @Test
    fun `getAvailableTools returns non-empty list`() {
        val tools = registry.getAvailableTools()
        assertTrue(tools.isNotEmpty())
    }

    @Test
    fun `all tools have unique names`() {
        val tools = registry.getAvailableTools()
        val names = tools.map { it.name }
        assertEquals(names.size, names.toSet().size, "Tool names must be unique")
    }

    @Test
    fun `all tools have required permission format`() {
        val tools = registry.getAvailableTools()
        tools.forEach { tool ->
            assertTrue(
                tool.requiredPermission.contains(":"),
                "Permission '${tool.requiredPermission}' for tool '${tool.name}' must be in RESOURCE:ACTION format"
            )
        }
    }

    @Test
    fun `all tools have a category`() {
        val tools = registry.getAvailableTools()
        val validCategories = setOf("query", "analytics", "report")
        tools.forEach { tool ->
            assertTrue(
                tool.category in validCategories,
                "Tool '${tool.name}' has invalid category '${tool.category}'"
            )
        }
    }

    @Test
    fun `all tools have Korean description`() {
        val tools = registry.getAvailableTools()
        tools.forEach { tool ->
            assertTrue(tool.descriptionKo.isNotBlank(), "Tool '${tool.name}' missing Korean description")
        }
    }

    @Test
    fun `executeTool with unknown tool throws exception`() {
        assertThrows<IllegalArgumentException> {
            registry.executeTool("unknown_tool", emptyMap(), "T1")
        }
    }

    @Test
    fun `search_items tool has expected parameters`() {
        val tool = registry.getAvailableTools().find { it.name == "search_items" }
        assertNotNull(tool)
        val paramNames = tool!!.parameters.map { it.name }
        assertTrue("keyword" in paramNames)
        assertTrue("itemType" in paramNames)
    }

    @Test
    fun `tool parameter enum values are provided where applicable`() {
        val tools = registry.getAvailableTools()
        val itemTool = tools.find { it.name == "search_items" }!!
        val itemTypeParam = itemTool.parameters.find { it.name == "itemType" }!!
        assertNotNull(itemTypeParam.enumValues)
        assertTrue(itemTypeParam.enumValues!!.contains("MATERIAL"))
    }

    @Test
    fun `report tools require REPORT EXPORT permission`() {
        val tools = registry.getAvailableTools()
        val reportTools = tools.filter { it.category == "report" }
        assertTrue(reportTools.isNotEmpty())
        reportTools.forEach { tool ->
            assertEquals("REPORT:EXPORT", tool.requiredPermission)
        }
    }
}
