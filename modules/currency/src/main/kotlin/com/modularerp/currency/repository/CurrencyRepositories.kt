package com.modularerp.currency.repository

import com.modularerp.currency.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.Optional

interface CurrencyRepository : JpaRepository<Currency, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<Currency>

    @Query("""
        SELECT c FROM Currency c WHERE c.tenantId = :tenantId AND c.active = true
        AND (:status IS NULL OR c.status = :status)
        ORDER BY c.currencyCode ASC
    """)
    fun search(tenantId: String, status: CurrencyStatus?, pageable: Pageable): Page<Currency>

    fun findByTenantIdAndCurrencyCodeAndActiveTrue(tenantId: String, currencyCode: String): Optional<Currency>
}

interface ExchangeRateRepository : JpaRepository<ExchangeRate, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<ExchangeRate>

    @Query("""
        SELECT er FROM ExchangeRate er WHERE er.tenantId = :tenantId AND er.active = true
        AND (:fromCurrency IS NULL OR er.fromCurrency = :fromCurrency)
        AND (:toCurrency IS NULL OR er.toCurrency = :toCurrency)
        ORDER BY er.rateDate DESC
    """)
    fun search(tenantId: String, fromCurrency: String?, toCurrency: String?,
               pageable: Pageable): Page<ExchangeRate>

    @Query("""
        SELECT er FROM ExchangeRate er WHERE er.tenantId = :tenantId AND er.active = true
        AND er.fromCurrency = :fromCurrency AND er.toCurrency = :toCurrency
        AND er.rateDate <= :date
        ORDER BY er.rateDate DESC
    """)
    fun findLatest(tenantId: String, fromCurrency: String, toCurrency: String,
                   date: LocalDate, pageable: Pageable): Page<ExchangeRate>

    @Query("""
        SELECT er FROM ExchangeRate er WHERE er.tenantId = :tenantId AND er.active = true
        AND er.fromCurrency = :fromCurrency AND er.toCurrency = :toCurrency
        AND er.rateDate BETWEEN :startDate AND :endDate
        ORDER BY er.rateDate ASC
    """)
    fun findHistory(tenantId: String, fromCurrency: String, toCurrency: String,
                    startDate: LocalDate, endDate: LocalDate): List<ExchangeRate>
}

interface CurrencyRevaluationRepository : JpaRepository<CurrencyRevaluation, Long> {
    fun findByTenantIdAndId(tenantId: String, id: Long): Optional<CurrencyRevaluation>

    @Query("""
        SELECT r FROM CurrencyRevaluation r WHERE r.tenantId = :tenantId AND r.active = true
        AND (:status IS NULL OR r.status = :status)
        AND (:fiscalYear IS NULL OR r.fiscalYear = :fiscalYear)
        ORDER BY r.revaluationDate DESC
    """)
    fun search(tenantId: String, status: RevaluationStatus?, fiscalYear: Int?,
               pageable: Pageable): Page<CurrencyRevaluation>
}
