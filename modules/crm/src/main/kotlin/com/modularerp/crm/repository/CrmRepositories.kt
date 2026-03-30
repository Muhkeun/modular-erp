package com.modularerp.crm.repository

import com.modularerp.crm.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface CustomerRepository : JpaRepository<Customer, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Customer>

    @Query("""
        SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.active = true
        AND (:status IS NULL OR c.status = :status)
        AND (:customerCode IS NULL OR c.customerCode LIKE %:customerCode%)
        AND (:customerName IS NULL OR c.customerName LIKE %:customerName%)
        AND (:applyOwnScope = false OR c.assignedTo = :ownUserId)
        ORDER BY c.createdAt DESC
    """)
    fun search(
        tenantId: String,
        status: CustomerStatus?,
        customerCode: String?,
        customerName: String?,
        applyOwnScope: Boolean,
        ownUserId: String,
        pageable: Pageable
    ): Page<Customer>
}

interface LeadRepository : JpaRepository<Lead, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Lead>

    @Query("""
        SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.active = true
        AND (:status IS NULL OR l.status = :status)
        AND (:source IS NULL OR l.source = :source)
        AND (:leadNo IS NULL OR l.leadNo LIKE %:leadNo%)
        AND (:applyOwnScope = false OR l.assignedTo = :ownUserId)
        ORDER BY l.createdAt DESC
    """)
    fun search(
        tenantId: String,
        status: LeadStatus?,
        source: LeadSource?,
        leadNo: String?,
        applyOwnScope: Boolean,
        ownUserId: String,
        pageable: Pageable
    ): Page<Lead>
}

interface OpportunityRepository : JpaRepository<Opportunity, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Opportunity>

    @Query("""
        SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId AND o.active = true
        AND (:stage IS NULL OR o.stage = :stage)
        AND (:assignedTo IS NULL OR o.assignedTo = :assignedTo)
        AND (:applyOwnScope = false OR o.assignedTo = :ownUserId)
        ORDER BY o.createdAt DESC
    """)
    fun search(
        tenantId: String,
        stage: OpportunityStage?,
        assignedTo: String?,
        applyOwnScope: Boolean,
        ownUserId: String,
        pageable: Pageable
    ): Page<Opportunity>

    @Query("""
        SELECT o.stage, COUNT(o), SUM(o.expectedAmount)
        FROM Opportunity o WHERE o.tenantId = :tenantId AND o.active = true
        AND o.stage NOT IN ('CLOSED_WON', 'CLOSED_LOST')
        AND (:applyOwnScope = false OR o.assignedTo = :ownUserId)
        GROUP BY o.stage ORDER BY o.stage
    """)
    fun getPipelineSummary(tenantId: String, applyOwnScope: Boolean, ownUserId: String): List<Array<Any>>

    fun findByTenantIdAndCustomerIdAndActiveTrue(tenantId: String, customerId: Long): List<Opportunity>
}

interface ActivityRepository : JpaRepository<Activity, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Activity>

    @Query("""
        SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND a.active = true
        AND (:referenceType IS NULL OR a.referenceType = :referenceType)
        AND (:referenceId IS NULL OR a.referenceId = :referenceId)
        AND (:applyOwnScope = false OR a.assignedTo = :ownUserId)
        ORDER BY a.activityDate DESC
    """)
    fun search(
        tenantId: String,
        referenceType: String?,
        referenceId: Long?,
        applyOwnScope: Boolean,
        ownUserId: String,
        pageable: Pageable
    ): Page<Activity>

    fun findByTenantIdAndReferenceTypeAndReferenceIdAndActiveTrue(
        tenantId: String, referenceType: String, referenceId: Long): List<Activity>
}
