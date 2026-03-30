package com.modularerp.crm.controller

import com.modularerp.admin.dto.DataScopeSearchFilter
import com.modularerp.admin.service.DataScopeService
import com.modularerp.core.exception.ForbiddenException
import com.modularerp.crm.domain.*
import com.modularerp.crm.dto.*
import com.modularerp.crm.service.CrmService
import com.modularerp.crm.service.ConvertLeadResponse
import com.modularerp.security.tenant.TenantContext
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/crm")
@Tag(name = "CRM")
class CrmController(
    private val crmService: CrmService,
    private val dataScopeService: DataScopeService
) {

    // ── Customers ──

    @GetMapping("/customers")
    fun searchCustomers(@RequestParam(required = false) status: CustomerStatus?,
                        @RequestParam(required = false) customerCode: String?,
                        @RequestParam(required = false) customerName: String?,
                        authentication: Authentication,
                        @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<CustomerResponse>> {
        val scopeFilter = resolveCrmScope(authentication, CRM_CUSTOMERS_RESOURCE)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else crmService.searchCustomers(status, customerCode, customerName, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/customers/{id}")
    fun getCustomer(@PathVariable id: Long, authentication: Authentication): ApiResponse<CustomerResponse> {
        val customer = crmService.getCustomerById(id)
        assertResourceAccessible(authentication, CRM_CUSTOMERS_RESOURCE, customer.assignedTo)
        return ApiResponse.ok(customer)
    }

    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
    fun createCustomer(
        @Valid @RequestBody req: CreateCustomerRequest,
        authentication: Authentication
    ): ApiResponse<CustomerResponse> {
        assertResourceWritable(authentication, CRM_CUSTOMERS_RESOURCE, req.assignedTo)
        return ApiResponse.ok(crmService.createCustomer(req))
    }

    @PutMapping("/customers/{id}")
    fun updateCustomer(
        @PathVariable id: Long,
        @Valid @RequestBody req: CreateCustomerRequest,
        authentication: Authentication
    ): ApiResponse<CustomerResponse> {
        val customer = crmService.getCustomerById(id)
        assertResourceAccessible(authentication, CRM_CUSTOMERS_RESOURCE, customer.assignedTo)
        assertResourceWritable(authentication, CRM_CUSTOMERS_RESOURCE, req.assignedTo)
        return ApiResponse.ok(crmService.updateCustomer(id, req))
    }

    @DeleteMapping("/customers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCustomer(@PathVariable id: Long, authentication: Authentication) {
        val customer = crmService.getCustomerById(id)
        assertResourceAccessible(authentication, CRM_CUSTOMERS_RESOURCE, customer.assignedTo)
        crmService.deleteCustomer(id)
    }

    // ── Leads ──

    @GetMapping("/leads")
    fun searchLeads(@RequestParam(required = false) status: LeadStatus?,
                    @RequestParam(required = false) source: LeadSource?,
                    @RequestParam(required = false) leadNo: String?,
                    authentication: Authentication,
                    @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<LeadResponse>> {
        val scopeFilter = resolveCrmScope(authentication, CRM_LEADS_RESOURCE)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else crmService.searchLeads(status, source, leadNo, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/leads/{id}")
    fun getLead(@PathVariable id: Long, authentication: Authentication): ApiResponse<LeadResponse> {
        val lead = crmService.getLeadById(id)
        assertResourceAccessible(authentication, CRM_LEADS_RESOURCE, lead.assignedTo)
        return ApiResponse.ok(lead)
    }

    @PostMapping("/leads") @ResponseStatus(HttpStatus.CREATED)
    fun createLead(@Valid @RequestBody req: CreateLeadRequest, authentication: Authentication): ApiResponse<LeadResponse> {
        assertResourceWritable(authentication, CRM_LEADS_RESOURCE, req.assignedTo)
        return ApiResponse.ok(crmService.createLead(req))
    }

    @PostMapping("/leads/{id}/convert")
    fun convertLead(@PathVariable id: Long, authentication: Authentication): ApiResponse<ConvertLeadResponse> {
        val lead = crmService.getLeadById(id)
        assertResourceAccessible(authentication, CRM_LEADS_RESOURCE, lead.assignedTo)
        assertResourceWritable(authentication, CRM_CUSTOMERS_RESOURCE, lead.assignedTo)
        assertResourceWritable(authentication, CRM_OPPORTUNITIES_RESOURCE, lead.assignedTo)
        return ApiResponse.ok(crmService.convertLead(id))
    }

    // ── Opportunities ──

    @GetMapping("/opportunities")
    fun searchOpportunities(@RequestParam(required = false) stage: OpportunityStage?,
                            @RequestParam(required = false) assignedTo: String?,
                            authentication: Authentication,
                            @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<OpportunityResponse>> {
        val scopeFilter = resolveCrmScope(authentication, CRM_OPPORTUNITIES_RESOURCE)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else crmService.searchOpportunities(stage, assignedTo, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/opportunities/{id}")
    fun getOpportunity(@PathVariable id: Long, authentication: Authentication): ApiResponse<OpportunityResponse> {
        val opportunity = crmService.getOpportunityById(id)
        assertResourceAccessible(authentication, CRM_OPPORTUNITIES_RESOURCE, opportunity.assignedTo)
        return ApiResponse.ok(opportunity)
    }

    @PostMapping("/opportunities") @ResponseStatus(HttpStatus.CREATED)
    fun createOpportunity(
        @Valid @RequestBody req: CreateOpportunityRequest,
        authentication: Authentication
    ): ApiResponse<OpportunityResponse> {
        val customer = crmService.getCustomerById(req.customerId)
        assertResourceAccessible(authentication, CRM_CUSTOMERS_RESOURCE, customer.assignedTo)
        assertResourceWritable(authentication, CRM_OPPORTUNITIES_RESOURCE, req.assignedTo)
        return ApiResponse.ok(crmService.createOpportunity(req))
    }

    @PutMapping("/opportunities/{id}/stage")
    fun updateStage(
        @PathVariable id: Long,
        @RequestBody req: UpdateStageRequest,
        authentication: Authentication
    ): ApiResponse<OpportunityResponse> {
        val opportunity = crmService.getOpportunityById(id)
        assertResourceAccessible(authentication, CRM_OPPORTUNITIES_RESOURCE, opportunity.assignedTo)
        return ApiResponse.ok(crmService.updateOpportunityStage(id, req))
    }

    // ── Activities ──

    @GetMapping("/activities")
    fun searchActivities(@RequestParam(required = false) referenceType: String?,
                         @RequestParam(required = false) referenceId: Long?,
                         authentication: Authentication,
                         @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<ActivityResponse>> {
        val scopeFilter = resolveCrmScope(authentication, CRM_ACTIVITIES_RESOURCE)
        val page =
            if (scopeFilter.denyAll) Page.empty(pageable)
            else crmService.searchActivities(referenceType, referenceId, scopeFilter, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/activities/{id}")
    fun getActivity(@PathVariable id: Long, authentication: Authentication): ApiResponse<ActivityResponse> {
        val activity = crmService.getActivityById(id)
        assertResourceAccessible(authentication, CRM_ACTIVITIES_RESOURCE, activity.assignedTo)
        return ApiResponse.ok(activity)
    }

    @PostMapping("/activities") @ResponseStatus(HttpStatus.CREATED)
    fun createActivity(
        @Valid @RequestBody req: CreateActivityRequest,
        authentication: Authentication
    ): ApiResponse<ActivityResponse> {
        assertResourceWritable(authentication, CRM_ACTIVITIES_RESOURCE, req.assignedTo)
        assertReferenceAccessible(authentication, req.referenceType, req.referenceId)
        return ApiResponse.ok(crmService.createActivity(req))
    }

    @PostMapping("/activities/{id}/complete")
    fun completeActivity(@PathVariable id: Long, authentication: Authentication): ApiResponse<ActivityResponse> {
        val activity = crmService.getActivityById(id)
        assertResourceAccessible(authentication, CRM_ACTIVITIES_RESOURCE, activity.assignedTo)
        return ApiResponse.ok(crmService.completeActivity(id))
    }

    // ── Pipeline ──

    @GetMapping("/pipeline")
    fun getPipeline(authentication: Authentication): ApiResponse<List<PipelineResponse>> {
        val scopeFilter = resolveCrmScope(authentication, CRM_OPPORTUNITIES_RESOURCE)
        return ApiResponse.ok(if (scopeFilter.denyAll) emptyList() else crmService.getSalesPipeline(scopeFilter))
    }

    private fun assertReferenceAccessible(authentication: Authentication, referenceType: String?, referenceId: Long?) {
        if (referenceType.isNullOrBlank() || referenceId == null) return

        when (referenceType.uppercase()) {
            "CUSTOMER" -> assertResourceAccessible(
                authentication,
                CRM_CUSTOMERS_RESOURCE,
                crmService.getCustomerById(referenceId).assignedTo
            )
            "LEAD" -> assertResourceAccessible(
                authentication,
                CRM_LEADS_RESOURCE,
                crmService.getLeadById(referenceId).assignedTo
            )
            "OPPORTUNITY" -> assertResourceAccessible(
                authentication,
                CRM_OPPORTUNITIES_RESOURCE,
                crmService.getOpportunityById(referenceId).assignedTo
            )
        }
    }

    private fun resolveCrmScope(authentication: Authentication, resource: String): DataScopeSearchFilter =
        dataScopeService.resolveSearchFilter(
            authentication.authorities.map { it.authority.removePrefix("ROLE_") },
            resource,
            TenantContext.getUserId()
        ).narrowToSupported(
            supportsOwn = true,
            supportsCompany = false,
            supportsDepartment = false,
            supportsPlant = false
        )

    private fun assertResourceAccessible(authentication: Authentication, resource: String, ownerId: String?) {
        val scopeFilter = resolveCrmScope(authentication, resource)
        if (!scopeFilter.matchesSupported(
                ownerId = ownerId,
                supportsOwn = true,
                supportsCompany = false,
                supportsDepartment = false,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow access to this document")
        }
    }

    private fun assertResourceWritable(authentication: Authentication, resource: String, ownerId: String?) {
        val scopeFilter = resolveCrmScope(authentication, resource)
        if (!scopeFilter.matchesSupported(
                ownerId = ownerId,
                supportsOwn = true,
                supportsCompany = false,
                supportsDepartment = false,
                supportsPlant = false
            )) {
            throw ForbiddenException("The current data scope does not allow writing this document")
        }
    }
}

private const val CRM_CUSTOMERS_RESOURCE = "crm-customers"
private const val CRM_LEADS_RESOURCE = "crm-leads"
private const val CRM_OPPORTUNITIES_RESOURCE = "crm-opportunities"
private const val CRM_ACTIVITIES_RESOURCE = "crm-activities"
