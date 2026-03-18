package com.modularerp.masterdata.controller

import com.modularerp.masterdata.domain.BomStatus
import com.modularerp.masterdata.dto.*
import com.modularerp.masterdata.service.BomService
import com.modularerp.masterdata.service.ExplodedBomLine
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/master-data/boms")
@Tag(name = "Bill of Materials", description = "BOM management and explosion")
class BomController(private val bomService: BomService) {

    @GetMapping
    fun search(@RequestParam(required = false) productCode: String?,
               @RequestParam(required = false) status: BomStatus?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<BomResponse>> {
        val page = bomService.search(productCode, status, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = ApiResponse.ok(bomService.getById(id))

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateBomRequest) = ApiResponse.ok(bomService.create(req))

    @PostMapping("/{id}/release")
    fun release(@PathVariable id: Long) = ApiResponse.ok(bomService.release(id))

    @GetMapping("/explode")
    @Operation(summary = "Multi-level BOM explosion", description = "Recursively explodes BOM for a product, handling phantom items")
    fun explode(
        @RequestParam productCode: String,
        @RequestParam plantCode: String,
        @RequestParam(defaultValue = "1") quantity: BigDecimal
    ): ApiResponse<List<ExplodedBomLine>> =
        ApiResponse.ok(bomService.explode(productCode, plantCode, quantity))
}
