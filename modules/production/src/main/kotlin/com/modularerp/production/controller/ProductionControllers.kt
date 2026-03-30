package com.modularerp.production.controller

import com.modularerp.admin.service.DataScopeService
import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.core.exception.ForbiddenException
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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/production/work-centers")
@Tag(name = "Work Centers", description = "Production resource management")
class WorkCenterController(
    private val repo: WorkCenterRepository,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(@RequestParam(required = false) plantCode: String?,
               authentication: Authentication,
               @PageableDefault(size = 50) pageable: Pageable): ApiResponse<List<WorkCenterResponse>> {
        val scopeFilter = resolvePlantOnlyScope(authentication, dataScopeService, "work-centers")
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else repo.search(
                tenantId = TenantContext.getTenantId(),
                plantCode = plantCode,
                applyPlantScope = scopeFilter.plantCodes.isNotEmpty(),
                plantCodes = scopeFilter.scopedPlantCodes(),
                pageable = pageable
            )
        return ApiResponse.ok(page.content.map { it.toResponse() },
            PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateWorkCenterRequest, authentication: Authentication): ApiResponse<WorkCenterResponse> {
        assertPlantWritable(authentication, dataScopeService, "work-centers", req.plantCode)
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
class RoutingController(
    private val repo: RoutingRepository,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(@RequestParam(required = false) productCode: String?,
               authentication: Authentication,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<RoutingResponse>> {
        val scopeFilter = resolvePlantOnlyScope(authentication, dataScopeService, "routings")
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else repo.search(
                tenantId = TenantContext.getTenantId(),
                productCode = productCode,
                applyPlantScope = scopeFilter.plantCodes.isNotEmpty(),
                plantCodes = scopeFilter.scopedPlantCodes(),
                pageable = pageable
            )
        return ApiResponse.ok(page.content.map { it.toResponse() },
            PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateRoutingRequest, authentication: Authentication): ApiResponse<RoutingResponse> {
        assertPlantWritable(authentication, dataScopeService, "routings", req.plantCode)
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
    fun release(@PathVariable id: Long, authentication: Authentication): ApiResponse<RoutingResponse> {
        val routing = repo.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { com.modularerp.core.exception.EntityNotFoundException("Routing", id) }
        assertPlantAccessible(authentication, dataScopeService, "routings", routing.plantCode)
        routing.release()
        return ApiResponse.ok(repo.save(routing).toResponse())
    }
}

@RestController
@RequestMapping("/api/v1/production/work-orders")
@Tag(name = "Work Orders", description = "Production order management")
class WorkOrderController(
    private val woService: WorkOrderService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(@RequestParam(required = false) status: WoStatus?,
               @RequestParam(required = false) plantCode: String?,
               @RequestParam(required = false) productCode: String?,
               @RequestParam(required = false) documentNo: String?,
               authentication: Authentication,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<WoResponse>> {
        val roles = authentication.authorities.map { it.authority.removePrefix("ROLE_") }
        val scopeFilter = dataScopeService.resolveSearchFilter(roles, "work-orders", TenantContext.getUserId())
        val page = woService.search(status, plantCode, productCode, documentNo, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<WoResponse> {
        val scopeFilter = resolveScope(authentication, "work-orders")
        val response = woService.getById(id)
        assertAccessible(scopeFilter, response.createdBy, response.companyCode, null, response.plantCode)
        return ApiResponse.ok(response)
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create work order", description = "Auto-populates operations from routing and materials from BOM when autoPopulate=true")
    fun create(@Valid @RequestBody req: CreateWorkOrderRequest, authentication: Authentication): ApiResponse<WoResponse> {
        assertWritable(authentication, "work-orders", req.companyCode, null, req.plantCode)
        return ApiResponse.ok(woService.create(req))
    }

    @PostMapping("/{id}/release")
    fun release(@PathVariable id: Long, authentication: Authentication): ApiResponse<WoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(woService.release(id))
    }

    @PostMapping("/{id}/start")
    fun start(@PathVariable id: Long, authentication: Authentication): ApiResponse<WoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(woService.start(id))
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Report production output", description = "Report good/scrap quantities, optionally per operation")
    fun report(@PathVariable id: Long, @RequestBody req: ReportProductionRequest, authentication: Authentication): ApiResponse<WoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(woService.reportProduction(id, req))
    }

    @PostMapping("/{id}/issue-material")
    @Operation(summary = "Issue material to work order")
    fun issueMaterial(@PathVariable id: Long, @RequestBody req: IssueMaterialRequest, authentication: Authentication): ApiResponse<WoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(woService.issueMaterial(id, req))
    }

    @PostMapping("/{id}/complete")
    fun complete(@PathVariable id: Long, authentication: Authentication): ApiResponse<WoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(woService.complete(id))
    }

    @PostMapping("/{id}/close")
    fun close(@PathVariable id: Long, authentication: Authentication): ApiResponse<WoResponse> {
        getById(id, authentication)
        return ApiResponse.ok(woService.close(id))
    }

    private fun resolveScope(authentication: Authentication, resource: String) =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            resource,
            TenantContext.getUserId()
        )

    private fun assertAccessible(
        scopeFilter: DataScopeSearchFilter,
        ownerId: String?,
        companyCode: String?,
        departmentCode: String?,
        plantCode: String?
    ) {
        if (!scopeFilter.matches(ownerId, companyCode, departmentCode, plantCode)) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertWritable(
        authentication: Authentication,
        resource: String,
        companyCode: String?,
        departmentCode: String?,
        plantCode: String?
    ) {
        val scopeFilter = resolveScope(authentication, resource)
        if (!scopeFilter.matches(TenantContext.getUserId(), companyCode, departmentCode, plantCode)) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}

private fun resolveScope(authentication: Authentication, dataScopeService: DataScopeService, resource: String) =
    dataScopeService.resolveSearchFilter(
        authentication.authorities.map { it.authority.removePrefix("ROLE_") },
        resource,
        TenantContext.getUserId()
    )

private fun resolvePlantOnlyScope(
    authentication: Authentication,
    dataScopeService: DataScopeService,
    resource: String
): DataScopeSearchFilter =
    resolveScope(authentication, dataScopeService, resource).narrowToSupported(
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
    val scopeFilter = resolvePlantOnlyScope(authentication, dataScopeService, resource)
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
    val scopeFilter = resolvePlantOnlyScope(authentication, dataScopeService, resource)
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
