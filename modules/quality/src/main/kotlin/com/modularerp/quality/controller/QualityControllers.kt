package com.modularerp.quality.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.quality.domain.*
import com.modularerp.quality.dto.*
import com.modularerp.quality.service.QualityInspectionService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/quality/inspections")
@Tag(name = "Quality Inspections")
class QualityInspectionController(
    private val qiService: QualityInspectionService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(@RequestParam(required = false) status: QiStatus?,
               @RequestParam(required = false) inspectionType: InspectionType?,
               authentication: Authentication,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<QiResponse>> {
        val scopeFilter = resolveQualityScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else qiService.search(status, inspectionType, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<QiResponse> {
        val inspection = qiService.getById(id)
        assertPlantAccessible(authentication, inspection.plantCode)
        return ApiResponse.ok(inspection)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: CreateQiRequest, authentication: Authentication): ApiResponse<QiResponse> {
        assertPlantWritable(authentication, req.plantCode)
        return ApiResponse.ok(qiService.create(req))
    }

    @PostMapping("/{id}/complete")
    fun complete(
        @PathVariable id: Long,
        @RequestBody req: CompleteQiRequest,
        authentication: Authentication
    ): ApiResponse<QiResponse> {
        val inspection = qiService.getById(id)
        assertPlantAccessible(authentication, inspection.plantCode)
        return ApiResponse.ok(qiService.complete(id, req))
    }

    private fun resolveQualityScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "quality-inspections",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = false,
            supportsDepartment = false,
            supportsPlant = true
        )

    private fun assertPlantAccessible(authentication: Authentication, plantCode: String?) {
        val scopeFilter = resolveQualityScope(authentication)
        if (!scopeFilter.matchesSupported(
                plantCode = plantCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = false,
                supportsPlant = true
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertPlantWritable(authentication: Authentication, plantCode: String?) {
        val scopeFilter = resolveQualityScope(authentication)
        if (!scopeFilter.matchesSupported(
                plantCode = plantCode,
                supportsOwn = false,
                supportsCompany = false,
                supportsDepartment = false,
                supportsPlant = true
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}
