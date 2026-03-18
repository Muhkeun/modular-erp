package com.modularerp.hr.controller

import com.modularerp.hr.domain.EmployeeStatus
import com.modularerp.hr.dto.*
import com.modularerp.hr.service.EmployeeService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/hr/employees")
@Tag(name = "Employees")
class EmployeeController(private val employeeService: EmployeeService) {

    @GetMapping
    fun search(@RequestParam(required = false) status: EmployeeStatus?,
               @RequestParam(required = false) departmentCode: String?,
               @RequestParam(required = false) name: String?,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<EmployeeResponse>> {
        val page = employeeService.search(status, departmentCode, name, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = ApiResponse.ok(employeeService.getById(id))

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateEmployeeRequest) = ApiResponse.ok(employeeService.create(req))
}
