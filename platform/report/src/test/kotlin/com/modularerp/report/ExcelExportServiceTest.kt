package com.modularerp.report

import com.modularerp.report.dto.ExcelExportRequest
import com.modularerp.report.dto.ReportColumn
import com.modularerp.report.dto.SheetData
import com.modularerp.report.service.ExcelService
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class ExcelExportServiceTest {

    private val excelService = ExcelService()

    @Test
    fun `should generate valid XLSX with headers and data`() {
        val request = ExcelExportRequest(
            sheetName = "Orders",
            columns = listOf(
                ReportColumn(field = "orderNo", header = "Order No", width = 20f),
                ReportColumn(field = "amount", header = "Amount", width = 15f, align = "right"),
                ReportColumn(field = "status", header = "Status", width = 12f)
            ),
            rows = listOf(
                mapOf("orderNo" to "PO-001", "amount" to 1500.50, "status" to "APPROVED"),
                mapOf("orderNo" to "PO-002", "amount" to 3200, "status" to "DRAFT")
            )
        )

        val bytes = excelService.generateExcel(request)

        assertNotNull(bytes)
        assertTrue(bytes.size > 100, "XLSX should not be empty")

        // Verify it's a valid XLSX by parsing it
        val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
        assertEquals(1, workbook.numberOfSheets)
        assertEquals("Orders", workbook.getSheetName(0))

        val sheet = workbook.getSheetAt(0)
        // Header row
        assertEquals("Order No", sheet.getRow(0).getCell(0).stringCellValue)
        assertEquals("Amount", sheet.getRow(0).getCell(1).stringCellValue)
        // Data row
        assertEquals("PO-001", sheet.getRow(1).getCell(0).stringCellValue)
        assertEquals(1500.50, sheet.getRow(1).getCell(1).numericCellValue, 0.01)

        workbook.close()
    }

    @Test
    fun `should generate multi-sheet Excel`() {
        val sheets = listOf(
            SheetData(
                sheetName = "Purchase Orders",
                columns = listOf(ReportColumn("no", "No"), ReportColumn("vendor", "Vendor")),
                rows = listOf(mapOf("no" to "PO-1", "vendor" to "ABC Corp"))
            ),
            SheetData(
                sheetName = "Sales Orders",
                columns = listOf(ReportColumn("no", "No"), ReportColumn("customer", "Customer")),
                rows = listOf(mapOf("no" to "SO-1", "customer" to "XYZ Inc"))
            )
        )

        val bytes = excelService.exportMultiSheet(sheets)
        val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
        assertEquals(2, workbook.numberOfSheets)
        assertEquals("Purchase Orders", workbook.getSheetName(0))
        assertEquals("Sales Orders", workbook.getSheetName(1))
        workbook.close()
    }

    @Test
    fun `should handle empty rows`() {
        val request = ExcelExportRequest(
            columns = listOf(ReportColumn("col1", "Column 1")),
            rows = emptyList()
        )

        val bytes = excelService.generateExcel(request)
        val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
        val sheet = workbook.getSheetAt(0)
        assertEquals(0, sheet.lastRowNum) // only header
        workbook.close()
    }

    @Test
    fun `should handle Korean characters`() {
        val request = ExcelExportRequest(
            sheetName = "주문목록",
            columns = listOf(ReportColumn("name", "품목명")),
            rows = listOf(mapOf("name" to "원자재 A"))
        )

        val bytes = excelService.generateExcel(request)
        val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
        assertEquals("주문목록", workbook.getSheetName(0))
        assertEquals("원자재 A", workbook.getSheetAt(0).getRow(1).getCell(0).stringCellValue)
        workbook.close()
    }
}
