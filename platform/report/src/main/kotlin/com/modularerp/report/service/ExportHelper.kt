package com.modularerp.report.service

import com.modularerp.report.dto.GenericExportRequest
import com.modularerp.report.dto.ReportColumn
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Helper utility for exporting grid data in various formats.
 * Can be injected into any module's controller to add export capability.
 */
@Component
class ExportHelper(
    private val excelService: ExcelService,
    private val pdfService: PdfService,
    private val csvExportService: CsvExportService
) {

    fun exportExcel(request: GenericExportRequest): ResponseEntity<ByteArray> {
        val bytes = excelService.exportToExcel(
            sheetName = request.moduleName,
            columns = request.columns,
            rows = request.rows
        )
        val fileName = request.fileName ?: "${request.moduleName}_export.xlsx"
        return buildFileResponse(bytes, fileName, XLSX_MEDIA_TYPE)
    }

    fun exportPdf(request: GenericExportRequest): ResponseEntity<ByteArray> {
        val bytes = pdfService.exportToPdf(
            title = request.title ?: request.moduleName,
            columns = request.columns,
            rows = request.rows,
            orientation = if (request.landscape)
                com.modularerp.report.domain.PageOrientation.LANDSCAPE
            else
                com.modularerp.report.domain.PageOrientation.PORTRAIT
        )
        val fileName = request.fileName ?: "${request.moduleName}_export.pdf"
        return buildFileResponse(bytes, fileName, MediaType.APPLICATION_PDF)
    }

    fun exportCsv(request: GenericExportRequest): ResponseEntity<ByteArray> {
        val bytes = csvExportService.exportToCsv(request.columns, request.rows)
        val fileName = request.fileName ?: "${request.moduleName}_export.csv"
        return buildFileResponse(bytes, fileName, CSV_MEDIA_TYPE)
    }

    fun buildFileResponse(data: ByteArray, fileName: String, mediaType: MediaType): ResponseEntity<ByteArray> {
        val encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20")
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$encodedName\"; filename*=UTF-8''$encodedName")
            .contentType(mediaType)
            .contentLength(data.size.toLong())
            .body(data)
    }

    companion object {
        val XLSX_MEDIA_TYPE: MediaType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        val CSV_MEDIA_TYPE: MediaType = MediaType.parseMediaType("text/csv")
    }
}
