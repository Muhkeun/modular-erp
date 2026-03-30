package com.modularerp.batch.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.batch.domain.*
import com.modularerp.batch.dto.*
import com.modularerp.batch.service.BatchService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/batch")
@Tag(name = "Batch Processing")
class BatchController(
    private val batchService: BatchService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping("/jobs")
    fun searchJobs(
        @RequestParam(required = false) jobType: BatchJobType?,
        @RequestParam(required = false) enabled: Boolean?,
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<BatchJobResponse>> {
        val scopeFilter = resolveBatchScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else batchService.searchJobs(jobType, enabled, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/jobs/{id}")
    fun getJob(@PathVariable id: Long, authentication: Authentication): ApiResponse<BatchJobResponse> {
        val response = batchService.getJobById(id)
        assertBatchAccessible(authentication, response.companyCode, response.departmentCode, response.plantCode)
        return ApiResponse.ok(response)
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    fun createJob(
        @Valid @RequestBody req: CreateBatchJobRequest,
        authentication: Authentication
    ): ApiResponse<BatchJobResponse> {
        assertBatchWritable(authentication, req.companyCode, req.departmentCode, req.plantCode)
        return ApiResponse.ok(batchService.createJob(req))
    }

    @PutMapping("/jobs/{id}")
    fun updateJob(
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateBatchJobRequest,
        authentication: Authentication
    ): ApiResponse<BatchJobResponse> {
        val current = batchService.getJobById(id)
        assertBatchAccessible(authentication, current.companyCode, current.departmentCode, current.plantCode)
        assertBatchWritable(
            authentication,
            req.companyCode ?: current.companyCode,
            req.departmentCode ?: current.departmentCode,
            req.plantCode ?: current.plantCode
        )
        return ApiResponse.ok(batchService.updateJob(id, req))
    }

    @PostMapping("/jobs/{id}/enable")
    fun enableJob(@PathVariable id: Long, authentication: Authentication): ApiResponse<BatchJobResponse> {
        getJob(id, authentication)
        return ApiResponse.ok(batchService.enableJob(id))
    }

    @PostMapping("/jobs/{id}/disable")
    fun disableJob(@PathVariable id: Long, authentication: Authentication): ApiResponse<BatchJobResponse> {
        getJob(id, authentication)
        return ApiResponse.ok(batchService.disableJob(id))
    }

    @PostMapping("/jobs/{id}/execute")
    fun executeJob(
        @PathVariable id: Long,
        @RequestBody(required = false) req: ExecuteJobRequest?,
        authentication: Authentication
    ): ApiResponse<BatchExecutionResponse> {
        getJob(id, authentication)
        return ApiResponse.ok(batchService.executeJob(id, req))
    }

    @GetMapping("/jobs/{id}/executions")
    fun getExecutionHistory(
        @PathVariable id: Long,
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<BatchExecutionResponse>> {
        getJob(id, authentication)
        val page = batchService.getExecutionHistory(id, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/executions/{id}/status")
    fun getExecutionStatus(@PathVariable id: Long, authentication: Authentication): ApiResponse<BatchExecutionResponse> {
        val response = batchService.getExecutionStatus(id)
        assertBatchAccessible(authentication, response.companyCode, response.departmentCode, response.plantCode)
        return ApiResponse.ok(response)
    }

    @PostMapping("/executions/{id}/cancel")
    fun cancelExecution(@PathVariable id: Long, authentication: Authentication): ApiResponse<BatchExecutionResponse> {
        getExecutionStatus(id, authentication)
        return ApiResponse.ok(batchService.cancelExecution(id))
    }

    @PostMapping("/executions/{id}/retry")
    fun retryExecution(@PathVariable id: Long, authentication: Authentication): ApiResponse<BatchExecutionResponse> {
        getExecutionStatus(id, authentication)
        return ApiResponse.ok(batchService.retryFailedExecution(id))
    }

    private fun resolveBatchScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "batch-jobs",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = true,
            supportsDepartment = true,
            supportsPlant = true
        )

    private fun assertBatchAccessible(
        authentication: Authentication,
        companyCode: String?,
        departmentCode: String?,
        plantCode: String?
    ) {
        val scopeFilter = resolveBatchScope(authentication)
        if (!matchesBatchScope(scopeFilter, companyCode, departmentCode, plantCode)) {
            throw ForbiddenException("The current data scope does not allow access to this batch job")
        }
    }

    private fun assertBatchWritable(
        authentication: Authentication,
        companyCode: String?,
        departmentCode: String?,
        plantCode: String?
    ) {
        val scopeFilter = resolveBatchScope(authentication)
        if (!matchesBatchScope(scopeFilter, companyCode, departmentCode, plantCode)) {
            throw ForbiddenException("The current data scope does not allow writing this batch job")
        }
    }

    private fun matchesBatchScope(
        scopeFilter: DataScopeSearchFilter,
        companyCode: String?,
        departmentCode: String?,
        plantCode: String?
    ): Boolean {
        if (scopeFilter.denyAll) return false

        val hasScopedAxis =
            scopeFilter.companyCodes.isNotEmpty() ||
            scopeFilter.departmentCodes.isNotEmpty() ||
            scopeFilter.plantCodes.isNotEmpty()

        if (!hasScopedAxis) return true

        return (departmentCode != null && departmentCode in scopeFilter.departmentCodes) ||
            (plantCode != null && plantCode in scopeFilter.plantCodes) ||
            (companyCode != null && companyCode in scopeFilter.companyCodes)
    }
}
