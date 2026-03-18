import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColDef } from "ag-grid-community";
import { Plus } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface PoRow {
  id: number; documentNo: string; vendorCode: string; vendorName: string;
  orderDate: string; deliveryDate: string | null; status: string;
  currencyCode: string; grandTotal: number;
}

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  SUBMITTED: "badge-info",
  APPROVED: "badge-success",
  REJECTED: "badge-danger",
  SENT: "badge bg-purple-50 text-purple-700 ring-1 ring-inset ring-purple-600/20",
  COMPLETED: "badge bg-slate-100 text-slate-500",
};

export default function PurchaseOrderPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["purchase-orders"],
    queryFn: async () => (await api.get("/api/v1/purchase/orders?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<PoRow>[]>(() => [
    { field: "documentNo", headerName: "PO No.", flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "vendorName", headerName: "Vendor", flex: 2 },
    { field: "orderDate", headerName: "Order Date", flex: 1 },
    { field: "deliveryDate", headerName: "Delivery", flex: 1 },
    { field: "grandTotal", headerName: "Grand Total", flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => p.value?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 }) },
    { field: "status", headerName: "Status", flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], []);

  return (
    <div>
      <PageHeader title="Purchase Orders" description="Manage purchase orders sent to vendors"
        breadcrumbs={[{ label: "Procurement" }, { label: "Purchase Orders" }]}
        actions={<button className="btn-primary"><Plus size={16} /> New PO</button>} />
      <div className="card overflow-hidden">
        <DataGrid<PoRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
