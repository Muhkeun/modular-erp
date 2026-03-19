package com.modularerp.report

import com.modularerp.report.domain.PageOrientation
import com.modularerp.report.dto.ReportColumn
import com.modularerp.report.service.PdfService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PdfExportServiceTest {

    private val pdfService = PdfService()

    @Test
    fun `should generate valid PDF with table data`() {
        val columns = listOf(
            ReportColumn(field = "orderNo", header = "Order No", width = 2f),
            ReportColumn(field = "amount", header = "Amount", width = 1f, align = "right"),
            ReportColumn(field = "status", header = "Status", width = 1f, align = "center")
        )
        val rows = listOf(
            mapOf("orderNo" to "PO-001", "amount" to "1,500", "status" to "APPROVED"),
            mapOf("orderNo" to "PO-002", "amount" to "3,200", "status" to "DRAFT")
        )

        val bytes = pdfService.exportToPdf(
            title = "Purchase Order Report",
            columns = columns,
            rows = rows,
            orientation = PageOrientation.LANDSCAPE
        )

        assertNotNull(bytes)
        assertTrue(bytes.size > 100, "PDF should not be empty")
        // Verify PDF magic bytes
        assertEquals('%'.code.toByte(), bytes[0])
        assertEquals('P'.code.toByte(), bytes[1])
        assertEquals('D'.code.toByte(), bytes[2])
        assertEquals('F'.code.toByte(), bytes[3])
    }

    @Test
    fun `should generate PDF in portrait mode`() {
        val bytes = pdfService.exportToPdf(
            title = "Test",
            columns = listOf(ReportColumn("a", "A")),
            rows = listOf(mapOf("a" to "value")),
            orientation = PageOrientation.PORTRAIT
        )

        assertTrue(bytes.isNotEmpty())
        assertEquals('%'.code.toByte(), bytes[0]) // PDF header
    }

    @Test
    fun `should handle empty rows in PDF`() {
        val bytes = pdfService.exportToPdf(
            title = "Empty Report",
            columns = listOf(ReportColumn("col", "Column")),
            rows = emptyList()
        )

        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun `should generate PDF from HTML`() {
        val html = """
            <html><body>
            <h1>Test Report</h1>
            <table><tr><th>Name</th></tr><tr><td>Test</td></tr></table>
            </body></html>
        """.trimIndent()

        val bytes = pdfService.generateFromHtml(html)
        assertTrue(bytes.isNotEmpty())
        assertEquals('%'.code.toByte(), bytes[0])
    }
}
