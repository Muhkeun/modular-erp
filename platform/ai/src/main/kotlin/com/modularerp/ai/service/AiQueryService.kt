package com.modularerp.ai.service

import com.modularerp.ai.config.AiProperties
import com.modularerp.ai.domain.QueryIntent
import com.modularerp.ai.dto.QueryResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Natural Language → ERP Query service.
 * Parses user intent from natural language and executes structured queries.
 * MVP mode uses pattern matching; production mode uses LLM for intent parsing.
 */
@Service
class AiQueryService(
    private val aiProperties: AiProperties,
    private val erpToolService: ErpToolService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun parseQuery(
        naturalLanguageQuery: String,
        tenantId: String,
        userPermissions: List<String>
    ): QueryResult {
        val intent = detectIntent(naturalLanguageQuery)
        log.debug("Detected intent: {} for query: {}", intent, naturalLanguageQuery)

        return executeIntent(intent, naturalLanguageQuery, tenantId, userPermissions)
    }

    private fun detectIntent(query: String): QueryIntent {
        val lowerQuery = query.lowercase()

        return when {
            // Korean patterns
            lowerQuery.containsAny("매출", "판매", "sales") -> QueryIntent.SALES_SUMMARY
            lowerQuery.containsAny("구매", "발주", "purchase") -> QueryIntent.PURCHASE_SUMMARY
            lowerQuery.containsAny("재고", "stock", "inventory") -> QueryIntent.STOCK_STATUS
            lowerQuery.containsAny("거래처", "고객", "customer", "client") -> QueryIntent.CUSTOMER_LIST
            lowerQuery.containsAny("주문", "order") && lowerQuery.containsAny("상태", "status") -> QueryIntent.ORDER_STATUS
            lowerQuery.containsAny("예산", "budget") -> QueryIntent.BUDGET_STATUS
            lowerQuery.containsAny("직원", "사원", "employee") -> QueryIntent.EMPLOYEE_LIST
            lowerQuery.containsAny("재무", "손익", "수익", "비용", "financial", "revenue", "profit") -> QueryIntent.FINANCIAL_SUMMARY
            lowerQuery.containsAny("생산", "작업지시", "production", "work order") -> QueryIntent.PRODUCTION_STATUS
            lowerQuery.containsAny("배송", "출하", "delivery", "shipment") -> QueryIntent.DELIVERY_STATUS
            lowerQuery.containsAny("결재", "승인", "approval") -> QueryIntent.APPROVAL_STATUS
            else -> QueryIntent.GENERAL_QUERY
        }
    }

    private fun executeIntent(
        intent: QueryIntent,
        query: String,
        tenantId: String,
        userPermissions: List<String>
    ): QueryResult {
        val toolName = intentToTool(intent) ?: return QueryResult(
            intent = intent,
            description = "General query processed",
            summary = "이 요청은 패턴 매칭으로 처리할 수 없습니다. AI 모드가 활성화되면 LLM을 통해 처리됩니다."
        )

        return try {
            val result = erpToolService.executeTool(
                toolName = toolName,
                parameters = emptyMap(),
                tenantId = tenantId,
                userId = "",
                userPermissions = userPermissions
            )

            @Suppress("UNCHECKED_CAST")
            val resultMap = result as? Map<String, Any> ?: emptyMap()

            QueryResult(
                intent = intent,
                description = "Query executed: $intent",
                data = (resultMap["results"] as? List<Map<String, Any?>>) ?: emptyList(),
                summary = resultMap["message"]?.toString()
            )
        } catch (e: SecurityException) {
            QueryResult(
                intent = intent,
                description = "Permission denied",
                error = "해당 데이터에 대한 조회 권한이 없습니다."
            )
        }
    }

    private fun intentToTool(intent: QueryIntent): String? {
        return when (intent) {
            QueryIntent.SALES_SUMMARY -> "get_sales_orders"
            QueryIntent.PURCHASE_SUMMARY -> "get_purchase_orders"
            QueryIntent.STOCK_STATUS -> "get_stock_status"
            QueryIntent.FINANCIAL_SUMMARY -> "get_financial_summary"
            QueryIntent.PRODUCTION_STATUS -> "get_production_status"
            QueryIntent.CUSTOMER_LIST -> "search_items" // would map to customer service
            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
