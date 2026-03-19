package com.modularerp.report.service

import com.modularerp.report.dto.ExcelExportRequest
import com.modularerp.report.dto.ReportColumn
import com.modularerp.report.dto.SheetData
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Apache POI based Excel generation service.
 * Uses SXSSFWorkbook for memory-efficient large dataset processing.
 */
@Service
class ExcelService {

    fun generateExcel(request: ExcelExportRequest): ByteArray {
        return exportToExcel(
            sheetName = request.sheetName ?: "Sheet1",
            columns = request.columns,
            rows = request.rows
        )
    }

    fun exportToExcel(
        sheetName: String,
        columns: List<ReportColumn>,
        rows: List<Map<String, Any?>>
    ): ByteArray {
        val workbook = SXSSFWorkbook(100)
        try {
            populateSheet(workbook, sheetName, columns, rows)
            return toByteArray(workbook)
        } finally {
            workbook.close()
            workbook.dispose()
        }
    }

    fun exportMultiSheet(sheets: List<SheetData>): ByteArray {
        val workbook = SXSSFWorkbook(100)
        try {
            sheets.forEach { sheet ->
                populateSheet(
                    workbook,
                    sheet.sheetName,
                    sheet.columns,
                    sheet.rows
                )
            }
            return toByteArray(workbook)
        } finally {
            workbook.close()
            workbook.dispose()
        }
    }

    private fun populateSheet(
        workbook: SXSSFWorkbook,
        sheetName: String,
        columns: List<ReportColumn>,
        rows: List<Map<String, Any?>>
    ) {
        val sheet = workbook.createSheet(sheetName)
        val headerStyle = createHeaderStyle(workbook)
        val dateStyle = createDateStyle(workbook)
        val numberStyle = createNumberStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)

        // Header row
        val headerRow = sheet.createRow(0)
        columns.forEachIndexed { idx, col ->
            val cell = headerRow.createCell(idx)
            cell.setCellValue(col.header)
            cell.cellStyle = headerStyle
            sheet.setColumnWidth(idx, ((col.width ?: 15f) * 256).toInt())
        }

        // Data rows
        rows.forEachIndexed { rowIdx, row ->
            val dataRow = sheet.createRow(rowIdx + 1)
            columns.forEachIndexed { colIdx, col ->
                val cell = dataRow.createCell(colIdx)
                setCellValue(cell, row[col.field], dateStyle, numberStyle, currencyStyle)
            }
        }
    }

    private fun setCellValue(
        cell: Cell,
        value: Any?,
        dateStyle: CellStyle,
        numberStyle: CellStyle,
        currencyStyle: CellStyle
    ) {
        when (value) {
            null -> cell.setCellValue("")
            is Number -> {
                cell.setCellValue(value.toDouble())
                cell.cellStyle = numberStyle
            }
            is Boolean -> cell.setCellValue(value)
            is LocalDate -> {
                cell.setCellValue(value.format(DateTimeFormatter.ISO_LOCAL_DATE))
                cell.cellStyle = dateStyle
            }
            is LocalDateTime -> {
                cell.setCellValue(value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                cell.cellStyle = dateStyle
            }
            else -> cell.setCellValue(value.toString())
        }
    }

    private fun createHeaderStyle(workbook: SXSSFWorkbook): CellStyle {
        val style = workbook.createCellStyle()
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        style.alignment = HorizontalAlignment.CENTER
        val font = workbook.createFont()
        font.bold = true
        font.fontHeightInPoints = 10
        style.setFont(font)
        return style
    }

    private fun createDateStyle(workbook: SXSSFWorkbook): CellStyle {
        val style = workbook.createCellStyle()
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    private fun createNumberStyle(workbook: SXSSFWorkbook): CellStyle {
        val style = workbook.createCellStyle()
        val format = workbook.creationHelper.createDataFormat()
        style.dataFormat = format.getFormat("#,##0.##")
        style.alignment = HorizontalAlignment.RIGHT
        return style
    }

    private fun createCurrencyStyle(workbook: SXSSFWorkbook): CellStyle {
        val style = workbook.createCellStyle()
        val format = workbook.creationHelper.createDataFormat()
        style.dataFormat = format.getFormat("#,##0")
        style.alignment = HorizontalAlignment.RIGHT
        return style
    }

    private fun toByteArray(workbook: SXSSFWorkbook): ByteArray {
        val baos = ByteArrayOutputStream()
        workbook.write(baos)
        return baos.toByteArray()
    }
}
