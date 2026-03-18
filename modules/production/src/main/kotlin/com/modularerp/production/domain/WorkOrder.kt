package com.modularerp.production.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 작업지시서(WO) — 특정 제품을 지정 수량만큼 생산하는 지시서.
 *
 * BOM(자재명세서)에서 소요 자재를, Routing(공정순서)에서 작업 공정을 가져온다.
 * 생산 진행 중 자재 소비, 공정별 실적, 완성품/불량 수량을 추적한다.
 *
 * 상태 흐름: PLANNED → RELEASED → IN_PROGRESS → COMPLETED → CLOSED
 *
 * 핵심 비즈니스 규칙:
 * - 수율(yieldRate) = 양품수량 / (양품 + 불량) — 품질 지표
 * - 자재 소비(issue)와 생산 실적(reportProduction)은 진행 중에만 가능
 * - MTO(주문생산) 시 salesOrderNo로 수주와 연결
 */
@Entity
@Table(name = "work_orders")
class WorkOrder(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 20)
    var companyCode: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 50)
    var productCode: String,

    @Column(nullable = false, length = 200)
    var productName: String,

    /** 계획 생산수량 */
    @Column(nullable = false, precision = 15, scale = 4)
    var plannedQuantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String = "EA",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: WoStatus = WoStatus.PLANNED,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var orderType: WoType = WoType.STANDARD,

    /** 수주번호 — 주문생산(MTO) 방식일 때 해당 수주(SO)와 연결 */
    @Column(length = 30)
    var salesOrderNo: String? = null,

    var plannedStartDate: LocalDate? = null,
    var plannedEndDate: LocalDate? = null,
    var actualStartDate: LocalDateTime? = null,
    var actualEndDate: LocalDateTime? = null,

    @Column(length = 10)
    var bomRevision: String? = null,

    @Column(length = 10)
    var routingRevision: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var priority: WoPriority = WoPriority.NORMAL,

    @Column(length = 1000)
    var remark: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "workOrder", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("operationNo ASC")
    val operations: MutableList<WorkOrderOperation> = mutableListOf()

    @OneToMany(mappedBy = "workOrder", cascade = [CascadeType.ALL], orphanRemoval = true)
    val materialRequirements: MutableList<WorkOrderMaterial> = mutableListOf()

    /** 누적 양품 수량 — 생산 실적 보고 시 증가 */
    @Column(nullable = false, precision = 15, scale = 4)
    var completedQuantity: BigDecimal = BigDecimal.ZERO
        protected set

    @Column(nullable = false, precision = 15, scale = 4)
    var scrapQuantity: BigDecimal = BigDecimal.ZERO
        protected set

    val remainingQuantity: BigDecimal
        get() = plannedQuantity.subtract(completedQuantity)

    val yieldRate: BigDecimal
        get() {
            val total = completedQuantity.add(scrapQuantity)
            return if (total > BigDecimal.ZERO) completedQuantity.divide(total, 4, java.math.RoundingMode.HALF_UP)
            else BigDecimal.ZERO
        }

    fun addOperation(
        operationNo: Int, operationName: String, workCenterCode: String,
        setupTime: BigDecimal, runTimePerUnit: BigDecimal
    ): WorkOrderOperation {
        val op = WorkOrderOperation(
            workOrder = this, operationNo = operationNo, operationName = operationName,
            workCenterCode = workCenterCode, setupTime = setupTime, runTimePerUnit = runTimePerUnit
        )
        operations.add(op)
        return op
    }

    fun addMaterial(
        itemCode: String, itemName: String, requiredQuantity: BigDecimal,
        unitOfMeasure: String, operationNo: Int? = null
    ): WorkOrderMaterial {
        val mat = WorkOrderMaterial(
            workOrder = this, itemCode = itemCode, itemName = itemName,
            requiredQuantity = requiredQuantity, unitOfMeasure = unitOfMeasure, operationNo = operationNo
        )
        materialRequirements.add(mat)
        return mat
    }

    /** 출고지시 — 자재 출고 및 생산 착수 가능 상태로 전환 */
    fun release() {
        check(status == WoStatus.PLANNED) { "Can only release from PLANNED" }
        status = WoStatus.RELEASED
    }

    /** 생산 착수 — 실제 작업 시작. 착수 시각을 기록 */
    fun start() {
        check(status == WoStatus.RELEASED) { "Can only start from RELEASED" }
        status = WoStatus.IN_PROGRESS
        actualStartDate = LocalDateTime.now()
    }

    /** 생산 실적 보고 — 양품/불량 수량 누적. 수율 산출에 사용 */
    fun reportProduction(goodQty: BigDecimal, scrapQty: BigDecimal = BigDecimal.ZERO) {
        check(status == WoStatus.IN_PROGRESS) { "Must be IN_PROGRESS to report" }
        completedQuantity = completedQuantity.add(goodQty)
        scrapQuantity = scrapQuantity.add(scrapQty)
    }

    /** 생산 완료 — 완료 시각 기록. 이후 원가 정산 및 마감 가능 */
    fun complete() {
        check(status == WoStatus.IN_PROGRESS) { "Can only complete from IN_PROGRESS" }
        status = WoStatus.COMPLETED
        actualEndDate = LocalDateTime.now()
    }

    /** 마감 — 원가 확정 후 최종 마감. 더 이상 실적 보고 불가 */
    fun close() {
        check(status == WoStatus.COMPLETED) { "Can only close from COMPLETED" }
        status = WoStatus.CLOSED
    }
}

/**
 * 작업지시 공정 — WO의 개별 생산 공정(작업 단계).
 * 각 공정은 특정 작업장(WorkCenter)에서 수행되며,
 * 준비시간/가동시간 실적과 양품/불량 수량을 공정 단위로 추적한다.
 */
@Entity
@Table(name = "work_order_operations")
class WorkOrderOperation(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    val workOrder: WorkOrder,

    val operationNo: Int,

    @Column(nullable = false, length = 200)
    var operationName: String,

    @Column(nullable = false, length = 20)
    var workCenterCode: String,

    @Column(nullable = false, precision = 10, scale = 4)
    var setupTime: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 10, scale = 6)
    var runTimePerUnit: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OpStatus = OpStatus.PENDING,

    /** 실제 소요시간 — 계획 대비 실적 분석용 */
    @Column(precision = 10, scale = 4)
    var actualSetupTime: BigDecimal? = null,

    @Column(precision = 10, scale = 4)
    var actualRunTime: BigDecimal? = null,

    @Column(precision = 15, scale = 4)
    var completedQuantity: BigDecimal = BigDecimal.ZERO,

    @Column(precision = 15, scale = 4)
    var scrapQuantity: BigDecimal = BigDecimal.ZERO,

    var startedAt: LocalDateTime? = null,
    var completedAt: LocalDateTime? = null

) : TenantEntity() {

    fun start() {
        check(status == OpStatus.PENDING) { "Operation already started" }
        status = OpStatus.IN_PROGRESS
        startedAt = LocalDateTime.now()
    }

    fun reportProgress(goodQty: BigDecimal, scrapQty: BigDecimal = BigDecimal.ZERO,
                       actualSetup: BigDecimal? = null, actualRun: BigDecimal? = null) {
        check(status == OpStatus.IN_PROGRESS) { "Operation not in progress" }
        completedQuantity = completedQuantity.add(goodQty)
        scrapQuantity = scrapQuantity.add(scrapQty)
        actualSetup?.let { actualSetupTime = it }
        actualRun?.let { actualRunTime = it }
    }

    fun complete() {
        check(status == OpStatus.IN_PROGRESS)
        status = OpStatus.COMPLETED
        completedAt = LocalDateTime.now()
    }
}

/**
 * 작업지시 소요자재 — WO 생산에 필요한 원자재/부품.
 * BOM 전개(explosion)로 자동 생성되며, 실제 출고(issue)된 수량을 추적한다.
 * 부족수량(shortageQuantity)으로 자재 조달 현황을 파악한다.
 */
@Entity
@Table(name = "work_order_materials")
class WorkOrderMaterial(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    val workOrder: WorkOrder,

    @Column(nullable = false, length = 50)
    var itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    @Column(nullable = false, precision = 15, scale = 4)
    var requiredQuantity: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String,

    /** 소비 공정번호 — 어떤 공정에서 이 자재를 사용하는지 */
    var operationNo: Int? = null,

    /** 실제 출고수량 — 창고에서 생산현장으로 불출된 수량 */
    @Column(nullable = false, precision = 15, scale = 4)
    var issuedQuantity: BigDecimal = BigDecimal.ZERO

) : TenantEntity() {

    val shortageQuantity: BigDecimal
        get() = requiredQuantity.subtract(issuedQuantity).coerceAtLeast(BigDecimal.ZERO)

    fun issue(qty: BigDecimal) {
        issuedQuantity = issuedQuantity.add(qty)
    }
}

/** 작업지시 상태: 계획 → 출고지시 → 진행중 → 완료 → 마감/취소 */
enum class WoStatus { PLANNED, RELEASED, IN_PROGRESS, COMPLETED, CLOSED, CANCELLED }
/** 작업지시 유형: 일반생산, 재작업, 시제품, 설비보전 */
enum class WoType { STANDARD, REWORK, PROTOTYPE, MAINTENANCE }
/** 우선순위: 낮음, 보통, 높음, 긴급 */
enum class WoPriority { LOW, NORMAL, HIGH, URGENT }
/** 공정 상태: 대기 → 진행중 → 완료 */
enum class OpStatus { PENDING, IN_PROGRESS, COMPLETED }
