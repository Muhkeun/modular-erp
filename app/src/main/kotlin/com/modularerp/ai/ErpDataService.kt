package com.modularerp.ai

import com.modularerp.account.repository.JournalEntryRepository
import com.modularerp.budget.repository.BudgetItemRepository
import com.modularerp.budget.repository.BudgetPeriodRepository
import com.modularerp.crm.repository.CustomerRepository
import com.modularerp.hr.repository.EmployeeRepository
import com.modularerp.logistics.repository.StockSummaryRepository
import com.modularerp.masterdata.domain.ItemType
import com.modularerp.masterdata.repository.ItemRepository
import com.modularerp.production.domain.WoStatus
import com.modularerp.production.repository.WorkOrderRepository
import com.modularerp.purchase.domain.PoStatus
import com.modularerp.purchase.repository.PurchaseOrderRepository
import com.modularerp.purchase.repository.PurchaseRequestRepository
import com.modularerp.sales.domain.SoStatus
import com.modularerp.sales.repository.SalesOrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

data class QueryResult(
    val columns: List<String>,
    val data: List<List<Any?>>,
    val totalCount: Int,
    val description: String
)

@Service
class ErpDataService(
    private val itemRepo: ItemRepository,
    private val purchaseRequestRepo: PurchaseRequestRepository,
    private val purchaseOrderRepo: PurchaseOrderRepository,
    private val salesOrderRepo: SalesOrderRepository,
    private val stockSummaryRepo: StockSummaryRepository,
    private val journalEntryRepo: JournalEntryRepository,
    private val employeeRepo: EmployeeRepository,
    private val workOrderRepo: WorkOrderRepository,
    private val customerRepo: CustomerRepository,
    private val budgetPeriodRepo: BudgetPeriodRepository,
    private val budgetItemRepo: BudgetItemRepository
) {

    fun queryItems(tenantId: String, filters: Map<String, String>): QueryResult {
        val itemType = filters["itemType"]?.let { runCatching { ItemType.valueOf(it) }.getOrNull() }
        val keyword = filters["keyword"]
        val pageable = parsePageable(filters)

        val page = itemRepo.search(tenantId, keyword, itemType, filters["itemGroup"], pageable)
        val columns = listOf("id", "code", "name", "itemType", "itemGroup", "unitOfMeasure", "active")
        val data = page.content.map { listOf<Any?>(it.id, it.code, it.getName("ko"), it.itemType.name, it.itemGroup, it.unitOfMeasure, it.active) }
        return QueryResult(columns, data, page.totalElements.toInt(), "Items matching filters")
    }

    fun querySalesOrders(tenantId: String, filters: Map<String, String>): QueryResult {
        val status = filters["status"]?.let { runCatching { SoStatus.valueOf(it) }.getOrNull() }
        val pageable = parsePageable(filters)

        val page = salesOrderRepo.search(tenantId, status, filters["customerCode"], filters["documentNo"], pageable)
        val columns = listOf("id", "documentNo", "customerCode", "status", "orderDate", "totalAmount", "currencyCode")
        val data = page.content.map {
            listOf<Any?>(it.id, it.documentNo, it.customerCode, it.status.name, it.orderDate, it.totalAmount, it.currencyCode)
        }
        return QueryResult(columns, data, page.totalElements.toInt(), "Sales orders matching filters")
    }

    fun queryPurchaseOrders(tenantId: String, filters: Map<String, String>): QueryResult {
        val status = filters["status"]?.let { runCatching { PoStatus.valueOf(it) }.getOrNull() }
        val pageable = parsePageable(filters)

        val page = purchaseOrderRepo.search(tenantId, status, filters["vendorCode"], filters["documentNo"], pageable)
        val columns = listOf("id", "documentNo", "vendorCode", "status", "orderDate", "totalAmount", "currencyCode")
        val data = page.content.map {
            listOf<Any?>(it.id, it.documentNo, it.vendorCode, it.status.name, it.orderDate, it.totalAmount, it.currencyCode)
        }
        return QueryResult(columns, data, page.totalElements.toInt(), "Purchase orders matching filters")
    }

    fun queryStockSummary(tenantId: String, filters: Map<String, String>): QueryResult {
        val pageable = parsePageable(filters)
        val page = stockSummaryRepo.search(tenantId, filters["plantCode"], filters["itemCode"], pageable)
        val columns = listOf("id", "itemCode", "plantCode", "storageLocation", "quantityOnHand", "availableQuantity", "unitOfMeasure")
        val data = page.content.map {
            listOf<Any?>(it.id, it.itemCode, it.plantCode, it.storageLocation, it.quantityOnHand, it.availableQuantity, it.unitOfMeasure)
        }
        return QueryResult(columns, data, page.totalElements.toInt(), "Stock summary")
    }

    fun queryJournalEntries(tenantId: String, filters: Map<String, String>): QueryResult {
        val pageable = parsePageable(filters)
        val page = journalEntryRepo.search(tenantId, null, null, filters["documentNo"], pageable)
        val columns = listOf("id", "documentNo", "entryType", "status", "postingDate", "description")
        val data = page.content.map {
            listOf<Any?>(it.id, it.documentNo, it.entryType.name, it.status.name, it.postingDate, it.description)
        }
        return QueryResult(columns, data, page.totalElements.toInt(), "Journal entries")
    }

    fun queryEmployees(tenantId: String, filters: Map<String, String>): QueryResult {
        val pageable = parsePageable(filters)
        val page = employeeRepo.search(tenantId, null, filters["departmentCode"], filters["name"], pageable)
        val columns = listOf("id", "employeeNo", "name", "departmentCode", "status")
        val data = page.content.map {
            listOf<Any?>(it.id, it.employeeNo, it.name, it.departmentCode, it.status.name)
        }
        return QueryResult(columns, data, page.totalElements.toInt(), "Employees")
    }

    fun queryWorkOrders(tenantId: String, filters: Map<String, String>): QueryResult {
        val status = filters["status"]?.let { runCatching { WoStatus.valueOf(it) }.getOrNull() }
        val pageable = parsePageable(filters)

        val page = workOrderRepo.search(tenantId, status, filters["plantCode"], filters["productCode"], filters["documentNo"], pageable)
        val columns = listOf("id", "documentNo", "productCode", "plantCode", "status", "plannedQuantity", "completedQuantity")
        val data = page.content.map {
            listOf<Any?>(it.id, it.documentNo, it.productCode, it.plantCode, it.status.name, it.plannedQuantity, it.completedQuantity)
        }
        return QueryResult(columns, data, page.totalElements.toInt(), "Work orders")
    }

    fun queryCustomers(tenantId: String, filters: Map<String, String>): QueryResult {
        val pageable = parsePageable(filters)
        val page = customerRepo.search(tenantId, null, filters["customerCode"], filters["customerName"], pageable)
        val columns = listOf("id", "customerCode", "customerName", "status")
        val data = page.content.map {
            listOf<Any?>(it.id, it.customerCode, it.customerName, it.status.name)
        }
        return QueryResult(columns, data, page.totalElements.toInt(), "Customers")
    }

    fun queryBudgetStatus(tenantId: String, filters: Map<String, String>): QueryResult {
        val fiscalYear = filters["fiscalYear"]?.toIntOrNull()
        val pageable = parsePageable(filters)
        val page = budgetPeriodRepo.search(tenantId, null, fiscalYear, pageable)
        val columns = listOf("id", "fiscalYear", "periodType", "status", "startDate", "endDate")
        val data = page.content.map {
            listOf<Any?>(it.id, it.fiscalYear, it.periodType.name, it.status.name, it.startDate, it.endDate)
        }
        return QueryResult(columns, data, page.totalElements.toInt(), "Budget periods")
    }

    // ── Aggregation queries ──

    fun getSalesSummary(tenantId: String, fromDate: LocalDate, toDate: LocalDate): Map<String, Any> {
        val pageable = PageRequest.of(0, 1000)
        val page = salesOrderRepo.search(tenantId, null, null, null, pageable)
        val orders = page.content.filter { it.orderDate in fromDate..toDate }

        return mapOf(
            "totalOrders" to orders.size,
            "totalAmount" to orders.sumOf { it.totalAmount },
            "byStatus" to orders.groupBy { it.status.name }.mapValues { it.value.size },
            "fromDate" to fromDate.toString(),
            "toDate" to toDate.toString()
        )
    }

    fun getPurchaseSummary(tenantId: String, fromDate: LocalDate, toDate: LocalDate): Map<String, Any> {
        val pageable = PageRequest.of(0, 1000)
        val page = purchaseOrderRepo.search(tenantId, null, null, null, pageable)
        val orders = page.content.filter { it.orderDate in fromDate..toDate }

        return mapOf(
            "totalOrders" to orders.size,
            "totalAmount" to orders.sumOf { it.totalAmount },
            "byStatus" to orders.groupBy { it.status.name }.mapValues { it.value.size },
            "fromDate" to fromDate.toString(),
            "toDate" to toDate.toString()
        )
    }

    fun getStockAlerts(tenantId: String): List<Map<String, Any>> {
        val pageable = PageRequest.of(0, 500)
        val page = stockSummaryRepo.search(tenantId, null, null, pageable)
        return page.content
            .filter { it.availableQuantity <= java.math.BigDecimal.ZERO }
            .map {
                mapOf(
                    "itemCode" to (it.itemCode as Any),
                    "plantCode" to (it.plantCode as Any),
                    "quantityOnHand" to (it.quantityOnHand as Any),
                    "availableQuantity" to (it.availableQuantity as Any),
                    "unitOfMeasure" to (it.unitOfMeasure as Any)
                )
            }
    }

    fun getFinancialSummary(tenantId: String, fiscalYear: Int, period: Int): Map<String, Any> {
        val pageable = PageRequest.of(0, 500)
        val budgetPage = budgetPeriodRepo.search(tenantId, null, fiscalYear, pageable)
        return mapOf(
            "fiscalYear" to fiscalYear,
            "period" to period,
            "budgetPeriods" to budgetPage.totalElements
        )
    }

    private fun parsePageable(filters: Map<String, String>): PageRequest {
        val page = filters["page"]?.toIntOrNull() ?: 0
        val size = (filters["size"]?.toIntOrNull() ?: 20).coerceAtMost(100)
        return PageRequest.of(page, size)
    }
}
