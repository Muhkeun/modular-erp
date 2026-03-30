package com.modularerp.planning.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.planning.dto.*
import com.modularerp.planning.repository.*
import com.modularerp.planning.service.MrpService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/planning/mrp")
@Tag(name = "MRP", description = "Material Requirements Planning")
class MrpController(
    private val mrpService: MrpService,
    private val dataScopeService: DataScopeService
) {

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Execute MRP", description = "Explodes BOMs, nets stock, generates planned purchase/production orders")
    fun run(@RequestBody request: RunMrpRequest, authentication: Authentication): ApiResponse<MrpRunResponse> {
        assertPlantWritable(authentication, dataScopeService, "mrp-runs", request.plantCode)
        return ApiResponse.ok(mrpService.runMrp(request))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<MrpRunResponse> {
        val mrpRun = mrpService.getById(id)
        assertPlantAccessible(authentication, dataScopeService, "mrp-runs", mrpRun.plantCode)
        return ApiResponse.ok(mrpRun)
    }

    @GetMapping
    fun findRecent(authentication: Authentication, @PageableDefault(size = 10) pageable: Pageable): ApiResponse<List<MrpRunResponse>> {
        val scopeFilter = resolvePlantScope(authentication, dataScopeService, "mrp-runs")
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else mrpService.findRecent(scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }
}

@RestController
@RequestMapping("/api/v1/planning/capacity")
@Tag(name = "Capacity Planning", description = "Work center capacity vs load")
class CapacityPlanController(
    private val repo: CapacityPlanRepository,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    @Operation(summary = "Get capacity plan for a plant", description = "Shows available vs planned hours per work center per day")
    fun getCapacity(
        @RequestParam plantCode: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate,
        authentication: Authentication
    ): ApiResponse<List<CapacityPlanResponse>> {
        assertPlantAccessible(authentication, dataScopeService, "capacity-plans", plantCode)
        val result = repo.findCapacity(TenantContext.getTenantId(), plantCode, fromDate, toDate)
        return ApiResponse.ok(result.map { it.toResponse() })
    }
}

@RestController
@RequestMapping("/api/v1/planning/schedule")
@Tag(name = "Production Schedule", description = "Work order scheduling by work center")
class ProductionScheduleController(
    private val repo: ProductionScheduleRepository,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    @Operation(summary = "Get production schedule", description = "Calendar view of scheduled work orders")
    fun getSchedule(
        @RequestParam plantCode: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate,
        @RequestParam(required = false) workCenterCode: String?,
        authentication: Authentication
    ): ApiResponse<List<ScheduleResponse>> {
        assertPlantAccessible(authentication, dataScopeService, "production-schedule", plantCode)
        val result = repo.findSchedule(TenantContext.getTenantId(), plantCode, fromDate, toDate, workCenterCode)
        return ApiResponse.ok(result.map { it.toResponse() })
    }
}

private fun resolvePlantScope(
    authentication: Authentication,
    dataScopeService: DataScopeService,
    resource: String
): DataScopeSearchFilter =
    dataScopeService.resolveSearchFilter(
        authentication.authorities.map { it.authority.removePrefix("ROLE_") },
        resource,
        TenantContext.getUserId()
    ).narrowToSupported(
        supportsOwn = false,
        supportsCompany = false,
        supportsDepartment = false,
        supportsPlant = true
    )

private fun assertPlantAccessible(
    authentication: Authentication,
    dataScopeService: DataScopeService,
    resource: String,
    plantCode: String?
) {
    val scopeFilter = resolvePlantScope(authentication, dataScopeService, resource)
    if (!scopeFilter.matchesSupported(
            plantCode = plantCode,
            supportsOwn = false,
            supportsCompany = false,
            supportsDepartment = false,
            supportsPlant = true
        )) {
        throw ForbiddenException("The current data scope does not allow access to this document")
    }
}

private fun assertPlantWritable(
    authentication: Authentication,
    dataScopeService: DataScopeService,
    resource: String,
    plantCode: String?
) {
    val scopeFilter = resolvePlantScope(authentication, dataScopeService, resource)
    if (!scopeFilter.matchesSupported(
            plantCode = plantCode,
            supportsOwn = false,
            supportsCompany = false,
            supportsDepartment = false,
            supportsPlant = true
        )) {
        throw ForbiddenException("The current data scope does not allow writing this document")
    }
}
