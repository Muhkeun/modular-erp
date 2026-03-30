package com.modularerp.periodclose.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.periodclose.domain.*
import com.modularerp.periodclose.dto.*
import com.modularerp.periodclose.service.PeriodCloseService
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
@RequestMapping("/api/v1/period-close")
@Tag(name = "Period Close")
class PeriodCloseController(
    private val periodCloseService: PeriodCloseService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping("/periods")
    fun searchPeriods(
        @RequestParam(required = false) fiscalYear: Int?,
        @RequestParam(required = false) status: FiscalPeriodStatus?,
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<FiscalPeriodResponse>> {
        val scopeFilter = resolvePeriodScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else periodCloseService.searchPeriods(fiscalYear, status, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/periods/{id}")
    fun getPeriodById(@PathVariable id: Long, authentication: Authentication): ApiResponse<FiscalPeriodResponse> {
        val period = periodCloseService.getPeriodById(id)
        assertPeriodAccessible(authentication, period.companyCode)
        return ApiResponse.ok(period)
    }

    @PostMapping("/periods/generate")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateFiscalYear(
        @Valid @RequestBody req: GenerateFiscalYearRequest,
        authentication: Authentication
    ): ApiResponse<List<FiscalPeriodResponse>> {
        assertPeriodWritable(authentication, req.companyCode)
        return ApiResponse.ok(periodCloseService.createFiscalYear(req.companyCode, req.fiscalYear))
    }

    @PostMapping("/periods/{id}/soft-close")
    fun softClose(@PathVariable id: Long, authentication: Authentication): ApiResponse<FiscalPeriodResponse> {
        getPeriodById(id, authentication)
        return ApiResponse.ok(periodCloseService.softClosePeriod(id))
    }

    @PostMapping("/periods/{id}/hard-close")
    fun hardClose(@PathVariable id: Long, authentication: Authentication): ApiResponse<FiscalPeriodResponse> {
        getPeriodById(id, authentication)
        return ApiResponse.ok(periodCloseService.hardClosePeriod(id))
    }

    @PostMapping("/periods/{id}/reopen")
    fun reopen(@PathVariable id: Long, authentication: Authentication): ApiResponse<FiscalPeriodResponse> {
        getPeriodById(id, authentication)
        return ApiResponse.ok(periodCloseService.reopenPeriod(id))
    }

    @GetMapping("/periods/{id}/checklist")
    fun getChecklist(@PathVariable id: Long, authentication: Authentication): ApiResponse<List<PeriodCloseTaskResponse>> {
        getPeriodById(id, authentication)
        return ApiResponse.ok(periodCloseService.getCloseChecklist(id))
    }

    @PostMapping("/periods/{periodId}/tasks/{taskId}/execute")
    fun executeTask(
        @PathVariable periodId: Long,
        @PathVariable taskId: Long,
        authentication: Authentication
    ): ApiResponse<PeriodCloseTaskResponse> {
        getPeriodById(periodId, authentication)
        return ApiResponse.ok(periodCloseService.executeCloseTask(periodId, taskId))
    }

    @PostMapping("/closing-entries")
    @ResponseStatus(HttpStatus.CREATED)
    fun createClosingEntry(
        @Valid @RequestBody req: CreateClosingEntryRequest,
        authentication: Authentication
    ): ApiResponse<ClosingEntryResponse> {
        val period = periodCloseService.getPeriodById(req.fiscalPeriodId)
        assertPeriodWritable(authentication, period.companyCode)
        return ApiResponse.ok(periodCloseService.createClosingEntry(req))
    }

    @GetMapping("/periods/{id}/closing-entries")
    fun getClosingEntries(
        @PathVariable id: Long,
        authentication: Authentication,
        @PageableDefault(size = 50) pageable: Pageable
    ): ApiResponse<List<ClosingEntryResponse>> {
        getPeriodById(id, authentication)
        val page = periodCloseService.getClosingEntries(id, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    private fun resolvePeriodScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "period-close",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = true,
            supportsDepartment = false,
            supportsPlant = false
        )

    private fun assertPeriodAccessible(authentication: Authentication, companyCode: String?) {
        val scopeFilter = resolvePeriodScope(authentication)
        if (!scopeFilter.matchesSupported(
                companyCode = companyCode,
                supportsOwn = false,
                supportsCompany = true,
                supportsDepartment = false,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertPeriodWritable(authentication: Authentication, companyCode: String?) {
        val scopeFilter = resolvePeriodScope(authentication)
        if (!scopeFilter.matchesSupported(
                companyCode = companyCode,
                supportsOwn = false,
                supportsCompany = true,
                supportsDepartment = false,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}
