package com.modularerp.currency.dto

import com.modularerp.currency.domain.*
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

// ── Currency DTOs ──

data class CreateCurrencyRequest(
    @field:NotBlank val currencyCode: String,
    @field:NotBlank val currencyName: String,
    @field:NotBlank val symbol: String,
    val decimalPlaces: Int = 2,
    val isBaseCurrency: Boolean = false,
    val status: CurrencyStatus = CurrencyStatus.ACTIVE
)

data class CurrencyResponse(
    val id: Long, val currencyCode: String, val currencyName: String,
    val symbol: String, val decimalPlaces: Int, val isBaseCurrency: Boolean,
    val status: CurrencyStatus
)

fun Currency.toResponse() = CurrencyResponse(
    id = id, currencyCode = currencyCode, currencyName = currencyName,
    symbol = symbol, decimalPlaces = decimalPlaces, isBaseCurrency = isBaseCurrency,
    status = status
)

// ── ExchangeRate DTOs ──

data class CreateExchangeRateRequest(
    @field:NotBlank val fromCurrency: String,
    @field:NotBlank val toCurrency: String,
    val rateDate: LocalDate = LocalDate.now(),
    val exchangeRate: BigDecimal,
    val rateType: RateType = RateType.SPOT,
    val source: String? = null
)

data class ExchangeRateResponse(
    val id: Long, val fromCurrency: String, val toCurrency: String,
    val rateDate: LocalDate, val exchangeRate: BigDecimal,
    val rateType: RateType, val source: String?
)

fun ExchangeRate.toResponse() = ExchangeRateResponse(
    id = id, fromCurrency = fromCurrency, toCurrency = toCurrency,
    rateDate = rateDate, exchangeRate = exchangeRate,
    rateType = rateType, source = source
)

// ── Conversion DTOs ──

data class ConvertRequest(
    val amount: BigDecimal,
    @field:NotBlank val fromCurrency: String,
    @field:NotBlank val toCurrency: String,
    val date: LocalDate = LocalDate.now()
)

data class ConvertResponse(
    val fromAmount: BigDecimal, val fromCurrency: String,
    val toAmount: BigDecimal, val toCurrency: String,
    val exchangeRate: BigDecimal, val rateDate: LocalDate
)

// ── Revaluation DTOs ──

data class CreateRevaluationRequest(
    val revaluationDate: LocalDate = LocalDate.now(),
    val fiscalYear: Int,
    val period: Int,
    @field:NotBlank val fromCurrency: String,
    @field:NotBlank val toCurrency: String,
    val originalRate: BigDecimal,
    val revaluationRate: BigDecimal,
    val unrealizedGainLoss: BigDecimal
)

data class RevaluationResponse(
    val id: Long, val documentNo: String, val revaluationDate: LocalDate,
    val fiscalYear: Int, val period: Int, val fromCurrency: String, val toCurrency: String,
    val originalRate: BigDecimal, val revaluationRate: BigDecimal,
    val unrealizedGainLoss: BigDecimal, val status: RevaluationStatus,
    val postedBy: String?, val postedAt: LocalDateTime?
)

fun CurrencyRevaluation.toResponse() = RevaluationResponse(
    id = id, documentNo = documentNo, revaluationDate = revaluationDate,
    fiscalYear = fiscalYear, period = period, fromCurrency = fromCurrency,
    toCurrency = toCurrency, originalRate = originalRate, revaluationRate = revaluationRate,
    unrealizedGainLoss = unrealizedGainLoss, status = status,
    postedBy = postedBy, postedAt = postedAt
)
