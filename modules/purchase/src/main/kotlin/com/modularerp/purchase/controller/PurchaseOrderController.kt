package com.modularerp.purchase.controller

import com.modularerp.purchase.domain.PoStatus
import com.modularerp.purchase.dto.*
import com.modularerp.purchase.service.PurchaseOrderService
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
@RequestMapping("/api/v1/purchase/orders")
@Tag(name = "Purchase Orders", description = "Purchase Order (PO) management")
class PurchaseOrderController(
    private val poService: PurchaseOrderService
) {

    @GetMapping
    @Operation(summary = "Search purchase orders")
    fun search(
        @RequestParam(required = false) status: PoStatus?,
        @RequestParam(required = false) vendorCode: String?,
        @RequestParam(required = false) documentNo: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<PoResponse>> {
        val page = poService.search(status, vendorCode, documentNo, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PO by ID")
    fun getById(@PathVariable id: Long): ApiResponse<PoResponse> =
        ApiResponse.ok(poService.getById(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new PO")
    fun create(@Valid @RequestBody request: CreatePoRequest): ApiResponse<PoResponse> =
        ApiResponse.ok(poService.create(request))

    @PostMapping("/from-pr/{prId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create PO from an approved PR")
    fun createFromPr(
        @PathVariable prId: Long,
        @Valid @RequestBody request: CreatePoFromPrRequest
    ): ApiResponse<PoResponse> =
        ApiResponse.ok(poService.createFromPr(prId, request))

    @PostMapping("/{id}/submit")
    fun submit(@PathVariable id: Long): ApiResponse<PoResponse> = ApiResponse.ok(poService.submit(id))

    @PostMapping("/{id}/approve")
    fun approve(@PathVariable id: Long): ApiResponse<PoResponse> = ApiResponse.ok(poService.approve(id))

    @PostMapping("/{id}/reject")
    fun reject(@PathVariable id: Long): ApiResponse<PoResponse> = ApiResponse.ok(poService.reject(id))
}
