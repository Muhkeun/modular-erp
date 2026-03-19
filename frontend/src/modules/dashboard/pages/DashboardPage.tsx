import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import PageHeader from "../../../shared/components/PageHeader";
import StatsCard from "../../../shared/components/StatsCard";
import { dashboardApi } from "../../../shared/api/dashboardApi";
import type { DashboardSummary, MonthlyTrend } from "../../../shared/api/dashboardApi";
import {
  ShoppingCart, DollarSign, Factory, Clock,
  TrendingUp, TrendingDown, AlertTriangle, Package,
  Users, BarChart3,
} from "lucide-react";
import { SkeletonDashboard } from "../../../shared/components/SkeletonLoader";

function BarChart({ data, label, color = "bg-brand-500" }: { data: MonthlyTrend[]; label: string; color?: string }) {
  const max = Math.max(...data.map((d) => d.count), 1);
  return (
    <div className="section-card">
      <p className="section-kicker">{label}</p>
      <div className="flex items-end gap-2 mt-4 h-40">
        {data.map((d) => (
          <div key={d.month} className="flex-1 flex flex-col items-center gap-1">
            <span className="text-xs font-semibold text-slate-600">{d.count}</span>
            <div
              className={`w-full rounded-t-md ${color} transition-all duration-300`}
              style={{ height: `${(d.count / max) * 100}%`, minHeight: d.count > 0 ? 8 : 2 }}
            />
            <span className="text-[10px] text-slate-400 mt-1">{d.month.slice(5)}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function ActivityList({ activities }: { activities: DashboardSummary["recentActivities"] }) {
  const { t } = useTranslation();
  const typeColors: Record<string, string> = {
    PO: "bg-blue-50 text-blue-600",
    SO: "bg-emerald-50 text-emerald-600",
    WO: "bg-amber-50 text-amber-600",
    GR: "bg-violet-50 text-violet-600",
    GI: "bg-rose-50 text-rose-600",
  };
  return (
    <div className="section-card">
      <p className="section-kicker">{t("dashboard.recentActivity")}</p>
      <h3 className="section-title">{t("dashboard.recentActivity")}</h3>
      <div className="divide-y divide-slate-50 mt-4">
        {activities.length === 0 ? (
          <div className="py-6 text-center text-slate-400 text-sm">{t("common.noData")}</div>
        ) : (
          activities.map((a, i) => (
            <div key={i} className="flex items-center gap-3 py-3">
              <div className={`flex h-8 w-8 items-center justify-center rounded-lg text-xs font-bold ${typeColors[a.type] || "bg-slate-100 text-slate-500"}`}>
                {a.type}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm text-slate-700 truncate">{a.description}</p>
                <p className="text-xs text-slate-400">{a.userId}</p>
              </div>
              <span className="text-xs text-slate-400 whitespace-nowrap">
                {new Date(a.timestamp).toLocaleDateString()}
              </span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

function TopItemList({ items, title, icon }: { items: DashboardSummary["topCustomers"]; title: string; icon: React.ReactNode }) {
  const { t } = useTranslation();
  return (
    <div className="section-card">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-slate-400">{icon}</span>
        <h3 className="section-title">{title}</h3>
      </div>
      <div className="divide-y divide-slate-50">
        {items.length === 0 ? (
          <div className="py-6 text-center text-slate-400 text-sm">{t("common.noData")}</div>
        ) : (
          items.map((item, i) => (
            <div key={i} className="flex items-center justify-between py-2.5">
              <div className="flex items-center gap-2">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-slate-100 text-xs font-bold text-slate-500">
                  {i + 1}
                </span>
                <span className="text-sm text-slate-700">{item.name}</span>
              </div>
              <div className="text-right">
                <span className="text-sm font-semibold text-slate-900">
                  {Number(item.value).toLocaleString()}
                </span>
                <span className="text-xs text-slate-400 ml-1">({item.count})</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { t } = useTranslation();

  const { data: summaryRes, isLoading } = useQuery({
    queryKey: ["dashboard-summary"],
    queryFn: () => dashboardApi.getSummary(),
    select: (res) => res.data?.data,
    refetchInterval: 60000,
  });

  const { data: salesTrend } = useQuery({
    queryKey: ["dashboard-sales-trend"],
    queryFn: () => dashboardApi.getSalesTrend(6),
    select: (res) => res.data?.data || [],
  });

  const { data: purchaseTrend } = useQuery({
    queryKey: ["dashboard-purchase-trend"],
    queryFn: () => dashboardApi.getPurchaseTrend(6),
    select: (res) => res.data?.data || [],
  });

  const s = summaryRes;

  if (isLoading) {
    return (
      <div>
        <PageHeader title={t("dashboard.title")} description={t("dashboard.description")} />
        <SkeletonDashboard />
      </div>
    );
  }

  return (
    <div>
      <PageHeader title={t("dashboard.title")} description={t("dashboard.description")} />

      {/* Row 1 - Key Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4 mb-6">
        <StatsCard
          title={t("dashboard.purchaseOrders")}
          value={s?.purchaseOrders?.total ?? 0}
          change={`${t("dashboard.thisMonth")}: ${s?.purchaseOrders?.thisMonth ?? 0}`}
          changeType="neutral"
          icon={<ShoppingCart size={22} />}
          iconColor="bg-blue-50 text-blue-600"
        />
        <StatsCard
          title={t("dashboard.salesOrders")}
          value={s?.salesOrders?.total ?? 0}
          change={`${t("dashboard.thisMonth")}: ${s?.salesOrders?.thisMonth ?? 0}`}
          changeType="neutral"
          icon={<DollarSign size={22} />}
          iconColor="bg-emerald-50 text-emerald-600"
        />
        <StatsCard
          title={t("dashboard.workOrdersLabel")}
          value={s?.workOrders?.total ?? 0}
          change={`${t("dashboard.inProgressCount")}: ${s?.workOrders?.approved ?? 0}`}
          changeType="neutral"
          icon={<Factory size={22} />}
          iconColor="bg-amber-50 text-amber-600"
        />
        <StatsCard
          title={t("dashboard.pendingApprovals")}
          value={s?.pendingApprovals ?? 0}
          changeType={s?.pendingApprovals && s.pendingApprovals > 0 ? "negative" : "positive"}
          change={s?.pendingApprovals && s.pendingApprovals > 0 ? t("dashboard.actionRequired") : t("dashboard.allClear")}
          icon={<Clock size={22} />}
          iconColor="bg-rose-50 text-rose-600"
        />
        <StatsCard
          title={t("dashboard.revenueThisMonth")}
          value={`${(s?.revenueThisMonth ?? 0).toLocaleString()}`}
          icon={<TrendingUp size={22} />}
          iconColor="bg-teal-50 text-teal-600"
        />
        <StatsCard
          title={t("dashboard.expenseThisMonth")}
          value={`${(s?.expenseThisMonth ?? 0).toLocaleString()}`}
          icon={<TrendingDown size={22} />}
          iconColor="bg-orange-50 text-orange-600"
        />
      </div>

      {/* Row 1.5 - Alert indicators */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <div className="card p-4 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-red-50">
            <AlertTriangle size={18} className="text-red-500" />
          </div>
          <div>
            <p className="text-sm font-medium text-slate-500">{t("dashboard.lowStockItems")}</p>
            <p className="text-xl font-bold text-slate-900">{s?.lowStockItems ?? 0}</p>
          </div>
        </div>
        <div className="card p-4 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-50">
            <Package size={18} className="text-amber-500" />
          </div>
          <div>
            <p className="text-sm font-medium text-slate-500">{t("dashboard.overdueDeliveries")}</p>
            <p className="text-xl font-bold text-slate-900">{s?.overdueDeliveries ?? 0}</p>
          </div>
        </div>
        <div className="card p-4 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-50">
            <BarChart3 size={18} className="text-violet-500" />
          </div>
          <div>
            <p className="text-sm font-medium text-slate-500">{t("dashboard.budgetUtilization")}</p>
            <p className="text-xl font-bold text-slate-900">
              {s?.budgetUtilization != null ? `${Number(s.budgetUtilization).toFixed(1)}%` : "-"}
            </p>
          </div>
        </div>
      </div>

      {/* Row 2 - Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {salesTrend && salesTrend.length > 0 && (
          <BarChart data={salesTrend} label={t("dashboard.salesTrend")} color="bg-emerald-500" />
        )}
        {purchaseTrend && purchaseTrend.length > 0 && (
          <BarChart data={purchaseTrend} label={t("dashboard.purchaseTrend")} color="bg-blue-500" />
        )}
      </div>

      {/* Row 3 - Lists */}
      <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-4 gap-6">
        <ActivityList activities={s?.recentActivities ?? []} />
        <TopItemList
          items={s?.topCustomers ?? []}
          title={t("dashboard.topCustomers")}
          icon={<Users size={16} />}
        />
        <TopItemList
          items={s?.topProducts ?? []}
          title={t("dashboard.topProducts")}
          icon={<Package size={16} />}
        />
        <div className="section-card">
          <p className="section-kicker">CRM</p>
          <h3 className="section-title">{t("dashboard.opportunities")}</h3>
          <div className="mt-4 space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-500">{t("dashboard.openOpportunities")}</span>
              <span className="text-lg font-bold text-slate-900">{s?.openOpportunities ?? 0}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-500">{t("dashboard.opportunityValue")}</span>
              <span className="text-lg font-bold text-slate-900">
                {(s?.opportunityValue ?? 0).toLocaleString()}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
