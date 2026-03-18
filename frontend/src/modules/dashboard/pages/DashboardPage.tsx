import { useTranslation } from "react-i18next";
import PageHeader from "../../../shared/components/PageHeader";
import StatsCard from "../../../shared/components/StatsCard";
import { Package, ShoppingCart, Truck, DollarSign, TrendingUp, AlertTriangle, Clock, CheckCircle } from "lucide-react";

export default function DashboardPage() {
  const { t } = useTranslation();

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
          value="23"
          change={"+3 " + t("dashboard.fromLastWeek")}
          changeType="neutral"
          icon={<ShoppingCart size={22} />}
          iconColor="bg-blue-50 text-blue-600"
        />
        <StatsCard
          title={t("dashboard.pendingDelivery")}
          value="12"
          change={"5 " + t("dashboard.overdue")}
          changeType="negative"
          icon={<Truck size={22} />}
          iconColor="bg-amber-50 text-amber-600"
        />
        <StatsCard
          title={t("dashboard.monthlyRevenue")}
          value="₩2.4B"
          change={"+12.5% " + t("dashboard.vsLastMonth")}
          changeType="positive"
          icon={<DollarSign size={22} />}
          iconColor="bg-emerald-50 text-emerald-600"
        />
        <StatsCard
          title={t("dashboard.stockItems")}
          value="1,847"
          change={"32 " + t("dashboard.belowReorder")}
          changeType="negative"
          icon={<Package size={22} />}
          iconColor="bg-purple-50 text-purple-600"
        />
      </div>

      {/* Content area */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Pending Approvals */}
        <div className="card lg:col-span-2">
          <div className="border-b border-slate-100 px-6 py-4">
            <h2 className="text-base font-semibold text-slate-900">{t("dashboard.pendingApprovals")}</h2>
          </div>
          <div className="divide-y divide-slate-50">
            {[
              { type: "PR", doc: "PR-202603-00012", title: "Office supplies procurement", by: "Kim Minhyuk", status: "pending", time: "2h ago" },
              { type: "PO", doc: "PO-202603-00045", title: "Raw materials order - Steel plates", by: "Lee Jiwon", status: "pending", time: "5h ago" },
              { type: "SO", doc: "SO-202603-00089", title: "Client delivery — ABC Corp", by: "Park Suji", status: "urgent", time: "1d ago" },
            ].map((item) => (
              <div key={item.doc} className="flex items-center gap-4 px-6 py-4 hover:bg-slate-50 transition-colors cursor-pointer">
                <div className={`flex h-10 w-10 items-center justify-center rounded-lg text-sm font-bold
                  ${item.status === "urgent" ? "bg-red-50 text-red-600" : "bg-brand-50 text-brand-600"}`}>
                  {item.type}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-slate-900">{item.doc}</span>
                    {item.status === "urgent" && <span className="badge-danger">{t("status.URGENT") || "Urgent"}</span>}
                  </div>
                  <p className="text-sm text-slate-500 truncate">{item.title}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm text-slate-600">{item.by}</p>
                  <p className="text-xs text-slate-400">{item.time}</p>
                </div>
              </div>
            ))}
          </div>
          <div className="border-t border-slate-100 px-6 py-3">
            <button className="text-sm text-brand-600 hover:text-brand-700 font-medium">
              {t("dashboard.viewAll")}
            </button>
          </div>
        </div>

        {/* Quick Status */}
        <div className="space-y-5">
          <div className="card p-6">
            <h2 className="text-base font-semibold text-slate-900 mb-4">{t("dashboard.systemStatus")}</h2>
            <div className="space-y-3">
              {[
                { label: t("nav.procurement"), icon: <CheckCircle size={16} />, status: t("dashboard.operational"), color: "text-emerald-500" },
                { label: t("nav.logistics"), icon: <AlertTriangle size={16} />, status: "3 " + t("dashboard.delayed"), color: "text-amber-500" },
                { label: t("nav.finance"), icon: <Clock size={16} />, status: t("dashboard.monthEndClose"), color: "text-blue-500" },
                { label: t("nav.sales"), icon: <TrendingUp size={16} />, status: t("dashboard.onTrack"), color: "text-emerald-500" },
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

          <div className="card p-6">
            <h2 className="text-base font-semibold text-slate-900 mb-4">{t("dashboard.recentActivity")}</h2>
            <div className="space-y-4">
              {[
                { action: "PO-202603-00044 approved", time: "10 min ago" },
                { action: "GR-202603-00031 confirmed", time: "1h ago" },
                { action: "New vendor registered", time: "3h ago" },
                { action: "Item BOM updated", time: "5h ago" },
              ].map((a, i) => (
                <div key={i} className="flex items-start gap-3">
                  <div className="mt-1 h-2 w-2 rounded-full bg-brand-400 flex-shrink-0" />
                  <div>
                    <p className="text-sm text-slate-700">{a.action}</p>
                    <p className="text-xs text-slate-400">{a.time}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
