package com.modularerp.hr.repository

import com.modularerp.hr.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface EmployeeRepository : JpaRepository<Employee, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Employee>

    @Query("""
        SELECT e FROM Employee e WHERE e.tenantId = :tenantId AND e.active = true
        AND (:status IS NULL OR e.status = :status)
        AND (:departmentCode IS NULL OR e.departmentCode = :departmentCode)
        AND (:name IS NULL OR e.name LIKE %:name%)
        ORDER BY e.employeeNo
    """)
    fun search(tenantId: String, status: EmployeeStatus?, departmentCode: String?,
               name: String?, pageable: Pageable): Page<Employee>
}

interface DepartmentRepository : JpaRepository<Department, Long> {
    @Query("SELECT d FROM Department d WHERE d.tenantId = :tenantId AND d.active = true ORDER BY d.sortOrder")
    fun findAllActive(tenantId: String): List<Department>
}
