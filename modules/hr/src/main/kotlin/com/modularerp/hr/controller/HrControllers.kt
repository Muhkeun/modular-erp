package com.modularerp.hr.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.hr.domain.EmployeeStatus
import com.modularerp.hr.dto.*
import com.modularerp.hr.service.EmployeeService
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/hr/employees")
@Tag(name = "Employees")
class EmployeeController(
    private val employeeService: EmployeeService,
    private val dataScopeService: DataScopeService
) {

    @GetMapping
    fun search(@RequestParam(required = false) status: EmployeeStatus?,
               @RequestParam(required = false) departmentCode: String?,
               @RequestParam(required = false) name: String?,
               authentication: Authentication,
               @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<EmployeeResponse>> {
        val scopeFilter = resolveEmployeeScope(authentication)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else employeeService.search(status, departmentCode, name, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long, authentication: Authentication): ApiResponse<EmployeeResponse> {
        val employee = employeeService.getById(id)
        assertEmployeeAccessible(authentication, employee.companyCode, employee.departmentCode)
        return ApiResponse.ok(employee)
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CreateEmployeeRequest, authentication: Authentication): ApiResponse<EmployeeResponse> {
        assertEmployeeWritable(authentication, req.companyCode, req.departmentCode)
        return ApiResponse.ok(employeeService.create(req))
    }

    private fun resolveEmployeeScope(authentication: Authentication): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            "employees",
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = false,
            supportsCompany = true,
            supportsDepartment = true,
            supportsPlant = false
        )

    private fun assertEmployeeAccessible(authentication: Authentication, companyCode: String?, departmentCode: String?) {
        val scopeFilter = resolveEmployeeScope(authentication)
        if (!scopeFilter.matchesSupported(
                companyCode = companyCode,
                departmentCode = departmentCode,
                supportsOwn = false,
                supportsCompany = true,
                supportsDepartment = true,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertEmployeeWritable(authentication: Authentication, companyCode: String?, departmentCode: String?) {
        val scopeFilter = resolveEmployeeScope(authentication)
        if (!scopeFilter.matchesSupported(
                companyCode = companyCode,
                departmentCode = departmentCode,
                supportsOwn = false,
                supportsCompany = true,
                supportsDepartment = true,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}
