package com.modularerp.batch.controller

import com.modularerp.batch.domain.*
import com.modularerp.batch.dto.*
import com.modularerp.batch.service.BatchService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/batch")
@Tag(name = "Batch Processing")
class BatchController(private val batchService: BatchService) {

    @GetMapping("/jobs")
    fun searchJobs(
        @RequestParam(required = false) jobType: BatchJobType?,
        @RequestParam(required = false) enabled: Boolean?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<BatchJobResponse>> {
        val page = batchService.searchJobs(jobType, enabled, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/jobs/{id}")
    fun getJob(@PathVariable id: Long) = ApiResponse.ok(batchService.getJobById(id))

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    fun createJob(@Valid @RequestBody req: CreateBatchJobRequest) = ApiResponse.ok(batchService.createJob(req))

    @PutMapping("/jobs/{id}")
    fun updateJob(@PathVariable id: Long, @Valid @RequestBody req: UpdateBatchJobRequest) =
        ApiResponse.ok(batchService.updateJob(id, req))

    @PostMapping("/jobs/{id}/enable")
    fun enableJob(@PathVariable id: Long) = ApiResponse.ok(batchService.enableJob(id))

    @PostMapping("/jobs/{id}/disable")
    fun disableJob(@PathVariable id: Long) = ApiResponse.ok(batchService.disableJob(id))

    @PostMapping("/jobs/{id}/execute")
    fun executeJob(@PathVariable id: Long, @RequestBody(required = false) req: ExecuteJobRequest?) =
        ApiResponse.ok(batchService.executeJob(id, req))

    @GetMapping("/jobs/{id}/executions")
    fun getExecutionHistory(@PathVariable id: Long, @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<BatchExecutionResponse>> {
        val page = batchService.getExecutionHistory(id, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/executions/{id}/status")
    fun getExecutionStatus(@PathVariable id: Long) = ApiResponse.ok(batchService.getExecutionStatus(id))

    @PostMapping("/executions/{id}/cancel")
    fun cancelExecution(@PathVariable id: Long) = ApiResponse.ok(batchService.cancelExecution(id))

    @PostMapping("/executions/{id}/retry")
    fun retryExecution(@PathVariable id: Long) = ApiResponse.ok(batchService.retryFailedExecution(id))
}
