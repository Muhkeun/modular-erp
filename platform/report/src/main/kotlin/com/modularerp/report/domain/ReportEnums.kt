package com.modularerp.report.domain

enum class ReportType {
    TABLE, SUMMARY, CHART, CUSTOM
}

enum class OutputFormat {
    EXCEL, PDF, CSV, HTML
}

enum class ExecutionStatus {
    QUEUED, GENERATING, COMPLETED, FAILED
}

enum class PageOrientation {
    PORTRAIT, LANDSCAPE
}
