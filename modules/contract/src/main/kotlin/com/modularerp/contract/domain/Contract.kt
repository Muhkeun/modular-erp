package com.modularerp.contract.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "contracts")
class Contract(

    @Column(nullable = false, unique = true, length = 30)
    var documentNo: String = "",

    @Column(nullable = false, length = 200)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var contractType: ContractType,

    @Column(nullable = false, length = 50)
    var counterpartyCode: String,

    @Column(nullable = false, length = 200)
    var counterpartyName: String,

    @Column(nullable = false)
    var startDate: LocalDate,

    @Column(nullable = false)
    var endDate: LocalDate,

    @Column(precision = 19, scale = 4)
    var contractAmount: BigDecimal? = null,

    @Column(length = 3)
    var currencyCode: String = "KRW",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ContractStatus = ContractStatus.DRAFT,

    @Column(length = 2000)
    var terms: String? = null,

    @Column(length = 1000)
    var description: String? = null

) : TenantEntity() {

    fun activateContract() { check(status == ContractStatus.DRAFT); status = ContractStatus.ACTIVE }
    fun expire() { status = ContractStatus.EXPIRED }
    fun terminate() { status = ContractStatus.TERMINATED }
}

enum class ContractType { PURCHASE, SALES, SERVICE, NDA, FRAMEWORK }
enum class ContractStatus { DRAFT, ACTIVE, EXPIRED, TERMINATED, CANCELLED }
