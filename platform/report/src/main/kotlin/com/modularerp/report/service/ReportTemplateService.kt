package com.modularerp.report.service

import com.modularerp.report.domain.*
import com.modularerp.report.dto.*
import com.modularerp.report.repository.ReportExecutionRepository
import com.modularerp.report.repository.ReportTemplateRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

@Service
@Transactional
class ReportTemplateService(
    private val templateRepository: ReportTemplateRepository,
    private val executionRepository: ReportExecutionRepository,
    private val excelService: ExcelService,
    private val pdfService: PdfService,
    private val csvExportService: CsvExportService
) {

    private val executionSeq = AtomicLong(System.currentTimeMillis())

    @Transactional(readOnly = true)
    fun listTemplates(pageable: Pageable): Page<ReportTemplateResponse> {
        return templateRepository.findByEnabledTrue(pageable).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getTemplate(id: Long): ReportTemplateResponse {
        return findTemplate(id).toResponse()
    }

    fun createTemplate(request: CreateReportTemplateRequest): ReportTemplateResponse {
        val template = ReportTemplate().apply {
            templateCode = request.templateCode
            templateName = request.templateName
            reportType = request.reportType
            outputFormat = request.outputFormat
            moduleCode = request.moduleCode
            queryDefinition = request.queryDefinition
            layoutDefinition = request.layoutDefinition
            description = request.description
        }
        return templateRepository.save(template).toResponse()
    }

    fun updateTemplate(id: Long, request: UpdateReportTemplateRequest): ReportTemplateResponse {
        val template = findTemplate(id)
        request.templateName?.let { template.templateName = it }
        request.reportType?.let { template.reportType = it }
        request.outputFormat?.let { template.outputFormat = it }
        request.queryDefinition?.let { template.queryDefinition = it }
        request.layoutDefinition?.let { template.layoutDefinition = it }
        request.enabled?.let { template.enabled = it }
        request.description?.let { template.description = it }
        return templateRepository.save(template).toResponse()
    }

    fun deleteTemplate(id: Long) {
        val template = findTemplate(id)
        template.deactivate()
        template.enabled = false
        templateRepository.save(template)
    }

    fun generateReport(request: GenerateReportRequest, username: String): ReportExecutionResponse {
        val template = if (request.templateId > 0) findTemplate(request.templateId) else null
        val format = request.outputFormat ?: template?.outputFormat ?: OutputFormat.EXCEL

        val execution = ReportExecution().apply {
            reportTemplate = template
            executionNo = generateExecutionNo()
            parameters = request.parameters
            outputFormat = format
            requestedBy = username
        }
        executionRepository.save(execution)

        try {
            execution.markGenerating()

            val columns = request.columns ?: emptyList()
            val rows = request.rows ?: emptyList()
            val title = template?.templateName ?: "Report"

            val data = when (format) {
                OutputFormat.EXCEL -> excelService.exportToExcel(title, columns, rows)
                OutputFormat.PDF -> pdfService.exportToPdf(title, columns, rows)
                OutputFormat.CSV -> csvExportService.exportToCsv(columns, rows)
                OutputFormat.HTML -> throw UnsupportedOperationException("HTML output not supported for report generation")
            }

            execution.markCompleted(data)
        } catch (e: Exception) {
            execution.markFailed(e.message ?: "Unknown error")
        }

        executionRepository.save(execution)
        return execution.toResponse()
    }

    @Transactional(readOnly = true)
    fun getExecution(id: Long): ReportExecution {
        return executionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Report execution not found: $id") }
    }

    private fun findTemplate(id: Long): ReportTemplate {
        return templateRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Report template not found: $id") }
    }

    private fun generateExecutionNo(): String {
        val prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return "RPT-$prefix-${executionSeq.incrementAndGet() % 100000}"
    }

    private fun ReportTemplate.toResponse() = ReportTemplateResponse(
        id = id,
        templateCode = templateCode,
        templateName = templateName,
        reportType = reportType,
        outputFormat = outputFormat,
        moduleCode = moduleCode,
        queryDefinition = queryDefinition,
        layoutDefinition = layoutDefinition,
        enabled = enabled,
        description = description
    )

    private fun ReportExecution.toResponse() = ReportExecutionResponse(
        id = id,
        executionNo = executionNo,
        templateId = reportTemplate?.id,
        templateName = reportTemplate?.templateName,
        outputFormat = outputFormat,
        status = status,
        fileSize = fileSize,
        generatedAt = generatedAt?.toString(),
        errorMessage = errorMessage,
        requestedBy = requestedBy
    )
}
