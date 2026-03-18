package com.modularerp.contract.dto

import com.modularerp.contract.domain.*
import java.math.BigDecimal
import java.time.LocalDate

data class CreateContractRequest(
    val title: String, val contractType: ContractType,
    val counterpartyCode: String, val counterpartyName: String,
    val startDate: LocalDate, val endDate: LocalDate,
    val contractAmount: BigDecimal? = null, val currencyCode: String = "KRW",
    val terms: String? = null, val description: String? = null
)

data class ContractResponse(
    val id: Long, val documentNo: String, val title: String, val contractType: ContractType,
    val counterpartyCode: String, val counterpartyName: String,
    val startDate: LocalDate, val endDate: LocalDate,
    val contractAmount: BigDecimal?, val currencyCode: String,
    val status: ContractStatus, val terms: String?, val description: String?
)

fun Contract.toResponse() = ContractResponse(
    id = id, documentNo = documentNo, title = title, contractType = contractType,
    counterpartyCode = counterpartyCode, counterpartyName = counterpartyName,
    startDate = startDate, endDate = endDate, contractAmount = contractAmount,
    currencyCode = currencyCode, status = status, terms = terms, description = description
)
