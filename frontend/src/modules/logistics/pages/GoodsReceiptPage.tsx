import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColDef } from "ag-grid-community";
import { Plus } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface GrRow {
  id: number; documentNo: string; vendorName: string; plantCode: string;
  storageLocation: string; poDocumentNo: string | null;
  receiptDate: string; status: string;
}

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  CONFIRMED: "badge-success",
  CANCELLED: "badge-danger",
};

export default function GoodsReceiptPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["goods-receipts"],
    queryFn: async () => (await api.get("/api/v1/logistics/goods-receipts?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<GrRow>[]>(() => [
    { field: "documentNo", headerName: "GR No.", flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "poDocumentNo", headerName: "PO Ref.", flex: 1, cellRenderer: (p: { value: string | null }) => p.value ? <span className="font-mono text-slate-600">{p.value}</span> : <span className="text-slate-300">-</span> },
    { field: "vendorName", headerName: "Vendor", flex: 2 },
    { field: "plantCode", headerName: "Plant", flex: 0.7 },
    { field: "storageLocation", headerName: "Storage", flex: 0.8 },
    { field: "receiptDate", headerName: "Receipt Date", flex: 1 },
    { field: "status", headerName: "Status", flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], []);

  return (
    <div>
      <PageHeader title="Goods Receipts" description="Inbound material receiving and inspection"
        breadcrumbs={[{ label: "Logistics" }, { label: "Goods Receipts" }]}
        actions={<button className="btn-primary"><Plus size={16} /> New GR</button>} />
      <div className="card overflow-hidden">
        <DataGrid<GrRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
