import { Routes, Route, Navigate } from "react-router-dom";
import { lazy, Suspense } from "react";
import { useAuth } from "../shared/hooks/useAuth";
import MainLayout from "./MainLayout";
import LoginPage from "./LoginPage";
import { ToastProvider } from "../shared/components/Toast";
import { ConfirmProvider } from "../shared/components/ConfirmDialog";
import FullPageLoader from "../shared/components/FullPageLoader";

// Lazy-loaded pages
const DashboardPage = lazy(() => import("../modules/dashboard/pages/DashboardPage"));
const ItemListPage = lazy(() => import("../modules/master-data/pages/ItemListPage"));
const ItemFormPage = lazy(() => import("../modules/master-data/pages/ItemFormPage"));
const PurchaseRequestPage = lazy(() => import("../modules/purchase/pages/PurchaseRequestPage"));
const PurchaseOrderPage = lazy(() => import("../modules/purchase/pages/PurchaseOrderPage"));
const WorkOrderPage = lazy(() => import("../modules/production/pages/WorkOrderPage"));
const WorkOrderDetailPage = lazy(() => import("../modules/production/pages/WorkOrderDetailPage"));
const GoodsReceiptPage = lazy(() => import("../modules/logistics/pages/GoodsReceiptPage"));
const GoodsIssuePage = lazy(() => import("../modules/logistics/pages/GoodsIssuePage"));
const StockOverviewPage = lazy(() => import("../modules/logistics/pages/StockOverviewPage"));
const MrpPage = lazy(() => import("../modules/planning/pages/MrpPage"));
const SalesOrderPage = lazy(() => import("../modules/sales/pages/SalesOrderPage"));
const JournalEntryPage = lazy(() => import("../modules/account/pages/JournalEntryPage"));
const EmployeePage = lazy(() => import("../modules/hr/pages/EmployeePage"));

// Phase 2 modules
const BudgetPage = lazy(() => import("../modules/budget/pages/BudgetPage"));
const AssetPage = lazy(() => import("../modules/asset/pages/AssetPage"));
const PeriodClosePage = lazy(() => import("../modules/period-close/pages/PeriodClosePage"));
const BatchPage = lazy(() => import("../modules/batch/pages/BatchPage"));
const NotificationPage = lazy(() => import("../modules/notification/pages/NotificationPage"));
const CrmPage = lazy(() => import("../modules/crm/pages/CrmPage"));
const CostingPage = lazy(() => import("../modules/costing/pages/CostingPage"));
const CurrencyPage = lazy(() => import("../modules/currency/pages/CurrencyPage"));

// Approval
const ApprovalInboxPage = lazy(() => import("../modules/approval/pages/ApprovalInboxPage"));

// Platform - Settings & Admin
const SettingsPage = lazy(() => import("../modules/settings/pages/SettingsPage"));
const RoleManagementPage = lazy(() => import("../modules/admin/pages/RoleManagementPage"));
const SystemCodePage = lazy(() => import("../modules/admin/pages/SystemCodePage"));
const AuditLogPage = lazy(() => import("../modules/admin/pages/AuditLogPage"));
const OrganizationPage = lazy(() => import("../modules/admin/pages/OrganizationPage"));
const WorkflowDesignerPage = lazy(() => import("../modules/admin/pages/WorkflowDesignerPage"));
const TenantManagementPage = lazy(() => import("../modules/admin/pages/TenantManagementPage"));
const ApiKeyPage = lazy(() => import("../modules/admin/pages/ApiKeyPage"));

// AI
const AiChatPage = lazy(() => import("../modules/ai/pages/AiChatPage"));

export default function App() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) return <LoginPage />;

  return (
    <ToastProvider>
    <ConfirmProvider>
    <Suspense fallback={<FullPageLoader />}>
      <Routes>
        <Route element={<MainLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />

          {/* Master Data */}
          <Route path="/master-data/items" element={<ItemListPage />} />
          <Route path="/master-data/items/new" element={<ItemFormPage />} />
          <Route path="/master-data/items/:id" element={<ItemFormPage />} />

          {/* Purchase */}
          <Route path="/purchase/requests" element={<PurchaseRequestPage />} />
          <Route path="/purchase/orders" element={<PurchaseOrderPage />} />

          {/* Production */}
          <Route path="/production/work-orders" element={<WorkOrderPage />} />
          <Route path="/production/work-orders/:id" element={<WorkOrderDetailPage />} />

          {/* Logistics */}
          <Route path="/logistics/gr" element={<GoodsReceiptPage />} />
          <Route path="/logistics/gi" element={<GoodsIssuePage />} />
          <Route path="/logistics/stock" element={<StockOverviewPage />} />

          {/* Planning */}
          <Route path="/planning/mrp" element={<MrpPage />} />

          {/* Sales */}
          <Route path="/sales/orders" element={<SalesOrderPage />} />

          {/* Finance */}
          <Route path="/account/journal" element={<JournalEntryPage />} />

          {/* HR */}
          <Route path="/hr" element={<EmployeePage />} />

          {/* Budget */}
          <Route path="/finance/budget" element={<BudgetPage />} />

          {/* Asset */}
          <Route path="/finance/assets" element={<AssetPage />} />

          {/* Period Close */}
          <Route path="/finance/period-close" element={<PeriodClosePage />} />

          {/* Costing */}
          <Route path="/costing" element={<CostingPage />} />

          {/* Currency */}
          <Route path="/finance/currency" element={<CurrencyPage />} />

          {/* CRM */}
          <Route path="/crm" element={<CrmPage />} />

          {/* Batch */}
          <Route path="/admin/batch" element={<BatchPage />} />

          {/* Approval */}
          <Route path="/approvals" element={<ApprovalInboxPage />} />

          {/* Notification */}
          <Route path="/notifications" element={<NotificationPage />} />

          {/* Settings */}
          <Route path="/settings" element={<SettingsPage />} />

          {/* Admin */}
          <Route path="/admin/roles" element={<RoleManagementPage />} />
          <Route path="/admin/system-codes" element={<SystemCodePage />} />
          <Route path="/admin/organizations" element={<OrganizationPage />} />
          <Route path="/admin/audit-logs" element={<AuditLogPage />} />
          <Route path="/admin/workflows" element={<WorkflowDesignerPage />} />
          <Route path="/admin/tenants" element={<TenantManagementPage />} />
          <Route path="/admin/api-keys" element={<ApiKeyPage />} />

          {/* AI */}
          <Route path="/ai-chat" element={<AiChatPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
    </ConfirmProvider>
    </ToastProvider>
  );
}
