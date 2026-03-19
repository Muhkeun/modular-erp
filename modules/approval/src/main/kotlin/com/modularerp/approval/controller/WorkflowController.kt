package com.modularerp.approval.controller

import com.modularerp.approval.dto.*
import com.modularerp.approval.service.WorkflowService
import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/workflows")
@Tag(name = "Admin - Workflows", description = "결재 워크플로우 디자이너")
@PreAuthorize("hasRole('ADMIN')")
class WorkflowController(
    private val workflowService: WorkflowService
) {

    @GetMapping
    @Operation(summary = "전체 워크플로우 목록")
    fun getAll(): ApiResponse<List<WorkflowResponse>> =
        ApiResponse.ok(workflowService.getAll())

    @GetMapping("/document-type/{documentType}")
    @Operation(summary = "문서 유형별 워크플로우 목록")
    fun getByDocType(@PathVariable documentType: String): ApiResponse<List<WorkflowResponse>> =
        ApiResponse.ok(workflowService.getByDocumentType(documentType))

    @GetMapping("/active/{documentType}")
    @Operation(summary = "문서 유형의 활성 워크플로우")
    fun getActive(@PathVariable documentType: String): ApiResponse<WorkflowResponse?> =
        ApiResponse.ok(workflowService.getActive(documentType))

    @GetMapping("/{id}")
    @Operation(summary = "워크플로우 상세")
    fun getById(@PathVariable id: Long): ApiResponse<WorkflowResponse> =
        ApiResponse.ok(workflowService.getById(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "워크플로우 생성")
    fun create(@Valid @RequestBody request: CreateWorkflowRequest): ApiResponse<WorkflowResponse> =
        ApiResponse.ok(workflowService.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "워크플로우 수정")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UpdateWorkflowRequest): ApiResponse<WorkflowResponse> =
        ApiResponse.ok(workflowService.update(id, request))

    @PostMapping("/{id}/activate")
    @Operation(summary = "워크플로우 활성화")
    fun activate(@PathVariable id: Long): ApiResponse<WorkflowResponse> =
        ApiResponse.ok(workflowService.activate(id))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "워크플로우 비활성화")
    fun delete(@PathVariable id: Long) = workflowService.delete(id)
}
