package com.modularerp.production.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * 작업장(Work Center) — 생산 자원(설비, 라인, 작업 스테이션).
 *
 * 생산 스케줄링과 원가 계산의 기본 단위.
 * 일일 가용 시간(capacityPerDay)과 병렬 자원 수(resourceCount)로
 * 총 일일 가용 능력을 산출하며, 시간당 비용으로 제조원가를 계산한다.
 *
 * 핵심 비즈니스 규칙:
 * - 총 일일능력 = 일일가동시간 × 병렬자원수
 * - 설비보전(MAINTENANCE) 상태에서는 생산 스케줄 배정 불가
 */
@Entity
@Table(name = "work_centers")
class WorkCenter(

    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var centerType: WorkCenterType = WorkCenterType.MACHINE,

    /** 일일 가용시간(시간/일) — 교대 근무 시 조정 가능 */
    @Column(nullable = false, precision = 5, scale = 2)
    var capacityPerDay: BigDecimal = BigDecimal("8.00"),

    /** 병렬 자원 수 (예: 동일 설비 3대) — 동시 작업 가능 수 */
    @Column(nullable = false)
    var resourceCount: Int = 1,

    /** 시간당 원가 (인건비 + 설비비) — 제조원가 산출에 사용 */
    @Column(nullable = false, precision = 19, scale = 4)
    var costPerHour: BigDecimal = BigDecimal.ZERO,

    /** 작업 준비비용 — 공정 전환 시 1회 발생하는 고정 비용 */
    @Column(nullable = false, precision = 19, scale = 4)
    var setupCost: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: WorkCenterStatus = WorkCenterStatus.ACTIVE,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity() {

    /** 총 일일 가용능력(시간) = 일일가동시간 × 병렬자원수 */
    val totalDailyCapacity: BigDecimal
        get() = capacityPerDay.multiply(BigDecimal(resourceCount))
}

/** 작업장 유형: 기계, 인력, 조립라인, 검사 */
enum class WorkCenterType { MACHINE, LABOR, ASSEMBLY_LINE, INSPECTION }
/** 작업장 상태: 가동중, 보전중, 비활성 */
enum class WorkCenterStatus { ACTIVE, MAINTENANCE, INACTIVE }
