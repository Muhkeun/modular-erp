package com.modularerp.account.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

/**
 * Chart of Accounts — the master list of GL accounts.
 */
@Entity
@Table(name = "account_masters")
class AccountMaster(

    @Column(nullable = false, length = 20)
    val code: String,

    @Column(nullable = false, length = 200)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var accountType: AccountType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var accountGroup: AccountGroup,

    @Column(length = 20)
    var parentCode: String? = null,

    var postable: Boolean = true,

    @Column(length = 500)
    var description: String? = null

) : TenantEntity()

enum class AccountType { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }
enum class AccountGroup { CASH, RECEIVABLE, PAYABLE, INVENTORY, FIXED_ASSET, SALES, COGS, OPERATING_EXPENSE, OTHER }
