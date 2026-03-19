package com.modularerp.report.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "report_executions")
class ReportExecution : TenantEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    var reportTemplate: ReportTemplate? = null

    @Column(name = "execution_no", nullable = false, length = 30)
    var executionNo: String = ""

    @Column(columnDefinition = "TEXT")
    var parameters: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false, length = 10)
    var outputFormat: OutputFormat = OutputFormat.EXCEL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ExecutionStatus = ExecutionStatus.QUEUED

    @Column(name = "file_path", length = 500)
    var filePath: String? = null

    @Column(name = "file_size")
    var fileSize: Long? = null

    @Column(name = "generated_at")
    var generatedAt: LocalDateTime? = null

    @Column(name = "error_message", length = 2000)
    var errorMessage: String? = null

    @Column(name = "requested_by", nullable = false, length = 100)
    var requestedBy: String = ""

    @Column(name = "file_data")
    var fileData: ByteArray? = null

    fun markGenerating() {
        this.status = ExecutionStatus.GENERATING
    }

    fun markCompleted(data: ByteArray) {
        this.status = ExecutionStatus.COMPLETED
        this.fileData = data
        this.fileSize = data.size.toLong()
        this.generatedAt = LocalDateTime.now()
    }

    fun markFailed(message: String) {
        this.status = ExecutionStatus.FAILED
        this.errorMessage = message
    }
}
