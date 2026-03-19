package com.modularerp.report.controller

import com.modularerp.report.dto.*
import com.modularerp.report.service.ExportHelper
import com.modularerp.report.service.ReportTemplateService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/v1/export")
@Tag(name = "Export", description = "Generic data export (Excel/PDF/CSV)")
class ExportController(
    private val exportHelper: ExportHelper
) {

    @PostMapping("/excel")
    @Operation(summary = "Export data to Excel")
    fun exportExcel(@Valid @RequestBody request: GenericExportRequest): ResponseEntity<ByteArray> {
        return exportHelper.exportExcel(request)
    }

    @PostMapping("/pdf")
    @Operation(summary = "Export data to PDF")
    fun exportPdf(@Valid @RequestBody request: GenericExportRequest): ResponseEntity<ByteArray> {
        return exportHelper.exportPdf(request)
    }

    @PostMapping("/csv")
    @Operation(summary = "Export data to CSV")
    fun exportCsv(@Valid @RequestBody request: GenericExportRequest): ResponseEntity<ByteArray> {
        return exportHelper.exportCsv(request)
    }
}

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Report template management & generation")
class ReportController(
    private val reportTemplateService: ReportTemplateService,
    private val exportHelper: ExportHelper,
    private val pdfService: com.modularerp.report.service.PdfService,
    private val excelService: com.modularerp.report.service.ExcelService
) {

    // ── Legacy direct export endpoints (backward compatible) ──

    @PostMapping("/pdf/table")
    @Operation(summary = "Table data to PDF (legacy)")
    fun generateTablePdf(@Valid @RequestBody request: PdfTableRequest): ResponseEntity<ByteArray> {
        val genericRequest = GenericExportRequest(
            moduleName = "report",
            columns = request.columns,
            rows = request.rows,
            title = request.title,
            landscape = request.landscape
        )
        return exportHelper.exportPdf(genericRequest)
    }

    @PostMapping("/pdf/html")
    @Operation(summary = "HTML to PDF")
    fun generateHtmlPdf(@Valid @RequestBody request: PdfHtmlRequest): ResponseEntity<ByteArray> {
        val bytes = pdfService.generateFromHtml(request.html, request.landscape)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(bytes)
    }

    @PostMapping("/excel")
    @Operation(summary = "Data to Excel (legacy)")
    fun generateExcel(@Valid @RequestBody request: ExcelExportRequest): ResponseEntity<ByteArray> {
        val genericRequest = GenericExportRequest(
            moduleName = "report",
            columns = request.columns,
            rows = request.rows,
            fileName = request.fileName
        )
        return exportHelper.exportExcel(genericRequest)
    }

    @PostMapping("/excel/multi-sheet")
    @Operation(summary = "Multi-sheet Excel export")
    fun generateMultiSheetExcel(@Valid @RequestBody request: MultiSheetExcelRequest): ResponseEntity<ByteArray> {
        val bytes = excelService.exportMultiSheet(request.sheets)
        val fileName = request.fileName ?: "export.xlsx"
        return exportHelper.buildFileResponse(bytes, fileName, ExportHelper.XLSX_MEDIA_TYPE)
    }

    // ── Report Template CRUD ──

    @GetMapping("/templates")
    @Operation(summary = "List report templates")
    fun listTemplates(@PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<ReportTemplateResponse>> {
        val page = reportTemplateService.listTemplates(pageable)
        return ApiResponse.ok(
            page.content,
            PageMeta(page.number, page.size, page.totalElements, page.totalPages)
        )
    }

    @GetMapping("/templates/{id}")
    @Operation(summary = "Get report template")
    fun getTemplate(@PathVariable id: Long): ApiResponse<ReportTemplateResponse> {
        return ApiResponse.ok(reportTemplateService.getTemplate(id))
    }

    @PostMapping("/templates")
    @Operation(summary = "Create report template")
    fun createTemplate(@Valid @RequestBody request: CreateReportTemplateRequest): ApiResponse<ReportTemplateResponse> {
        return ApiResponse.ok(reportTemplateService.createTemplate(request))
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "Update report template")
    fun updateTemplate(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateReportTemplateRequest
    ): ApiResponse<ReportTemplateResponse> {
        return ApiResponse.ok(reportTemplateService.updateTemplate(id, request))
    }

    @DeleteMapping("/templates/{id}")
    @Operation(summary = "Delete report template")
    fun deleteTemplate(@PathVariable id: Long): ApiResponse<Unit> {
        reportTemplateService.deleteTemplate(id)
        return ApiResponse.ok(Unit)
    }

    // ── Report Generation ──

    @PostMapping("/generate")
    @Operation(summary = "Generate report from template")
    fun generateReport(
        @Valid @RequestBody request: GenerateReportRequest,
        principal: Principal?
    ): ApiResponse<ReportExecutionResponse> {
        val username = principal?.name ?: "system"
        return ApiResponse.ok(reportTemplateService.generateReport(request, username))
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download generated report")
    fun downloadReport(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val execution = reportTemplateService.getExecution(id)
        val data = execution.fileData
            ?: throw IllegalStateException("Report file not available")

        val ext = when (execution.outputFormat) {
            com.modularerp.report.domain.OutputFormat.EXCEL -> "xlsx"
            com.modularerp.report.domain.OutputFormat.PDF -> "pdf"
            com.modularerp.report.domain.OutputFormat.CSV -> "csv"
            com.modularerp.report.domain.OutputFormat.HTML -> "html"
        }
        val mediaType = when (execution.outputFormat) {
            com.modularerp.report.domain.OutputFormat.EXCEL -> ExportHelper.XLSX_MEDIA_TYPE
            com.modularerp.report.domain.OutputFormat.PDF -> MediaType.APPLICATION_PDF
            com.modularerp.report.domain.OutputFormat.CSV -> ExportHelper.CSV_MEDIA_TYPE
            com.modularerp.report.domain.OutputFormat.HTML -> MediaType.TEXT_HTML
        }
        val fileName = "${execution.executionNo}.$ext"

        return exportHelper.buildFileResponse(data, fileName, mediaType)
    }
}
