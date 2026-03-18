package com.modularerp.account.service

import com.modularerp.account.domain.*
import com.modularerp.account.dto.*
import com.modularerp.account.repository.JournalEntryRepository
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class JournalEntryService(
    private val jeRepository: JournalEntryRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    fun getById(id: Long): JeResponse = findJe(id).toResponse()

    fun search(status: JeStatus?, entryType: JournalEntryType?, documentNo: String?, pageable: Pageable): Page<JeResponse> =
        jeRepository.search(TenantContext.getTenantId(), status, entryType, documentNo, pageable).map { it.toResponse() }

    @Transactional
    fun create(request: CreateJeRequest): JeResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("JE", "JE")

        val je = JournalEntry(
            documentNo = docNo, companyCode = request.companyCode,
            postingDate = request.postingDate, entryType = request.entryType,
            referenceDocNo = request.referenceDocNo, referenceDocType = request.referenceDocType,
            description = request.description, currencyCode = request.currencyCode
        ).apply { assignTenant(tenantId) }

        request.lines.forEach { line ->
            if (line.debitAmount > BigDecimal.ZERO) {
                je.addDebitLine(line.accountCode, line.accountName, line.debitAmount, line.costCenter, line.description)
                    .assignTenant(tenantId)
            }
            if (line.creditAmount > BigDecimal.ZERO) {
                je.addCreditLine(line.accountCode, line.accountName, line.creditAmount, line.costCenter, line.description)
                    .assignTenant(tenantId)
            }
        }

        return jeRepository.save(je).toResponse()
    }

    @Transactional
    fun post(id: Long): JeResponse {
        val je = findJe(id)
        je.post()
        return jeRepository.save(je).toResponse()
    }

    @Transactional
    fun reverse(id: Long): JeResponse {
        val je = findJe(id)
        je.reverse()
        return jeRepository.save(je).toResponse()
    }

    private fun findJe(id: Long): JournalEntry =
        jeRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("JournalEntry", id) }
}
