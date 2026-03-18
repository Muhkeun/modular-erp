import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ["goods-receipts"],
    queryFn: async () => (await api.get("/api/v1/logistics/goods-receipts?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<GrRow>[]>(() => [
    { field: "documentNo", headerName: t("gr.docNo"), flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "poDocumentNo", headerName: t("gr.poRef"), flex: 1, cellRenderer: (p: { value: string | null }) => p.value ? <span className="font-mono text-slate-600">{p.value}</span> : <span className="text-slate-300">-</span> },
    { field: "vendorName", headerName: t("gr.vendor"), flex: 2 },
    { field: "plantCode", headerName: t("gr.plant"), flex: 0.7 },
    { field: "storageLocation", headerName: t("gr.storage"), flex: 0.8 },
    { field: "receiptDate", headerName: t("gr.receiptDate"), flex: 1 },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  return (
    <div>
      <PageHeader title={t("gr.title")} description={t("gr.description")}
        breadcrumbs={[{ label: t("nav.logistics") }, { label: t("nav.goodsReceipt") }]}
        actions={<button className="btn-primary"><Plus size={16} /> {t("gr.newGr")}</button>} />
      <div className="card overflow-hidden">
        <DataGrid<GrRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
