package com.modularerp.crm.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "crm_customers")
class Customer(

    @Column(nullable = false, length = 30)
    var customerCode: String = "",

    @Column(nullable = false, length = 200)
    var customerName: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var customerType: CustomerType = CustomerType.CORPORATE,

    @Column(length = 100)
    var industry: String? = null,

    @Column(length = 30)
    var phone: String? = null,

    @Column(length = 100)
    var email: String? = null,

    @Column(length = 200)
    var website: String? = null,

    @Column(length = 500)
    var address: String? = null,

    @Column(length = 100)
    var contactPerson: String? = null,

    @Column(length = 30)
    var contactPhone: String? = null,

    @Column(length = 100)
    var contactEmail: String? = null,

    @Column(nullable = false, precision = 19, scale = 4)
    var creditLimit: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var paymentTermDays: Int = 30,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CustomerStatus = CustomerStatus.PROSPECT,

    @Column(length = 1000)
    var notes: String? = null,

    @Column(length = 100)
    var assignedTo: String? = null

) : TenantEntity()

enum class CustomerType { INDIVIDUAL, CORPORATE, GOVERNMENT }
enum class CustomerStatus { PROSPECT, ACTIVE, INACTIVE, BLACKLISTED }
