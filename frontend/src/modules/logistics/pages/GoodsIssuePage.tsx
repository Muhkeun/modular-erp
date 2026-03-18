import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColDef } from "ag-grid-community";
import { Plus } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface GiRow {
  id: number; documentNo: string; issueType: string; referenceDocNo: string | null;
  plantCode: string; storageLocation: string; issueDate: string; status: string;
}

const typeLabel: Record<string, string> = {
  SALES: "Sales", TRANSFER: "Transfer", PRODUCTION: "Production", SCRAP: "Scrap", RETURN: "Return",
};

export default function GoodsIssuePage() {
  const { data, isLoading } = useQuery({
    queryKey: ["goods-issues"],
    queryFn: async () => (await api.get("/api/v1/logistics/goods-issues?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<GiRow>[]>(() => [
    { field: "documentNo", headerName: "GI No.", flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "issueType", headerName: "Type", flex: 0.8, valueFormatter: (p: { value: string }) => typeLabel[p.value] || p.value },
    { field: "referenceDocNo", headerName: "Ref. Doc", flex: 1 },
    { field: "plantCode", headerName: "Plant", flex: 0.7 },
    { field: "storageLocation", headerName: "Storage", flex: 0.8 },
    { field: "issueDate", headerName: "Issue Date", flex: 1 },
    { field: "status", headerName: "Status", flex: 0.8,
      cellRenderer: (p: { value: string }) => {
        const s: Record<string, string> = { DRAFT: "badge bg-slate-100 text-slate-600", CONFIRMED: "badge-success", CANCELLED: "badge-danger" };
        return <span className={s[p.value] || "badge"}>{p.value}</span>;
      }},
  ], []);

  return (
    <div>
      <PageHeader title="Goods Issues" description="Outbound material shipment and consumption"
        breadcrumbs={[{ label: "Logistics" }, { label: "Goods Issues" }]}
        actions={<button className="btn-primary"><Plus size={16} /> New GI</button>} />
      <div className="card overflow-hidden">
        <DataGrid<GiRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
