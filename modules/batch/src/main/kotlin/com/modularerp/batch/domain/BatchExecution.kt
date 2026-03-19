package com.modularerp.batch.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "batch_executions")
class BatchExecution(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_job_id", nullable = false)
    val batchJob: BatchJob,

    @Column(nullable = false, unique = true, length = 30)
    var executionNo: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ExecutionStatus = ExecutionStatus.QUEUED,

    @Column(nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now(),

    var completedAt: LocalDateTime? = null,

    var totalRecords: Int = 0,

    var processedRecords: Int = 0,

    var failedRecords: Int = 0,

    @Column(length = 2000)
    var errorMessage: String? = null,

    @Column(columnDefinition = "TEXT")
    var parameters: String? = null,

    @Column(columnDefinition = "TEXT")
    var result: String? = null,

    @Column(nullable = false, length = 20)
    var triggeredBy: String = "USER",

    @Column(length = 100)
    var executedBy: String? = null

) : TenantEntity() {

    fun start() {
        check(status == ExecutionStatus.QUEUED) { "Can only start from QUEUED status" }
        status = ExecutionStatus.RUNNING
        startedAt = LocalDateTime.now()
    }

    fun complete(processed: Int, failed: Int, resultJson: String? = null) {
        check(status == ExecutionStatus.RUNNING) { "Can only complete from RUNNING status" }
        status = ExecutionStatus.COMPLETED
        completedAt = LocalDateTime.now()
        processedRecords = processed
        failedRecords = failed
        result = resultJson
    }

    fun fail(error: String) {
        check(status == ExecutionStatus.RUNNING) { "Can only fail from RUNNING status" }
        status = ExecutionStatus.FAILED
        completedAt = LocalDateTime.now()
        errorMessage = error
    }

    fun cancel() {
        check(status == ExecutionStatus.QUEUED || status == ExecutionStatus.RUNNING) { "Can only cancel QUEUED or RUNNING" }
        status = ExecutionStatus.CANCELLED
        completedAt = LocalDateTime.now()
    }
}

enum class ExecutionStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }
