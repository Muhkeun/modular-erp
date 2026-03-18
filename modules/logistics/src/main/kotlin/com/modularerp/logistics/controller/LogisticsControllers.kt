package com.modularerp.logistics.controller

import com.modularerp.logistics.domain.*
import com.modularerp.logistics.dto.*
import com.modularerp.logistics.repository.StockSummaryRepository
import com.modularerp.logistics.service.*
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/logistics/goods-receipts")
@Tag(name = "Goods Receipts")
class GoodsReceiptController(private val grService: GoodsReceiptService) {

    @GetMapping
    fun search(@RequestParam(required = false) status: GrStatus?,
               @RequestParam(required = false) documentNo: String?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<GrResponse>> {
        val page = grService.search(status, documentNo, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = ApiResponse.ok(grService.getById(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateGrRequest) = ApiResponse.ok(grService.create(req))

    @PostMapping("/{id}/confirm")
    fun confirm(@PathVariable id: Long) = ApiResponse.ok(grService.confirm(id))
}

@RestController
@RequestMapping("/api/v1/logistics/goods-issues")
@Tag(name = "Goods Issues")
class GoodsIssueController(private val giService: GoodsIssueService) {

    @GetMapping
    fun search(@RequestParam(required = false) status: GiStatus?,
               @RequestParam(required = false) documentNo: String?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<GiResponse>> {
        val page = giService.search(status, documentNo, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = ApiResponse.ok(giService.getById(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateGiRequest) = ApiResponse.ok(giService.create(req))

    @PostMapping("/{id}/confirm")
    fun confirm(@PathVariable id: Long) = ApiResponse.ok(giService.confirm(id))
}

@RestController
@RequestMapping("/api/v1/logistics/stock")
@Tag(name = "Stock")
class StockController(private val stockRepository: StockSummaryRepository) {

    @GetMapping
    fun search(@RequestParam(required = false) plantCode: String?,
               @RequestParam(required = false) itemCode: String?,
               @PageableDefault(size = 50) pageable: Pageable): ApiResponse<List<StockResponse>> {
        val page = stockRepository.search(TenantContext.getTenantId(), plantCode, itemCode, pageable)
        return ApiResponse.ok(page.content.map { it.toResponse() },
            PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }
}
