package com.modularerp.ai

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for QueryResult data class and ErpDataService helper logic.
 * Full integration tests with repositories require Spring context / H2.
 */
class ErpDataServiceTest {

    @Test
    fun `QueryResult holds correct data`() {
        val result = QueryResult(
            columns = listOf("id", "name"),
            data = listOf(listOf(1L, "Test Item")),
            totalCount = 1,
            description = "Test query"
        )
        assertEquals(2, result.columns.size)
        assertEquals(1, result.data.size)
        assertEquals(1, result.totalCount)
        assertEquals("Test query", result.description)
    }

    @Test
    fun `QueryResult with empty data`() {
        val result = QueryResult(
            columns = listOf("id"),
            data = emptyList(),
            totalCount = 0,
            description = "Empty result"
        )
        assertTrue(result.data.isEmpty())
        assertEquals(0, result.totalCount)
    }

    @Test
    fun `QueryResult data preserves null values`() {
        val result = QueryResult(
            columns = listOf("id", "name", "group"),
            data = listOf(listOf(1L, "Item A", null)),
            totalCount = 1,
            description = "Nullable test"
        )
        assertNull(result.data[0][2])
    }

    @Test
    fun `QueryResult data preserves various types`() {
        val result = QueryResult(
            columns = listOf("id", "name", "amount", "active"),
            data = listOf(listOf(1L, "Test", java.math.BigDecimal("100.50"), true)),
            totalCount = 1,
            description = "Type test"
        )
        assertEquals(java.math.BigDecimal("100.50"), result.data[0][2])
        assertEquals(true, result.data[0][3])
    }
}
