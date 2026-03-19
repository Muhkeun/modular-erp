package com.modularerp.crm.controller

import com.modularerp.crm.domain.*
import com.modularerp.crm.dto.*
import com.modularerp.crm.service.CrmService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/crm")
@Tag(name = "CRM")
class CrmController(private val crmService: CrmService) {

    // ── Customers ──

    @GetMapping("/customers")
    fun searchCustomers(@RequestParam(required = false) status: CustomerStatus?,
                        @RequestParam(required = false) customerCode: String?,
                        @RequestParam(required = false) customerName: String?,
                        @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<CustomerResponse>> {
        val page = crmService.searchCustomers(status, customerCode, customerName, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/customers/{id}")
    fun getCustomer(@PathVariable id: Long) = ApiResponse.ok(crmService.getCustomerById(id))

    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
    fun createCustomer(@Valid @RequestBody req: CreateCustomerRequest) = ApiResponse.ok(crmService.createCustomer(req))

    @PutMapping("/customers/{id}")
    fun updateCustomer(@PathVariable id: Long, @Valid @RequestBody req: CreateCustomerRequest) =
        ApiResponse.ok(crmService.updateCustomer(id, req))

    @DeleteMapping("/customers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCustomer(@PathVariable id: Long) = crmService.deleteCustomer(id)

    // ── Leads ──

    @GetMapping("/leads")
    fun searchLeads(@RequestParam(required = false) status: LeadStatus?,
                    @RequestParam(required = false) source: LeadSource?,
                    @RequestParam(required = false) leadNo: String?,
                    @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<LeadResponse>> {
        val page = crmService.searchLeads(status, source, leadNo, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/leads/{id}")
    fun getLead(@PathVariable id: Long) = ApiResponse.ok(crmService.getLeadById(id))

    @PostMapping("/leads") @ResponseStatus(HttpStatus.CREATED)
    fun createLead(@Valid @RequestBody req: CreateLeadRequest) = ApiResponse.ok(crmService.createLead(req))

    @PostMapping("/leads/{id}/convert")
    fun convertLead(@PathVariable id: Long) = ApiResponse.ok(crmService.convertLead(id))

    // ── Opportunities ──

    @GetMapping("/opportunities")
    fun searchOpportunities(@RequestParam(required = false) stage: OpportunityStage?,
                            @RequestParam(required = false) assignedTo: String?,
                            @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<OpportunityResponse>> {
        val page = crmService.searchOpportunities(stage, assignedTo, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/opportunities/{id}")
    fun getOpportunity(@PathVariable id: Long) = ApiResponse.ok(crmService.getOpportunityById(id))

    @PostMapping("/opportunities") @ResponseStatus(HttpStatus.CREATED)
    fun createOpportunity(@Valid @RequestBody req: CreateOpportunityRequest) =
        ApiResponse.ok(crmService.createOpportunity(req))

    @PutMapping("/opportunities/{id}/stage")
    fun updateStage(@PathVariable id: Long, @RequestBody req: UpdateStageRequest) =
        ApiResponse.ok(crmService.updateOpportunityStage(id, req))

    // ── Activities ──

    @GetMapping("/activities")
    fun searchActivities(@RequestParam(required = false) referenceType: String?,
                         @RequestParam(required = false) referenceId: Long?,
                         @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<ActivityResponse>> {
        val page = crmService.searchActivities(referenceType, referenceId, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/activities/{id}")
    fun getActivity(@PathVariable id: Long) = ApiResponse.ok(crmService.getActivityById(id))

    @PostMapping("/activities") @ResponseStatus(HttpStatus.CREATED)
    fun createActivity(@Valid @RequestBody req: CreateActivityRequest) = ApiResponse.ok(crmService.createActivity(req))

    @PostMapping("/activities/{id}/complete")
    fun completeActivity(@PathVariable id: Long) = ApiResponse.ok(crmService.completeActivity(id))

    // ── Pipeline ──

    @GetMapping("/pipeline")
    fun getPipeline() = ApiResponse.ok(crmService.getSalesPipeline())
}
