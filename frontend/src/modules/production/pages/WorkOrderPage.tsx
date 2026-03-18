import { useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import StatsCard from "../../../shared/components/StatsCard";
import api from "../../../shared/api/client";
import { Factory, Clock, CheckCircle } from "lucide-react";

interface WoRow {
  id: number; documentNo: string; productCode: string; productName: string;
  plannedQuantity: number; completedQuantity: number; scrapQuantity: number;
  yieldRate: number; status: string; priority: string; orderType: string;
  plannedStartDate: string | null; plannedEndDate: string | null;
}

const statusStyle: Record<string, string> = {
  PLANNED: "badge bg-slate-100 text-slate-600",
  RELEASED: "badge-info",
  IN_PROGRESS: "badge-warning",
  COMPLETED: "badge-success",
  CLOSED: "badge bg-slate-100 text-slate-500",
};

const priorityStyle: Record<string, string> = {
  LOW: "text-slate-400", NORMAL: "text-slate-600",
  HIGH: "text-amber-600 font-semibold", URGENT: "text-red-600 font-bold",
};

export default function WorkOrderPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ["work-orders"],
    queryFn: async () => (await api.get("/api/v1/production/work-orders?size=100")).data,
  });

  const rows: WoRow[] = data?.data || [];
  const inProgress = rows.filter(r => r.status === "IN_PROGRESS").length;
  const planned = rows.filter(r => r.status === "PLANNED" || r.status === "RELEASED").length;
  const completed = rows.filter(r => r.status === "COMPLETED").length;

  const columnDefs = useMemo<ColDef<WoRow>[]>(() => [
    { field: "documentNo", headerName: t("wo.docNo"), flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "productName", headerName: t("wo.product"), flex: 2 },
    { field: "plannedQuantity", headerName: t("wo.planned"), flex: 0.8, type: "numericColumn" },
    { field: "completedQuantity", headerName: t("wo.completed"), flex: 0.8, type: "numericColumn" },
    { field: "scrapQuantity", headerName: t("wo.scrap"), flex: 0.7, type: "numericColumn",
      cellRenderer: (p: { value: number }) => p.value > 0 ? <span className="text-red-600">{p.value}</span> : <span className="text-slate-300">0</span> },
    { field: "yieldRate", headerName: t("wo.yield"), flex: 0.7,
      valueFormatter: (p: { value: number }) => `${(p.value * 100).toFixed(1)}%`,
      cellClass: (p: { value: number }) => p.value < 0.95 ? "text-red-600" : "text-emerald-600" },
    { field: "priority", headerName: t("wo.priority"), flex: 0.7,
      cellRenderer: (p: { value: string }) => <span className={priorityStyle[p.value] || ""}>{p.value}</span> },
    { field: "plannedStartDate", headerName: t("wo.startDate"), flex: 1 },
    { field: "status", headerName: t("common.status"), flex: 0.9,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  const handleRowClick = useCallback((row: WoRow) => navigate(`/production/work-orders/${row.id}`), [navigate]);

  return (
    <div>
      <PageHeader title={t("wo.title")} description={t("wo.description")}
        breadcrumbs={[{ label: t("nav.production") }, { label: t("nav.workOrders") }]}
        actions={<button className="btn-primary"><Plus size={16} /> {t("wo.newWo")}</button>} />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <StatsCard title={t("wo.inProgress")} value={inProgress} icon={<Factory size={20} />} iconColor="bg-amber-50 text-amber-600" />
        <StatsCard title={t("wo.plannedReleased")} value={planned} icon={<Clock size={20} />} iconColor="bg-blue-50 text-blue-600" />
        <StatsCard title={t("wo.completedWo")} value={completed} icon={<CheckCircle size={20} />} iconColor="bg-emerald-50 text-emerald-600" />
      </div>

      <div className="card overflow-hidden">
        <DataGrid<WoRow> rowData={rows} columnDefs={columnDefs} loading={isLoading} onRowClicked={handleRowClick} />
      </div>
    </div>
  );
}
