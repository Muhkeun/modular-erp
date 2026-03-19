package com.modularerp.crm.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.crm.domain.*
import com.modularerp.crm.dto.*
import com.modularerp.crm.repository.*
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class CrmService(
    private val customerRepository: CustomerRepository,
    private val leadRepository: LeadRepository,
    private val opportunityRepository: OpportunityRepository,
    private val activityRepository: ActivityRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    // ── Customer ──

    fun getCustomerById(id: Long): CustomerResponse = findCustomer(id).toResponse()

    fun searchCustomers(status: CustomerStatus?, customerCode: String?, customerName: String?,
                        pageable: Pageable): Page<CustomerResponse> =
        customerRepository.search(TenantContext.getTenantId(), status, customerCode, customerName, pageable)
            .map { it.toResponse() }

    @Transactional
    fun createCustomer(request: CreateCustomerRequest): CustomerResponse {
        val tenantId = TenantContext.getTenantId()
        val customer = Customer(
            customerCode = request.customerCode, customerName = request.customerName,
            customerType = request.customerType, industry = request.industry,
            phone = request.phone, email = request.email, website = request.website,
            address = request.address, contactPerson = request.contactPerson,
            contactPhone = request.contactPhone, contactEmail = request.contactEmail,
            creditLimit = request.creditLimit, paymentTermDays = request.paymentTermDays,
            status = request.status, notes = request.notes, assignedTo = request.assignedTo
        ).apply { assignTenant(tenantId) }
        return customerRepository.save(customer).toResponse()
    }

    @Transactional
    fun updateCustomer(id: Long, request: CreateCustomerRequest): CustomerResponse {
        val customer = findCustomer(id)
        customer.customerName = request.customerName
        customer.customerType = request.customerType
        customer.industry = request.industry
        customer.phone = request.phone
        customer.email = request.email
        customer.website = request.website
        customer.address = request.address
        customer.contactPerson = request.contactPerson
        customer.contactPhone = request.contactPhone
        customer.contactEmail = request.contactEmail
        customer.creditLimit = request.creditLimit
        customer.paymentTermDays = request.paymentTermDays
        customer.status = request.status
        customer.notes = request.notes
        customer.assignedTo = request.assignedTo
        return customerRepository.save(customer).toResponse()
    }

    @Transactional
    fun deleteCustomer(id: Long) { findCustomer(id).deactivate() }

    // ── Lead ──

    fun getLeadById(id: Long): LeadResponse = findLead(id).toResponse()

    fun searchLeads(status: LeadStatus?, source: LeadSource?, leadNo: String?,
                    pageable: Pageable): Page<LeadResponse> =
        leadRepository.search(TenantContext.getTenantId(), status, source, leadNo, pageable)
            .map { it.toResponse() }

    @Transactional
    fun createLead(request: CreateLeadRequest): LeadResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("LEAD", "LEAD")
        val lead = Lead(
            leadNo = docNo, companyName = request.companyName, contactName = request.contactName,
            contactEmail = request.contactEmail, contactPhone = request.contactPhone,
            source = request.source, estimatedValue = request.estimatedValue,
            assignedTo = request.assignedTo, notes = request.notes
        ).apply { assignTenant(tenantId) }
        return leadRepository.save(lead).toResponse()
    }

    @Transactional
    fun convertLead(leadId: Long): ConvertLeadResponse {
        val lead = findLead(leadId)
        val tenantId = TenantContext.getTenantId()

        // Create customer from lead
        val custCode = docNumberGenerator.next("CUST", "CUST")
        val customer = Customer(
            customerCode = custCode,
            customerName = lead.companyName ?: lead.contactName,
            contactPerson = lead.contactName,
            contactEmail = lead.contactEmail,
            contactPhone = lead.contactPhone,
            status = CustomerStatus.ACTIVE,
            assignedTo = lead.assignedTo
        ).apply { assignTenant(tenantId) }
        val savedCustomer = customerRepository.save(customer)

        // Create opportunity
        val oppNo = docNumberGenerator.next("OPP", "OPP")
        val opportunity = Opportunity(
            opportunityNo = oppNo,
            customer = savedCustomer,
            title = "Opportunity from Lead ${lead.leadNo}",
            expectedAmount = lead.estimatedValue ?: BigDecimal.ZERO,
            assignedTo = lead.assignedTo
        ).apply { assignTenant(tenantId) }
        val savedOpp = opportunityRepository.save(opportunity)

        // Convert lead
        lead.convert(savedCustomer.id)
        leadRepository.save(lead)

        return ConvertLeadResponse(
            lead = lead.toResponse(),
            customer = savedCustomer.toResponse(),
            opportunity = savedOpp.toResponse()
        )
    }

    // ── Opportunity ──

    fun getOpportunityById(id: Long): OpportunityResponse = findOpportunity(id).toResponse()

    fun searchOpportunities(stage: OpportunityStage?, assignedTo: String?,
                            pageable: Pageable): Page<OpportunityResponse> =
        opportunityRepository.search(TenantContext.getTenantId(), stage, assignedTo, pageable)
            .map { it.toResponse() }

    @Transactional
    fun createOpportunity(request: CreateOpportunityRequest): OpportunityResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("OPP", "OPP")
        val customer = findCustomer(request.customerId)
        val opp = Opportunity(
            opportunityNo = docNo, customer = customer, title = request.title,
            description = request.description, stage = request.stage,
            probability = request.probability, expectedAmount = request.expectedAmount,
            expectedCloseDate = request.expectedCloseDate, assignedTo = request.assignedTo
        ).apply { assignTenant(tenantId) }
        return opportunityRepository.save(opp).toResponse()
    }

    @Transactional
    fun updateOpportunityStage(id: Long, request: UpdateStageRequest): OpportunityResponse {
        val opp = findOpportunity(id)
        opp.updateStage(request.stage)
        if (request.actualAmount != null) opp.actualAmount = request.actualAmount
        if (request.lostReason != null) opp.lostReason = request.lostReason
        return opportunityRepository.save(opp).toResponse()
    }

    fun getSalesPipeline(): List<PipelineResponse> =
        opportunityRepository.getPipelineSummary(TenantContext.getTenantId()).map { row ->
            PipelineResponse(
                stage = (row[0] as OpportunityStage).name,
                count = row[1] as Long,
                totalAmount = row[2] as BigDecimal
            )
        }

    fun getCustomerActivities(customerId: Long, pageable: Pageable): Page<ActivityResponse> =
        activityRepository.search(TenantContext.getTenantId(), "CUSTOMER", customerId, pageable)
            .map { it.toResponse() }

    fun getAssignedOpportunities(assignedTo: String, pageable: Pageable): Page<OpportunityResponse> =
        opportunityRepository.search(TenantContext.getTenantId(), null, assignedTo, pageable)
            .map { it.toResponse() }

    // ── Activity ──

    fun getActivityById(id: Long): ActivityResponse = findActivity(id).toResponse()

    fun searchActivities(referenceType: String?, referenceId: Long?,
                         pageable: Pageable): Page<ActivityResponse> =
        activityRepository.search(TenantContext.getTenantId(), referenceType, referenceId, pageable)
            .map { it.toResponse() }

    @Transactional
    fun createActivity(request: CreateActivityRequest): ActivityResponse {
        val tenantId = TenantContext.getTenantId()
        val activity = Activity(
            activityType = request.activityType, subject = request.subject,
            description = request.description, activityDate = request.activityDate,
            dueDate = request.dueDate, referenceType = request.referenceType,
            referenceId = request.referenceId, assignedTo = request.assignedTo
        ).apply { assignTenant(tenantId) }
        return activityRepository.save(activity).toResponse()
    }

    @Transactional
    fun completeActivity(id: Long): ActivityResponse {
        val activity = findActivity(id)
        activity.complete()
        return activityRepository.save(activity).toResponse()
    }

    // ── Private helpers ──

    private fun findCustomer(id: Long): Customer =
        customerRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("Customer", id) }

    private fun findLead(id: Long): Lead =
        leadRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("Lead", id) }

    private fun findOpportunity(id: Long): Opportunity =
        opportunityRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("Opportunity", id) }

    private fun findActivity(id: Long): Activity =
        activityRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("Activity", id) }
}

data class ConvertLeadResponse(
    val lead: LeadResponse,
    val customer: CustomerResponse,
    val opportunity: OpportunityResponse
)
