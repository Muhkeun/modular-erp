package com.modularerp.supplychain.controller

import com.modularerp.supplychain.dto.*
import com.modularerp.supplychain.service.SupplierEvaluationService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/supply-chain/evaluations")
@Tag(name = "Supplier Evaluations")
class SupplierEvaluationController(private val service: SupplierEvaluationService) {

    @GetMapping
    fun search(@RequestParam(required = false) vendorCode: String?,
               @RequestParam(required = false) period: String?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<EvaluationResponse>> {
        val page = service.search(vendorCode, period, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}") fun getById(@PathVariable id: Long) = ApiResponse.ok(service.getById(id))
    @PostMapping @ResponseStatus(HttpStatus.CREATED) fun create(@RequestBody req: CreateEvaluationRequest) = ApiResponse.ok(service.create(req))
}
