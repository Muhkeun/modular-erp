package com.modularerp.sales.controller

import com.modularerp.sales.domain.SoStatus
import com.modularerp.sales.dto.*
import com.modularerp.sales.service.SalesOrderService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/sales/orders")
@Tag(name = "Sales Orders")
class SalesOrderController(private val soService: SalesOrderService) {

    @GetMapping
    fun search(@RequestParam(required = false) status: SoStatus?,
               @RequestParam(required = false) customerCode: String?,
               @RequestParam(required = false) documentNo: String?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<SoResponse>> {
        val page = soService.search(status, customerCode, documentNo, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = ApiResponse.ok(soService.getById(id))

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateSoRequest) = ApiResponse.ok(soService.create(req))

    @PostMapping("/{id}/submit") fun submit(@PathVariable id: Long) = ApiResponse.ok(soService.submit(id))
    @PostMapping("/{id}/approve") fun approve(@PathVariable id: Long) = ApiResponse.ok(soService.approve(id))
    @PostMapping("/{id}/reject") fun reject(@PathVariable id: Long) = ApiResponse.ok(soService.reject(id))
}
