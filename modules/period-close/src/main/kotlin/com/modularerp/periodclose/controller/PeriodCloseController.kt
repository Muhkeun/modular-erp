package com.modularerp.periodclose.controller

import com.modularerp.periodclose.domain.*
import com.modularerp.periodclose.dto.*
import com.modularerp.periodclose.service.PeriodCloseService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/period-close")
@Tag(name = "Period Close")
class PeriodCloseController(private val periodCloseService: PeriodCloseService) {

    @GetMapping("/periods")
    fun searchPeriods(
        @RequestParam(required = false) fiscalYear: Int?,
        @RequestParam(required = false) status: FiscalPeriodStatus?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<FiscalPeriodResponse>> {
        val page = periodCloseService.searchPeriods(fiscalYear, status, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/periods/{id}")
    fun getPeriodById(@PathVariable id: Long) = ApiResponse.ok(periodCloseService.getPeriodById(id))

    @PostMapping("/periods/generate")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateFiscalYear(@Valid @RequestBody req: GenerateFiscalYearRequest) =
        ApiResponse.ok(periodCloseService.createFiscalYear(req.fiscalYear))

    @PostMapping("/periods/{id}/soft-close")
    fun softClose(@PathVariable id: Long) = ApiResponse.ok(periodCloseService.softClosePeriod(id))

    @PostMapping("/periods/{id}/hard-close")
    fun hardClose(@PathVariable id: Long) = ApiResponse.ok(periodCloseService.hardClosePeriod(id))

    @PostMapping("/periods/{id}/reopen")
    fun reopen(@PathVariable id: Long) = ApiResponse.ok(periodCloseService.reopenPeriod(id))

    @GetMapping("/periods/{id}/checklist")
    fun getChecklist(@PathVariable id: Long) = ApiResponse.ok(periodCloseService.getCloseChecklist(id))

    @PostMapping("/periods/{periodId}/tasks/{taskId}/execute")
    fun executeTask(@PathVariable periodId: Long, @PathVariable taskId: Long) =
        ApiResponse.ok(periodCloseService.executeCloseTask(periodId, taskId))

    @PostMapping("/closing-entries")
    @ResponseStatus(HttpStatus.CREATED)
    fun createClosingEntry(@Valid @RequestBody req: CreateClosingEntryRequest) =
        ApiResponse.ok(periodCloseService.createClosingEntry(req))

    @GetMapping("/periods/{id}/closing-entries")
    fun getClosingEntries(
        @PathVariable id: Long,
        @PageableDefault(size = 50) pageable: Pageable
    ): ApiResponse<List<ClosingEntryResponse>> {
        val page = periodCloseService.getClosingEntries(id, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }
}
