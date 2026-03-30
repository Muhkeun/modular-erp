package com.modularerp.costing.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.costing.domain.*
import com.modularerp.costing.dto.*
import com.modularerp.costing.service.CostingService
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
@RequestMapping("/api/v1/costing")
@Tag(name = "Costing")
class CostingController(
    private val costingService: CostingService,
    private val dataScopeService: DataScopeService
) {

    // ── Cost Centers ──

    @GetMapping("/cost-centers")
    fun searchCostCenters(@RequestParam(required = false) status: CostCenterStatus?,
                          @RequestParam(required = false) costCenterCode: String?,
                          authentication: Authentication,
                          @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<CostCenterResponse>> {
        val scopeFilter = resolveCostCenterScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else costingService.searchCostCenters(status, costCenterCode, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/cost-centers/{id}")
    fun getCostCenter(@PathVariable id: Long, authentication: Authentication): ApiResponse<CostCenterResponse> {
        val costCenter = costingService.getCostCenterById(id)
        assertCostCenterAccessible(authentication, costCenter.departmentCode)
        return ApiResponse.ok(costCenter)
    }

    @PostMapping("/cost-centers") @ResponseStatus(HttpStatus.CREATED)
    fun createCostCenter(@Valid @RequestBody req: CreateCostCenterRequest, authentication: Authentication): ApiResponse<CostCenterResponse> {
        assertCostCenterWritable(authentication, req.departmentCode)
        return ApiResponse.ok(costingService.createCostCenter(req))
    }

    @PutMapping("/cost-centers/{id}")
    fun updateCostCenter(
        @PathVariable id: Long,
        @Valid @RequestBody req: CreateCostCenterRequest,
        authentication: Authentication
    ): ApiResponse<CostCenterResponse> {
        val costCenter = costingService.getCostCenterById(id)
        assertCostCenterAccessible(authentication, costCenter.departmentCode)
        assertCostCenterWritable(authentication, req.departmentCode)
        return ApiResponse.ok(costingService.updateCostCenter(id, req))
    }

    @DeleteMapping("/cost-centers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCostCenter(@PathVariable id: Long, authentication: Authentication) {
        val costCenter = costingService.getCostCenterById(id)
        assertCostCenterAccessible(authentication, costCenter.departmentCode)
        costingService.deleteCostCenter(id)
    }

    // ── Standard Costs ──

    @GetMapping("/standard-costs")
    fun searchStandardCosts(@RequestParam(required = false) itemCode: String?,
                            @RequestParam(required = false) costType: CostType?,
                            authentication: Authentication,
                            @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<StandardCostResponse>> {
        val scopeFilter = resolveStandardCostScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else costingService.searchStandardCosts(itemCode, costType, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/standard-costs/{id}")
    fun getStandardCost(@PathVariable id: Long, authentication: Authentication): ApiResponse<StandardCostResponse> {
        val standardCost = costingService.getStandardCostById(id)
        assertStandardCostAccessible(authentication, standardCost.costCenterCode)
        return ApiResponse.ok(standardCost)
    }

    @PostMapping("/standard-costs") @ResponseStatus(HttpStatus.CREATED)
    fun createStandardCost(
        @Valid @RequestBody req: CreateStandardCostRequest,
        authentication: Authentication
    ): ApiResponse<StandardCostResponse> {
        assertStandardCostWritable(authentication, req.costCenterCode)
        return ApiResponse.ok(costingService.createStandardCost(req))
    }

    @PutMapping("/standard-costs/{id}")
    fun updateStandardCost(
        @PathVariable id: Long,
        @Valid @RequestBody req: CreateStandardCostRequest,
        authentication: Authentication
    ): ApiResponse<StandardCostResponse> {
        val standardCost = costingService.getStandardCostById(id)
        assertStandardCostAccessible(authentication, standardCost.costCenterCode)
        assertStandardCostWritable(authentication, req.costCenterCode)
        return ApiResponse.ok(costingService.updateStandardCost(id, req))
    }

    // ── Product Costs ──

    @GetMapping("/product-costs")
    fun searchProductCosts(@RequestParam(required = false) itemCode: String?,
                           @RequestParam(required = false) fiscalYear: Int?,
                           authentication: Authentication,
                           @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<ProductCostResponse>> {
        val scopeFilter = resolveProductCostScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else costingService.searchProductCosts(itemCode, fiscalYear, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping("/product-costs/calculate") @ResponseStatus(HttpStatus.CREATED)
    fun calculateProductCost(
        @Valid @RequestBody req: CalculateProductCostRequest,
        authentication: Authentication
    ): ApiResponse<ProductCostResponse> {
        assertProductCostWritable(authentication, req.costCenterCode)
        return ApiResponse.ok(costingService.calculateProductCost(req))
    }

    // ── Allocations ──

    @GetMapping("/allocations")
    fun searchAllocations(@RequestParam(required = false) status: CostAllocationStatus?,
                          @RequestParam(required = false) fiscalYear: Int?,
                          authentication: Authentication,
                          @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<CostAllocationResponse>> {
        val scopeFilter = resolveCostCenterScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else costingService.searchAllocations(status, fiscalYear, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping("/allocations") @ResponseStatus(HttpStatus.CREATED)
    fun createAllocation(
        @Valid @RequestBody req: CreateCostAllocationRequest,
        authentication: Authentication
    ): ApiResponse<CostAllocationResponse> {
        assertAllocationWritable(authentication, req.fromCostCenter, req.toCostCenter)
        return ApiResponse.ok(costingService.createAllocation(req))
    }

    @PostMapping("/allocations/{id}/post")
    fun postAllocation(@PathVariable id: Long, authentication: Authentication): ApiResponse<CostAllocationResponse> {
        val allocation = costingService.getAllocationById(id)
        assertAllocationAccessible(authentication, allocation.fromCostCenter, allocation.toCostCenter)
        return ApiResponse.ok(costingService.postAllocation(id))
    }

    // ── Variance ──

    @GetMapping("/variance")
    fun getVariance(@RequestParam itemCode: String) = ApiResponse.ok(costingService.getVarianceAnalysis(itemCode))

    private fun resolveCostCenterScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "cost-centers",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = false,
            supportsDepartment = true,
            supportsPlant = false
        )

    private fun assertCostCenterAccessible(authentication: Authentication, departmentCode: String?) {
        val scopeFilter = resolveCostCenterScope(authentication)
        if (!scopeFilter.matchesSupported(
                departmentCode = departmentCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = true,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertCostCenterWritable(authentication: Authentication, departmentCode: String?) {
        val scopeFilter = resolveCostCenterScope(authentication)
        if (!scopeFilter.matchesSupported(
                departmentCode = departmentCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = true,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }

    private fun resolveStandardCostScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "standard-costs",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = false,
            supportsDepartment = true,
            supportsPlant = false
        )

    private fun resolveProductCostScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "product-costs",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = false,
            supportsDepartment = true,
            supportsPlant = false
        )

    private fun assertStandardCostAccessible(authentication: Authentication, costCenterCode: String?) {
        val scopeFilter = resolveStandardCostScope(authentication)
        val departmentCode = costCenterCode?.let { costingService.getCostCenterByCode(it).departmentCode }
        if (!scopeFilter.matchesSupported(
                departmentCode = departmentCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = true,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertStandardCostWritable(authentication: Authentication, costCenterCode: String?) {
        val scopeFilter = resolveStandardCostScope(authentication)
        val departmentCode = costCenterCode?.let { costingService.getCostCenterByCode(it).departmentCode }
        if (!scopeFilter.matchesSupported(
                departmentCode = departmentCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = true,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }

    private fun assertProductCostWritable(authentication: Authentication, costCenterCode: String?) {
        val scopeFilter = resolveProductCostScope(authentication)
        val departmentCode = costCenterCode?.let { costingService.getCostCenterByCode(it).departmentCode }
        if (!scopeFilter.matchesSupported(
                departmentCode = departmentCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = true,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }

    private fun assertAllocationAccessible(authentication: Authentication, fromCostCenter: String, toCostCenter: String) {
        val from = costingService.getCostCenterByCode(fromCostCenter)
        val to = costingService.getCostCenterByCode(toCostCenter)
        assertCostCenterAccessible(authentication, from.departmentCode)
        assertCostCenterAccessible(authentication, to.departmentCode)
    }

    private fun assertAllocationWritable(authentication: Authentication, fromCostCenter: String, toCostCenter: String) {
        val from = costingService.getCostCenterByCode(fromCostCenter)
        val to = costingService.getCostCenterByCode(toCostCenter)
        assertCostCenterWritable(authentication, from.departmentCode)
        assertCostCenterWritable(authentication, to.departmentCode)
    }
}
