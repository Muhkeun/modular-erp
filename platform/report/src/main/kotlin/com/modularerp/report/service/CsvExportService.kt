package com.modularerp.report.service

import com.modularerp.report.dto.ReportColumn
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

@Service
class CsvExportService {

    fun exportToCsv(columns: List<ReportColumn>, rows: List<Map<String, Any?>>): ByteArray {
        val baos = ByteArrayOutputStream()
        // BOM for Excel UTF-8 compatibility
        baos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

        val writer = OutputStreamWriter(baos, Charsets.UTF_8)

        // Header row
        writer.write(columns.joinToString(",") { escapeCsv(it.header) })
        writer.write("\n")

        // Data rows
        rows.forEach { row ->
            writer.write(columns.joinToString(",") { col ->
                escapeCsv(row[col.field]?.toString() ?: "")
            })
            writer.write("\n")
        }

        writer.flush()
        return baos.toByteArray()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
