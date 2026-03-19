package com.modularerp.crm.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "crm_opportunities")
class Opportunity(

    @Column(nullable = false, length = 30)
    var opportunityNo: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: Customer,

    @Column(nullable = false, length = 200)
    var title: String = "",

    @Column(length = 1000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var stage: OpportunityStage = OpportunityStage.PROSPECTING,

    @Column(nullable = false)
    var probability: Int = 0,

    @Column(nullable = false, precision = 19, scale = 4)
    var expectedAmount: BigDecimal = BigDecimal.ZERO,

    var expectedCloseDate: LocalDate? = null,

    @Column(precision = 19, scale = 4)
    var actualAmount: BigDecimal? = null,

    var closedAt: LocalDateTime? = null,

    @Column(length = 500)
    var lostReason: String? = null,

    @Column(length = 100)
    var assignedTo: String? = null

) : TenantEntity() {

    fun updateStage(newStage: OpportunityStage) {
        stage = newStage
        if (newStage == OpportunityStage.CLOSED_WON || newStage == OpportunityStage.CLOSED_LOST) {
            closedAt = LocalDateTime.now()
        }
    }
}

enum class OpportunityStage { PROSPECTING, QUALIFICATION, PROPOSAL, NEGOTIATION, CLOSED_WON, CLOSED_LOST }
