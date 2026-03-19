package com.modularerp.report

import com.modularerp.report.dto.ReportColumn
import com.modularerp.report.service.CsvExportService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CsvExportServiceTest {

    private val csvService = CsvExportService()

    @Test
    fun `should generate CSV with BOM and correct content`() {
        val columns = listOf(
            ReportColumn("name", "Name"),
            ReportColumn("amount", "Amount")
        )
        val rows = listOf(
            mapOf("name" to "Item A", "amount" to "1000"),
            mapOf("name" to "Item B", "amount" to "2000")
        )

        val bytes = csvService.exportToCsv(columns, rows)
        val content = String(bytes, Charsets.UTF_8)

        // BOM
        assertTrue(bytes[0] == 0xEF.toByte())
        assertTrue(bytes[1] == 0xBB.toByte())
        assertTrue(bytes[2] == 0xBF.toByte())

        assertTrue(content.contains("Name,Amount"))
        assertTrue(content.contains("Item A,1000"))
        assertTrue(content.contains("Item B,2000"))
    }

    @Test
    fun `should escape commas and quotes in CSV`() {
        val columns = listOf(ReportColumn("desc", "Description"))
        val rows = listOf(
            mapOf("desc" to "Item with, comma"),
            mapOf("desc" to "Item with \"quotes\"")
        )

        val bytes = csvService.exportToCsv(columns, rows)
        val content = String(bytes, Charsets.UTF_8)

        assertTrue(content.contains("\"Item with, comma\""))
        assertTrue(content.contains("\"Item with \"\"quotes\"\"\""))
    }
}
