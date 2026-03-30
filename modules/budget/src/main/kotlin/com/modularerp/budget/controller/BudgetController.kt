package com.modularerp.budget.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.budget.domain.*
import com.modularerp.budget.dto.*
import com.modularerp.budget.service.BudgetService
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
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/budgets")
@Tag(name = "Budget Management")
class BudgetController(
    private val budgetService: BudgetService,
    private val dataScopeService: DataScopeService
) {

    // --- Budget Periods ---

    @GetMapping("/periods")
    fun searchPeriods(
        @RequestParam(required = false) status: BudgetPeriodStatus?,
        @RequestParam(required = false) fiscalYear: Int?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<BudgetPeriodResponse>> {
        val page = budgetService.searchPeriods(status, fiscalYear, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/periods/{id}")
    fun getPeriodById(@PathVariable id: Long) = ApiResponse.ok(budgetService.getPeriodById(id))

    @PostMapping("/periods")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPeriod(@Valid @RequestBody req: CreateBudgetPeriodRequest) =
        ApiResponse.ok(budgetService.createBudgetPeriod(req))

    @PutMapping("/periods/{id}")
    fun updatePeriod(@PathVariable id: Long, @Valid @RequestBody req: UpdateBudgetPeriodRequest) =
        ApiResponse.ok(budgetService.updateBudgetPeriod(id, req))

    @PostMapping("/periods/{id}/approve")
    fun approvePeriod(@PathVariable id: Long) = ApiResponse.ok(budgetService.approvePeriod(id))

    @PostMapping("/periods/{id}/close")
    fun closePeriod(@PathVariable id: Long) = ApiResponse.ok(budgetService.closeBudgetPeriod(id))

    // --- Budget Items ---

    @GetMapping("/periods/{periodId}/items")
    fun getItemsByPeriod(
        @PathVariable periodId: Long,
        authentication: Authentication,
        @PageableDefault(size = 50) pageable: Pageable
    ): ApiResponse<List<BudgetItemResponse>> {
        val scopeFilter = resolveBudgetItemScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else budgetService.getItemsByPeriod(periodId, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/items/{id}")
    fun getItemById(@PathVariable id: Long, authentication: Authentication): ApiResponse<BudgetItemResponse> {
        val item = budgetService.getItemById(id)
        assertBudgetItemAccessible(authentication, item.departmentCode, item.plantCode)
        return ApiResponse.ok(item)
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun createItem(@Valid @RequestBody req: CreateBudgetItemRequest, authentication: Authentication) =
        ApiResponse.ok(createBudgetItemWithScopeGuard(authentication, req))

    private fun createBudgetItemWithScopeGuard(authentication: Authentication, req: CreateBudgetItemRequest): BudgetItemResponse {
        assertBudgetItemWritable(authentication, req.departmentCode, req.plantCode)
        return budgetService.createBudgetItem(req)
    }

    @PutMapping("/items/{id}")
    fun updateItem(
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateBudgetItemRequest,
        authentication: Authentication
    ): ApiResponse<BudgetItemResponse> {
        val item = budgetService.getItemById(id)
        assertBudgetItemAccessible(authentication, item.departmentCode, item.plantCode)
        return ApiResponse.ok(budgetService.updateBudgetItem(id, req))
    }

    // --- Budget Transfer ---

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    fun transferBudget(
        @Valid @RequestBody req: CreateBudgetTransferRequest,
        authentication: Authentication
    ): ApiResponse<BudgetTransferResponse> {
        val fromItem = budgetService.getItemById(req.fromBudgetItemId)
        val toItem = budgetService.getItemById(req.toBudgetItemId)
        assertBudgetItemAccessible(authentication, fromItem.departmentCode, fromItem.plantCode)
        assertBudgetItemWritable(authentication, toItem.departmentCode, toItem.plantCode)
        return ApiResponse.ok(budgetService.transferBudget(req))
    }

    // --- Analysis ---

    @GetMapping("/analysis")
    fun getBudgetVsActual(
        @RequestParam periodId: Long,
        @PageableDefault(size = 50) pageable: Pageable
    ): ApiResponse<List<BudgetAnalysisResponse>> {
        val page = budgetService.getBudgetVsActual(periodId, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/availability")
    fun checkAvailability(
        @RequestParam accountCode: String,
        @RequestParam amount: BigDecimal
    ) = ApiResponse.ok(budgetService.checkBudgetAvailability(accountCode, amount))

    private fun resolveBudgetItemScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "budget-items",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = false,
            supportsDepartment = true,
            supportsPlant = true
        )

    private fun assertBudgetItemAccessible(
        authentication: Authentication,
        departmentCode: String?,
        plantCode: String?
    ) {
        val scopeFilter = resolveBudgetItemScope(authentication)
        if (!scopeFilter.matchesSupported(
                departmentCode = departmentCode,
                plantCode = plantCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = true,
                supportsPlant = true
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertBudgetItemWritable(
        authentication: Authentication,
        departmentCode: String?,
        plantCode: String?
    ) {
        val scopeFilter = resolveBudgetItemScope(authentication)
        if (!scopeFilter.matchesSupported(
                departmentCode = departmentCode,
                plantCode = plantCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = true,
                supportsPlant = true
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}
