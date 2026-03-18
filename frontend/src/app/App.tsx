import { Routes, Route, Navigate } from "react-router-dom";
import { lazy, Suspense } from "react";
import { useAuth } from "../shared/hooks/useAuth";
import MainLayout from "./MainLayout";
import LoginPage from "./LoginPage";

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

function Loading() {
  return (
    <div className="flex items-center justify-center h-64">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-600" />
    </div>
  );
}

export default function App() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) return <LoginPage />;

  return (
    <Suspense fallback={<Loading />}>
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
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
