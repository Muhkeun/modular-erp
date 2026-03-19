package com.modularerp.asset.controller

import com.modularerp.asset.domain.*
import com.modularerp.asset.dto.*
import com.modularerp.asset.service.AssetService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Asset Management")
class AssetController(private val assetService: AssetService) {

    @GetMapping
    fun search(
        @RequestParam(required = false) status: AssetStatus?,
        @RequestParam(required = false) category: AssetCategory?,
        @RequestParam(required = false) name: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<AssetResponse>> {
        val page = assetService.search(status, category, name, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = ApiResponse.ok(assetService.getById(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody req: CreateAssetRequest) = ApiResponse.ok(assetService.registerAsset(req))

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: UpdateAssetRequest) =
        ApiResponse.ok(assetService.updateAsset(id, req))

    @PostMapping("/{id}/activate")
    fun activate(@PathVariable id: Long) = ApiResponse.ok(assetService.activateAsset(id))

    @PostMapping("/{id}/dispose")
    fun dispose(@PathVariable id: Long, @Valid @RequestBody req: DisposeAssetRequest) =
        ApiResponse.ok(assetService.disposeAsset(id, req))

    @PostMapping("/depreciation/run")
    fun runDepreciation(@Valid @RequestBody req: RunDepreciationRequest) =
        ApiResponse.ok(assetService.runDepreciation(req.year, req.month))

    @GetMapping("/{id}/schedule")
    fun getSchedule(@PathVariable id: Long) = ApiResponse.ok(assetService.getDepreciationSchedule(id))

    @GetMapping("/summary")
    fun getSummary() = ApiResponse.ok(assetService.getAssetSummary())
}
