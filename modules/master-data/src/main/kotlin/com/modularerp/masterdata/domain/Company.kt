package com.modularerp.masterdata.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

@Entity
@Table(name = "companies")
class Company(
    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(length = 50)
    var businessRegistrationNo: String? = null,

    @Column(length = 100)
    var ceoName: String? = null,

    @Column(length = 500)
    var address: String? = null,

    @Column(length = 5)
    var countryCode: String? = null,

    @Column(length = 3)
    var defaultCurrency: String = "KRW"

) : TenantEntity() {

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL])
    val plants: MutableSet<Plant> = mutableSetOf()
}

@Entity
@Table(name = "plants")
class Plant(
    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(length = 500)
    var address: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    val company: Company

) : TenantEntity()
