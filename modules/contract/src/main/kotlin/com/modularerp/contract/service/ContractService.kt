package com.modularerp.contract.service

import com.modularerp.contract.domain.*
import com.modularerp.contract.dto.*
import com.modularerp.contract.repository.ContractRepository
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ContractService(
    private val repo: ContractRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {
    fun getById(id: Long) = find(id).toResponse()

    fun search(status: ContractStatus?, type: ContractType?, pageable: Pageable): Page<ContractResponse> =
        repo.search(TenantContext.getTenantId(), status, type, pageable).map { it.toResponse() }

    @Transactional
    fun create(req: CreateContractRequest): ContractResponse {
        val contract = Contract(
            documentNo = docNumberGenerator.next("CT", "CT"),
            title = req.title, contractType = req.contractType,
            counterpartyCode = req.counterpartyCode, counterpartyName = req.counterpartyName,
            startDate = req.startDate, endDate = req.endDate,
            contractAmount = req.contractAmount, currencyCode = req.currencyCode,
            terms = req.terms, description = req.description
        ).apply { assignTenant(TenantContext.getTenantId()) }
        return repo.save(contract).toResponse()
    }

    @Transactional fun activate(id: Long) = find(id).also { it.activateContract() }.let { repo.save(it).toResponse() }
    @Transactional fun terminate(id: Long) = find(id).also { it.terminate() }.let { repo.save(it).toResponse() }

    private fun find(id: Long) = repo.findByTenantIdAndId(TenantContext.getTenantId(), id)
        .orElseThrow { EntityNotFoundException("Contract", id) }
}
