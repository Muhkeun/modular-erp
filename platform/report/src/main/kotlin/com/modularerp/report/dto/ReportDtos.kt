package com.modularerp.report.dto

import com.modularerp.report.domain.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

// ── PDF ──

data class PdfTableRequest(
    @field:NotBlank val title: String,
    val subtitle: String? = null,
    @field:NotEmpty val columns: List<ReportColumn>,
    @field:NotEmpty val rows: List<Map<String, Any?>>,
    val landscape: Boolean = false,
    val footer: String? = null
)

data class PdfHtmlRequest(
    @field:NotBlank val html: String,
    val landscape: Boolean = false
)

// ── Excel ──

data class ExcelExportRequest(
    val sheetName: String? = null,
    @field:NotEmpty val columns: List<ReportColumn>,
    @field:NotEmpty val rows: List<Map<String, Any?>>,
    val fileName: String? = null
)

// ── Multi-Sheet Excel ──

data class MultiSheetExcelRequest(
    val sheets: List<SheetData>,
    val fileName: String? = null
)

data class SheetData(
    val sheetName: String,
    val columns: List<ReportColumn>,
    val rows: List<Map<String, Any?>>
)

// ── CSV ──

data class CsvExportRequest(
    @field:NotEmpty val columns: List<ReportColumn>,
    @field:NotEmpty val rows: List<Map<String, Any?>>,
    val fileName: String? = null
)

// ── Generic Export (from frontend grids) ──

data class GenericExportRequest(
    @field:NotBlank val moduleName: String,
    @field:NotEmpty val columns: List<ReportColumn>,
    @field:NotEmpty val rows: List<Map<String, Any?>>,
    val title: String? = null,
    val fileName: String? = null,
    val landscape: Boolean = false
)

// ── Shared ──

data class ReportColumn(
    @field:NotBlank val field: String,
    @field:NotBlank val header: String,
    val width: Float? = null,
    val align: String? = null  // "left", "center", "right"
)

// ── Report Template DTOs ──

data class CreateReportTemplateRequest(
    @field:NotBlank val templateCode: String,
    @field:NotBlank val templateName: String,
    val reportType: ReportType = ReportType.TABLE,
    val outputFormat: OutputFormat = OutputFormat.EXCEL,
    @field:NotBlank val moduleCode: String,
    val queryDefinition: String = "{}",
    val layoutDefinition: String? = null,
    val description: String? = null
)

data class UpdateReportTemplateRequest(
    val templateName: String? = null,
    val reportType: ReportType? = null,
    val outputFormat: OutputFormat? = null,
    val queryDefinition: String? = null,
    val layoutDefinition: String? = null,
    val enabled: Boolean? = null,
    val description: String? = null
)

data class ReportTemplateResponse(
    val id: Long,
    val templateCode: String,
    val templateName: String,
    val reportType: ReportType,
    val outputFormat: OutputFormat,
    val moduleCode: String,
    val queryDefinition: String,
    val layoutDefinition: String?,
    val enabled: Boolean,
    val description: String?
)

data class GenerateReportRequest(
    val templateId: Long,
    val outputFormat: OutputFormat? = null, // override template default
    val parameters: String? = null, // JSON filter values
    val columns: List<ReportColumn>? = null,
    val rows: List<Map<String, Any?>>? = null
)

data class ReportExecutionResponse(
    val id: Long,
    val executionNo: String,
    val templateId: Long?,
    val templateName: String?,
    val outputFormat: OutputFormat,
    val status: ExecutionStatus,
    val fileSize: Long?,
    val generatedAt: String?,
    val errorMessage: String?,
    val requestedBy: String
)
