package com.modularerp.crm.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "crm_leads")
class Lead(

    @Column(nullable = false, length = 30)
    var leadNo: String = "",

    @Column(length = 200)
    var companyName: String? = null,

    @Column(nullable = false, length = 100)
    var contactName: String = "",

    @Column(length = 100)
    var contactEmail: String? = null,

    @Column(length = 30)
    var contactPhone: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var source: LeadSource = LeadSource.OTHER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: LeadStatus = LeadStatus.NEW,

    @Column(precision = 19, scale = 4)
    var estimatedValue: BigDecimal? = null,

    @Column(length = 100)
    var assignedTo: String? = null,

    @Column(length = 1000)
    var notes: String? = null,

    var convertedCustomerId: Long? = null,

    var convertedAt: LocalDateTime? = null

) : TenantEntity() {

    fun convert(customerId: Long) {
        check(status == LeadStatus.QUALIFIED) { "Lead must be QUALIFIED to convert" }
        status = LeadStatus.CONVERTED
        convertedCustomerId = customerId
        convertedAt = LocalDateTime.now()
    }
}

enum class LeadSource { WEB, REFERRAL, TRADE_SHOW, COLD_CALL, ADVERTISEMENT, PARTNER, OTHER }
enum class LeadStatus { NEW, CONTACTED, QUALIFIED, UNQUALIFIED, CONVERTED }
