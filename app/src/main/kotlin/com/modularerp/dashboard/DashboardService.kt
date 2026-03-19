package com.modularerp.dashboard

import com.modularerp.security.tenant.TenantContext
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class DashboardService(
    @PersistenceContext private val em: EntityManager
) {

    fun getSummary(): DashboardSummary {
        val tenantId = TenantContext.getTenantId()
        val now = LocalDate.now()
        val monthStart = now.withDayOfMonth(1)

        return DashboardSummary(
            purchaseOrders = countDocuments("PurchaseOrder", "com.modularerp.purchase.domain.PoStatus", tenantId, monthStart),
            salesOrders = countDocuments("SalesOrder", "com.modularerp.sales.domain.SoStatus", tenantId, monthStart),
            workOrders = countWorkOrders(tenantId, monthStart),
            pendingApprovals = countPendingApprovals(tenantId),
            lowStockItems = countLowStockItems(tenantId),
            overdueDeliveries = countOverdueDeliveries(tenantId, now),
            revenueThisMonth = getRevenueThisMonth(tenantId, monthStart),
            expenseThisMonth = getExpenseThisMonth(tenantId, monthStart),
            topCustomers = getTopCustomers(tenantId),
            topProducts = getTopProducts(tenantId),
            recentActivities = getRecentActivities(tenantId),
            budgetUtilization = getBudgetUtilization(tenantId),
            openOpportunities = countOpenOpportunities(tenantId),
            opportunityValue = getOpportunityValue(tenantId)
        )
    }

    fun getSalesTrend(months: Int): List<MonthlyTrend> {
        val tenantId = TenantContext.getTenantId()
        return getMonthlyTrend("SalesOrder", "orderDate", tenantId, months)
    }

    fun getPurchaseTrend(months: Int): List<MonthlyTrend> {
        val tenantId = TenantContext.getTenantId()
        return getMonthlyTrend("PurchaseOrder", "orderDate", tenantId, months)
    }

    // ── Private helpers ──

    private fun countDocuments(entity: String, statusEnum: String, tenantId: String, monthStart: LocalDate): DocumentSummary {
        val total = countByTenant(entity, tenantId)
        val draft = countByStatus(entity, tenantId, "DRAFT")
        val submitted = countByStatus(entity, tenantId, "SUBMITTED")
        val approved = countByStatus(entity, tenantId, "APPROVED")
        val thisMonth = countThisMonth(entity, tenantId, monthStart)
        return DocumentSummary(total, draft, submitted, approved, thisMonth)
    }

    private fun countWorkOrders(tenantId: String, monthStart: LocalDate): DocumentSummary {
        val total = countByTenant("WorkOrder", tenantId)
        val draft = countByStatus("WorkOrder", tenantId, "PLANNED")
        val submitted = countByStatus("WorkOrder", tenantId, "RELEASED")
        val approved = countByStatus("WorkOrder", tenantId, "IN_PROGRESS")
        val thisMonth = countThisMonth("WorkOrder", tenantId, monthStart)
        return DocumentSummary(total, draft, submitted, approved, thisMonth)
    }

    private fun countByTenant(entity: String, tenantId: String): Long =
        try {
            em.createQuery("SELECT COUNT(e) FROM $entity e WHERE e.tenantId = :t AND e.active = true", Long::class.java)
                .setParameter("t", tenantId).singleResult
        } catch (_: Exception) { 0L }

    private fun countByStatus(entity: String, tenantId: String, status: String): Long =
        try {
            em.createQuery("SELECT COUNT(e) FROM $entity e WHERE e.tenantId = :t AND e.active = true AND CAST(e.status AS string) = :s", Long::class.java)
                .setParameter("t", tenantId).setParameter("s", status).singleResult
        } catch (_: Exception) { 0L }

    private fun countThisMonth(entity: String, tenantId: String, monthStart: LocalDate): Long =
        try {
            val instant = monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant()
            em.createQuery("SELECT COUNT(e) FROM $entity e WHERE e.tenantId = :t AND e.active = true AND e.createdAt >= :d", Long::class.java)
                .setParameter("t", tenantId).setParameter("d", instant).singleResult
        } catch (_: Exception) { 0L }

    private fun countPendingApprovals(tenantId: String): Int {
        val poSubmitted = countByStatus("PurchaseOrder", tenantId, "SUBMITTED")
        val prSubmitted = countByStatus("PurchaseRequest", tenantId, "SUBMITTED")
        val soSubmitted = countByStatus("SalesOrder", tenantId, "SUBMITTED")
        return (poSubmitted + prSubmitted + soSubmitted).toInt()
    }

    private fun countLowStockItems(tenantId: String): Int =
        try {
            em.createQuery(
                "SELECT COUNT(s) FROM StockSummary s WHERE s.tenantId = :t AND s.active = true AND s.quantityOnHand <= 0",
                Long::class.java
            ).setParameter("t", tenantId).singleResult.toInt()
        } catch (_: Exception) { 0 }

    private fun countOverdueDeliveries(tenantId: String, now: LocalDate): Int =
        try {
            em.createQuery(
                "SELECT COUNT(po) FROM PurchaseOrder po WHERE po.tenantId = :t AND po.active = true AND CAST(po.status AS string) = 'APPROVED' AND po.deliveryDate < :now",
                Long::class.java
            ).setParameter("t", tenantId).setParameter("now", now).singleResult.toInt()
        } catch (_: Exception) { 0 }

    private fun getRevenueThisMonth(tenantId: String, monthStart: LocalDate): BigDecimal =
        try {
            @Suppress("UNCHECKED_CAST")
            val result = em.createQuery(
                "SELECT COALESCE(SUM(l.quantity * l.unitPrice), 0) FROM SalesOrderLine l JOIN l.salesOrder so WHERE so.tenantId = :t AND so.active = true AND CAST(so.status AS string) IN ('APPROVED','SHIPPED','COMPLETED') AND so.orderDate >= :d"
            ).setParameter("t", tenantId).setParameter("d", monthStart).singleResult
            result as? BigDecimal ?: BigDecimal.ZERO
        } catch (_: Exception) { BigDecimal.ZERO }

    private fun getExpenseThisMonth(tenantId: String, monthStart: LocalDate): BigDecimal =
        try {
            @Suppress("UNCHECKED_CAST")
            val result = em.createQuery(
                "SELECT COALESCE(SUM(l.quantity * l.unitPrice), 0) FROM PurchaseOrderLine l JOIN l.purchaseOrder po WHERE po.tenantId = :t AND po.active = true AND CAST(po.status AS string) IN ('APPROVED','CLOSED') AND po.orderDate >= :d"
            ).setParameter("t", tenantId).setParameter("d", monthStart).singleResult
            result as? BigDecimal ?: BigDecimal.ZERO
        } catch (_: Exception) { BigDecimal.ZERO }

    private fun getTopCustomers(tenantId: String): List<TopItem> =
        try {
            @Suppress("UNCHECKED_CAST")
            val results = em.createQuery(
                "SELECT so.customerName, COALESCE(SUM(l.quantity * l.unitPrice), 0), COUNT(DISTINCT so.id) FROM SalesOrderLine l JOIN l.salesOrder so WHERE so.tenantId = :t AND so.active = true GROUP BY so.customerName ORDER BY SUM(l.quantity * l.unitPrice) DESC"
            ).setParameter("t", tenantId).setMaxResults(5).resultList as List<Array<Any>>
            results.map { TopItem(it[0] as String, it[1] as BigDecimal, (it[2] as Long).toInt()) }
        } catch (_: Exception) { emptyList() }

    private fun getTopProducts(tenantId: String): List<TopItem> =
        try {
            @Suppress("UNCHECKED_CAST")
            val results = em.createQuery(
                "SELECT l.itemName, COALESCE(SUM(l.quantity * l.unitPrice), 0), COUNT(DISTINCT l.salesOrder.id) FROM SalesOrderLine l JOIN l.salesOrder so WHERE so.tenantId = :t AND so.active = true GROUP BY l.itemName ORDER BY SUM(l.quantity * l.unitPrice) DESC"
            ).setParameter("t", tenantId).setMaxResults(5).resultList as List<Array<Any>>
            results.map { TopItem(it[0] as String, it[1] as BigDecimal, (it[2] as Long).toInt()) }
        } catch (_: Exception) { emptyList() }

    private fun getRecentActivities(tenantId: String): List<ActivityItem> {
        val activities = mutableListOf<ActivityItem>()
        try {
            @Suppress("UNCHECKED_CAST")
            val pos = em.createQuery(
                "SELECT po.documentNo, CAST(po.status AS string), po.createdAt, po.createdBy FROM PurchaseOrder po WHERE po.tenantId = :t AND po.active = true ORDER BY po.createdAt DESC"
            ).setParameter("t", tenantId).setMaxResults(3).resultList as List<Array<Any>>
            pos.forEach {
                val ts = (it[2] as Instant).atZone(ZoneId.systemDefault()).toLocalDateTime()
                activities.add(ActivityItem("PO", "${it[0]} - ${it[1]}", ts, (it[3] as? String) ?: "system"))
            }
        } catch (_: Exception) {}
        try {
            @Suppress("UNCHECKED_CAST")
            val sos = em.createQuery(
                "SELECT so.documentNo, CAST(so.status AS string), so.createdAt, so.createdBy FROM SalesOrder so WHERE so.tenantId = :t AND so.active = true ORDER BY so.createdAt DESC"
            ).setParameter("t", tenantId).setMaxResults(3).resultList as List<Array<Any>>
            sos.forEach {
                val ts = (it[2] as Instant).atZone(ZoneId.systemDefault()).toLocalDateTime()
                activities.add(ActivityItem("SO", "${it[0]} - ${it[1]}", ts, (it[3] as? String) ?: "system"))
            }
        } catch (_: Exception) {}
        return activities.sortedByDescending { it.timestamp }.take(5)
    }

    private fun getBudgetUtilization(tenantId: String): BigDecimal? =
        try {
            @Suppress("UNCHECKED_CAST")
            val result = em.createQuery(
                "SELECT CASE WHEN SUM(bi.budgetAmount) > 0 THEN (SUM(bi.actualAmount) * 100 / SUM(bi.budgetAmount)) ELSE 0 END FROM BudgetItem bi WHERE bi.tenantId = :t AND bi.active = true"
            ).setParameter("t", tenantId).singleResult
            result as? BigDecimal
        } catch (_: Exception) { null }

    private fun countOpenOpportunities(tenantId: String): Int =
        try {
            em.createQuery(
                "SELECT COUNT(o) FROM Opportunity o WHERE o.tenantId = :t AND o.active = true AND CAST(o.stage AS string) NOT IN ('CLOSED_WON','CLOSED_LOST')",
                Long::class.java
            ).setParameter("t", tenantId).singleResult.toInt()
        } catch (_: Exception) { 0 }

    private fun getOpportunityValue(tenantId: String): BigDecimal =
        try {
            @Suppress("UNCHECKED_CAST")
            val result = em.createQuery(
                "SELECT COALESCE(SUM(o.expectedAmount), 0) FROM Opportunity o WHERE o.tenantId = :t AND o.active = true AND CAST(o.stage AS string) NOT IN ('CLOSED_WON','CLOSED_LOST')"
            ).setParameter("t", tenantId).singleResult
            result as? BigDecimal ?: BigDecimal.ZERO
        } catch (_: Exception) { BigDecimal.ZERO }

    private fun getMonthlyTrend(entity: String, dateField: String, tenantId: String, months: Int): List<MonthlyTrend> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val result = mutableListOf<MonthlyTrend>()
        val now = YearMonth.now()

        for (i in (months - 1) downTo 0) {
            val ym = now.minusMonths(i.toLong())
            val start = ym.atDay(1)
            val end = ym.atEndOfMonth()
            try {
                val count = em.createQuery(
                    "SELECT COUNT(e) FROM $entity e WHERE e.tenantId = :t AND e.active = true AND e.$dateField >= :s AND e.$dateField <= :e",
                    Long::class.java
                ).setParameter("t", tenantId).setParameter("s", start).setParameter("e", end).singleResult

                result.add(MonthlyTrend(ym.format(formatter), count, BigDecimal.ZERO))
            } catch (_: Exception) {
                result.add(MonthlyTrend(ym.format(formatter), 0, BigDecimal.ZERO))
            }
        }
        return result
    }
}
