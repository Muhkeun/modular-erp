package com.modularerp.quality.controller

import com.modularerp.quality.domain.*
import com.modularerp.quality.dto.*
import com.modularerp.quality.service.QualityInspectionService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/quality/inspections")
@Tag(name = "Quality Inspections")
class QualityInspectionController(private val qiService: QualityInspectionService) {

    @GetMapping
    fun search(@RequestParam(required = false) status: QiStatus?,
               @RequestParam(required = false) inspectionType: InspectionType?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<QiResponse>> {
        val page = qiService.search(status, inspectionType, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}") fun getById(@PathVariable id: Long) = ApiResponse.ok(qiService.getById(id))
    @PostMapping @ResponseStatus(HttpStatus.CREATED) fun create(@RequestBody req: CreateQiRequest) = ApiResponse.ok(qiService.create(req))
    @PostMapping("/{id}/complete") fun complete(@PathVariable id: Long, @RequestBody req: CompleteQiRequest) = ApiResponse.ok(qiService.complete(id, req))
}
