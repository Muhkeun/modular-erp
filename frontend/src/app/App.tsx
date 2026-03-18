import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "../shared/hooks/useAuth";
import MainLayout from "./MainLayout";
import LoginPage from "./LoginPage";
import DashboardPage from "../modules/dashboard/pages/DashboardPage";
import ItemListPage from "../modules/master-data/pages/ItemListPage";
import ItemFormPage from "../modules/master-data/pages/ItemFormPage";

export default function App() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) return <LoginPage />;

  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/master-data/items" element={<ItemListPage />} />
        <Route path="/master-data/items/new" element={<ItemFormPage />} />
        <Route path="/master-data/items/:id" element={<ItemFormPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
