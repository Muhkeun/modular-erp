package com.modularerp.crm.dto

import com.modularerp.crm.domain.*
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

// ── Customer DTOs ──

data class CreateCustomerRequest(
    @field:NotBlank val customerCode: String,
    @field:NotBlank val customerName: String,
    val customerType: CustomerType = CustomerType.CORPORATE,
    val industry: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val address: String? = null,
    val contactPerson: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val creditLimit: BigDecimal = BigDecimal.ZERO,
    val paymentTermDays: Int = 30,
    val status: CustomerStatus = CustomerStatus.PROSPECT,
    val notes: String? = null,
    val assignedTo: String? = null
)

data class CustomerResponse(
    val id: Long, val customerCode: String, val customerName: String,
    val customerType: CustomerType, val industry: String?, val phone: String?,
    val email: String?, val website: String?, val address: String?,
    val contactPerson: String?, val contactPhone: String?, val contactEmail: String?,
    val creditLimit: BigDecimal, val paymentTermDays: Int, val status: CustomerStatus,
    val notes: String?, val assignedTo: String?
)

fun Customer.toResponse() = CustomerResponse(
    id = id, customerCode = customerCode, customerName = customerName,
    customerType = customerType, industry = industry, phone = phone,
    email = email, website = website, address = address,
    contactPerson = contactPerson, contactPhone = contactPhone, contactEmail = contactEmail,
    creditLimit = creditLimit, paymentTermDays = paymentTermDays, status = status,
    notes = notes, assignedTo = assignedTo
)

// ── Lead DTOs ──

data class CreateLeadRequest(
    val companyName: String? = null,
    @field:NotBlank val contactName: String,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val source: LeadSource = LeadSource.OTHER,
    val estimatedValue: BigDecimal? = null,
    val assignedTo: String? = null,
    val notes: String? = null
)

data class LeadResponse(
    val id: Long, val leadNo: String, val companyName: String?, val contactName: String,
    val contactEmail: String?, val contactPhone: String?, val source: LeadSource,
    val status: LeadStatus, val estimatedValue: BigDecimal?, val assignedTo: String?,
    val notes: String?, val convertedCustomerId: Long?, val convertedAt: LocalDateTime?
)

fun Lead.toResponse() = LeadResponse(
    id = id, leadNo = leadNo, companyName = companyName, contactName = contactName,
    contactEmail = contactEmail, contactPhone = contactPhone, source = source,
    status = status, estimatedValue = estimatedValue, assignedTo = assignedTo,
    notes = notes, convertedCustomerId = convertedCustomerId, convertedAt = convertedAt
)

// ── Opportunity DTOs ──

data class CreateOpportunityRequest(
    val customerId: Long,
    @field:NotBlank val title: String,
    val description: String? = null,
    val stage: OpportunityStage = OpportunityStage.PROSPECTING,
    val probability: Int = 0,
    val expectedAmount: BigDecimal = BigDecimal.ZERO,
    val expectedCloseDate: LocalDate? = null,
    val assignedTo: String? = null
)

data class OpportunityResponse(
    val id: Long, val opportunityNo: String, val customerId: Long, val customerName: String,
    val title: String, val description: String?, val stage: OpportunityStage,
    val probability: Int, val expectedAmount: BigDecimal, val expectedCloseDate: LocalDate?,
    val actualAmount: BigDecimal?, val closedAt: LocalDateTime?, val lostReason: String?,
    val assignedTo: String?
)

fun Opportunity.toResponse() = OpportunityResponse(
    id = id, opportunityNo = opportunityNo, customerId = customer.id,
    customerName = customer.customerName, title = title, description = description,
    stage = stage, probability = probability, expectedAmount = expectedAmount,
    expectedCloseDate = expectedCloseDate, actualAmount = actualAmount,
    closedAt = closedAt, lostReason = lostReason, assignedTo = assignedTo
)

data class UpdateStageRequest(
    val stage: OpportunityStage,
    val actualAmount: BigDecimal? = null,
    val lostReason: String? = null
)

data class PipelineResponse(
    val stage: String, val count: Long, val totalAmount: BigDecimal
)

// ── Activity DTOs ──

data class CreateActivityRequest(
    val activityType: ActivityType = ActivityType.NOTE,
    @field:NotBlank val subject: String,
    val description: String? = null,
    val activityDate: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDateTime? = null,
    val referenceType: String? = null,
    val referenceId: Long? = null,
    val assignedTo: String? = null
)

data class ActivityResponse(
    val id: Long, val activityType: ActivityType, val subject: String,
    val description: String?, val activityDate: LocalDateTime, val dueDate: LocalDateTime?,
    val completed: Boolean, val completedAt: LocalDateTime?,
    val referenceType: String?, val referenceId: Long?, val assignedTo: String?
)

fun Activity.toResponse() = ActivityResponse(
    id = id, activityType = activityType, subject = subject, description = description,
    activityDate = activityDate, dueDate = dueDate, completed = completed,
    completedAt = completedAt, referenceType = referenceType, referenceId = referenceId,
    assignedTo = assignedTo
)
