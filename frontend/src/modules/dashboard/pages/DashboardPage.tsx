import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import PageHeader from "../../../shared/components/PageHeader";
import StatsCard from "../../../shared/components/StatsCard";
import api from "../../../shared/api/client";
import { Package, ShoppingCart, Truck, Factory, CheckCircle, AlertTriangle, Clock, TrendingUp } from "lucide-react";

export default function DashboardPage() {
  const { t } = useTranslation();

  /* ── real API calls for stats ────────────────────── */
  const openPo = useQuery({
    queryKey: ["dashboard-open-po"],
    queryFn: async () => (await api.get("/api/v1/purchase/orders?status=APPROVED&size=1")).data,
  });

  const pendingGr = useQuery({
    queryKey: ["dashboard-pending-gr"],
    queryFn: async () => (await api.get("/api/v1/logistics/goods-receipts?status=DRAFT&size=1")).data,
  });

  const prodInProgress = useQuery({
    queryKey: ["dashboard-prod-progress"],
    queryFn: async () => (await api.get("/api/v1/production/work-orders?status=IN_PROGRESS&size=1")).data,
  });

  const stockItems = useQuery({
    queryKey: ["dashboard-stock"],
    queryFn: async () => (await api.get("/api/v1/logistics/stock?size=1")).data,
  });

  const recentPr = useQuery({
    queryKey: ["dashboard-recent-pr"],
    queryFn: async () => (await api.get("/api/v1/purchase/requests?size=5")).data,
  });

  const count = (q: typeof openPo) => q.data?.meta?.totalElements ?? 0;

  return (
    <div>
      <PageHeader
        title={t("dashboard.title")}
        description={t("dashboard.description")}
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <StatsCard
          title={t("dashboard.openPo")}
          value={count(openPo).toLocaleString()}
          icon={<ShoppingCart size={22} />}
          iconColor="bg-blue-50 text-blue-600"
        />
        <StatsCard
          title={t("dashboard.pendingDelivery")}
          value={count(pendingGr).toLocaleString()}
          icon={<Truck size={22} />}
          iconColor="bg-amber-50 text-amber-600"
        />
        <StatsCard
          title={t("dashboard.productionInProgress")}
          value={count(prodInProgress).toLocaleString()}
          icon={<Factory size={22} />}
          iconColor="bg-emerald-50 text-emerald-600"
        />
        <StatsCard
          title={t("dashboard.stockItems")}
          value={count(stockItems).toLocaleString()}
          icon={<Package size={22} />}
          iconColor="bg-purple-50 text-purple-600"
        />
      </div>

      {/* Content area */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Purchase Requests */}
        <div className="card lg:col-span-2">
          <div className="border-b border-slate-100 px-6 py-4">
            <h2 className="text-base font-semibold text-slate-900">{t("dashboard.recentPr")}</h2>
          </div>
          <div className="divide-y divide-slate-50">
            {recentPr.isLoading ? (
              <div className="px-6 py-8 text-center text-slate-400">{t("common.loading")}</div>
            ) : (recentPr.data?.data || []).length === 0 ? (
              <div className="px-6 py-8 text-center text-slate-400">{t("common.noData")}</div>
            ) : (
              (recentPr.data?.data || []).map((item: { id: number; documentNo: string; description: string | null; requestedBy: string | null; status: string; requestDate: string }) => (
                <div key={item.id} className="flex items-center gap-4 px-6 py-4 hover:bg-slate-50 transition-colors cursor-pointer">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg text-sm font-bold bg-brand-50 text-brand-600">
                    PR
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-slate-900 font-mono">{item.documentNo}</span>
                      <span className={`badge ${item.status === "DRAFT" ? "bg-slate-100 text-slate-600" : item.status === "APPROVED" ? "badge-success" : item.status === "SUBMITTED" ? "badge-info" : item.status === "REJECTED" ? "badge-danger" : "bg-slate-100 text-slate-500"}`}>
                        {t("status." + item.status, item.status)}
                      </span>
                    </div>
                    <p className="text-sm text-slate-500 truncate">{item.description || "-"}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-slate-600">{item.requestedBy || "-"}</p>
                    <p className="text-xs text-slate-400">{item.requestDate}</p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* System Status */}
        <div className="space-y-5">
          <div className="card p-6">
            <h2 className="text-base font-semibold text-slate-900 mb-4">{t("dashboard.systemStatus")}</h2>
            <div className="space-y-3">
              {[
                { label: t("nav.procurement"), icon: <CheckCircle size={16} />, status: t("dashboard.operational"), color: "text-emerald-500" },
                { label: t("nav.logistics"), icon: <AlertTriangle size={16} />, status: t("dashboard.operational"), color: "text-amber-500" },
                { label: t("nav.finance"), icon: <Clock size={16} />, status: t("dashboard.operational"), color: "text-blue-500" },
                { label: t("nav.sales"), icon: <TrendingUp size={16} />, status: t("dashboard.operational"), color: "text-emerald-500" },
              ].map((s) => (
                <div key={s.label} className="flex items-center justify-between py-2">
                  <span className="text-sm text-slate-600">{s.label}</span>
                  <span className={`flex items-center gap-1.5 text-sm font-medium ${s.color}`}>
                    {s.icon} {s.status}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
