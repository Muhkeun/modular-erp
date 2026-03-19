package com.modularerp.budget.controller

import com.modularerp.budget.domain.*
import com.modularerp.budget.dto.*
import com.modularerp.budget.service.BudgetService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/budgets")
@Tag(name = "Budget Management")
class BudgetController(private val budgetService: BudgetService) {

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
        @PageableDefault(size = 50) pageable: Pageable
    ): ApiResponse<List<BudgetItemResponse>> {
        val page = budgetService.getItemsByPeriod(periodId, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/items/{id}")
    fun getItemById(@PathVariable id: Long) = ApiResponse.ok(budgetService.getItemById(id))

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun createItem(@Valid @RequestBody req: CreateBudgetItemRequest) =
        ApiResponse.ok(budgetService.createBudgetItem(req))

    @PutMapping("/items/{id}")
    fun updateItem(@PathVariable id: Long, @Valid @RequestBody req: UpdateBudgetItemRequest) =
        ApiResponse.ok(budgetService.updateBudgetItem(id, req))

    // --- Budget Transfer ---

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    fun transferBudget(@Valid @RequestBody req: CreateBudgetTransferRequest) =
        ApiResponse.ok(budgetService.transferBudget(req))

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
}
