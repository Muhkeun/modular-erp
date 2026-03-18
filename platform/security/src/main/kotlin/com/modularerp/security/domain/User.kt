package com.modularerp.security.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*

@Entity
@Table(name = "users", uniqueConstraints = [UniqueConstraint(columnNames = ["tenant_id", "login_id"])])
class User(
    @Column(nullable = false, length = 50)
    val loginId: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 200)
    var email: String? = null,

    @Column(length = 5)
    var locale: String = "ko",

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "role")
    val roles: MutableSet<String> = mutableSetOf("USER")
) : TenantEntity()
