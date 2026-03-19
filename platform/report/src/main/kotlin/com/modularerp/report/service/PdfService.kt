package com.modularerp.report.service

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.modularerp.report.domain.PageOrientation
import com.modularerp.report.dto.PdfTableRequest
import com.modularerp.report.dto.ReportColumn
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * iText based PDF generation service with Korean font support.
 */
@Service
class PdfService {

    fun generateTablePdf(request: PdfTableRequest): ByteArray {
        return exportToPdf(
            title = request.title,
            columns = request.columns,
            rows = request.rows,
            orientation = if (request.landscape) PageOrientation.LANDSCAPE else PageOrientation.PORTRAIT,
            subtitle = request.subtitle,
            footer = request.footer
        )
    }

    fun exportToPdf(
        title: String,
        columns: List<ReportColumn>,
        rows: List<Map<String, Any?>>,
        orientation: PageOrientation = PageOrientation.PORTRAIT,
        subtitle: String? = null,
        footer: String? = null
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = PdfWriter(baos)
        val pdfDoc = PdfDocument(writer)
        val pageSize = if (orientation == PageOrientation.LANDSCAPE) PageSize.A4.rotate() else PageSize.A4
        val document = Document(pdfDoc, pageSize)
        document.setMargins(36f, 36f, 50f, 36f)

        val font = loadKoreanFont()

        // Title
        document.add(
            Paragraph(title)
                .setFont(font)
                .setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5f)
        )

        // Subtitle / date
        val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        document.add(
            Paragraph(subtitle ?: dateStr)
                .setFont(font)
                .setFontSize(9f)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(10f)
        )

        // Table
        val columnWidths = columns.map { it.width ?: 1f }.toFloatArray()
        val table = Table(UnitValue.createPercentArray(columnWidths))
            .useAllAvailableWidth()

        // Header cells
        columns.forEach { col ->
            table.addHeaderCell(
                Cell().add(Paragraph(col.header).setFont(font).setFontSize(9f))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
            )
        }

        // Data cells
        rows.forEach { row ->
            columns.forEach { col ->
                val value = row[col.field]?.toString() ?: ""
                val alignment = when (col.align) {
                    "right" -> TextAlignment.RIGHT
                    "center" -> TextAlignment.CENTER
                    else -> TextAlignment.LEFT
                }
                table.addCell(
                    Cell().add(Paragraph(value).setFont(font).setFontSize(8f))
                        .setTextAlignment(alignment)
                )
            }
        }

        document.add(table)

        // Footer
        val footerText = footer ?: "Page 1 | Generated: $dateStr"
        document.add(
            Paragraph(footerText)
                .setFont(font)
                .setFontSize(7f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10f)
        )

        document.close()
        return baos.toByteArray()
    }

    fun generateFromHtml(html: String, landscape: Boolean = false): ByteArray {
        val baos = ByteArrayOutputStream()
        val writer = PdfWriter(baos)
        val pdfDoc = PdfDocument(writer)
        val pageSize = if (landscape) PageSize.A4.rotate() else PageSize.A4
        pdfDoc.defaultPageSize = pageSize

        com.itextpdf.html2pdf.HtmlConverter.convertToPdf(html, pdfDoc, com.itextpdf.html2pdf.ConverterProperties())

        return baos.toByteArray()
    }

    private fun loadKoreanFont(): PdfFont {
        val fontPaths = listOf(
            "/System/Library/Fonts/AppleSDGothicNeo.ttc,0",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc,0",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
            "C:/Windows/Fonts/malgun.ttf"
        )

        for (path in fontPaths) {
            try {
                return PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H)
            } catch (_: Exception) {
                // try next
            }
        }

        return PdfFontFactory.createFont()
    }
}
