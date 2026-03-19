package com.modularerp.currency.controller

import com.modularerp.currency.domain.*
import com.modularerp.currency.dto.*
import com.modularerp.currency.service.CurrencyService
import com.modularerp.web.dto.ApiResponse
import com.modularerp.web.dto.PageMeta
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/currencies")
@Tag(name = "Multi-Currency")
class CurrencyController(private val currencyService: CurrencyService) {

    // ── Currencies ──

    @GetMapping
    fun searchCurrencies(@RequestParam(required = false) status: CurrencyStatus?,
                         @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<CurrencyResponse>> {
        val page = currencyService.searchCurrencies(status, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @GetMapping("/{id}")
    fun getCurrency(@PathVariable id: Long) = ApiResponse.ok(currencyService.getCurrencyById(id))

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun createCurrency(@Valid @RequestBody req: CreateCurrencyRequest) =
        ApiResponse.ok(currencyService.createCurrency(req))

    @PutMapping("/{id}")
    fun updateCurrency(@PathVariable id: Long, @Valid @RequestBody req: CreateCurrencyRequest) =
        ApiResponse.ok(currencyService.updateCurrency(id, req))

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCurrency(@PathVariable id: Long) = currencyService.deleteCurrency(id)

    // ── Exchange Rates ──

    @GetMapping("/exchange-rates")
    fun searchExchangeRates(@RequestParam(required = false) fromCurrency: String?,
                            @RequestParam(required = false) toCurrency: String?,
                            @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<ExchangeRateResponse>> {
        val page = currencyService.searchExchangeRates(fromCurrency, toCurrency, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping("/exchange-rates") @ResponseStatus(HttpStatus.CREATED)
    fun createExchangeRate(@Valid @RequestBody req: CreateExchangeRateRequest) =
        ApiResponse.ok(currencyService.createExchangeRate(req))

    @GetMapping("/exchange-rates/latest")
    fun getLatestRate(@RequestParam fromCurrency: String,
                      @RequestParam toCurrency: String) =
        ApiResponse.ok(currencyService.getLatestRate(fromCurrency, toCurrency))

    // ── Convert ──

    @PostMapping("/convert")
    fun convert(@Valid @RequestBody req: ConvertRequest) = ApiResponse.ok(currencyService.convert(req))

    // ── Revaluations ──

    @GetMapping("/revaluations")
    fun searchRevaluations(@RequestParam(required = false) status: RevaluationStatus?,
                           @RequestParam(required = false) fiscalYear: Int?,
                           @PageableDefault(size = 20) pageable: Pageable): ApiResponse<List<RevaluationResponse>> {
        val page = currencyService.searchRevaluations(status, fiscalYear, pageable)
        return ApiResponse.ok(page.content, PageMeta(page.number, page.size, page.totalElements, page.totalPages))
    }

    @PostMapping("/revaluations") @ResponseStatus(HttpStatus.CREATED)
    fun createRevaluation(@Valid @RequestBody req: CreateRevaluationRequest) =
        ApiResponse.ok(currencyService.createRevaluation(req))

    @PostMapping("/revaluations/{id}/post")
    fun postRevaluation(@PathVariable id: Long) =
        ApiResponse.ok(currencyService.postRevaluation(id, "system"))

    @PostMapping("/revaluations/{id}/reverse")
    fun reverseRevaluation(@PathVariable id: Long) =
        ApiResponse.ok(currencyService.reverseRevaluation(id))
}
