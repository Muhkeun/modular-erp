package com.modularerp.account.controller

import com.modularerp.account.domain.*
import com.modularerp.account.dto.*
import com.modularerp.account.service.JournalEntryService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/account/journal-entries")
@Tag(name = "Journal Entries")
class JournalEntryController(private val jeService: JournalEntryService) {

    @GetMapping
    fun search(@RequestParam(required = false) status: JeStatus?,
               @RequestParam(required = false) entryType: JournalEntryType?,
               @RequestParam(required = false) documentNo: String?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<JeResponse>> {
        val page = jeService.search(status, entryType, documentNo, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = ApiResponse.ok(jeService.getById(id))

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateJeRequest) = ApiResponse.ok(jeService.create(req))

    @PostMapping("/{id}/post") fun post(@PathVariable id: Long) = ApiResponse.ok(jeService.post(id))
    @PostMapping("/{id}/reverse") fun reverse(@PathVariable id: Long) = ApiResponse.ok(jeService.reverse(id))
}
