import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Play, Power, PowerOff } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface BatchJob {
  id: number;
  jobName: string;
  jobType: string;
  cronExpression: string;
  enabled: boolean;
  status: string;
  lastRunAt: string | null;
  nextRunAt: string | null;
  description: string | null;
}

interface ExecutionHistory {
  id: number;
  startTime: string;
  endTime: string | null;
  status: string;
  recordsProcessed: number;
  errorMessage: string | null;
  duration: number;
}

type Mode = "list" | "create" | "detail";

const statusStyle: Record<string, string> = {
  IDLE: "badge bg-slate-100 text-slate-600",
  RUNNING: "badge-info",
  SUCCESS: "badge-success",
  FAILED: "badge-danger",
  DISABLED: "badge bg-slate-100 text-slate-500",
};

export default function BatchPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [mode, setMode] = useState<Mode>("list");
  const [selected, setSelected] = useState<BatchJob | null>(null);
  const [form, setForm] = useState({
    jobName: "", jobType: "", cronExpression: "", description: "",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["batch-jobs"],
    queryFn: async () => (await api.get("/api/v1/batch/jobs?size=100")).data,
  });

  const historyQuery = useQuery({
    queryKey: ["batch-history", selected?.id],
    queryFn: async () => (await api.get(`/api/v1/batch/jobs/${selected!.id}/history?size=50`)).data,
    enabled: mode === "detail" && !!selected?.id,
  });

  const createMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/batch/jobs", body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["batch-jobs"] }); setMode("list"); },
  });

  const executeMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/batch/jobs/${id}/execute`)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["batch-jobs"] });
      qc.invalidateQueries({ queryKey: ["batch-history", selected?.id] });
    },
  });

  const enableMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/batch/jobs/${id}/enable`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["batch-jobs"] }); },
  });

  const disableMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/batch/jobs/${id}/disable`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["batch-jobs"] }); },
  });

  const columnDefs = useMemo<ColDef<BatchJob>[]>(() => [
    { field: "jobName", headerName: t("batch.jobName"), flex: 1.5,
      cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span> },
    { field: "jobType", headerName: t("batch.jobType"), flex: 1 },
    { field: "cronExpression", headerName: t("batch.cron"), flex: 1 },
    { field: "enabled", headerName: t("batch.enabled"), flex: 0.6,
      cellRenderer: (p: { value: boolean }) => p.value
        ? <span className="badge-success">{t("common.active")}</span>
        : <span className="badge bg-slate-100 text-slate-500">{t("common.inactive")}</span> },
    { field: "lastRunAt", headerName: t("batch.lastRun"), flex: 1.2 },
    { field: "nextRunAt", headerName: t("batch.nextRun"), flex: 1.2 },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], [t]);

  const historyColDefs = useMemo<ColDef<ExecutionHistory>[]>(() => [
    { field: "startTime", headerName: t("batch.startTime"), flex: 1.2 },
    { field: "endTime", headerName: t("batch.endTime"), flex: 1.2 },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
    { field: "recordsProcessed", headerName: t("batch.records"), flex: 0.8, type: "numericColumn" },
    { field: "duration", headerName: t("batch.duration"), flex: 0.8,
      valueFormatter: (p: { value: number }) => p.value ? `${(p.value / 1000).toFixed(1)}s` : "-" },
    { field: "errorMessage", headerName: t("batch.error"), flex: 2 },
  ], [t]);

  const onRowClicked = useCallback((data: BatchJob) => {
    setSelected(data); setMode("detail");
  }, []);

  const history: ExecutionHistory[] = historyQuery.data?.data || [];

  if (mode === "list") {
    return (
      <div>
        <PageHeader title={t("batch.title")} description={t("batch.description")}
          breadcrumbs={[{ label: t("nav.admin") }, { label: t("nav.batch") }]}
          actions={<button className="btn-primary" onClick={() => { setForm({ jobName: "", jobType: "", cronExpression: "", description: "" }); setMode("create"); }}>
            <Plus size={16} /> {t("batch.newJob")}
          </button>} />
        <div className="card overflow-hidden">
          <DataGrid<BatchJob> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} onRowClicked={onRowClicked} />
        </div>
      </div>
    );
  }

  if (mode === "create") {
    return (
      <div>
        <PageHeader title={t("batch.newJob")}
          breadcrumbs={[{ label: t("nav.admin") }, { label: t("nav.batch") }, { label: t("common.create") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />
        <div className="section-card">
          <p className="section-kicker">Batch Job</p>
          <h3 className="section-title">{t("common.basicInfo")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
            <div>
              <label className="field-label">{t("batch.jobName")}</label>
              <input className="input" value={form.jobName} onChange={e => setForm(p => ({ ...p, jobName: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("batch.jobType")}</label>
              <input className="input" value={form.jobType} onChange={e => setForm(p => ({ ...p, jobType: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("batch.cron")}</label>
              <input className="input" placeholder="0 0 * * *" value={form.cronExpression} onChange={e => setForm(p => ({ ...p, cronExpression: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("common.description")}</label>
              <input className="input" value={form.description} onChange={e => setForm(p => ({ ...p, description: e.target.value }))} />
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
      <PageHeader title={selected?.jobName || ""}
        breadcrumbs={[{ label: t("nav.admin") }, { label: t("nav.batch") }, { label: t("common.detail") }]}
        actions={
          <div className="flex gap-2">
            <button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>
            <button className="btn-primary" onClick={() => executeMut.mutate(selected!.id)} disabled={executeMut.isPending}>
              <Play size={16} /> {t("batch.execute")}
            </button>
            {selected?.enabled ? (
              <button className="btn-danger" onClick={() => disableMut.mutate(selected.id)} disabled={disableMut.isPending}>
                <PowerOff size={16} /> {t("batch.disable")}
              </button>
            ) : (
              <button className="btn-secondary" onClick={() => enableMut.mutate(selected!.id)} disabled={enableMut.isPending}>
                <Power size={16} /> {t("batch.enable")}
              </button>
            )}
          </div>
        } />

      <div className="section-card">
        <p className="section-kicker">Job Info</p>
        <h3 className="section-title">{t("common.basicInfo")}</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("common.status")}</span>
            <span className={statusStyle[selected?.status || ""] || "badge"}>{selected?.status}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("batch.enabled")}</span>
            <span className="font-medium text-slate-900">{selected?.enabled ? t("common.yes") : t("common.no")}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("batch.cron")}</span>
            <span className="font-mono font-medium text-slate-900">{selected?.cronExpression}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("batch.lastRun")}</span>
            <span className="font-medium text-slate-900">{selected?.lastRunAt || "-"}</span>
          </div>
        </div>
      </div>

      <div className="section-card mt-6">
        <p className="section-kicker">Execution History</p>
        <h3 className="section-title">{t("batch.history")}</h3>
        <div className="mt-4">
          <DataGrid<ExecutionHistory> rowData={history} columnDefs={historyColDefs} loading={historyQuery.isLoading} height="400px" />
        </div>
      </div>
    </div>
  );
}
