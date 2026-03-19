package com.modularerp.security.repository

import com.modularerp.security.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByTenantIdAndLoginId(tenantId: String, loginId: String): Optional<User>
    fun countByTenantId(tenantId: String): Long
}
