package com.modularerp.batch.service

import com.modularerp.batch.domain.*
import com.modularerp.batch.dto.*
import com.modularerp.batch.repository.*
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

@Service
@Transactional(readOnly = true)
class BatchService(
    private val jobRepository: BatchJobRepository,
    private val executionRepository: BatchExecutionRepository
) {
    private val executionSeq = AtomicLong(System.currentTimeMillis())

    fun getJobById(id: Long): BatchJobResponse = findJob(id).toResponse()

    fun searchJobs(jobType: BatchJobType?, enabled: Boolean?, pageable: Pageable): Page<BatchJobResponse> =
        jobRepository.search(TenantContext.getTenantId(), jobType, enabled, pageable).map { it.toResponse() }

    @Transactional
    fun createJob(request: CreateBatchJobRequest): BatchJobResponse {
        val tenantId = TenantContext.getTenantId()
        val job = BatchJob(
            jobCode = request.jobCode, jobName = request.jobName,
            jobType = request.jobType, cronExpression = request.cronExpression,
            enabled = request.enabled, description = request.description
        ).apply { assignTenant(tenantId) }
        return jobRepository.save(job).toResponse()
    }

    @Transactional
    fun updateJob(id: Long, request: UpdateBatchJobRequest): BatchJobResponse {
        val job = findJob(id)
        request.jobName?.let { job.jobName = it }
        request.cronExpression?.let { job.cronExpression = it }
        request.description?.let { job.description = it }
        return jobRepository.save(job).toResponse()
    }

    @Transactional
    fun enableJob(id: Long): BatchJobResponse {
        val job = findJob(id)
        job.enabled = true
        return jobRepository.save(job).toResponse()
    }

    @Transactional
    fun disableJob(id: Long): BatchJobResponse {
        val job = findJob(id)
        job.enabled = false
        return jobRepository.save(job).toResponse()
    }

    @Transactional
    fun executeJob(id: Long, request: ExecuteJobRequest? = null): BatchExecutionResponse {
        val tenantId = TenantContext.getTenantId()
        val job = findJob(id)
        check(job.enabled) { "Cannot execute a disabled job" }

        val executionNo = "EXE-${executionSeq.incrementAndGet()}"
        val execution = BatchExecution(
            batchJob = job, executionNo = executionNo,
            parameters = request?.parameters, triggeredBy = "USER"
        ).apply { assignTenant(tenantId) }

        job.lastRunAt = LocalDateTime.now()
        jobRepository.save(job)

        return executionRepository.save(execution).toResponse()
    }

    fun getExecutionHistory(jobId: Long, pageable: Pageable): Page<BatchExecutionResponse> =
        executionRepository.findByJobId(TenantContext.getTenantId(), jobId, pageable).map { it.toResponse() }

    fun getExecutionStatus(executionId: Long): BatchExecutionResponse =
        findExecution(executionId).toResponse()

    @Transactional
    fun cancelExecution(executionId: Long): BatchExecutionResponse {
        val execution = findExecution(executionId)
        execution.cancel()
        return executionRepository.save(execution).toResponse()
    }

    @Transactional
    fun retryFailedExecution(executionId: Long): BatchExecutionResponse {
        val tenantId = TenantContext.getTenantId()
        val failed = findExecution(executionId)
        check(failed.status == ExecutionStatus.FAILED) { "Can only retry FAILED executions" }

        val executionNo = "EXE-${executionSeq.incrementAndGet()}"
        val execution = BatchExecution(
            batchJob = failed.batchJob, executionNo = executionNo,
            parameters = failed.parameters, triggeredBy = "USER"
        ).apply { assignTenant(tenantId) }

        return executionRepository.save(execution).toResponse()
    }

    private fun findJob(id: Long): BatchJob =
        jobRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("BatchJob", id) }

    private fun findExecution(id: Long): BatchExecution =
        executionRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("BatchExecution", id) }
}
