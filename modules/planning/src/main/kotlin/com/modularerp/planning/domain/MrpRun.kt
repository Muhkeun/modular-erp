package com.modularerp.planning.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * MRP 실행(MRP Run) — 자재소요계획의 일괄 실행 단위.
 *
 * 수요(WO 소요자재, 수주)를 기반으로 BOM을 전개하고,
 * 현재고/입고예정을 차감하여 순소요량을 산출한 뒤
 * 구매(PR) 또는 생산(WO) 계획 오더를 생성한다.
 *
 * 상태 흐름: PLANNED → EXECUTED → CONFIRMED
 *
 * 핵심 비즈니스 규칙:
 * - 계획 기간(planningHorizonDays) 내의 수요만 대상
 * - 순소요량 > 0인 품목에 대해서만 계획 오더 생성
 * - BOM 보유 품목은 PRODUCE, 미보유 품목은 PURCHASE로 분류
 */
@Entity
@Table(name = "mrp_runs")
class MrpRun(

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false)
    var planningHorizonDays: Int = 30,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MrpStatus = MrpStatus.PLANNED,

    var executedAt: LocalDateTime? = null,

    @Column(length = 50)
    var executedBy: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "mrpRun", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("itemCode ASC")
    val results: MutableList<MrpResult> = mutableListOf()

    fun addResult(
        itemCode: String, itemName: String,
        grossRequirement: BigDecimal, onHandStock: BigDecimal,
        scheduledReceipts: BigDecimal, netRequirement: BigDecimal,
        plannedOrderQty: BigDecimal, unitOfMeasure: String,
        actionType: MrpActionType, requiredDate: LocalDate?
    ): MrpResult {
        val result = MrpResult(
            mrpRun = this, itemCode = itemCode, itemName = itemName,
            grossRequirement = grossRequirement, onHandStock = onHandStock,
            scheduledReceipts = scheduledReceipts, netRequirement = netRequirement,
            plannedOrderQty = plannedOrderQty, unitOfMeasure = unitOfMeasure,
            actionType = actionType, requiredDate = requiredDate
        )
        results.add(result)
        return result
    }

    fun execute() {
        status = MrpStatus.EXECUTED
        executedAt = LocalDateTime.now()
    }
}

/**
 * MRP 결과 — 품목별 소요량 계산 결과.
 *
 * 총소요량(gross) - 현재고(onHand) - 입고예정(scheduled) = 순소요량(net)
 * 순소요량 > 0이면 계획 오더 수량(plannedOrderQty)이 생성된다.
 */
@Entity
@Table(name = "mrp_results")
class MrpResult(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mrp_run_id", nullable = false)
    val mrpRun: MrpRun,

    @Column(nullable = false, length = 50)
    val itemCode: String,

    @Column(nullable = false, length = 200)
    var itemName: String,

    /** 총소요량 — WO 소요자재, 수주, 수요예측의 합계 */
    @Column(nullable = false, precision = 15, scale = 4)
    var grossRequirement: BigDecimal,

    /** 현재고 — MRP 실행 시점의 가용 재고 */
    @Column(nullable = false, precision = 15, scale = 4)
    var onHandStock: BigDecimal,

    /** 입고예정 — 미입고 PO + 운송 중 수량 */
    @Column(nullable = false, precision = 15, scale = 4)
    var scheduledReceipts: BigDecimal,

    /** 순소요량 = 총소요량 - 현재고 - 입고예정 (0 이상) */
    @Column(nullable = false, precision = 15, scale = 4)
    var netRequirement: BigDecimal,

    /** 계획 오더 수량 — 발주 또는 생산해야 할 수량 */
    @Column(nullable = false, precision = 15, scale = 4)
    var plannedOrderQty: BigDecimal,

    @Column(nullable = false, length = 10)
    var unitOfMeasure: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var actionType: MrpActionType,

    var requiredDate: LocalDate? = null,

    /** 생성된 문서번호 — 확정 시 실제 생성된 PR 또는 WO 번호 */
    @Column(length = 30)
    var generatedDocNo: String? = null

) : TenantEntity()

/**
 * 생산 스케줄 — 작업장별·일자별 생산 계획 달력.
 * WO를 작업장에 배정하고 순서(sequenceNo)를 정하여 생산 일정을 관리한다.
 */
@Entity
@Table(name = "production_schedules")
class ProductionSchedule(

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 20)
    var workCenterCode: String,

    @Column(nullable = false)
    var scheduleDate: LocalDate,

    @Column(length = 30)
    var workOrderNo: String? = null,

    @Column(nullable = false, length = 50)
    var productCode: String,

    @Column(nullable = false, length = 200)
    var productName: String,

    @Column(nullable = false, precision = 15, scale = 4)
    var plannedQuantity: BigDecimal,

    /** 해당 스케줄 슬롯의 계획 시간(시간) */
    @Column(nullable = false, precision = 10, scale = 4)
    var plannedHours: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ScheduleStatus = ScheduleStatus.PLANNED,

    var sequenceNo: Int = 0

) : TenantEntity()

/**
 * 능력계획(CRP) — 작업장별·일자별 부하 대 가용능력 추적.
 * 가동률(utilizationRate)과 과부하(isOverloaded) 여부로 병목을 식별한다.
 */
@Entity
@Table(name = "capacity_plans")
class CapacityPlan(

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 20)
    var workCenterCode: String,

    @Column(nullable = false, length = 200)
    var workCenterName: String,

    @Column(nullable = false)
    var planDate: LocalDate,

    /** 가용능력(시간) — 해당 일자에 투입 가능한 총 시간 */
    @Column(nullable = false, precision = 10, scale = 4)
    var availableHours: BigDecimal,

    /** 계획 부하(시간) — 배정된 작업의 총 소요시간 */
    @Column(nullable = false, precision = 10, scale = 4)
    var plannedLoadHours: BigDecimal = BigDecimal.ZERO,

    /** 실적 시간 — 실제 소비된 작업 시간 */
    @Column(nullable = false, precision = 10, scale = 4)
    var actualHours: BigDecimal = BigDecimal.ZERO

) : TenantEntity() {

    val remainingCapacity: BigDecimal
        get() = availableHours.subtract(plannedLoadHours)

    val utilizationRate: BigDecimal
        get() = if (availableHours > BigDecimal.ZERO)
            plannedLoadHours.divide(availableHours, 4, java.math.RoundingMode.HALF_UP)
        else BigDecimal.ZERO

    val isOverloaded: Boolean
        get() = plannedLoadHours > availableHours

    fun addLoad(hours: BigDecimal) {
        plannedLoadHours = plannedLoadHours.add(hours)
    }
}

/** MRP 상태: 계획 → 실행완료 → 확정(실제 오더 생성) */
enum class MrpStatus { PLANNED, EXECUTED, CONFIRMED }
/** MRP 조치유형: 구매(외부조달), 생산(내부제조), 조치불필요 */
enum class MrpActionType { PURCHASE, PRODUCE, NONE }
/** 스케줄 상태: 계획 → 확정 → 진행중 → 완료/취소 */
enum class ScheduleStatus { PLANNED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED }
