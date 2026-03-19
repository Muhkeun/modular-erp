package com.modularerp.approval.service

import com.modularerp.approval.domain.WorkflowDefinition
import com.modularerp.approval.dto.*
import com.modularerp.approval.repository.WorkflowDefinitionRepository
import com.modularerp.core.exception.BusinessException
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class WorkflowService(
    private val workflowRepo: WorkflowDefinitionRepository
) {

    fun getAll(): List<WorkflowResponse> =
        workflowRepo.findAllByTenantId(TenantContext.getTenantId()).map(WorkflowResponse::from)

    fun getByDocumentType(documentType: String): List<WorkflowResponse> =
        workflowRepo.findAllByTenantIdAndDocumentType(TenantContext.getTenantId(), documentType)
            .map(WorkflowResponse::from)

    fun getActive(documentType: String): WorkflowResponse? =
        workflowRepo.findByTenantIdAndDocumentTypeAndIsCurrentTrue(TenantContext.getTenantId(), documentType)
            ?.let(WorkflowResponse::from)

    fun getById(id: Long): WorkflowResponse {
        val wf = workflowRepo.findById(id).orElseThrow { BusinessException("WF_NOT_FOUND", "Workflow not found") }
        return WorkflowResponse.from(wf)
    }

    @Transactional
    fun create(request: CreateWorkflowRequest): WorkflowResponse {
        val tenantId = TenantContext.getTenantId()
        val existingVersions = workflowRepo.findAllByTenantIdAndDocumentType(tenantId, request.documentType)
        val nextVersion = (existingVersions.maxOfOrNull { it.version } ?: 0) + 1

        val wf = WorkflowDefinition(
            documentType = request.documentType,
            name = request.name,
            description = request.description,
            version = nextVersion,
            designerLayout = request.designerLayout
        ).apply { assignTenant(tenantId) }

        request.steps.forEach { step ->
            wf.addStep(step.stepOrder, step.name, step.stepType, step.approverType, step.approverValue, step.condition)
        }

        return WorkflowResponse.from(workflowRepo.save(wf))
    }

    @Transactional
    fun update(id: Long, request: UpdateWorkflowRequest): WorkflowResponse {
        val wf = workflowRepo.findById(id).orElseThrow { BusinessException("WF_NOT_FOUND", "Workflow not found") }
        wf.update(request.name, request.description)
        wf.designerLayout = request.designerLayout

        wf.clearSteps()
        request.steps.forEach { step ->
            wf.addStep(step.stepOrder, step.name, step.stepType, step.approverType, step.approverValue, step.condition)
        }

        return WorkflowResponse.from(workflowRepo.save(wf))
    }

    @Transactional
    fun activate(id: Long): WorkflowResponse {
        val wf = workflowRepo.findById(id).orElseThrow { BusinessException("WF_NOT_FOUND", "Workflow not found") }

        // 같은 documentType의 기존 active 비활성화
        workflowRepo.findByTenantIdAndDocumentTypeAndIsCurrentTrue(wf.tenantId, wf.documentType)
            ?.deactivateWorkflow()

        wf.activateWorkflow()
        return WorkflowResponse.from(workflowRepo.save(wf))
    }

    @Transactional
    fun delete(id: Long) {
        val wf = workflowRepo.findById(id).orElseThrow { BusinessException("WF_NOT_FOUND", "Workflow not found") }
        wf.deactivateWorkflow()
    }
}
