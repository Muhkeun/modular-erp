import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
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
  const { data, isLoading } = useQuery({
    queryKey: ["sales-orders"],
    queryFn: async () => (await api.get("/api/v1/sales/orders?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<SoRow>[]>(() => [
    { field: "documentNo", headerName: "SO No.", flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "customerName", headerName: "Customer", flex: 2 },
    { field: "orderDate", headerName: "Order Date", flex: 1 },
    { field: "deliveryDate", headerName: "Delivery", flex: 1 },
    { field: "grandTotal", headerName: "Total (incl. Tax)", flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => p.value?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 }) },
    { field: "status", headerName: "Status", flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], []);

  return (
    <div>
      <PageHeader title="Sales Orders" description="Customer order management and delivery tracking"
        breadcrumbs={[{ label: "Sales" }, { label: "Sales Orders" }]}
        actions={<button className="btn-primary"><Plus size={16} /> New Order</button>} />
      <div className="card overflow-hidden">
        <DataGrid<SoRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
