package com.modularerp.planning.controller

import com.modularerp.planning.dto.*
import com.modularerp.planning.repository.*
import com.modularerp.planning.service.MrpService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/planning/mrp")
@Tag(name = "MRP", description = "Material Requirements Planning")
class MrpController(private val mrpService: MrpService) {

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Execute MRP", description = "Explodes BOMs, nets stock, generates planned purchase/production orders")
    fun run(@RequestBody request: RunMrpRequest) = ApiResponse.ok(mrpService.runMrp(request))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = ApiResponse.ok(mrpService.getById(id))

    @GetMapping
    fun findRecent(@PageableDefault(size = 10) pageable: Pageable): ApiResponse<List<MrpRunResponse>> {
        val page = mrpService.findRecent(pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }
}

@RestController
@RequestMapping("/api/v1/planning/capacity")
@Tag(name = "Capacity Planning", description = "Work center capacity vs load")
class CapacityPlanController(private val repo: CapacityPlanRepository) {

    @GetMapping
    @Operation(summary = "Get capacity plan for a plant", description = "Shows available vs planned hours per work center per day")
    fun getCapacity(
        @RequestParam plantCode: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): ApiResponse<List<CapacityPlanResponse>> {
        val result = repo.findCapacity(TenantContext.getTenantId(), plantCode, fromDate, toDate)
        return ApiResponse.ok(result.map { it.toResponse() })
    }
}

@RestController
@RequestMapping("/api/v1/planning/schedule")
@Tag(name = "Production Schedule", description = "Work order scheduling by work center")
class ProductionScheduleController(private val repo: ProductionScheduleRepository) {

    @GetMapping
    @Operation(summary = "Get production schedule", description = "Calendar view of scheduled work orders")
    fun getSchedule(
        @RequestParam plantCode: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate,
        @RequestParam(required = false) workCenterCode: String?
    ): ApiResponse<List<ScheduleResponse>> {
        val result = repo.findSchedule(TenantContext.getTenantId(), plantCode, fromDate, toDate, workCenterCode)
        return ApiResponse.ok(result.map { it.toResponse() })
    }
}
