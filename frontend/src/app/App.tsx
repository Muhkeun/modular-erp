import { Routes, Route, Navigate } from "react-router-dom";
import { lazy, Suspense, useEffect } from "react";
import { useAuth } from "../shared/hooks/useAuth";
import MainLayout from "./MainLayout";
import LoginPage from "./LoginPage";
import { ToastProvider } from "../shared/components/Toast";
import { ConfirmProvider } from "../shared/components/ConfirmDialog";
import FullPageLoader from "../shared/components/FullPageLoader";
import { usePreferenceStore } from "../shared/hooks/usePreference";
import ProtectedRoute from "../shared/components/ProtectedRoute";
import AccessDenied from "../shared/components/AccessDenied";
import { useMenuProfile } from "../shared/hooks/useMenuProfile";
import { findFirstAccessiblePath } from "../shared/navigation";

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
  const { isAuthenticated, userId, tenantId } = useAuth();
  const loadPreferences = usePreferenceStore((state) => state.load);
  const menuProfile = useMenuProfile();

  useEffect(() => {
    if (!isAuthenticated || !userId || !tenantId) return;
    void loadPreferences();
  }, [isAuthenticated, userId, tenantId, loadPreferences]);

  if (!isAuthenticated) return <LoginPage />;

  const homePath = menuProfile.isResolved
    ? findFirstAccessiblePath(menuProfile.visibleMenuCodes) ?? "/settings"
    : null;

  return (
    <ToastProvider>
    <ConfirmProvider>
    <Suspense fallback={<FullPageLoader />}>
      <Routes>
        <Route element={<MainLayout />}>
          <Route path="/" element={homePath ? <Navigate to={homePath} replace /> : <FullPageLoader />} />
          <Route path="/dashboard" element={<ProtectedRoute menuCode="dashboard"><DashboardPage /></ProtectedRoute>} />

          {/* Master Data */}
          <Route path="/master-data/items" element={<ProtectedRoute menuCode="master-data.items"><ItemListPage /></ProtectedRoute>} />
          <Route path="/master-data/items/new" element={<ProtectedRoute menuCode="master-data.items"><ItemFormPage /></ProtectedRoute>} />
          <Route path="/master-data/items/:id" element={<ProtectedRoute menuCode="master-data.items"><ItemFormPage /></ProtectedRoute>} />

          {/* Purchase */}
          <Route path="/purchase/requests" element={<ProtectedRoute menuCode="purchase.requests"><PurchaseRequestPage /></ProtectedRoute>} />
          <Route path="/purchase/orders" element={<ProtectedRoute menuCode="purchase.orders"><PurchaseOrderPage /></ProtectedRoute>} />

          {/* Production */}
          <Route path="/production/work-orders" element={<ProtectedRoute menuCode="production.work-orders"><WorkOrderPage /></ProtectedRoute>} />
          <Route path="/production/work-orders/:id" element={<ProtectedRoute menuCode="production.work-orders"><WorkOrderDetailPage /></ProtectedRoute>} />

          {/* Logistics */}
          <Route path="/logistics/gr" element={<ProtectedRoute menuCode="logistics.gr"><GoodsReceiptPage /></ProtectedRoute>} />
          <Route path="/logistics/gi" element={<ProtectedRoute menuCode="logistics.gi"><GoodsIssuePage /></ProtectedRoute>} />
          <Route path="/logistics/stock" element={<ProtectedRoute menuCode="logistics.stock"><StockOverviewPage /></ProtectedRoute>} />

          {/* Planning */}
          <Route path="/planning/mrp" element={<ProtectedRoute menuCode="planning.mrp"><MrpPage /></ProtectedRoute>} />

          {/* Sales */}
          <Route path="/sales/orders" element={<ProtectedRoute menuCode="sales.orders"><SalesOrderPage /></ProtectedRoute>} />

          {/* Finance */}
          <Route path="/account/journal" element={<ProtectedRoute menuCode="finance.journal"><JournalEntryPage /></ProtectedRoute>} />

          {/* HR */}
          <Route path="/hr" element={<ProtectedRoute menuCode="hr"><EmployeePage /></ProtectedRoute>} />

          {/* Budget */}
          <Route path="/finance/budget" element={<ProtectedRoute menuCode="finance.budget"><BudgetPage /></ProtectedRoute>} />

          {/* Asset */}
          <Route path="/finance/assets" element={<ProtectedRoute menuCode="finance.assets"><AssetPage /></ProtectedRoute>} />

          {/* Period Close */}
          <Route path="/finance/period-close" element={<ProtectedRoute menuCode="finance.period-close"><PeriodClosePage /></ProtectedRoute>} />

          {/* Costing */}
          <Route path="/costing" element={<ProtectedRoute menuCode="costing.main"><CostingPage /></ProtectedRoute>} />

          {/* Currency */}
          <Route path="/finance/currency" element={<ProtectedRoute menuCode="finance.currency"><CurrencyPage /></ProtectedRoute>} />

          {/* CRM */}
          <Route path="/crm" element={<ProtectedRoute menuCode="crm.main"><CrmPage /></ProtectedRoute>} />

          {/* Batch */}
          <Route path="/admin/batch" element={<ProtectedRoute menuCode="admin.batch"><BatchPage /></ProtectedRoute>} />

          {/* Approval */}
          <Route path="/approvals" element={<ProtectedRoute menuCode="approvals"><ApprovalInboxPage /></ProtectedRoute>} />

          {/* Notification */}
          <Route path="/notifications" element={<ProtectedRoute menuCode="notifications"><NotificationPage /></ProtectedRoute>} />

          {/* Settings */}
          <Route path="/settings" element={<SettingsPage />} />

          {/* Admin */}
          <Route path="/admin/roles" element={<ProtectedRoute menuCode="admin.roles"><RoleManagementPage /></ProtectedRoute>} />
          <Route path="/admin/system-codes" element={<ProtectedRoute menuCode="admin.system-codes"><SystemCodePage /></ProtectedRoute>} />
          <Route path="/admin/organizations" element={<ProtectedRoute menuCode="admin.organizations"><OrganizationPage /></ProtectedRoute>} />
          <Route path="/admin/audit-logs" element={<ProtectedRoute menuCode="admin.audit-logs"><AuditLogPage /></ProtectedRoute>} />
          <Route path="/admin/workflows" element={<ProtectedRoute menuCode="admin.workflows"><WorkflowDesignerPage /></ProtectedRoute>} />
          <Route path="/admin/tenants" element={<ProtectedRoute menuCode="admin.tenants"><TenantManagementPage /></ProtectedRoute>} />
          <Route path="/admin/api-keys" element={<ProtectedRoute menuCode="admin.api-keys"><ApiKeyPage /></ProtectedRoute>} />

          {/* AI */}
          <Route path="/ai-chat" element={<ProtectedRoute menuCode="ai-chat"><AiChatPage /></ProtectedRoute>} />
        </Route>
        <Route path="*" element={<AccessDenied description="Route not found or not included in your menu profile." />} />
      </Routes>
    </Suspense>
    </ConfirmProvider>
    </ToastProvider>
  );
}
