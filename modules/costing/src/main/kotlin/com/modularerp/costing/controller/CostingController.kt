package com.modularerp.costing.controller

import com.modularerp.costing.domain.*
import com.modularerp.costing.dto.*
import com.modularerp.costing.service.CostingService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/costing")
@Tag(name = "Costing")
class CostingController(private val costingService: CostingService) {

    // ── Cost Centers ──

    @GetMapping("/cost-centers")
    fun searchCostCenters(@RequestParam(required = false) status: CostCenterStatus?,
                          @RequestParam(required = false) costCenterCode: String?,
                          @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<CostCenterResponse>> {
        val page = costingService.searchCostCenters(status, costCenterCode, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/cost-centers/{id}")
    fun getCostCenter(@PathVariable id: Long) = ApiResponse.ok(costingService.getCostCenterById(id))

    @PostMapping("/cost-centers") @ResponseStatus(HttpStatus.CREATED)
    fun createCostCenter(@Valid @RequestBody req: CreateCostCenterRequest) =
        ApiResponse.ok(costingService.createCostCenter(req))

    @PutMapping("/cost-centers/{id}")
    fun updateCostCenter(@PathVariable id: Long, @Valid @RequestBody req: CreateCostCenterRequest) =
        ApiResponse.ok(costingService.updateCostCenter(id, req))

    @DeleteMapping("/cost-centers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCostCenter(@PathVariable id: Long) = costingService.deleteCostCenter(id)

    // ── Standard Costs ──

    @GetMapping("/standard-costs")
    fun searchStandardCosts(@RequestParam(required = false) itemCode: String?,
                            @RequestParam(required = false) costType: CostType?,
                            @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<StandardCostResponse>> {
        val page = costingService.searchStandardCosts(itemCode, costType, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/standard-costs/{id}")
    fun getStandardCost(@PathVariable id: Long) = ApiResponse.ok(costingService.getStandardCostById(id))

    @PostMapping("/standard-costs") @ResponseStatus(HttpStatus.CREATED)
    fun createStandardCost(@Valid @RequestBody req: CreateStandardCostRequest) =
        ApiResponse.ok(costingService.createStandardCost(req))

    @PutMapping("/standard-costs/{id}")
    fun updateStandardCost(@PathVariable id: Long, @Valid @RequestBody req: CreateStandardCostRequest) =
        ApiResponse.ok(costingService.updateStandardCost(id, req))

    // ── Product Costs ──

    @GetMapping("/product-costs")
    fun searchProductCosts(@RequestParam(required = false) itemCode: String?,
                           @RequestParam(required = false) fiscalYear: Int?,
                           @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<ProductCostResponse>> {
        val page = costingService.searchProductCosts(itemCode, fiscalYear, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping("/product-costs/calculate") @ResponseStatus(HttpStatus.CREATED)
    fun calculateProductCost(@Valid @RequestBody req: CalculateProductCostRequest) =
        ApiResponse.ok(costingService.calculateProductCost(req))

    // ── Allocations ──

    @GetMapping("/allocations")
    fun searchAllocations(@RequestParam(required = false) status: CostAllocationStatus?,
                          @RequestParam(required = false) fiscalYear: Int?,
                          @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<CostAllocationResponse>> {
        val page = costingService.searchAllocations(status, fiscalYear, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping("/allocations") @ResponseStatus(HttpStatus.CREATED)
    fun createAllocation(@Valid @RequestBody req: CreateCostAllocationRequest) =
        ApiResponse.ok(costingService.createAllocation(req))

    @PostMapping("/allocations/{id}/post")
    fun postAllocation(@PathVariable id: Long) = ApiResponse.ok(costingService.postAllocation(id))

    // ── Variance ──

    @GetMapping("/variance")
    fun getVariance(@RequestParam itemCode: String) = ApiResponse.ok(costingService.getVarianceAnalysis(itemCode))
}
