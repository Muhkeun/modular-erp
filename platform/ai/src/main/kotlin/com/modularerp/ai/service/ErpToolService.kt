package com.modularerp.ai.service

import com.modularerp.ai.dto.ErpTool
import com.modularerp.ai.dto.ToolParameter
import org.springframework.stereotype.Service

/**
 * Manages ERP tool definitions and execution for AI function calling.
 * Tools map to existing ERP service methods with permission checks.
 */
@Service
class ErpToolService {

    private val tools: Map<String, ErpTool> = buildToolRegistry()

    fun getAvailableTools(userPermissions: List<String>): List<ErpTool> {
        return tools.values.filter { tool ->
            hasPermission(userPermissions, tool.requiresPermission)
        }
    }

    fun getToolDefinitions(userPermissions: List<String>): String {
        val available = getAvailableTools(userPermissions)
        return available.joinToString("\n\n") { tool ->
            val params = tool.parameters.entries.joinToString(", ") { (name, param) ->
                "$name: ${param.type}${if (param.required) " (required)" else ""} - ${param.description}"
            }
            "- ${tool.name}: ${tool.description}\n  Parameters: $params"
        }
    }

    fun executeTool(
        toolName: String,
        parameters: Map<String, Any>,
        tenantId: String,
        userId: String,
        userPermissions: List<String>
    ): Any {
        val tool = tools[toolName]
            ?: throw IllegalArgumentException("Unknown tool: $toolName")

        if (!hasPermission(userPermissions, tool.requiresPermission)) {
            throw SecurityException("Insufficient permissions for tool: $toolName")
        }

        // MVP: Return structured mock responses
        // In production, these would call actual ERP service methods
        return executeMockTool(toolName, parameters, tenantId)
    }

    private fun executeMockTool(
        toolName: String,
        parameters: Map<String, Any>,
        tenantId: String
    ): Map<String, Any> {
        return when (toolName) {
            "search_items" -> mapOf(
                "success" to true,
                "message" to "Item search executed",
                "results" to emptyList<Any>(),
                "note" to "Connect to ItemService for real data"
            )
            "get_sales_orders" -> mapOf(
                "success" to true,
                "message" to "Sales order query executed",
                "results" to emptyList<Any>(),
                "note" to "Connect to SalesOrderService for real data"
            )
            "get_stock_status" -> mapOf(
                "success" to true,
                "message" to "Stock status retrieved",
                "results" to emptyList<Any>(),
                "note" to "Connect to StockService for real data"
            )
            "get_purchase_orders" -> mapOf(
                "success" to true,
                "message" to "Purchase order query executed",
                "results" to emptyList<Any>(),
                "note" to "Connect to PurchaseOrderService for real data"
            )
            "create_purchase_request" -> mapOf(
                "success" to true,
                "message" to "Purchase request creation requested (dry-run in MVP)",
                "note" to "Connect to PurchaseRequestService for real execution"
            )
            else -> mapOf("success" to false, "error" to "Tool not implemented: $toolName")
        }
    }

    private fun hasPermission(userPermissions: List<String>, required: String): Boolean {
        if (required.isBlank()) return true
        val (module, action) = required.split(":")
        return userPermissions.any { perm ->
            perm == required || perm == "$module:*" || perm == "*:*" || perm == "ADMIN"
        }
    }

    private fun buildToolRegistry(): Map<String, ErpTool> {
        return listOf(
            ErpTool(
                name = "search_items",
                description = "Search items/products in master data",
                parameters = mapOf(
                    "keyword" to ToolParameter("string", "Search keyword for item name or code"),
                    "category" to ToolParameter("string", "Item category filter", false)
                ),
                requiresPermission = "ITEM:READ"
            ),
            ErpTool(
                name = "get_sales_orders",
                description = "Query sales orders by date range, customer, or status",
                parameters = mapOf(
                    "startDate" to ToolParameter("string", "Start date (yyyy-MM-dd)", false),
                    "endDate" to ToolParameter("string", "End date (yyyy-MM-dd)", false),
                    "status" to ToolParameter("string", "Order status", false, listOf("DRAFT", "CONFIRMED", "SHIPPED", "COMPLETED")),
                    "customerCode" to ToolParameter("string", "Customer code", false)
                ),
                requiresPermission = "SALES:READ"
            ),
            ErpTool(
                name = "get_stock_status",
                description = "Get current stock levels for items",
                parameters = mapOf(
                    "itemCode" to ToolParameter("string", "Item code", false),
                    "warehouse" to ToolParameter("string", "Warehouse code", false),
                    "belowReorderPoint" to ToolParameter("boolean", "Only items below reorder point", false)
                ),
                requiresPermission = "STOCK:READ"
            ),
            ErpTool(
                name = "get_purchase_orders",
                description = "Query purchase orders",
                parameters = mapOf(
                    "startDate" to ToolParameter("string", "Start date (yyyy-MM-dd)", false),
                    "endDate" to ToolParameter("string", "End date (yyyy-MM-dd)", false),
                    "status" to ToolParameter("string", "Order status", false),
                    "vendorCode" to ToolParameter("string", "Vendor code", false)
                ),
                requiresPermission = "PURCHASE:READ"
            ),
            ErpTool(
                name = "create_purchase_request",
                description = "Create a new purchase request",
                parameters = mapOf(
                    "itemCode" to ToolParameter("string", "Item code", true),
                    "quantity" to ToolParameter("number", "Requested quantity", true),
                    "reason" to ToolParameter("string", "Request reason", false)
                ),
                requiresPermission = "PURCHASE:WRITE"
            ),
            ErpTool(
                name = "get_financial_summary",
                description = "Get financial summary (revenue, expenses, profit)",
                parameters = mapOf(
                    "period" to ToolParameter("string", "Period (yyyy-MM or yyyy)", false),
                    "type" to ToolParameter("string", "Summary type", false, listOf("MONTHLY", "QUARTERLY", "YEARLY"))
                ),
                requiresPermission = "ACCOUNT:READ"
            ),
            ErpTool(
                name = "get_production_status",
                description = "Get work order / production status",
                parameters = mapOf(
                    "status" to ToolParameter("string", "Work order status", false, listOf("PLANNED", "IN_PROGRESS", "COMPLETED")),
                    "startDate" to ToolParameter("string", "Start date", false)
                ),
                requiresPermission = "PRODUCTION:READ"
            )
        ).associateBy { it.name }
    }
}
