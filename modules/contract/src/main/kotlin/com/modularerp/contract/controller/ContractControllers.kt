package com.modularerp.contract.controller

import com.modularerp.contract.domain.*
import com.modularerp.contract.dto.*
import com.modularerp.contract.service.ContractService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/contracts")
@Tag(name = "Contracts")
class ContractController(private val service: ContractService) {

    @GetMapping
    fun search(@RequestParam(required = false) status: ContractStatus?,
               @RequestParam(required = false) contractType: ContractType?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<ContractResponse>> {
        val page = service.search(status, contractType, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}") fun getById(@PathVariable id: Long) = ApiResponse.ok(service.getById(id))
    @PostMapping @ResponseStatus(HttpStatus.CREATED) fun create(@RequestBody req: CreateContractRequest) = ApiResponse.ok(service.create(req))
    @PostMapping("/{id}/activate") fun activate(@PathVariable id: Long) = ApiResponse.ok(service.activate(id))
    @PostMapping("/{id}/terminate") fun terminate(@PathVariable id: Long) = ApiResponse.ok(service.terminate(id))
}
