import { useState } from "react";
import { Outlet, Link, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "../shared/hooks/useAuth";
import {
  LayoutGrid, Package, ShoppingCart, Truck, DollarSign, Users,
  ClipboardCheck, Building2, ChevronDown, ChevronRight, LogOut,
  Search, Bell, Menu, X, Settings, Factory, CalendarClock,
} from "lucide-react";
import { clsx } from "clsx";

interface NavItem {
  labelKey: string;
  icon: React.ReactNode;
  path?: string;
  children?: { labelKey: string; path: string }[];
}

const navigation: NavItem[] = [
  { labelKey: "nav.dashboard", icon: <LayoutGrid size={20} />, path: "/dashboard" },
  {
    labelKey: "nav.masterData", icon: <Package size={20} />,
    children: [
      { labelKey: "nav.items", path: "/master-data/items" },
      { labelKey: "nav.vendors", path: "/master-data/vendors" },
      { labelKey: "nav.customers", path: "/master-data/customers" },
      { labelKey: "nav.companies", path: "/master-data/companies" },
    ],
  },
  {
    labelKey: "nav.procurement", icon: <ShoppingCart size={20} />,
    children: [
      { labelKey: "nav.purchaseRequests", path: "/purchase/requests" },
      { labelKey: "nav.rfq", path: "/purchase/rfq" },
      { labelKey: "nav.purchaseOrders", path: "/purchase/orders" },
    ],
  },
  {
    labelKey: "nav.logistics", icon: <Truck size={20} />,
    children: [
      { labelKey: "nav.goodsReceipt", path: "/logistics/gr" },
      { labelKey: "nav.goodsIssue", path: "/logistics/gi" },
      { labelKey: "nav.stockOverview", path: "/logistics/stock" },
    ],
  },
  {
    labelKey: "nav.production", icon: <Factory size={20} />,
    children: [
      { labelKey: "nav.workOrders", path: "/production/work-orders" },
      { labelKey: "nav.workCenters", path: "/production/work-centers" },
      { labelKey: "nav.routings", path: "/production/routings" },
    ],
  },
  {
    labelKey: "nav.planning", icon: <CalendarClock size={20} />,
    children: [
      { labelKey: "nav.mrp", path: "/planning/mrp" },
      { labelKey: "nav.schedule", path: "/planning/schedule" },
      { labelKey: "nav.capacity", path: "/planning/capacity" },
    ],
  },
  {
    labelKey: "nav.sales", icon: <DollarSign size={20} />,
    children: [
      { labelKey: "nav.salesOrders", path: "/sales/orders" },
      { labelKey: "nav.invoices", path: "/sales/invoices" },
    ],
  },
  {
    labelKey: "nav.finance", icon: <Building2 size={20} />,
    children: [
      { labelKey: "nav.journalEntries", path: "/account/journal" },
      { labelKey: "nav.budget", path: "/account/budget" },
    ],
  },
  { labelKey: "nav.hr", icon: <Users size={20} />, path: "/hr" },
  { labelKey: "nav.quality", icon: <ClipboardCheck size={20} />, path: "/quality" },
];

function NavGroup({ item }: { item: NavItem }) {
  const location = useLocation();
  const { t } = useTranslation();
  const isActive = item.path
    ? location.pathname === item.path
    : item.children?.some((c) => location.pathname.startsWith(c.path));
  const [open, setOpen] = useState(isActive);

  if (item.path) {
    return (
      <Link to={item.path}
        className={clsx(
          "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
          isActive
            ? "bg-brand-50 text-brand-700"
            : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
        )}>
        <span className={clsx(isActive ? "text-brand-600" : "text-slate-400")}>{item.icon}</span>
        {t(item.labelKey)}
      </Link>
    );
  }

  return (
    <div>
      <button onClick={() => setOpen(!open)}
        className={clsx(
          "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
          isActive
            ? "text-brand-700"
            : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
        )}>
        <span className={clsx(isActive ? "text-brand-600" : "text-slate-400")}>{item.icon}</span>
        <span className="flex-1 text-left">{t(item.labelKey)}</span>
        {open ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
      </button>
      {open && (
        <div className="ml-8 mt-1 space-y-0.5 border-l-2 border-slate-100 pl-3">
          {item.children!.map((child) => {
            const childActive = location.pathname.startsWith(child.path);
            return (
              <Link key={child.path} to={child.path}
                className={clsx(
                  "block rounded-md px-3 py-2 text-sm transition-colors",
                  childActive
                    ? "bg-brand-50 text-brand-700 font-medium"
                    : "text-slate-500 hover:bg-slate-50 hover:text-slate-900"
                )}>
                {t(child.labelKey)}
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function MainLayout() {
  const { name, tenantId, logout } = useAuth();
  const { t, i18n } = useTranslation();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  return (
    <div className="flex h-screen overflow-hidden bg-surface-secondary">
      {/* Sidebar */}
      <aside className={clsx(
        "flex flex-col border-r border-slate-200 bg-white transition-all duration-200",
        sidebarOpen ? "w-64" : "w-0 overflow-hidden"
      )}>
        {/* Logo */}
        <div className="flex h-16 items-center gap-3 border-b border-slate-100 px-5">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600">
            <LayoutGrid size={18} className="text-white" />
          </div>
          <div>
            <div className="text-sm font-bold text-slate-900 tracking-tight">ModularERP</div>
            <div className="text-[11px] text-slate-400 font-medium">{tenantId}</div>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto p-3 space-y-1">
          {navigation.map((item) => (
            <NavGroup key={item.labelKey} item={item} />
          ))}
        </nav>

        {/* User */}
        <div className="border-t border-slate-100 p-3">
          <div className="flex items-center gap-3 rounded-lg px-3 py-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-brand-700 text-sm font-bold">
              {name?.charAt(0)?.toUpperCase() || "U"}
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-medium text-slate-900 truncate">{name}</div>
            </div>
            <button onClick={logout} className="text-slate-400 hover:text-red-500 transition-colors" title="Logout">
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top bar */}
        <header className="flex h-14 items-center justify-between border-b border-slate-200 bg-white px-6">
          <div className="flex items-center gap-4">
            <button onClick={() => setSidebarOpen(!sidebarOpen)}
              className="text-slate-400 hover:text-slate-600">
              {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
            <div className="relative">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input placeholder={t("common.search") + "..."} className="input pl-9 w-72 bg-slate-50 border-slate-200" />
            </div>
          </div>
          <div className="flex items-center gap-3">
            <button onClick={() => { const next = i18n.language === "ko" ? "en" : "ko"; i18n.changeLanguage(next); localStorage.setItem("locale", next); }} className="btn-ghost text-xs font-bold px-2">
              {i18n.language === "ko" ? "EN" : "KO"}
            </button>
            <button className="relative text-slate-400 hover:text-slate-600 p-2 rounded-lg hover:bg-slate-50">
              <Bell size={20} />
              <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-red-500" />
            </button>
            <button className="text-slate-400 hover:text-slate-600 p-2 rounded-lg hover:bg-slate-50">
              <Settings size={20} />
            </button>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
