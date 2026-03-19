package com.modularerp.ai

import org.springframework.stereotype.Component

@Component
class ErpToolRegistry(private val erpDataService: ErpDataService) {

    data class ToolDefinition(
        val name: String,
        val description: String,
        val descriptionKo: String,
        val parameters: List<ToolParameter>,
        val requiredPermission: String,
        val category: String
    )

    data class ToolParameter(
        val name: String,
        val type: String,
        val description: String,
        val required: Boolean = false,
        val enumValues: List<String>? = null
    )

    fun getAvailableTools(): List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "search_items",
            description = "Search items/products in master data",
            descriptionKo = "품목/제품 마스터 데이터 검색",
            parameters = listOf(
                ToolParameter("keyword", "string", "Search keyword"),
                ToolParameter("itemType", "string", "Item type filter", enumValues = listOf("MATERIAL", "PRODUCT", "SEMI_PRODUCT", "SERVICE", "ASSET")),
                ToolParameter("status", "string", "Status filter", enumValues = listOf("ACTIVE", "INACTIVE"))
            ),
            requiredPermission = "MASTER_DATA:READ",
            category = "query"
        ),
        ToolDefinition(
            name = "get_sales_orders",
            description = "Get sales orders with filters",
            descriptionKo = "판매주문 조회",
            parameters = listOf(
                ToolParameter("status", "string", "Order status", enumValues = listOf("DRAFT", "CONFIRMED", "SHIPPED", "COMPLETED")),
                ToolParameter("fromDate", "date", "Start date (YYYY-MM-DD)"),
                ToolParameter("toDate", "date", "End date (YYYY-MM-DD)"),
                ToolParameter("customerName", "string", "Customer name")
            ),
            requiredPermission = "SALES:READ",
            category = "query"
        ),
        ToolDefinition(
            name = "get_purchase_orders",
            description = "Get purchase orders with filters",
            descriptionKo = "구매주문 조회",
            parameters = listOf(
                ToolParameter("status", "string", "Order status"),
                ToolParameter("fromDate", "date", "Start date"),
                ToolParameter("toDate", "date", "End date"),
                ToolParameter("vendorName", "string", "Vendor name")
            ),
            requiredPermission = "PURCHASE:READ",
            category = "query"
        ),
        ToolDefinition(
            name = "get_stock_status",
            description = "Get current stock levels",
            descriptionKo = "현재 재고 현황 조회",
            parameters = listOf(
                ToolParameter("itemCode", "string", "Item code"),
                ToolParameter("plantCode", "string", "Plant code"),
                ToolParameter("lowStockOnly", "boolean", "Show only low stock items")
            ),
            requiredPermission = "LOGISTICS:READ",
            category = "query"
        ),
        ToolDefinition(
            name = "get_sales_summary",
            description = "Get sales summary/analytics for a date range",
            descriptionKo = "매출 요약/분석 조회",
            parameters = listOf(
                ToolParameter("fromDate", "date", "Start date", required = true),
                ToolParameter("toDate", "date", "End date", required = true),
                ToolParameter("groupBy", "string", "Group by", enumValues = listOf("customer", "product", "month"))
            ),
            requiredPermission = "SALES:READ",
            category = "analytics"
        ),
        ToolDefinition(
            name = "get_budget_status",
            description = "Get budget vs actual status",
            descriptionKo = "예산 대비 실적 현황 조회",
            parameters = listOf(
                ToolParameter("fiscalYear", "number", "Fiscal year", required = true),
                ToolParameter("departmentCode", "string", "Department code")
            ),
            requiredPermission = "BUDGET:READ",
            category = "analytics"
        ),
        ToolDefinition(
            name = "get_production_status",
            description = "Get work order / production status",
            descriptionKo = "생산/작업지시 현황 조회",
            parameters = listOf(
                ToolParameter("status", "string", "WO status", enumValues = listOf("CREATED", "RELEASED", "IN_PROGRESS", "COMPLETED")),
                ToolParameter("fromDate", "date", "Start date"),
                ToolParameter("toDate", "date", "End date")
            ),
            requiredPermission = "PRODUCTION:READ",
            category = "query"
        ),
        ToolDefinition(
            name = "get_employees",
            description = "Get employee information",
            descriptionKo = "직원 정보 조회",
            parameters = listOf(
                ToolParameter("name", "string", "Employee name"),
                ToolParameter("departmentCode", "string", "Department code")
            ),
            requiredPermission = "HR:READ",
            category = "query"
        ),
        ToolDefinition(
            name = "get_customers",
            description = "Get customer information",
            descriptionKo = "고객 정보 조회",
            parameters = listOf(
                ToolParameter("customerCode", "string", "Customer code"),
                ToolParameter("customerName", "string", "Customer name")
            ),
            requiredPermission = "CRM:READ",
            category = "query"
        ),
        ToolDefinition(
            name = "get_journal_entries",
            description = "Get journal entries / accounting records",
            descriptionKo = "분개 전표 조회",
            parameters = listOf(
                ToolParameter("documentNo", "string", "Document number"),
                ToolParameter("fromDate", "date", "Start date"),
                ToolParameter("toDate", "date", "End date")
            ),
            requiredPermission = "ACCOUNT:READ",
            category = "query"
        ),
        ToolDefinition(
            name = "generate_excel_report",
            description = "Generate an Excel report from query results",
            descriptionKo = "조회 결과를 Excel 보고서로 생성",
            parameters = listOf(
                ToolParameter("title", "string", "Report title", required = true),
                ToolParameter("queryName", "string", "Which query to run", required = true),
                ToolParameter("filters", "string", "JSON filters for the query")
            ),
            requiredPermission = "REPORT:EXPORT",
            category = "report"
        ),
        ToolDefinition(
            name = "generate_pdf_report",
            description = "Generate a PDF report",
            descriptionKo = "PDF 보고서 생성",
            parameters = listOf(
                ToolParameter("title", "string", "Report title", required = true),
                ToolParameter("queryName", "string", "Which query to run", required = true),
                ToolParameter("filters", "string", "JSON filters for the query")
            ),
            requiredPermission = "REPORT:EXPORT",
            category = "report"
        )
    )

    fun executeTool(toolName: String, params: Map<String, Any>, tenantId: String): QueryResult {
        val filters = params.mapValues { it.value.toString() }
        return when (toolName) {
            "search_items" -> erpDataService.queryItems(tenantId, filters)
            "get_sales_orders" -> erpDataService.querySalesOrders(tenantId, filters)
            "get_purchase_orders" -> erpDataService.queryPurchaseOrders(tenantId, filters)
            "get_stock_status" -> erpDataService.queryStockSummary(tenantId, filters)
            "get_production_status" -> erpDataService.queryWorkOrders(tenantId, filters)
            "get_employees" -> erpDataService.queryEmployees(tenantId, filters)
            "get_customers" -> erpDataService.queryCustomers(tenantId, filters)
            "get_journal_entries" -> erpDataService.queryJournalEntries(tenantId, filters)
            "get_budget_status" -> erpDataService.queryBudgetStatus(tenantId, filters)
            else -> throw IllegalArgumentException("Unknown tool: $toolName")
        }
    }
}
