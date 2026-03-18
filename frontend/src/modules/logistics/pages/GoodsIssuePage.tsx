import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface GiRow {
  id: number; documentNo: string; issueType: string; referenceDocNo: string | null;
  plantCode: string; storageLocation: string; issueDate: string; status: string;
}

export default function GoodsIssuePage() {
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ["goods-issues"],
    queryFn: async () => (await api.get("/api/v1/logistics/goods-issues?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<GiRow>[]>(() => [
    { field: "documentNo", headerName: t("gi.docNo"), flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "issueType", headerName: t("gi.issueType"), flex: 0.8, valueFormatter: (p: { value: string }) => t("gi.types." + p.value, p.value) },
    { field: "referenceDocNo", headerName: t("gi.refDoc"), flex: 1 },
    { field: "plantCode", headerName: t("gr.plant"), flex: 0.7 },
    { field: "storageLocation", headerName: t("gr.storage"), flex: 0.8 },
    { field: "issueDate", headerName: t("gi.issueDate"), flex: 1 },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => {
        const s: Record<string, string> = { DRAFT: "badge bg-slate-100 text-slate-600", CONFIRMED: "badge-success", CANCELLED: "badge-danger" };
        return <span className={s[p.value] || "badge"}>{t("status." + p.value, p.value)}</span>;
      }},
  ], [t]);

  return (
    <div>
      <PageHeader title={t("gi.title")} description={t("gi.description")}
        breadcrumbs={[{ label: t("nav.logistics") }, { label: t("nav.goodsIssue") }]}
        actions={<button className="btn-primary"><Plus size={16} /> {t("gi.newGi")}</button>} />
      <div className="card overflow-hidden">
        <DataGrid<GiRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
