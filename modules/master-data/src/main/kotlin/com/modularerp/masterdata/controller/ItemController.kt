package com.modularerp.masterdata.controller

import com.modularerp.masterdata.domain.ItemType
import com.modularerp.masterdata.dto.CreateItemRequest
import com.modularerp.masterdata.dto.ItemResponse
import com.modularerp.masterdata.dto.UpdateItemRequest
import com.modularerp.masterdata.service.ItemService
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
@RequestMapping("/api/v1/master-data/items")
@Tag(name = "Items", description = "Item master data management")
class ItemController(
    private val itemService: ItemService
) {

    @GetMapping
    @Operation(summary = "Search items with pagination")
    fun search(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) itemType: ItemType?,
        @RequestParam(required = false) itemGroup: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<ItemResponse>> {
        val page = itemService.search(code, itemType, itemGroup, pageable)
        return ApiResponse.ok(
            page.content,
            PageMeta(page.number, page.size, page.totalElements, page.totalPages)
        )
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID")
    fun getById(@PathVariable id: Long): ApiResponse<ItemResponse> =
        ApiResponse.ok(itemService.getById(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new item")
    fun create(@Valid @RequestBody request: CreateItemRequest): ApiResponse<ItemResponse> =
        ApiResponse.ok(itemService.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Update an item")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateItemRequest
    ): ApiResponse<ItemResponse> =
        ApiResponse.ok(itemService.update(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete an item")
    fun delete(@PathVariable id: Long) {
        itemService.delete(id)
    }
}
