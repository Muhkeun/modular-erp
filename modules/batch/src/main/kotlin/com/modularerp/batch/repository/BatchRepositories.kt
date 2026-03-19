package com.modularerp.batch.repository

import com.modularerp.batch.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface BatchJobRepository : JpaRepository<BatchJob, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<BatchJob>
    fun findByTenantIdAndJobCode(tenantId: String, jobCode: String): Optional<BatchJob>

    @Query("""
        SELECT j FROM BatchJob j WHERE j.tenantId = :tenantId AND j.active = true
        AND (:jobType IS NULL OR j.jobType = :jobType)
        AND (:enabled IS NULL OR j.enabled = :enabled)
        ORDER BY j.jobCode ASC
    """)
    fun search(tenantId: String, jobType: BatchJobType?, enabled: Boolean?, pageable: Pageable): Page<BatchJob>
}

interface BatchExecutionRepository : JpaRepository<BatchExecution, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<BatchExecution>

    @Query("""
        SELECT e FROM BatchExecution e WHERE e.tenantId = :tenantId AND e.active = true
        AND e.batchJob.id = :jobId
        ORDER BY e.startedAt DESC
    """)
    fun findByJobId(tenantId: String, jobId: Long, pageable: Pageable): Page<BatchExecution>

    @Query("""
        SELECT e FROM BatchExecution e WHERE e.tenantId = :tenantId AND e.active = true
        AND (:status IS NULL OR e.status = :status)
        ORDER BY e.startedAt DESC
    """)
    fun search(tenantId: String, status: ExecutionStatus?, pageable: Pageable): Page<BatchExecution>
}
