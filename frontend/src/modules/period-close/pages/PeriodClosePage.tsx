import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Lock, CheckCircle2, Play } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface FiscalPeriod {
  id: number;
  name: string;
  companyCode: string;
  fiscalYear: number;
  periodNo: number;
  startDate: string;
  endDate: string;
  status: string;
}

interface ChecklistTask {
  id: number;
  taskName: string;
  taskType: string;
  status: string;
  executedBy: string | null;
  executedAt: string | null;
  required: boolean;
  sortOrder: number;
}

interface ClosingEntry {
  id: number;
  documentNo: string;
  accountCode: string;
  accountName: string;
  debitAmount: number;
  creditAmount: number;
  description: string;
}

type Mode = "list" | "create" | "detail";

const statusStyle: Record<string, string> = {
  OPEN: "badge-success",
  SOFT_CLOSED: "badge-warning",
  HARD_CLOSED: "badge bg-slate-100 text-slate-500",
  DRAFT: "badge bg-slate-100 text-slate-600",
};

const taskStatusStyle: Record<string, string> = {
  PENDING: "badge bg-slate-100 text-slate-600",
  COMPLETED: "badge-success",
  FAILED: "badge-danger",
  SKIPPED: "badge-warning",
};

const fmt = (v: number) =>
  v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

export default function PeriodClosePage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [mode, setMode] = useState<Mode>("list");
  const [selected, setSelected] = useState<FiscalPeriod | null>(null);
  const [activeTab, setActiveTab] = useState<"checklist" | "entries">("checklist");
  const [form, setForm] = useState({
    name: "", companyCode: "", fiscalYear: new Date().getFullYear(),
    periodNo: 1, startDate: "", endDate: "",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["fiscal-periods"],
    queryFn: async () => (await api.get("/api/v1/period-close/periods?size=100")).data,
  });

  const checklistQuery = useQuery({
    queryKey: ["period-checklist", selected?.id],
    queryFn: async () => (await api.get(`/api/v1/period-close/periods/${selected!.id}/checklist`)).data,
    enabled: mode === "detail" && !!selected?.id && activeTab === "checklist",
  });

  const entriesQuery = useQuery({
    queryKey: ["closing-entries", selected?.id],
    queryFn: async () => (await api.get(`/api/v1/period-close/periods/${selected!.id}/closing-entries`)).data,
    enabled: mode === "detail" && !!selected?.id && activeTab === "entries",
  });

  const createMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/period-close/periods", body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["fiscal-periods"] }); setMode("list"); },
  });

  const executeTaskMut = useMutation({
    mutationFn: async ({ periodId, taskId }: { periodId: number; taskId: number }) =>
      (await api.post(`/api/v1/period-close/periods/${periodId}/checklist/${taskId}/execute`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["period-checklist", selected?.id] }); },
  });

  const softCloseMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/period-close/periods/${id}/soft-close`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["fiscal-periods"] }); },
  });

  const hardCloseMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/period-close/periods/${id}/hard-close`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["fiscal-periods"] }); },
  });

  const columnDefs = useMemo<ColDef<FiscalPeriod>[]>(() => [
    { field: "name", headerName: t("periodClose.name"), flex: 1.5,
      cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span> },
    { field: "companyCode", headerName: t("nav.companies"), flex: 0.8 },
    { field: "fiscalYear", headerName: t("periodClose.fiscalYear"), flex: 0.7 },
    { field: "periodNo", headerName: t("periodClose.periodNo"), flex: 0.6 },
    { field: "startDate", headerName: t("periodClose.startDate"), flex: 1 },
    { field: "endDate", headerName: t("periodClose.endDate"), flex: 1 },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  const entryColDefs = useMemo<ColDef<ClosingEntry>[]>(() => [
    { field: "documentNo", headerName: t("periodClose.docNo"), flex: 1 },
    { field: "accountCode", headerName: t("periodClose.accountCode"), flex: 1 },
    { field: "accountName", headerName: t("periodClose.accountName"), flex: 1.5 },
    { field: "debitAmount", headerName: t("je.debit"), flex: 1, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "creditAmount", headerName: t("je.credit"), flex: 1, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "description", headerName: t("common.description"), flex: 2 },
  ], [t]);

  const onRowClicked = useCallback((data: FiscalPeriod) => {
    setSelected(data); setActiveTab("checklist"); setMode("detail");
  }, []);

  const checklist: ChecklistTask[] = checklistQuery.data?.data || [];
  const entries: ClosingEntry[] = entriesQuery.data?.data || [];

  if (mode === "list") {
    return (
      <div>
        <PageHeader title={t("periodClose.title")} description={t("periodClose.description")}
          breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.periodClose") }]}
          actions={<button className="btn-primary" onClick={() => { setForm({ name: "", companyCode: "", fiscalYear: new Date().getFullYear(), periodNo: 1, startDate: "", endDate: "" }); setMode("create"); }}>
            <Plus size={16} /> {t("periodClose.newPeriod")}
          </button>} />
        <div className="card overflow-hidden">
          <DataGrid<FiscalPeriod> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} onRowClicked={onRowClicked} />
        </div>
      </div>
    );
  }

  if (mode === "create") {
    return (
      <div>
        <PageHeader title={t("periodClose.newPeriod")}
          breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.periodClose") }, { label: t("common.create") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />
        <div className="section-card">
          <p className="section-kicker">Fiscal Period</p>
          <h3 className="section-title">{t("common.basicInfo")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            <div>
              <label className="field-label">{t("periodClose.name")}</label>
              <input className="input" value={form.name} onChange={e => setForm(p => ({ ...p, name: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("nav.companies")}</label>
              <input className="input" value={form.companyCode} onChange={e => setForm(p => ({ ...p, companyCode: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("periodClose.fiscalYear")}</label>
              <input className="input" type="number" value={form.fiscalYear} onChange={e => setForm(p => ({ ...p, fiscalYear: +e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("periodClose.periodNo")}</label>
              <input className="input" type="number" value={form.periodNo} onChange={e => setForm(p => ({ ...p, periodNo: +e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("periodClose.startDate")}</label>
              <input className="input" type="date" value={form.startDate} onChange={e => setForm(p => ({ ...p, startDate: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("periodClose.endDate")}</label>
              <input className="input" type="date" value={form.endDate} onChange={e => setForm(p => ({ ...p, endDate: e.target.value }))} />
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

  // detail
  return (
    <div>
      <PageHeader title={selected?.name || ""}
        breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.periodClose") }, { label: t("common.detail") }]}
        actions={
          <div className="flex gap-2">
            <button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>
            {selected?.status === "OPEN" && (
              <button className="btn-warning" onClick={() => softCloseMut.mutate(selected.id)} disabled={softCloseMut.isPending}>
                <Lock size={16} /> {t("periodClose.softClose")}
              </button>
            )}
            {selected?.status === "SOFT_CLOSED" && (
              <button className="btn-danger" onClick={() => hardCloseMut.mutate(selected.id)} disabled={hardCloseMut.isPending}>
                <Lock size={16} /> {t("periodClose.hardClose")}
              </button>
            )}
          </div>
        } />

      <div className="section-card">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("common.status")}</span>
            <span className={statusStyle[selected?.status || ""] || "badge"}>{t("status." + selected?.status, { defaultValue: selected?.status ?? "" })}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("periodClose.fiscalYear")}</span>
            <span className="font-medium text-slate-900">{selected?.fiscalYear}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("periodClose.startDate")}</span>
            <span className="font-medium text-slate-900">{selected?.startDate}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("periodClose.endDate")}</span>
            <span className="font-medium text-slate-900">{selected?.endDate}</span>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mt-6 mb-4">
        <button className={activeTab === "checklist" ? "btn-primary" : "btn-ghost"} onClick={() => setActiveTab("checklist")}>
          <CheckCircle2 size={16} /> {t("periodClose.checklist")}
        </button>
        <button className={activeTab === "entries" ? "btn-primary" : "btn-ghost"} onClick={() => setActiveTab("entries")}>
          {t("periodClose.closingEntries")}
        </button>
      </div>

      {activeTab === "checklist" && (
        <div className="section-card">
          <p className="section-kicker">Closing Checklist</p>
          <h3 className="section-title">{t("periodClose.checklist")}</h3>
          <div className="mt-4 space-y-3">
            {checklistQuery.isLoading ? (
              <div className="text-center text-slate-400">{t("common.loading")}</div>
            ) : checklist.length === 0 ? (
              <div className="text-center text-slate-400">{t("common.noData")}</div>
            ) : (
              checklist.map(task => (
                <div key={task.id} className="flex items-center justify-between rounded-[22px] border border-slate-200/80 bg-slate-50/70 p-4">
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-medium text-slate-400">#{task.sortOrder}</span>
                    <div>
                      <div className="text-sm font-medium text-slate-900">{task.taskName}</div>
                      <div className="text-xs text-slate-400">{task.taskType} {task.required && "- Required"}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={taskStatusStyle[task.status] || "badge"}>{task.status}</span>
                    {task.status === "PENDING" && selected && (
                      <button className="btn-ghost text-sm" onClick={() => executeTaskMut.mutate({ periodId: selected.id, taskId: task.id })}
                        disabled={executeTaskMut.isPending}>
                        <Play size={14} /> {t("periodClose.executeTask")}
                      </button>
                    )}
                    {task.executedAt && (
                      <span className="text-xs text-slate-400">{task.executedBy} @ {task.executedAt}</span>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {activeTab === "entries" && (
        <div className="section-card">
          <p className="section-kicker">Closing Entries</p>
          <h3 className="section-title">{t("periodClose.closingEntries")}</h3>
          <div className="mt-4">
            <DataGrid<ClosingEntry> rowData={entries} columnDefs={entryColDefs} loading={entriesQuery.isLoading} height="400px" />
          </div>
        </div>
      )}
    </div>
  );
}
