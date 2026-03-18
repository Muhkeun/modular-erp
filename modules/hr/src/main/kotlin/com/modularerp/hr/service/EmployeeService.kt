package com.modularerp.hr.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.hr.domain.*
import com.modularerp.hr.dto.*
import com.modularerp.hr.repository.EmployeeRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class EmployeeService(private val employeeRepository: EmployeeRepository) {

    fun getById(id: Long): EmployeeResponse =
        employeeRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("Employee", id) }.toResponse()

    fun search(status: EmployeeStatus?, departmentCode: String?, name: String?, pageable: Pageable): Page<EmployeeResponse> =
        employeeRepository.search(TenantContext.getTenantId(), status, departmentCode, name, pageable).map { it.toResponse() }

    @Transactional
    fun create(request: CreateEmployeeRequest): EmployeeResponse {
        val tenantId = TenantContext.getTenantId()
        val employee = Employee(
            employeeNo = request.employeeNo, name = request.name, companyCode = request.companyCode,
            departmentCode = request.departmentCode, departmentName = request.departmentName,
            positionTitle = request.positionTitle, jobTitle = request.jobTitle,
            email = request.email, phone = request.phone, hireDate = request.hireDate
        ).apply { assignTenant(tenantId) }
        return employeeRepository.save(employee).toResponse()
    }
}
