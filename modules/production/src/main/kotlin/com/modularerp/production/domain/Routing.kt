package com.modularerp.production.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * 공정순서(Routing) — 제품 생산을 위한 작업 공정의 순서와 표준시간을 정의.
 *
 * 제품별/공장별로 관리되며, 리비전(revision)으로 버전을 추적한다.
 * 작업지시(WO) 생성 시 RELEASED 상태의 Routing에서 공정을 복사한다.
 *
 * 핵심 비즈니스 규칙:
 * - 최소 1개 공정이 있어야 RELEASED 가능
 * - 총 표준시간(totalStandardTime) = 전체 공정의 단위당 소요시간 합계
 * - 공정번호(operationNo) 10, 20, 30... 단위로 부여하여 중간 삽입 여유 확보
 */
@Entity
@Table(
    name = "routings",
    uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "product_code", "plant_code", "revision"])]
)
class Routing(

    @Column(nullable = false, length = 50)
    val productCode: String,

    @Column(nullable = false, length = 200)
    var productName: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, length = 10)
    var revision: String = "001",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: RoutingStatus = RoutingStatus.DRAFT,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "routing", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("operationNo ASC")
    val operations: MutableList<RoutingOperation> = mutableListOf()

    /** 전체 공정 표준시간 합계 — 생산 리드타임 산출 기준 */
    val totalStandardTime: BigDecimal
        get() = operations.sumOf { it.totalTimePerUnit }

    fun addOperation(
        operationNo: Int, operationName: String, workCenterCode: String,
        setupTime: BigDecimal = BigDecimal.ZERO, runTimePerUnit: BigDecimal,
        description: String? = null
    ): RoutingOperation {
        val op = RoutingOperation(
            routing = this, operationNo = operationNo, operationName = operationName,
            workCenterCode = workCenterCode, setupTime = setupTime,
            runTimePerUnit = runTimePerUnit, description = description
        )
        operations.add(op)
        return op
    }

    /** 확정 — 생산에 사용 가능 상태로 전환. 공정이 1개 이상이어야 함 */
    fun release() {
        check(status == RoutingStatus.DRAFT)
        check(operations.isNotEmpty()) { "At least one operation required" }
        status = RoutingStatus.RELEASED
    }
}

/**
 * 공정순서 상세 — Routing의 개별 공정 단계.
 * 준비시간(setupTime)과 단위당 가동시간(runTimePerUnit)으로
 * 생산 수량에 따른 총 소요시간을 계산한다.
 */
@Entity
@Table(name = "routing_operations")
class RoutingOperation(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_id", nullable = false)
    val routing: Routing,

    /** 공정 순번 (10, 20, 30...) — 10 단위로 부여하여 중간 삽입 여유 확보 */
    @Column(nullable = false)
    val operationNo: Int,

    @Column(nullable = false, length = 200)
    var operationName: String,

    @Column(nullable = false, length = 20)
    var workCenterCode: String,

    /** 준비시간(시간) — 설비/라인 세팅에 소요되는 고정 시간 */
    @Column(nullable = false, precision = 10, scale = 4)
    var setupTime: BigDecimal = BigDecimal.ZERO,

    /** 단위당 가동시간(시간/EA) — 1개 생산에 소요되는 변동 시간 */
    @Column(nullable = false, precision = 10, scale = 6)
    var runTimePerUnit: BigDecimal,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity() {

    val totalTimePerUnit: BigDecimal
        get() = setupTime.add(runTimePerUnit)

    /** 주어진 수량에 대한 총 소요시간 = 준비시간 + (단위당 가동시간 × 수량) */
    fun totalTimeForQuantity(qty: BigDecimal): BigDecimal =
        setupTime.add(runTimePerUnit.multiply(qty))
}

/** 공정순서 상태: 작성중 → 확정(사용가능) → 폐기(구버전) */
enum class RoutingStatus { DRAFT, RELEASED, OBSOLETE }
