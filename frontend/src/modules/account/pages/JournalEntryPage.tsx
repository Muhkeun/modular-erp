import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColDef } from "ag-grid-community";
import { Plus } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface JeRow {
  id: number; documentNo: string; companyCode: string; postingDate: string;
  entryType: string; status: string; totalDebit: number; totalCredit: number;
  isBalanced: boolean; description: string | null; referenceDocNo: string | null;
}

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600", POSTED: "badge-success", REVERSED: "badge-danger",
};

export default function JournalEntryPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["journal-entries"],
    queryFn: async () => (await api.get("/api/v1/account/journal-entries?size=100")).data,
  });

  const fmt = (v: number) => v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

  const columnDefs = useMemo<ColDef<JeRow>[]>(() => [
    { field: "documentNo", headerName: "JE No.", flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "postingDate", headerName: "Posting Date", flex: 1 },
    { field: "entryType", headerName: "Type", flex: 1 },
    { field: "referenceDocNo", headerName: "Ref. Doc", flex: 1 },
    { field: "description", headerName: "Description", flex: 2 },
    { field: "totalDebit", headerName: "Debit", flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "totalCredit", headerName: "Credit", flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "isBalanced", headerName: "Balanced", flex: 0.7,
      cellRenderer: (p: { value: boolean }) => p.value ? <span className="badge-success">Yes</span> : <span className="badge-danger">No</span> },
    { field: "status", headerName: "Status", flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], []);

  return (
    <div>
      <PageHeader title="Journal Entries" description="General ledger posting and financial transactions"
        breadcrumbs={[{ label: "Finance" }, { label: "Journal Entries" }]}
        actions={<button className="btn-primary"><Plus size={16} /> New Entry</button>} />
      <div className="card overflow-hidden">
        <DataGrid<JeRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
