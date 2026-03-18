package com.modularerp.quality.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "quality_inspections")
class QualityInspection(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var inspectionType: InspectionType = InspectionType.INCOMING,

    @Column(length = 30)
    var referenceDocNo: String? = null,

    @Column(nullable = false, length = 50)
    var itemCode: String,
    @Column(nullable = false, length = 200)
    var itemName: String,

    @Column(nullable = false, length = 20)
    var plantCode: String,

    @Column(nullable = false, precision = 15, scale = 4)
    var inspectedQuantity: BigDecimal,

    @Column(precision = 15, scale = 4)
    var acceptedQuantity: BigDecimal? = null,

    @Column(precision = 15, scale = 4)
    var rejectedQuantity: BigDecimal? = null,

    @Column(nullable = false)
    var inspectionDate: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: QiStatus = QiStatus.PENDING,

    @Enumerated(EnumType.STRING)
    var result: QiResult? = null,

    @Column(length = 1000)
    var remarks: String? = null,

    @Column(length = 50)
    var inspectorId: String? = null

) : TenantEntity() {

    fun complete(accepted: BigDecimal, rejected: BigDecimal, result: QiResult, remarks: String? = null) {
        check(status == QiStatus.PENDING) { "Inspection already completed" }
        this.acceptedQuantity = accepted
        this.rejectedQuantity = rejected
        this.result = result
        this.remarks = remarks
        this.status = QiStatus.COMPLETED
    }
}

enum class InspectionType { INCOMING, IN_PROCESS, FINAL, RETURN }
enum class QiStatus { PENDING, COMPLETED, CANCELLED }
enum class QiResult { PASS, FAIL, CONDITIONAL }
