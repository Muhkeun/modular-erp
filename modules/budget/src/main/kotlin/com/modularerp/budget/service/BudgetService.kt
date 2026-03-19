package com.modularerp.budget.service

import com.modularerp.budget.domain.*
import com.modularerp.budget.dto.*
import com.modularerp.budget.repository.*
import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
@Transactional(readOnly = true)
class BudgetService(
    private val periodRepository: BudgetPeriodRepository,
    private val itemRepository: BudgetItemRepository,
    private val transferRepository: BudgetTransferRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    // --- Budget Period ---

    fun getPeriodById(id: Long): BudgetPeriodResponse = findPeriod(id).toResponse()

    fun searchPeriods(status: BudgetPeriodStatus?, fiscalYear: Int?, pageable: Pageable): Page<BudgetPeriodResponse> =
        periodRepository.search(TenantContext.getTenantId(), status, fiscalYear, pageable).map { it.toResponse() }

    @Transactional
    fun createBudgetPeriod(request: CreateBudgetPeriodRequest): BudgetPeriodResponse {
        val tenantId = TenantContext.getTenantId()
        val period = BudgetPeriod(
            fiscalYear = request.fiscalYear, periodType = request.periodType,
            startDate = request.startDate, endDate = request.endDate,
            description = request.description
        ).apply { assignTenant(tenantId) }
        return periodRepository.save(period).toResponse()
    }

    @Transactional
    fun updateBudgetPeriod(id: Long, request: UpdateBudgetPeriodRequest): BudgetPeriodResponse {
        val period = findPeriod(id)
        check(period.status == BudgetPeriodStatus.DRAFT) { "Can only update DRAFT periods" }
        request.description?.let { period.description = it }
        request.startDate?.let { period.startDate = it }
        request.endDate?.let { period.endDate = it }
        return periodRepository.save(period).toResponse()
    }

    @Transactional
    fun approvePeriod(id: Long): BudgetPeriodResponse {
        val period = findPeriod(id)
        period.approve()
        period.activatePeriod()
        return periodRepository.save(period).toResponse()
    }

    @Transactional
    fun closeBudgetPeriod(id: Long): BudgetPeriodResponse {
        val period = findPeriod(id)
        period.close()
        return periodRepository.save(period).toResponse()
    }

    // --- Budget Item ---

    fun getItemById(id: Long): BudgetItemResponse = findItem(id).toResponse()

    fun getItemsByPeriod(periodId: Long, pageable: Pageable): Page<BudgetItemResponse> =
        itemRepository.findByPeriod(TenantContext.getTenantId(), periodId, pageable).map { it.toResponse() }

    @Transactional
    fun createBudgetItem(request: CreateBudgetItemRequest): BudgetItemResponse {
        val tenantId = TenantContext.getTenantId()
        val period = findPeriod(request.budgetPeriodId)
        val item = BudgetItem(
            budgetPeriod = period, accountCode = request.accountCode,
            accountName = request.accountName, departmentCode = request.departmentCode,
            plantCode = request.plantCode, budgetAmount = request.budgetAmount,
            revisedAmount = request.revisedAmount ?: request.budgetAmount,
            currency = request.currency, notes = request.notes
        ).apply { assignTenant(tenantId) }
        return itemRepository.save(item).toResponse()
    }

    @Transactional
    fun updateBudgetItem(id: Long, request: UpdateBudgetItemRequest): BudgetItemResponse {
        val item = findItem(id)
        request.accountName?.let { item.accountName = it }
        request.budgetAmount?.let { item.budgetAmount = it }
        request.revisedAmount?.let { item.revisedAmount = it }
        request.notes?.let { item.notes = it }
        return itemRepository.save(item).toResponse()
    }

    // --- Budget Transfer ---

    @Transactional
    fun transferBudget(request: CreateBudgetTransferRequest): BudgetTransferResponse {
        val tenantId = TenantContext.getTenantId()
        val fromItem = findItem(request.fromBudgetItemId)
        val toItem = findItem(request.toBudgetItemId)

        check(fromItem.remainingAmount >= request.amount) {
            "Insufficient budget. Available: ${fromItem.remainingAmount}, Requested: ${request.amount}"
        }

        val docNo = docNumberGenerator.next("BT", "BT")
        val transfer = BudgetTransfer(
            documentNo = docNo, transferDate = request.transferDate,
            fromBudgetItem = fromItem, toBudgetItem = toItem,
            amount = request.amount, reason = request.reason
        ).apply { assignTenant(tenantId) }

        // Apply transfer immediately
        transfer.approve(TenantContext.getTenantId())
        transfer.complete()
        fromItem.revisedAmount = fromItem.revisedAmount.subtract(request.amount)
        toItem.revisedAmount = toItem.revisedAmount.add(request.amount)

        itemRepository.save(fromItem)
        itemRepository.save(toItem)
        return transferRepository.save(transfer).toResponse()
    }

    // --- Analysis ---

    fun getBudgetVsActual(periodId: Long, pageable: Pageable): Page<BudgetAnalysisResponse> {
        return itemRepository.findByPeriod(TenantContext.getTenantId(), periodId, pageable).map { item ->
            BudgetAnalysisResponse(
                accountCode = item.accountCode, accountName = item.accountName,
                budgetAmount = item.budgetAmount, revisedAmount = item.revisedAmount,
                actualAmount = item.actualAmount, remainingAmount = item.remainingAmount,
                utilizationRate = if (item.revisedAmount.compareTo(BigDecimal.ZERO) != 0)
                    item.actualAmount.multiply(BigDecimal(100)).divide(item.revisedAmount, 2, RoundingMode.HALF_UP)
                else BigDecimal.ZERO
            )
        }
    }

    fun checkBudgetAvailability(accountCode: String, amount: BigDecimal): Boolean {
        val items = itemRepository.findActiveByAccountCode(TenantContext.getTenantId(), accountCode)
        return items.any { it.remainingAmount >= amount }
    }

    // --- Private helpers ---

    private fun findPeriod(id: Long): BudgetPeriod =
        periodRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("BudgetPeriod", id) }

    private fun findItem(id: Long): BudgetItem =
        itemRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("BudgetItem", id) }
}
