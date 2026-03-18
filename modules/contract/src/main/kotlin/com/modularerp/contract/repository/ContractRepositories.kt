package com.modularerp.contract.repository

import com.modularerp.contract.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface ContractRepository : JpaRepository<Contract, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Contract>

    @Query("""
        SELECT c FROM Contract c WHERE c.tenantId = :tenantId AND c.active = true
        AND (:status IS NULL OR c.status = :status)
        AND (:contractType IS NULL OR c.contractType = :contractType)
        ORDER BY c.createdAt DESC
    """)
    fun search(tenantId: String, status: ContractStatus?, contractType: ContractType?, pageable: Pageable): Page<Contract>
}
