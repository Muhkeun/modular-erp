package com.modularerp.dashboard

import java.math.BigDecimal
import java.time.LocalDateTime

data class DashboardSummary(
    val purchaseOrders: DocumentSummary,
    val salesOrders: DocumentSummary,
    val workOrders: DocumentSummary,
    val pendingApprovals: Int,
    val lowStockItems: Int,
    val overdueDeliveries: Int,
    val revenueThisMonth: BigDecimal,
    val expenseThisMonth: BigDecimal,
    val topCustomers: List<TopItem>,
    val topProducts: List<TopItem>,
    val recentActivities: List<ActivityItem>,
    val budgetUtilization: BigDecimal?,
    val openOpportunities: Int,
    val opportunityValue: BigDecimal
)

data class DocumentSummary(
    val total: Long,
    val draft: Long,
    val submitted: Long,
    val approved: Long,
    val thisMonth: Long
)

data class TopItem(
    val name: String,
    val value: BigDecimal,
    val count: Int
)

data class ActivityItem(
    val type: String,
    val description: String,
    val timestamp: LocalDateTime,
    val userId: String
)

data class MonthlyTrend(
    val month: String,
    val count: Long,
    val amount: BigDecimal
)
