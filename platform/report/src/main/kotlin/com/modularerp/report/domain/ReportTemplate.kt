package com.modularerp.report.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "report_templates",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "template_code"])]
)
class ReportTemplate : TenantEntity() {

    @Column(name = "template_code", nullable = false, length = 50)
    var templateCode: String = ""

    @Column(name = "template_name", nullable = false, length = 200)
    var templateName: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    var reportType: ReportType = ReportType.TABLE

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false, length = 10)
    var outputFormat: OutputFormat = OutputFormat.EXCEL

    @Column(name = "module_code", nullable = false, length = 30)
    var moduleCode: String = ""

    @Column(name = "query_definition", columnDefinition = "TEXT")
    var queryDefinition: String = "{}"

    @Column(name = "layout_definition", columnDefinition = "TEXT")
    var layoutDefinition: String? = null

    @Column(nullable = false)
    var enabled: Boolean = true

    @Column(length = 500)
    var description: String? = null
}
