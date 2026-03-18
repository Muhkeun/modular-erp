package com.modularerp.hr.dto

import com.modularerp.hr.domain.*
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateEmployeeRequest(
    @field:NotBlank val employeeNo: String,
    @field:NotBlank val name: String,
    @field:NotBlank val companyCode: String,
    val departmentCode: String? = null,
    val departmentName: String? = null,
    val positionTitle: String? = null,
    val jobTitle: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val hireDate: LocalDate? = null
)

data class EmployeeResponse(
    val id: Long, val employeeNo: String, val name: String, val companyCode: String,
    val departmentCode: String?, val departmentName: String?,
    val positionTitle: String?, val jobTitle: String?,
    val email: String?, val phone: String?,
    val hireDate: LocalDate?, val terminationDate: LocalDate?,
    val status: EmployeeStatus, val active: Boolean
)

fun Employee.toResponse() = EmployeeResponse(
    id = id, employeeNo = employeeNo, name = name, companyCode = companyCode,
    departmentCode = departmentCode, departmentName = departmentName,
    positionTitle = positionTitle, jobTitle = jobTitle,
    email = email, phone = phone, hireDate = hireDate,
    terminationDate = terminationDate, status = status, active = active
)
