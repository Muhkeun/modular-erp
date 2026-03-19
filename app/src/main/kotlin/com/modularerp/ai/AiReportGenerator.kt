package com.modularerp.ai

import com.modularerp.report.domain.PageOrientation
import com.modularerp.report.dto.ReportColumn
import com.modularerp.report.service.ExcelService
import com.modularerp.report.service.PdfService
import org.springframework.stereotype.Service

@Service
class AiReportGenerator(
    private val excelService: ExcelService,
    private val pdfService: PdfService,
    private val erpDataService: ErpDataService
) {

    fun generateExcelFromQuery(title: String, queryResult: QueryResult): ByteArray {
        val columns = queryResult.columns.map { col ->
            ReportColumn(field = col, header = col)
        }
        val rows = queryResult.data.map { row ->
            queryResult.columns.zip(row).toMap()
        }
        return excelService.exportToExcel(title, columns, rows)
    }

    fun generatePdfFromQuery(title: String, queryResult: QueryResult): ByteArray {
        val columns = queryResult.columns.map { col ->
            ReportColumn(field = col, header = col, width = 1f)
        }
        val rows = queryResult.data.map { row ->
            queryResult.columns.zip(row).toMap()
        }
        return pdfService.exportToPdf(
            title = title,
            columns = columns,
            rows = rows,
            orientation = PageOrientation.LANDSCAPE
        )
    }
}
