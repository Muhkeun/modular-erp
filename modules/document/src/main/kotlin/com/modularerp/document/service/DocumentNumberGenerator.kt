package com.modularerp.document.service

import com.modularerp.document.domain.DocumentSequence
import com.modularerp.document.repository.DocumentSequenceRepository
import com.modularerp.security.tenant.TenantContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DocumentNumberGenerator(
    private val repository: DocumentSequenceRepository
) {
    private val periodFormat = DateTimeFormatter.ofPattern("yyyyMM")

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun next(documentType: String, prefix: String): String {
        val tenantId = TenantContext.getTenantId()
        val period = LocalDate.now().format(periodFormat)

        val seq = repository.findForUpdate(tenantId, documentType, period)
            .orElseGet {
                val newSeq = DocumentSequence(
                    documentType = documentType,
                    prefix = prefix,
                    period = period
                ).apply { assignTenant(tenantId) }
                repository.save(newSeq)
            }

        val docNo = seq.next()
        repository.save(seq)
        return docNo
    }
}
