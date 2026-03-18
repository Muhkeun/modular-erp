import { useMemo, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColDef } from "ag-grid-community";
import { Plus, FileText } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface PrRow {
  id: number; documentNo: string; companyCode: string; plantCode: string;
  requestDate: string; deliveryDate: string | null; status: string;
  prType: string; requestedBy: string | null; totalAmount: number;
  description: string | null;
}

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  SUBMITTED: "badge-info",
  APPROVED: "badge-success",
  REJECTED: "badge-danger",
  CLOSED: "badge bg-slate-100 text-slate-500",
};

export default function PurchaseRequestPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["purchase-requests"],
    queryFn: async () => (await api.get("/api/v1/purchase/requests?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<PrRow>[]>(() => [
    { field: "documentNo", headerName: "PR No.", flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "prType", headerName: "Type", flex: 0.8 },
    { field: "companyCode", headerName: "Company", flex: 0.8 },
    { field: "plantCode", headerName: "Plant", flex: 0.7 },
    { field: "requestDate", headerName: "Request Date", flex: 1 },
    { field: "deliveryDate", headerName: "Delivery Date", flex: 1 },
    { field: "requestedBy", headerName: "Requested By", flex: 1 },
    { field: "totalAmount", headerName: "Total", flex: 1, type: "numericColumn",
      valueFormatter: (p: { value: number }) => p.value?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 }) },
    { field: "status", headerName: "Status", flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], []);

  return (
    <div>
      <PageHeader title="Purchase Requests" description="Create and manage purchase requisitions"
        breadcrumbs={[{ label: "Procurement" }, { label: "Purchase Requests" }]}
        actions={<button className="btn-primary"><Plus size={16} /> New PR</button>} />
      <div className="card overflow-hidden">
        <DataGrid<PrRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
