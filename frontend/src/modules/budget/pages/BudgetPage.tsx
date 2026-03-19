import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, ArrowRightLeft } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface BudgetPeriod {
  id: number;
  name: string;
  companyCode: string;
  fiscalYear: number;
  startDate: string;
  endDate: string;
  status: string;
  totalBudget: number;
  totalActual: number;
  description: string | null;
}

interface BudgetItem {
  id: number;
  accountCode: string;
  accountName: string;
  costCenter: string;
  budgetAmount: number;
  actualAmount: number;
  variance: number;
  utilizationRate: number;
}

type Mode = "list" | "create" | "detail" | "transfer";

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  APPROVED: "badge-success",
  CLOSED: "badge bg-slate-100 text-slate-500",
  REJECTED: "badge-danger",
};

const fmt = (v: number) =>
  v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

export default function BudgetPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [mode, setMode] = useState<Mode>("list");
  const [selected, setSelected] = useState<BudgetPeriod | null>(null);
  const [form, setForm] = useState({
    name: "", companyCode: "", fiscalYear: new Date().getFullYear(),
    startDate: "", endDate: "", description: "",
  });
  const [transferForm, setTransferForm] = useState({
    fromAccountCode: "", toAccountCode: "", amount: 0, reason: "",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["budget-periods"],
    queryFn: async () => (await api.get("/api/v1/budget/periods?size=100")).data,
  });

  const detailQuery = useQuery({
    queryKey: ["budget-period", selected?.id],
    queryFn: async () => (await api.get(`/api/v1/budget/periods/${selected!.id}`)).data,
    enabled: mode === "detail" && !!selected?.id,
  });

  const itemsQuery = useQuery({
    queryKey: ["budget-items", selected?.id],
    queryFn: async () => (await api.get(`/api/v1/budget/periods/${selected!.id}/items?size=200`)).data,
    enabled: mode === "detail" && !!selected?.id,
  });

  const createMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/budget/periods", body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["budget-periods"] }); setMode("list"); },
  });

  const approveMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/budget/periods/${id}/approve`)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["budget-periods"] });
      qc.invalidateQueries({ queryKey: ["budget-period", selected?.id] });
    },
  });

  const closeMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/budget/periods/${id}/close`)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["budget-periods"] });
      qc.invalidateQueries({ queryKey: ["budget-period", selected?.id] });
    },
  });

  const transferMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/budget/transfers", body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["budget-items", selected?.id] });
      setMode("detail");
    },
  });

  const columnDefs = useMemo<ColDef<BudgetPeriod>[]>(() => [
    { field: "name", headerName: t("budget.name"), flex: 1.5,
      cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span> },
    { field: "companyCode", headerName: t("nav.companies"), flex: 0.8 },
    { field: "fiscalYear", headerName: t("budget.fiscalYear"), flex: 0.7 },
    { field: "startDate", headerName: t("budget.startDate"), flex: 1 },
    { field: "endDate", headerName: t("budget.endDate"), flex: 1 },
    { field: "totalBudget", headerName: t("budget.totalBudget"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "totalActual", headerName: t("budget.totalActual"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  const itemColumnDefs = useMemo<ColDef<BudgetItem>[]>(() => [
    { field: "accountCode", headerName: t("budget.accountCode"), flex: 1 },
    { field: "accountName", headerName: t("budget.accountName"), flex: 1.5 },
    { field: "costCenter", headerName: t("budget.costCenter"), flex: 1 },
    { field: "budgetAmount", headerName: t("budget.budgetAmount"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "actualAmount", headerName: t("budget.actualAmount"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "variance", headerName: t("budget.variance"), flex: 1, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value),
      cellRenderer: (p: { value: number }) => (
        <span className={p.value >= 0 ? "text-emerald-600" : "text-red-600 font-semibold"}>{fmt(p.value)}</span>
      ) },
    { field: "utilizationRate", headerName: t("budget.utilization"), flex: 0.8,
      valueFormatter: (p: { value: number }) => `${(p.value * 100).toFixed(1)}%` },
  ], [t]);

  const onRowClicked = useCallback((data: BudgetPeriod) => {
    setSelected(data); setMode("detail");
  }, []);

  const openCreate = () => {
    setForm({ name: "", companyCode: "", fiscalYear: new Date().getFullYear(), startDate: "", endDate: "", description: "" });
    setMode("create");
  };

  const detail: BudgetPeriod | undefined = detailQuery.data?.data ?? selected;
  const items: BudgetItem[] = itemsQuery.data?.data || [];

  if (mode === "list") {
    return (
      <div>
        <PageHeader title={t("budget.title")} description={t("budget.description")}
          breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.budget") }]}
          actions={<button className="btn-primary" onClick={openCreate}><Plus size={16} /> {t("budget.newBudget")}</button>} />
        <div className="card overflow-hidden">
          <DataGrid<BudgetPeriod> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} onRowClicked={onRowClicked} />
        </div>
      </div>
    );
  }

  if (mode === "create") {
    return (
      <div>
        <PageHeader title={t("budget.newBudget")}
          breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.budget") }, { label: t("common.create") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />
        <div className="workspace-hero">
          <p className="section-kicker">Finance Workspace</p>
          <h3 className="section-title">{t("budget.newBudget")}</h3>
        </div>
        <div className="section-card">
          <p className="section-kicker">Budget Period</p>
          <h3 className="section-title">{t("common.basicInfo")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            <div>
              <label className="field-label">{t("budget.name")}</label>
              <input className="input" value={form.name} onChange={e => setForm(p => ({ ...p, name: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("nav.companies")}</label>
              <input className="input" value={form.companyCode} onChange={e => setForm(p => ({ ...p, companyCode: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("budget.fiscalYear")}</label>
              <input className="input" type="number" value={form.fiscalYear} onChange={e => setForm(p => ({ ...p, fiscalYear: +e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("budget.startDate")}</label>
              <input className="input" type="date" value={form.startDate} onChange={e => setForm(p => ({ ...p, startDate: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("budget.endDate")}</label>
              <input className="input" type="date" value={form.endDate} onChange={e => setForm(p => ({ ...p, endDate: e.target.value }))} />
            </div>
            <div className="md:col-span-3">
              <label className="field-label">{t("common.description")}</label>
              <textarea className="input" rows={2} value={form.description} onChange={e => setForm(p => ({ ...p, description: e.target.value }))} />
            </div>
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button className="btn-ghost" onClick={() => setMode("list")}>{t("common.cancel")}</button>
          <button className="btn-primary" onClick={() => createMut.mutate(form)} disabled={createMut.isPending}>
            {createMut.isPending ? t("common.saving") : t("common.save")}
          </button>
        </div>
      </div>
    );
  }

  if (mode === "transfer") {
    return (
      <div>
        <PageHeader title={t("budget.transfer")}
          breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.budget") }, { label: t("budget.transfer") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("detail")}><ArrowLeft size={16} /> {t("common.back")}</button>} />
        <div className="section-card">
          <p className="section-kicker">Budget Transfer</p>
          <h3 className="section-title">{t("budget.transfer")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
            <div>
              <label className="field-label">{t("budget.fromAccount")}</label>
              <input className="input" value={transferForm.fromAccountCode} onChange={e => setTransferForm(p => ({ ...p, fromAccountCode: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("budget.toAccount")}</label>
              <input className="input" value={transferForm.toAccountCode} onChange={e => setTransferForm(p => ({ ...p, toAccountCode: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("budget.amount")}</label>
              <input className="input" type="number" value={transferForm.amount} onChange={e => setTransferForm(p => ({ ...p, amount: +e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("budget.reason")}</label>
              <input className="input" value={transferForm.reason} onChange={e => setTransferForm(p => ({ ...p, reason: e.target.value }))} />
            </div>
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button className="btn-ghost" onClick={() => setMode("detail")}>{t("common.cancel")}</button>
          <button className="btn-primary" onClick={() => transferMut.mutate({ ...transferForm, budgetPeriodId: selected?.id })} disabled={transferMut.isPending}>
            {transferMut.isPending ? t("common.saving") : t("budget.executeTransfer")}
          </button>
        </div>
      </div>
    );
  }

  // detail
  return (
    <div>
      <PageHeader title={detail?.name || ""}
        breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.budget") }, { label: t("common.detail") }]}
        actions={
          <div className="flex gap-2">
            <button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>
            {detail?.status === "APPROVED" && (
              <button className="btn-secondary" onClick={() => { setTransferForm({ fromAccountCode: "", toAccountCode: "", amount: 0, reason: "" }); setMode("transfer"); }}>
                <ArrowRightLeft size={16} /> {t("budget.transfer")}
              </button>
            )}
            {detail?.status === "DRAFT" && (
              <button className="btn-primary" onClick={() => approveMut.mutate(detail.id)} disabled={approveMut.isPending}>
                {t("common.approve")}
              </button>
            )}
            {detail?.status === "APPROVED" && (
              <button className="btn-danger" onClick={() => closeMut.mutate(detail.id)} disabled={closeMut.isPending}>
                {t("budget.closeBudget")}
              </button>
            )}
          </div>
        } />

      {detailQuery.isLoading ? (
        <div className="section-card text-center text-slate-400">{t("common.loading")}</div>
      ) : detail ? (
        <>
          <div className="section-card">
            <p className="section-kicker">Budget Overview</p>
            <h3 className="section-title">{t("common.basicInfo")}</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("common.status")}</span>
                <span className={statusStyle[detail.status] || "badge"}>{t("status." + detail.status, detail.status)}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("budget.fiscalYear")}</span>
                <span className="font-medium text-slate-900">{detail.fiscalYear}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("budget.totalBudget")}</span>
                <span className="font-medium text-slate-900">{fmt(detail.totalBudget)}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("budget.totalActual")}</span>
                <span className="font-medium text-slate-900">{fmt(detail.totalActual)}</span>
              </div>
            </div>
          </div>

          <div className="section-card mt-6">
            <p className="section-kicker">Budget vs Actual</p>
            <h3 className="section-title">{t("budget.budgetVsActual")}</h3>
            <div className="mt-4">
              <DataGrid<BudgetItem> rowData={items} columnDefs={itemColumnDefs} loading={itemsQuery.isLoading} height="400px" />
            </div>
          </div>
        </>
      ) : (
        <div className="section-card text-center text-slate-400">{t("common.noData")}</div>
      )}
    </div>
  );
}
