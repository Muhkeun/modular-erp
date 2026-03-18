package com.modularerp.production.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Routing — defines the sequence of operations to manufacture a product.
 * Each operation is performed at a Work Center with defined times.
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

    /** Total standard time for all operations */
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

    fun release() {
        check(status == RoutingStatus.DRAFT)
        check(operations.isNotEmpty()) { "At least one operation required" }
        status = RoutingStatus.RELEASED
    }
}

@Entity
@Table(name = "routing_operations")
class RoutingOperation(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_id", nullable = false)
    val routing: Routing,

    /** Operation sequence number (10, 20, 30...) */
    @Column(nullable = false)
    val operationNo: Int,

    @Column(nullable = false, length = 200)
    var operationName: String,

    @Column(nullable = false, length = 20)
    var workCenterCode: String,

    /** Machine/line setup time in hours */
    @Column(nullable = false, precision = 10, scale = 4)
    var setupTime: BigDecimal = BigDecimal.ZERO,

    /** Run time per unit in hours */
    @Column(nullable = false, precision = 10, scale = 6)
    var runTimePerUnit: BigDecimal,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity() {

    val totalTimePerUnit: BigDecimal
        get() = setupTime.add(runTimePerUnit)

    /** Calculate total time for a given quantity */
    fun totalTimeForQuantity(qty: BigDecimal): BigDecimal =
        setupTime.add(runTimePerUnit.multiply(qty))
}

enum class RoutingStatus { DRAFT, RELEASED, OBSOLETE }
