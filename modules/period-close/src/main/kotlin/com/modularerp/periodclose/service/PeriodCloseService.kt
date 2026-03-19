package com.modularerp.periodclose.service

import com.modularerp.periodclose.domain.*
import com.modularerp.periodclose.dto.*
import com.modularerp.periodclose.repository.*
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
@Transactional(readOnly = true)
class PeriodCloseService(
    private val periodRepository: FiscalPeriodRepository,
    private val taskRepository: PeriodCloseTaskRepository,
    private val closingEntryRepository: ClosingEntryRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    fun searchPeriods(fiscalYear: Int?, status: FiscalPeriodStatus?, pageable: Pageable): Page<FiscalPeriodResponse> =
        periodRepository.search(TenantContext.getTenantId(), fiscalYear, status, pageable).map { it.toResponse() }

    fun getPeriodById(id: Long): FiscalPeriodResponse = findPeriod(id).toResponse()

    /**
     * Generate 12 fiscal periods for a given year.
     */
    @Transactional
    fun createFiscalYear(fiscalYear: Int): List<FiscalPeriodResponse> {
        val tenantId = TenantContext.getTenantId()
        val existing = periodRepository.findByFiscalYear(tenantId, fiscalYear)
        check(existing.isEmpty()) { "Fiscal year $fiscalYear already exists" }

        val periods = (1..12).map { month ->
            val ym = YearMonth.of(fiscalYear, month)
            FiscalPeriod(
                fiscalYear = fiscalYear, period = month,
                periodName = "$fiscalYear-%02d".format(month),
                startDate = ym.atDay(1), endDate = ym.atEndOfMonth()
            ).apply {
                assignTenant(tenantId)
                // Add default close tasks
                val defaultTasks = listOf(
                    CloseTaskType.AP_CLOSE to "Accounts Payable Close",
                    CloseTaskType.AR_CLOSE to "Accounts Receivable Close",
                    CloseTaskType.INVENTORY_CLOSE to "Inventory Close",
                    CloseTaskType.DEPRECIATION_RUN to "Depreciation Run",
                    CloseTaskType.ACCRUAL_POSTING to "Accrual Posting",
                    CloseTaskType.RECONCILIATION to "Reconciliation",
                    CloseTaskType.FINANCIAL_REPORT to "Financial Report Generation"
                )
                defaultTasks.forEachIndexed { idx, (type, name) ->
                    val task = PeriodCloseTask(
                        fiscalPeriod = this, taskType = type,
                        taskName = name, sequence = idx + 1
                    ).apply { assignTenant(tenantId) }
                    tasks.add(task)
                }
            }
        }
        return periodRepository.saveAll(periods).map { it.toResponse() }
    }

    @Transactional
    fun softClosePeriod(id: Long): FiscalPeriodResponse {
        val period = findPeriod(id)
        period.softClose(TenantContext.getTenantId())
        return periodRepository.save(period).toResponse()
    }

    @Transactional
    fun hardClosePeriod(id: Long): FiscalPeriodResponse {
        val period = findPeriod(id)
        period.hardClose(TenantContext.getTenantId())
        return periodRepository.save(period).toResponse()
    }

    @Transactional
    fun reopenPeriod(id: Long): FiscalPeriodResponse {
        val period = findPeriod(id)
        period.reopen()
        return periodRepository.save(period).toResponse()
    }

    fun getCloseChecklist(periodId: Long): List<PeriodCloseTaskResponse> =
        taskRepository.findByPeriod(TenantContext.getTenantId(), periodId).map { it.toResponse() }

    @Transactional
    fun executeCloseTask(periodId: Long, taskId: Long): PeriodCloseTaskResponse {
        val task = taskRepository.findByTenantIdAndId(TenantContext.getTenantId(), taskId)
            .orElseThrow { EntityNotFoundException("PeriodCloseTask", taskId) }
        check(task.fiscalPeriod.id == periodId) { "Task does not belong to period" }
        task.start(TenantContext.getTenantId())
        task.complete()
        return taskRepository.save(task).toResponse()
    }

    @Transactional
    fun createClosingEntry(request: CreateClosingEntryRequest): ClosingEntryResponse {
        val tenantId = TenantContext.getTenantId()
        val period = findPeriod(request.fiscalPeriodId)
        check(period.status != FiscalPeriodStatus.HARD_CLOSE) { "Cannot create entries for HARD_CLOSED period" }

        val docNo = docNumberGenerator.next("CE", "CE")
        val entry = ClosingEntry(
            fiscalPeriod = period, documentNo = docNo,
            entryType = request.entryType, description = request.description,
            debitAccount = request.debitAccount, creditAccount = request.creditAccount,
            amount = request.amount, reversalDate = request.reversalDate
        ).apply { assignTenant(tenantId) }
        return closingEntryRepository.save(entry).toResponse()
    }

    fun getClosingEntries(periodId: Long, pageable: Pageable): Page<ClosingEntryResponse> =
        closingEntryRepository.findByPeriod(TenantContext.getTenantId(), periodId, pageable).map { it.toResponse() }

    private fun findPeriod(id: Long): FiscalPeriod =
        periodRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("FiscalPeriod", id) }
}
