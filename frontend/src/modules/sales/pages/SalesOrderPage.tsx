import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface SoRow {
  id: number; documentNo: string; customerName: string; orderDate: string;
  deliveryDate: string | null; status: string; grandTotal: number;
}

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600", SUBMITTED: "badge-info",
  APPROVED: "badge-success", REJECTED: "badge-danger",
  SHIPPED: "badge bg-purple-50 text-purple-700 ring-1 ring-inset ring-purple-600/20",
  COMPLETED: "badge bg-slate-100 text-slate-500",
};

export default function SalesOrderPage() {
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ["sales-orders"],
    queryFn: async () => (await api.get("/api/v1/sales/orders?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<SoRow>[]>(() => [
    { field: "documentNo", headerName: t("so.docNo"), flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "customerName", headerName: t("so.customer"), flex: 2 },
    { field: "orderDate", headerName: t("so.orderDate"), flex: 1 },
    { field: "deliveryDate", headerName: t("so.deliveryDate"), flex: 1 },
    { field: "grandTotal", headerName: t("so.grandTotal"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => p.value?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 }) },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  return (
    <div>
      <PageHeader title={t("so.title")} description={t("so.description")}
        breadcrumbs={[{ label: t("nav.sales") }, { label: t("nav.salesOrders") }]}
        actions={<button className="btn-primary"><Plus size={16} /> {t("so.newSo")}</button>} />
      <div className="card overflow-hidden">
        <DataGrid<SoRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
