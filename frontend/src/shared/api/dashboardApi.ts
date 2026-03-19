import api from "./client";

export interface DocumentSummary {
  total: number;
  draft: number;
  submitted: number;
  approved: number;
  thisMonth: number;
}

export interface TopItem {
  name: string;
  value: number;
  count: number;
}

export interface ActivityItem {
  type: string;
  description: string;
  timestamp: string;
  userId: string;
}

export interface DashboardSummary {
  purchaseOrders: DocumentSummary;
  salesOrders: DocumentSummary;
  workOrders: DocumentSummary;
  pendingApprovals: number;
  lowStockItems: number;
  overdueDeliveries: number;
  revenueThisMonth: number;
  expenseThisMonth: number;
  topCustomers: TopItem[];
  topProducts: TopItem[];
  recentActivities: ActivityItem[];
  budgetUtilization: number | null;
  openOpportunities: number;
  opportunityValue: number;
}

export interface MonthlyTrend {
  month: string;
  count: number;
  amount: number;
}

export const dashboardApi = {
  getSummary: () =>
    api.get<{ success: boolean; data: DashboardSummary }>("/api/v1/dashboard/summary"),

  getSalesTrend: (months = 6) =>
    api.get<{ success: boolean; data: MonthlyTrend[] }>(`/api/v1/dashboard/charts/sales-trend?months=${months}`),

  getPurchaseTrend: (months = 6) =>
    api.get<{ success: boolean; data: MonthlyTrend[] }>(`/api/v1/dashboard/charts/purchase-trend?months=${months}`),
};
