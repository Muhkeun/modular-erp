package com.modularerp.currency.service

import com.modularerp.core.exception.EntityNotFoundException
import com.modularerp.currency.domain.*
import com.modularerp.currency.dto.*
import com.modularerp.currency.repository.*
import com.modularerp.document.service.DocumentNumberGenerator
import com.modularerp.security.tenant.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class CurrencyService(
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val revaluationRepository: CurrencyRevaluationRepository,
    private val docNumberGenerator: DocumentNumberGenerator
) {

    // ── Currency ──

    fun getCurrencyById(id: Long): CurrencyResponse = findCurrency(id).toResponse()

    fun searchCurrencies(status: CurrencyStatus?, pageable: Pageable): Page<CurrencyResponse> =
        currencyRepository.search(TenantContext.getTenantId(), status, pageable).map { it.toResponse() }

    @Transactional
    fun createCurrency(request: CreateCurrencyRequest): CurrencyResponse {
        val tenantId = TenantContext.getTenantId()
        val currency = Currency(
            currencyCode = request.currencyCode, currencyName = request.currencyName,
            symbol = request.symbol, decimalPlaces = request.decimalPlaces,
            isBaseCurrency = request.isBaseCurrency, status = request.status
        ).apply { assignTenant(tenantId) }
        return currencyRepository.save(currency).toResponse()
    }

    @Transactional
    fun updateCurrency(id: Long, request: CreateCurrencyRequest): CurrencyResponse {
        val currency = findCurrency(id)
        currency.currencyName = request.currencyName
        currency.symbol = request.symbol
        currency.decimalPlaces = request.decimalPlaces
        currency.isBaseCurrency = request.isBaseCurrency
        currency.status = request.status
        return currencyRepository.save(currency).toResponse()
    }

    @Transactional
    fun deleteCurrency(id: Long) { findCurrency(id).deactivate() }

    // ── ExchangeRate ──

    fun searchExchangeRates(fromCurrency: String?, toCurrency: String?,
                            pageable: Pageable): Page<ExchangeRateResponse> =
        exchangeRateRepository.search(TenantContext.getTenantId(), fromCurrency, toCurrency, pageable)
            .map { it.toResponse() }

    @Transactional
    fun createExchangeRate(request: CreateExchangeRateRequest): ExchangeRateResponse {
        val tenantId = TenantContext.getTenantId()
        val rate = ExchangeRate(
            fromCurrency = request.fromCurrency, toCurrency = request.toCurrency,
            rateDate = request.rateDate, exchangeRate = request.exchangeRate,
            rateType = request.rateType, source = request.source
        ).apply { assignTenant(tenantId) }
        return exchangeRateRepository.save(rate).toResponse()
    }

    fun getLatestRate(fromCurrency: String, toCurrency: String): ExchangeRateResponse {
        val tenantId = TenantContext.getTenantId()
        val page = exchangeRateRepository.findLatest(tenantId, fromCurrency, toCurrency,
            LocalDate.now(), PageRequest.of(0, 1))
        if (page.isEmpty) throw EntityNotFoundException("ExchangeRate", "from=$fromCurrency, to=$toCurrency")
        return page.content[0].toResponse()
    }

    fun convert(request: ConvertRequest): ConvertResponse {
        val tenantId = TenantContext.getTenantId()
        val page = exchangeRateRepository.findLatest(tenantId, request.fromCurrency, request.toCurrency,
            request.date, PageRequest.of(0, 1))
        if (page.isEmpty) throw EntityNotFoundException("ExchangeRate",
            "from=${request.fromCurrency}, to=${request.toCurrency}")
        val rate = page.content[0]
        val convertedAmount = request.amount.multiply(rate.exchangeRate).setScale(4, RoundingMode.HALF_UP)
        return ConvertResponse(
            fromAmount = request.amount, fromCurrency = request.fromCurrency,
            toAmount = convertedAmount, toCurrency = request.toCurrency,
            exchangeRate = rate.exchangeRate, rateDate = rate.rateDate
        )
    }

    fun getExchangeRateHistory(fromCurrency: String, toCurrency: String,
                               startDate: LocalDate, endDate: LocalDate): List<ExchangeRateResponse> =
        exchangeRateRepository.findHistory(TenantContext.getTenantId(), fromCurrency, toCurrency,
            startDate, endDate).map { it.toResponse() }

    // ── Revaluation ──

    fun searchRevaluations(status: RevaluationStatus?, fiscalYear: Int?,
                           pageable: Pageable): Page<RevaluationResponse> =
        revaluationRepository.search(TenantContext.getTenantId(), status, fiscalYear, pageable)
            .map { it.toResponse() }

    @Transactional
    fun createRevaluation(request: CreateRevaluationRequest): RevaluationResponse {
        val tenantId = TenantContext.getTenantId()
        val docNo = docNumberGenerator.next("REVAL", "REVAL")
        val reval = CurrencyRevaluation(
            documentNo = docNo, revaluationDate = request.revaluationDate,
            fiscalYear = request.fiscalYear, period = request.period,
            fromCurrency = request.fromCurrency, toCurrency = request.toCurrency,
            originalRate = request.originalRate, revaluationRate = request.revaluationRate,
            unrealizedGainLoss = request.unrealizedGainLoss
        ).apply { assignTenant(tenantId) }
        return revaluationRepository.save(reval).toResponse()
    }

    @Transactional
    fun postRevaluation(id: Long, userId: String): RevaluationResponse {
        val reval = findRevaluation(id)
        reval.post(userId)
        return revaluationRepository.save(reval).toResponse()
    }

    @Transactional
    fun reverseRevaluation(id: Long): RevaluationResponse {
        val reval = findRevaluation(id)
        reval.reverse()
        return revaluationRepository.save(reval).toResponse()
    }

    // ── Private helpers ──

    private fun findCurrency(id: Long): Currency =
        currencyRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("Currency", id) }

    private fun findRevaluation(id: Long): CurrencyRevaluation =
        revaluationRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
            .orElseThrow { EntityNotFoundException("CurrencyRevaluation", id) }
}
