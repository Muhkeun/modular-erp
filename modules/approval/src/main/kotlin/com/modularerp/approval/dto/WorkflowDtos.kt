package com.modularerp.approval.dto

import com.modularerp.approval.domain.*
import jakarta.validation.constraints.NotBlank

data class CreateWorkflowRequest(
    @field:NotBlank val documentType: String,
    @field:NotBlank val name: String,
    val description: String? = null,
    val steps: List<WorkflowStepRequest> = emptyList(),
    val designerLayout: String? = null
)

data class UpdateWorkflowRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    val steps: List<WorkflowStepRequest> = emptyList(),
    val designerLayout: String? = null
)

data class WorkflowStepRequest(
    val stepOrder: Int,
    @field:NotBlank val name: String,
    val stepType: WorkflowStepType,
    val approverType: ApproverType,
    @field:NotBlank val approverValue: String,
    val condition: String? = null,
    val autoApproveHours: Int = 0
)

data class WorkflowResponse(
    val id: Long,
    val documentType: String,
    val name: String,
    val description: String?,
    val version: Int,
    val isCurrent: Boolean,
    val steps: List<WorkflowStepResponse>,
    val designerLayout: String?
) {
    companion object {
        fun from(e: WorkflowDefinition) = WorkflowResponse(
            id = e.id,
            documentType = e.documentType,
            name = e.name,
            description = e.description,
            version = e.version,
            isCurrent = e.isCurrent,
            steps = e.steps.map(WorkflowStepResponse::from),
            designerLayout = e.designerLayout
        )
    }
}

data class WorkflowStepResponse(
    val id: Long,
    val stepOrder: Int,
    val name: String,
    val stepType: WorkflowStepType,
    val approverType: ApproverType,
    val approverValue: String,
    val condition: String?,
    val autoApproveHours: Int
) {
    companion object {
        fun from(e: WorkflowStep) = WorkflowStepResponse(
            id = e.id,
            stepOrder = e.stepOrder,
            name = e.name,
            stepType = e.stepType,
            approverType = e.approverType,
            approverValue = e.approverValue,
            condition = e.condition,
            autoApproveHours = e.autoApproveHours
        )
    }
}
