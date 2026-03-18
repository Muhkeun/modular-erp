package com.modularerp.core.port

import com.modularerp.core.vo.Money

/**
 * Cross-module query ports.
 * Each module implements the ports it owns.
 * In monolith: direct method call. In MSA: REST adapter.
 */

// Master Data ports
data class ItemInfo(
    val id: Long,
    val code: String,
    val name: String,
    val unitOfMeasure: String,
    val itemType: String,
    val itemGroup: String?
)

interface ItemQueryPort {
    fun findById(tenantId: String, id: Long): ItemInfo?
    fun findByCode(tenantId: String, code: String): ItemInfo?
}

data class VendorInfo(
    val id: Long,
    val code: String,
    val name: String,
    val country: String?
)

interface VendorQueryPort {
    fun findById(tenantId: String, id: Long): VendorInfo?
    fun findByCode(tenantId: String, code: String): VendorInfo?
}

data class CustomerInfo(
    val id: Long,
    val code: String,
    val name: String,
    val country: String?
)

interface CustomerQueryPort {
    fun findById(tenantId: String, id: Long): CustomerInfo?
}

data class CompanyInfo(
    val id: Long,
    val code: String,
    val name: String
)

interface CompanyQueryPort {
    fun findById(tenantId: String, id: Long): CompanyInfo?
}
