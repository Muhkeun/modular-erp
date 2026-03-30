import type { ReactNode } from "react";
import {
  Bell,
  Building2,
  CalendarClock,
  Calculator,
  DollarSign,
  Factory,
  FileCheck2,
  LayoutGrid,
  Package,
  Shield,
  ShoppingCart,
  Sparkles,
  Target,
  Truck,
  Users,
  Plus,
} from "lucide-react";

export interface NavigationChild {
  code: string;
  labelKey: string;
  path: string;
}

export interface NavigationItem {
  code: string;
  labelKey: string;
  icon: ReactNode;
  path?: string;
  children?: NavigationChild[];
}

export interface CommandItemDefinition {
  id: string;
  menuCode: string;
  titleKey: string;
  category: "page" | "action";
  icon: ReactNode;
  path: string;
  keywords?: string[];
  shortcut?: string;
}

export const navigationItems: NavigationItem[] = [
  { code: "dashboard", labelKey: "nav.dashboard", icon: <LayoutGrid size={20} />, path: "/dashboard" },
  {
    code: "master-data",
    labelKey: "nav.masterData",
    icon: <Package size={20} />,
    children: [{ code: "master-data.items", labelKey: "nav.items", path: "/master-data/items" }],
  },
  {
    code: "purchase",
    labelKey: "nav.procurement",
    icon: <ShoppingCart size={20} />,
    children: [
      { code: "purchase.requests", labelKey: "nav.purchaseRequests", path: "/purchase/requests" },
      { code: "purchase.orders", labelKey: "nav.purchaseOrders", path: "/purchase/orders" },
    ],
  },
  {
    code: "logistics",
    labelKey: "nav.logistics",
    icon: <Truck size={20} />,
    children: [
      { code: "logistics.gr", labelKey: "nav.goodsReceipt", path: "/logistics/gr" },
      { code: "logistics.gi", labelKey: "nav.goodsIssue", path: "/logistics/gi" },
      { code: "logistics.stock", labelKey: "nav.stockOverview", path: "/logistics/stock" },
    ],
  },
  {
    code: "production",
    labelKey: "nav.production",
    icon: <Factory size={20} />,
    children: [{ code: "production.work-orders", labelKey: "nav.workOrders", path: "/production/work-orders" }],
  },
  {
    code: "planning",
    labelKey: "nav.planning",
    icon: <CalendarClock size={20} />,
    children: [{ code: "planning.mrp", labelKey: "nav.mrp", path: "/planning/mrp" }],
  },
  {
    code: "sales",
    labelKey: "nav.sales",
    icon: <DollarSign size={20} />,
    children: [{ code: "sales.orders", labelKey: "nav.salesOrders", path: "/sales/orders" }],
  },
  {
    code: "finance",
    labelKey: "nav.finance",
    icon: <Building2 size={20} />,
    children: [
      { code: "finance.journal", labelKey: "nav.journalEntries", path: "/account/journal" },
      { code: "finance.budget", labelKey: "nav.budget", path: "/finance/budget" },
      { code: "finance.assets", labelKey: "nav.assets", path: "/finance/assets" },
      { code: "finance.period-close", labelKey: "nav.periodClose", path: "/finance/period-close" },
      { code: "finance.currency", labelKey: "nav.currency", path: "/finance/currency" },
    ],
  },
  {
    code: "costing",
    labelKey: "nav.costing",
    icon: <Calculator size={20} />,
    children: [{ code: "costing.main", labelKey: "nav.costingMain", path: "/costing" }],
  },
  {
    code: "crm",
    labelKey: "nav.crm",
    icon: <Target size={20} />,
    children: [{ code: "crm.main", labelKey: "nav.crmMain", path: "/crm" }],
  },
  { code: "hr", labelKey: "nav.hr", icon: <Users size={20} />, path: "/hr" },
  { code: "approvals", labelKey: "nav.approvals", icon: <FileCheck2 size={20} />, path: "/approvals" },
  { code: "notifications", labelKey: "nav.notifications", icon: <Bell size={20} />, path: "/notifications" },
  { code: "ai-chat", labelKey: "nav.aiChat", icon: <Sparkles size={20} />, path: "/ai-chat" },
  {
    code: "admin",
    labelKey: "nav.admin",
    icon: <Shield size={20} />,
    children: [
      { code: "admin.roles", labelKey: "nav.roles", path: "/admin/roles" },
      { code: "admin.system-codes", labelKey: "nav.systemCodes", path: "/admin/system-codes" },
      { code: "admin.organizations", labelKey: "nav.organizations", path: "/admin/organizations" },
      { code: "admin.audit-logs", labelKey: "nav.auditLogs", path: "/admin/audit-logs" },
      { code: "admin.workflows", labelKey: "nav.workflows", path: "/admin/workflows" },
      { code: "admin.tenants", labelKey: "nav.tenants", path: "/admin/tenants" },
      { code: "admin.api-keys", labelKey: "nav.apiKeys", path: "/admin/api-keys" },
      { code: "admin.batch", labelKey: "nav.batch", path: "/admin/batch" },
    ],
  },
];

export const commandActionItems: CommandItemDefinition[] = [
  {
    id: "new-pr",
    menuCode: "purchase.requests",
    titleKey: "pr.newPr",
    category: "action",
    icon: <Plus size={16} />,
    path: "/purchase/requests",
    shortcut: "Ctrl+N",
    keywords: ["create", "new"],
  },
  {
    id: "new-po",
    menuCode: "purchase.orders",
    titleKey: "po.newPo",
    category: "action",
    icon: <Plus size={16} />,
    path: "/purchase/orders",
  },
  {
    id: "new-so",
    menuCode: "sales.orders",
    titleKey: "so.newSo",
    category: "action",
    icon: <Plus size={16} />,
    path: "/sales/orders",
  },
  {
    id: "new-item",
    menuCode: "master-data.items",
    titleKey: "item.newItem",
    category: "action",
    icon: <Plus size={16} />,
    path: "/master-data/items/new",
  },
];

export function buildPageCommandItems(): CommandItemDefinition[] {
  const pageItems: CommandItemDefinition[] = [];

  navigationItems.forEach((item) => {
    if (item.path) {
      pageItems.push({
        id: item.code,
        menuCode: item.code,
        titleKey: item.labelKey,
        category: "page",
        icon: item.icon,
        path: item.path,
        keywords: ["page", "search"],
      });
      return;
    }

    item.children?.forEach((child) => {
      pageItems.push({
        id: child.code,
        menuCode: child.code,
        titleKey: child.labelKey,
        category: "page",
        icon: item.icon,
        path: child.path,
        keywords: [item.labelKey, child.labelKey],
      });
    });
  });

  return pageItems;
}

export function buildMenuSortMap(items: { menuCode: string; sortOrder: number }[]): Map<string, number> {
  return new Map(items.map((item) => [item.menuCode, item.sortOrder]));
}

export function buildVisibleMenuCodeSet(menuCodes: string[]): Set<string> {
  return new Set(menuCodes);
}

export function findPathForMenuCode(menuCode: string): string | null {
  for (const item of navigationItems) {
    if (item.path && item.code === menuCode) return item.path;
    const child = item.children?.find((entry) => entry.code === menuCode);
    if (child) return child.path;
  }
  return null;
}

export function findFirstAccessiblePath(menuCodes: string[]): string | null {
  for (const menuCode of menuCodes) {
    const path = findPathForMenuCode(menuCode);
    if (path) return path;
  }
  return null;
}
