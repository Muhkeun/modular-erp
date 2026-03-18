import PageHeader from "../../../shared/components/PageHeader";
import StatsCard from "../../../shared/components/StatsCard";
import { Package, ShoppingCart, Truck, DollarSign, TrendingUp, AlertTriangle, Clock, CheckCircle } from "lucide-react";

export default function DashboardPage() {
  return (
    <div>
      <PageHeader
        title="Dashboard"
        description="Overview of your enterprise operations"
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <StatsCard
          title="Open Purchase Orders"
          value="23"
          change="+3 from last week"
          changeType="neutral"
          icon={<ShoppingCart size={22} />}
          iconColor="bg-blue-50 text-blue-600"
        />
        <StatsCard
          title="Pending Deliveries"
          value="12"
          change="5 overdue"
          changeType="negative"
          icon={<Truck size={22} />}
          iconColor="bg-amber-50 text-amber-600"
        />
        <StatsCard
          title="Monthly Revenue"
          value="₩2.4B"
          change="+12.5% vs last month"
          changeType="positive"
          icon={<DollarSign size={22} />}
          iconColor="bg-emerald-50 text-emerald-600"
        />
        <StatsCard
          title="Items in Stock"
          value="1,847"
          change="32 below reorder point"
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
            <h2 className="text-base font-semibold text-slate-900">Pending Approvals</h2>
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
                    {item.status === "urgent" && <span className="badge-danger">Urgent</span>}
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
              View all approvals →
            </button>
          </div>
        </div>

        {/* Quick Status */}
        <div className="space-y-5">
          <div className="card p-6">
            <h2 className="text-base font-semibold text-slate-900 mb-4">System Status</h2>
            <div className="space-y-3">
              {[
                { label: "Procurement", icon: <CheckCircle size={16} />, status: "Operational", color: "text-emerald-500" },
                { label: "Logistics", icon: <AlertTriangle size={16} />, status: "3 delayed GR", color: "text-amber-500" },
                { label: "Finance", icon: <Clock size={16} />, status: "Month-end close", color: "text-blue-500" },
                { label: "Sales", icon: <TrendingUp size={16} />, status: "On track", color: "text-emerald-500" },
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
            <h2 className="text-base font-semibold text-slate-900 mb-4">Recent Activity</h2>
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
