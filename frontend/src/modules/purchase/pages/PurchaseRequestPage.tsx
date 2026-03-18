import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus } from "lucide-react";
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
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ["purchase-requests"],
    queryFn: async () => (await api.get("/api/v1/purchase/requests?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<PrRow>[]>(() => [
    { field: "documentNo", headerName: t("pr.docNo"), flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "prType", headerName: t("common.type"), flex: 0.8 },
    { field: "companyCode", headerName: t("nav.companies"), flex: 0.8 },
    { field: "plantCode", headerName: t("gr.plant"), flex: 0.7 },
    { field: "requestDate", headerName: t("pr.requestDate"), flex: 1 },
    { field: "deliveryDate", headerName: t("pr.deliveryDate"), flex: 1 },
    { field: "requestedBy", headerName: t("pr.requestedBy"), flex: 1 },
    { field: "totalAmount", headerName: t("pr.totalAmount"), flex: 1, type: "numericColumn",
      valueFormatter: (p: { value: number }) => p.value?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 }) },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  return (
    <div>
      <PageHeader title={t("pr.title")} description={t("pr.description")}
        breadcrumbs={[{ label: t("nav.procurement") }, { label: t("nav.purchaseRequests") }]}
        actions={<button className="btn-primary"><Plus size={16} /> {t("pr.newPr")}</button>} />
      <div className="card overflow-hidden">
        <DataGrid<PrRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
