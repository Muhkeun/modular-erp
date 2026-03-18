import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ["journal-entries"],
    queryFn: async () => (await api.get("/api/v1/account/journal-entries?size=100")).data,
  });

  const fmt = (v: number) => v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

  const columnDefs = useMemo<ColDef<JeRow>[]>(() => [
    { field: "documentNo", headerName: t("je.docNo"), flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "postingDate", headerName: t("je.postingDate"), flex: 1 },
    { field: "entryType", headerName: t("je.entryType"), flex: 1 },
    { field: "referenceDocNo", headerName: t("je.refDoc"), flex: 1 },
    { field: "description", headerName: t("je.description_"), flex: 2 },
    { field: "totalDebit", headerName: t("je.debit"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "totalCredit", headerName: t("je.credit"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "isBalanced", headerName: t("je.balanced"), flex: 0.7,
      cellRenderer: (p: { value: boolean }) => p.value ? <span className="badge-success">{t("common.yes")}</span> : <span className="badge-danger">{t("common.no")}</span> },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  return (
    <div>
      <PageHeader title={t("je.title")} description={t("je.description")}
        breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.journalEntries") }]}
        actions={<button className="btn-primary"><Plus size={16} /> {t("je.newJe")}</button>} />
      <div className="card overflow-hidden">
        <DataGrid<JeRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
