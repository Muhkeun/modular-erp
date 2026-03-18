package com.modularerp.hr.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "employees")
class Employee(

    @Column(nullable = false, unique = true, length = 20)
    val employeeNo: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(length = 20)
    var departmentCode: String? = null,

    @Column(length = 100)
    var departmentName: String? = null,

    @Column(length = 50)
    var positionTitle: String? = null,

    @Column(length = 50)
    var jobTitle: String? = null,

    @Column(length = 200)
    var email: String? = null,

    @Column(length = 20)
    var phone: String? = null,

    var hireDate: LocalDate? = null,

    var terminationDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: EmployeeStatus = EmployeeStatus.ACTIVE

) : TenantEntity()

enum class EmployeeStatus { ACTIVE, ON_LEAVE, RESIGNED, TERMINATED }

@Entity
@Table(name = "departments")
class Department(

    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(length = 20)
    var parentCode: String? = null,

    var sortOrder: Int = 0

) : TenantEntity()
