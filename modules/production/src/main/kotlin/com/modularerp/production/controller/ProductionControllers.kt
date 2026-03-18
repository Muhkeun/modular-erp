package com.modularerp.production.controller

import com.modularerp.production.domain.*
import com.modularerp.production.dto.*
import com.modularerp.production.repository.WorkCenterRepository
import com.modularerp.production.repository.RoutingRepository
import com.modularerp.production.service.WorkOrderService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/production/work-centers")
@Tag(name = "Work Centers", description = "Production resource management")
class WorkCenterController(private val repo: WorkCenterRepository) {

    @GetMapping
    fun search(@RequestParam(required = false) plantCode: String?,
               @PageableDefault(size = 50) pageable: Pageable): ApiResponse<List<WorkCenterResponse>> {
        val page = repo.search(TenantContext.getTenantId(), plantCode, pageable)
        return ApiResponse.ok(page.content.map { it.toResponse() },
            PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateWorkCenterRequest): ApiResponse<WorkCenterResponse> {
        val wc = WorkCenter(
            code = req.code, name = req.name, plantCode = req.plantCode,
            centerType = req.centerType, capacityPerDay = req.capacityPerDay,
            resourceCount = req.resourceCount, costPerHour = req.costPerHour,
            setupCost = req.setupCost, description = req.description
        ).apply { assignTenant(TenantContext.getTenantId()) }
        return ApiResponse.ok(repo.save(wc).toResponse())
    }
}

@RestController
@RequestMapping("/api/v1/production/routings")
@Tag(name = "Routings", description = "Production operation sequences")
class RoutingController(private val repo: RoutingRepository) {

    @GetMapping
    fun search(@RequestParam(required = false) productCode: String?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<RoutingResponse>> {
        val page = repo.search(TenantContext.getTenantId(), productCode, pageable)
        return ApiResponse.ok(page.content.map { it.toResponse() },
            PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateRoutingRequest): ApiResponse<RoutingResponse> {
        val tenantId = TenantContext.getTenantId()
        val routing = Routing(
            productCode = req.productCode, productName = req.productName,
            plantCode = req.plantCode, revision = req.revision, description = req.description
        ).apply { assignTenant(tenantId) }
        req.operations.forEach { op ->
            routing.addOperation(op.operationNo, op.operationName, op.workCenterCode,
                op.setupTime, op.runTimePerUnit, op.description).assignTenant(tenantId)
        }
        return ApiResponse.ok(repo.save(routing).toResponse())
    }

    @PostMapping("/{id}/release")
    fun release(@PathVariable id: Long): ApiResponse<RoutingResponse> {
        val routing = repo.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { com.modularerp.core.exception.EntityNotFoundException("Routing", id) }
        routing.release()
        return ApiResponse.ok(repo.save(routing).toResponse())
    }
}

@RestController
@RequestMapping("/api/v1/production/work-orders")
@Tag(name = "Work Orders", description = "Production order management")
class WorkOrderController(private val woService: WorkOrderService) {

    @GetMapping
    fun search(@RequestParam(required = false) status: WoStatus?,
               @RequestParam(required = false) plantCode: String?,
               @RequestParam(required = false) productCode: String?,
               @RequestParam(required = false) documentNo: String?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<WoResponse>> {
        val page = woService.search(status, plantCode, productCode, documentNo, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = ApiResponse.ok(woService.getById(id))

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create work order", description = "Auto-populates operations from routing and materials from BOM when autoPopulate=true")
    fun create(@Valid @RequestBody req: CreateWorkOrderRequest) = ApiResponse.ok(woService.create(req))

    @PostMapping("/{id}/release") fun release(@PathVariable id: Long) = ApiResponse.ok(woService.release(id))
    @PostMapping("/{id}/start") fun start(@PathVariable id: Long) = ApiResponse.ok(woService.start(id))

    @PostMapping("/{id}/report")
    @Operation(summary = "Report production output", description = "Report good/scrap quantities, optionally per operation")
    fun report(@PathVariable id: Long, @RequestBody req: ReportProductionRequest) = ApiResponse.ok(woService.reportProduction(id, req))

    @PostMapping("/{id}/issue-material")
    @Operation(summary = "Issue material to work order")
    fun issueMaterial(@PathVariable id: Long, @RequestBody req: IssueMaterialRequest) = ApiResponse.ok(woService.issueMaterial(id, req))

    @PostMapping("/{id}/complete") fun complete(@PathVariable id: Long) = ApiResponse.ok(woService.complete(id))
    @PostMapping("/{id}/close") fun close(@PathVariable id: Long) = ApiResponse.ok(woService.close(id))
}
