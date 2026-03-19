package com.modularerp.approval.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

/**
 * 워크플로우 정의.
 * 문서 유형별로 결재 흐름을 정의. 시각적 디자이너에서 편집 가능.
 */
@Entity
@Table(
    name = "workflow_definitions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "document_type", "version"])]
)
class WorkflowDefinition(

    /** 대상 문서 유형: PR, PO, SO 등 */
    @Column(name = "document_type", nullable = false, length = 30)
    val documentType: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    /** 버전 (같은 documentType에 여러 버전 관리) */
    @Column(nullable = false)
    var version: Int = 1,

    /** 활성 여부 (documentType당 하나만 active) */
    @Column(name = "is_current", nullable = false)
    var isCurrent: Boolean = false,

    @OneToMany(mappedBy = "workflow", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    val steps: MutableList<WorkflowStep> = mutableListOf(),

    /** 시각적 디자이너 레이아웃 (React Flow JSON) */
    @Column(name = "designer_layout", columnDefinition = "TEXT")
    var designerLayout: String? = null

) : TenantEntity() {

    fun addStep(
        stepOrder: Int,
        name: String,
        stepType: WorkflowStepType,
        approverType: ApproverType,
        approverValue: String,
        condition: String? = null
    ): WorkflowStep {
        val step = WorkflowStep(
            workflow = this,
            stepOrder = stepOrder,
            name = name,
            stepType = stepType,
            approverType = approverType,
            approverValue = approverValue,
            condition = condition
        )
        steps.add(step)
        return step
    }

    fun clearSteps() {
        steps.clear()
    }

    fun activateWorkflow() { isCurrent = true }
    fun deactivateWorkflow() { isCurrent = false }

    fun update(name: String, description: String?) {
        this.name = name
        this.description = description
    }
}
