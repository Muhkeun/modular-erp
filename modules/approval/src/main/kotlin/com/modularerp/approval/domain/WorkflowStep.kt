package com.modularerp.approval.domain

import com.modularerp.core.domain.BaseEntity
import jakarta.persistence.*

/**
 * 워크플로우 단계 정의.
 */
@Entity
@Table(name = "workflow_steps")
class WorkflowStep(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    val workflow: WorkflowDefinition,

    @Column(name = "step_order", nullable = false)
    var stepOrder: Int,

    @Column(nullable = false, length = 100)
    var name: String,

    /** 단계 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 20)
    var stepType: WorkflowStepType,

    /** 결재자 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "approver_type", nullable = false, length = 20)
    var approverType: ApproverType,

    /**
     * 결재자 값:
     * - SPECIFIC_USER: userId
     * - ROLE: roleCode
     * - DEPARTMENT_HEAD: departmentCode
     * - MANAGER_LEVEL: "1" (1단계 상위 관리자)
     */
    @Column(name = "approver_value", nullable = false, length = 200)
    var approverValue: String,

    /** 조건식 (SpEL 또는 간단한 표현식). null이면 무조건 실행 */
    @Column(columnDefinition = "TEXT")
    var condition: String? = null,

    /** 자동 승인 시간(시). 0이면 자동승인 없음 */
    @Column(name = "auto_approve_hours")
    var autoApproveHours: Int = 0

) : BaseEntity() {

    fun update(
        name: String,
        stepType: WorkflowStepType,
        approverType: ApproverType,
        approverValue: String,
        condition: String?,
        autoApproveHours: Int
    ) {
        this.name = name
        this.stepType = stepType
        this.approverType = approverType
        this.approverValue = approverValue
        this.condition = condition
        this.autoApproveHours = autoApproveHours
    }
}

enum class WorkflowStepType {
    APPROVAL,       // 결재 (승인/반려)
    NOTIFICATION,   // 통보 (참조)
    CONDITION,      // 조건 분기
    PARALLEL        // 병렬 결재 (모두 승인 필요)
}

enum class ApproverType {
    SPECIFIC_USER,      // 특정 사용자
    ROLE,               // 역할
    DEPARTMENT_HEAD,    // 부서장
    MANAGER_LEVEL,      // N단계 상위 관리자
    REQUESTER_MANAGER   // 요청자의 직속 상관
}
