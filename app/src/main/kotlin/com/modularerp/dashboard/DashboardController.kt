package com.modularerp.dashboard

import com.modularerp.web.dto.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard KPIs and chart data")
class DashboardController(
    private val dashboardService: DashboardService
) {

    @GetMapping("/summary")
    fun getSummary(): ApiResponse<DashboardSummary> =
        ApiResponse.ok(dashboardService.getSummary())

    @GetMapping("/charts/sales-trend")
    fun getSalesTrend(
        @RequestParam(defaultValue = "6") months: Int
    ): ApiResponse<List<MonthlyTrend>> =
        ApiResponse.ok(dashboardService.getSalesTrend(months))

    @GetMapping("/charts/purchase-trend")
    fun getPurchaseTrend(
        @RequestParam(defaultValue = "6") months: Int
    ): ApiResponse<List<MonthlyTrend>> =
        ApiResponse.ok(dashboardService.getPurchaseTrend(months))
}
