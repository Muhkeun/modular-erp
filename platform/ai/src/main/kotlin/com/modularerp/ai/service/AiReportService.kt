package com.modularerp.ai.service

import com.modularerp.ai.config.AiProperties
import com.modularerp.ai.dto.ReportResult
import com.modularerp.report.dto.ExcelExportRequest
import com.modularerp.report.dto.PdfTableRequest
import com.modularerp.report.dto.ReportColumn
import com.modularerp.report.service.ExcelService
import com.modularerp.report.service.PdfService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

/**
 * AI-driven report generation service.
 * Interprets natural language report requests and generates Excel/PDF output.
 */
@Service
class AiReportService(
    private val aiProperties: AiProperties,
    private val excelService: ExcelService,
    private val pdfService: PdfService,
    private val aiQueryService: AiQueryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // In-memory file store for download (production: use S3/file storage)
    private val fileStore = mutableMapOf<String, Pair<ByteArray, String>>()

    fun generateReport(
        userId: String,
        tenantId: String,
        request: String,
        format: String = "excel",
        userPermissions: List<String> = emptyList()
    ): ReportResult {
        log.info("Generating report for user={}, request={}, format={}", userId, request, format)

        // Parse the report request to understand what data is needed
        val queryResult = aiQueryService.parseQuery(request, tenantId, userPermissions)

        val reportTitle = extractReportTitle(request)
        val columns = inferColumns(queryResult.intent.name)
        val rows = queryResult.data ?: emptyList()

        val fileId = UUID.randomUUID().toString()
        val filename: String
        val fileBytes: ByteArray

        if (format.lowercase() == "pdf") {
            filename = "${reportTitle}.pdf"
            fileBytes = pdfService.generateTablePdf(
                PdfTableRequest(
                    title = reportTitle,
                    columns = columns,
                    rows = rows
                )
            )
        } else {
            filename = "${reportTitle}.xlsx"
            fileBytes = excelService.generateExcel(
                ExcelExportRequest(
                    sheetName = reportTitle,
                    columns = columns,
                    rows = rows
                )
            )
        }

        fileStore[fileId] = fileBytes to filename

        return ReportResult(
            filename = filename,
            format = format,
            fileId = fileId,
            fileBytes = fileBytes,
            summary = "보고서 '${reportTitle}'가 생성되었습니다. ${rows.size}건의 데이터가 포함되어 있습니다."
        )
    }

    fun getFile(fileId: String): Pair<ByteArray, String>? = fileStore[fileId]

    private fun extractReportTitle(request: String): String {
        val lowerRequest = request.lowercase()
        return when {
            lowerRequest.contains("매출") -> "매출_보고서"
            lowerRequest.contains("구매") || lowerRequest.contains("발주") -> "구매_보고서"
            lowerRequest.contains("재고") -> "재고_현황_보고서"
            lowerRequest.contains("미수금") -> "미수금_현황"
            lowerRequest.contains("재무") || lowerRequest.contains("손익") -> "재무_보고서"
            lowerRequest.contains("생산") -> "생산_현황_보고서"
            else -> "AI_보고서"
        }
    }

    private fun inferColumns(intentName: String): List<ReportColumn> {
        return when (intentName) {
            "SALES_SUMMARY" -> listOf(
                ReportColumn(field = "orderNo", header = "주문번호"),
                ReportColumn(field = "customerName", header = "거래처"),
                ReportColumn(field = "orderDate", header = "주문일", align = "center"),
                ReportColumn(field = "totalAmount", header = "금액", align = "right"),
                ReportColumn(field = "status", header = "상태", align = "center")
            )
            "STOCK_STATUS" -> listOf(
                ReportColumn(field = "itemCode", header = "품목코드"),
                ReportColumn(field = "itemName", header = "품목명"),
                ReportColumn(field = "warehouse", header = "창고"),
                ReportColumn(field = "quantity", header = "수량", align = "right"),
                ReportColumn(field = "unit", header = "단위", align = "center")
            )
            "PURCHASE_SUMMARY" -> listOf(
                ReportColumn(field = "poNo", header = "발주번호"),
                ReportColumn(field = "vendorName", header = "공급업체"),
                ReportColumn(field = "orderDate", header = "발주일", align = "center"),
                ReportColumn(field = "totalAmount", header = "금액", align = "right"),
                ReportColumn(field = "status", header = "상태", align = "center")
            )
            else -> listOf(
                ReportColumn(field = "key", header = "항목"),
                ReportColumn(field = "value", header = "값")
            )
        }
    }
}
