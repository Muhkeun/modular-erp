package com.modularerp.batch.dto

import com.modularerp.batch.domain.BatchExecution
import com.modularerp.batch.domain.BatchJob
import com.modularerp.batch.domain.BatchJobType
import com.modularerp.batch.domain.ExecutionStatus
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class CreateBatchJobRequest(
    @field:NotBlank val jobCode: String,
    @field:NotBlank val jobName: String,
    val jobType: BatchJobType,
    val companyCode: String? = null,
    val departmentCode: String? = null,
    val plantCode: String? = null,
    val cronExpression: String? = null,
    val enabled: Boolean = true,
    val description: String? = null
)

data class UpdateBatchJobRequest(
    val jobName: String? = null,
    val companyCode: String? = null,
    val departmentCode: String? = null,
    val plantCode: String? = null,
    val cronExpression: String? = null,
    val description: String? = null
)

data class ExecuteJobRequest(
    val parameters: String? = null
)

data class BatchJobResponse(
    val id: Long,
    val jobCode: String,
    val jobName: String,
    val jobType: BatchJobType,
    val companyCode: String?,
    val departmentCode: String?,
    val plantCode: String?,
    val cronExpression: String?,
    val enabled: Boolean,
    val lastRunAt: LocalDateTime?,
    val nextRunAt: LocalDateTime?,
    val description: String?
)

data class BatchExecutionResponse(
    val id: Long,
    val jobId: Long,
    val jobCode: String,
    val companyCode: String?,
    val departmentCode: String?,
    val plantCode: String?,
    val executionNo: String,
    val status: ExecutionStatus,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val totalRecords: Int,
    val processedRecords: Int,
    val failedRecords: Int,
    val errorMessage: String?,
    val parameters: String?,
    val result: String?,
    val triggeredBy: String,
    val executedBy: String?
)

fun BatchJob.toResponse() = BatchJobResponse(
    id = id, jobCode = jobCode, jobName = jobName, jobType = jobType,
    companyCode = companyCode, departmentCode = departmentCode, plantCode = plantCode,
    cronExpression = cronExpression, enabled = enabled,
    lastRunAt = lastRunAt, nextRunAt = nextRunAt, description = description
)

fun BatchExecution.toResponse() = BatchExecutionResponse(
    id = id, jobId = batchJob.id, jobCode = batchJob.jobCode,
    companyCode = batchJob.companyCode, departmentCode = batchJob.departmentCode, plantCode = batchJob.plantCode,
    executionNo = executionNo, status = status,
    startedAt = startedAt, completedAt = completedAt,
    totalRecords = totalRecords, processedRecords = processedRecords,
    failedRecords = failedRecords, errorMessage = errorMessage,
    parameters = parameters, result = result,
    triggeredBy = triggeredBy, executedBy = executedBy
)
