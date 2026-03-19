package com.modularerp.report.repository

import com.modularerp.report.domain.ReportExecution
import com.modularerp.report.domain.ReportTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReportTemplateRepository : JpaRepository<ReportTemplate, Long> {
    fun findByTemplateCodeAndTenantId(templateCode: String, tenantId: String): ReportTemplate?
    fun findByModuleCodeAndEnabledTrue(moduleCode: String): List<ReportTemplate>
    fun findByEnabledTrue(pageable: Pageable): Page<ReportTemplate>
}

@Repository
interface ReportExecutionRepository : JpaRepository<ReportExecution, Long> {
    fun findByRequestedByOrderByCreatedAtDesc(requestedBy: String, pageable: Pageable): Page<ReportExecution>
}
