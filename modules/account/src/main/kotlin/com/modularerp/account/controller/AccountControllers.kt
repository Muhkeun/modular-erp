package com.modularerp.account.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.account.domain.*
import com.modularerp.account.dto.*
import com.modularerp.account.service.JournalEntryService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/account/journal-entries")
@Tag(name = "Journal Entries")
class JournalEntryController(
    private val jeService: JournalEntryService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(
        @RequestParam(required = false) status: JeStatus?,
        @RequestParam(required = false) entryType: JournalEntryType?,
        @RequestParam(required = false) documentNo: String?,
        authentication: Authentication,
        @PageableDefault(size = 20) pageable: Pageable
    ): ApiResponse<List<JeResponse>> {
        val scopeFilter = resolveJournalEntryScope(authentication)
        val page = jeService.search(status, entryType, documentNo, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<JeResponse> {
        val response = jeService.getById(id)
        assertJournalEntryAccessible(authentication, response.companyCode)
        return ApiResponse.ok(response)
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateJeRequest, authentication: Authentication): ApiResponse<JeResponse> {
        assertJournalEntryWritable(authentication, req.companyCode)
        return ApiResponse.ok(jeService.create(req))
    }

    @PostMapping("/{id}/post")
    fun post(@PathVariable id: Long, authentication: Authentication): ApiResponse<JeResponse> {
        getById(id, authentication)
        return ApiResponse.ok(jeService.post(id))
    }

    @PostMapping("/{id}/reverse")
    fun reverse(@PathVariable id: Long, authentication: Authentication): ApiResponse<JeResponse> {
        getById(id, authentication)
        return ApiResponse.ok(jeService.reverse(id))
    }

    private fun resolveJournalEntryScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "journal-entries",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = true,
            supportsDepartment = false,
            supportsPlant = false
        )

    private fun assertJournalEntryAccessible(authentication: Authentication, companyCode: String?) {
        val scopeFilter = resolveJournalEntryScope(authentication)
        if (!scopeFilter.matchesSupported(
                companyCode = companyCode,
                supportsOwn = false,
                supportsCompany = true,
                supportsDepartment = false,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertJournalEntryWritable(authentication: Authentication, companyCode: String?) {
        val scopeFilter = resolveJournalEntryScope(authentication)
        if (!scopeFilter.matchesSupported(
                companyCode = companyCode,
                supportsOwn = false,
                supportsCompany = true,
                supportsDepartment = false,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}
