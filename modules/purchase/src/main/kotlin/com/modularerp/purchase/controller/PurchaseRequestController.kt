package com.modularerp.purchase.controller

import com.modularerp.purchase.domain.PrStatus
import com.modularerp.purchase.dto.*
import com.modularerp.purchase.service.PurchaseRequestService
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
@RequestMapping("/api/v1/purchase/requests")
@Tag(name = "Purchase Requests", description = "Purchase Request (PR) management")
class PurchaseRequestController(
    private val prService: PurchaseRequestService
) {

    @GetMapping
    @Operation(summary = "Search purchase requests")
    fun search(
        @RequestParam(required = false) status: PrStatus?,
        @RequestParam(required = false) companyCode: String?,
        @RequestParam(required = false) documentNo: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<PrResponse>> {
        val page = prService.search(status, companyCode, documentNo, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PR by ID")
    fun getById(@PathVariable id: Long): ApiResponse<PrResponse> =
        ApiResponse.ok(prService.getById(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new PR")
    fun create(@Valid @RequestBody request: CreatePrRequest): ApiResponse<PrResponse> =
        ApiResponse.ok(prService.create(request))

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit PR for approval")
    fun submit(@PathVariable id: Long): ApiResponse<PrResponse> =
        ApiResponse.ok(prService.submit(id))

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve PR")
    fun approve(@PathVariable id: Long): ApiResponse<PrResponse> =
        ApiResponse.ok(prService.approve(id))

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject PR")
    fun reject(@PathVariable id: Long): ApiResponse<PrResponse> =
        ApiResponse.ok(prService.reject(id))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = prService.delete(id)
}
