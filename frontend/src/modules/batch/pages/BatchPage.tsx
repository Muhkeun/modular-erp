import { useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Play, Power, PowerOff } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import {
  batchApi,
  type BatchExecution,
  type BatchJob,
  type BatchJobType,
  type CreateBatchJobRequest,
} from "../../../shared/api/batchApi";

type Mode = "list" | "create" | "detail";

const statusStyle: Record<string, string> = {
  QUEUED: "badge bg-slate-100 text-slate-600",
  RUNNING: "badge-info",
  COMPLETED: "badge-success",
  FAILED: "badge-danger",
  CANCELLED: "badge bg-slate-100 text-slate-500",
};

const jobTypes: BatchJobType[] = [
  "GL_POSTING",
  "DEPRECIATION",
  "MRP_RUN",
  "STOCK_REVALUATION",
  "EXCHANGE_RATE_UPDATE",
  "DATA_IMPORT",
  "DATA_EXPORT",
  "REPORT_GENERATION",
  "EMAIL_SENDING",
  "CLEANUP",
];

const scopeLabel = (job: Pick<BatchJob, "companyCode" | "plantCode" | "departmentCode">) =>
  [job.companyCode, job.plantCode, job.departmentCode].filter(Boolean).join(" / ") || "TENANT";

export default function BatchPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [mode, setMode] = useState<Mode>("list");
  const [selected, setSelected] = useState<BatchJob | null>(null);
  const [form, setForm] = useState<CreateBatchJobRequest>({
    jobCode: "",
    jobName: "",
    jobType: "GL_POSTING",
    companyCode: "",
    departmentCode: "",
    plantCode: "",
    cronExpression: "",
    description: "",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["batch-jobs"],
    queryFn: () => batchApi.getJobs(),
  });

  const historyQuery = useQuery({
    queryKey: ["batch-history", selected?.id],
    queryFn: () => batchApi.getHistory(selected!.id),
    enabled: mode === "detail" && !!selected?.id,
  });

  const createMut = useMutation({
    mutationFn: (body: CreateBatchJobRequest) =>
      batchApi.createJob({
        ...body,
        companyCode: body.companyCode || null,
        departmentCode: body.departmentCode || null,
        plantCode: body.plantCode || null,
        cronExpression: body.cronExpression || null,
        description: body.description || null,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["batch-jobs"] });
      setMode("list");
    },
  });

  const executeMut = useMutation({
    mutationFn: (id: number) => batchApi.executeJob(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["batch-jobs"] });
      queryClient.invalidateQueries({ queryKey: ["batch-history", selected?.id] });
    },
  });

  const enableMut = useMutation({
    mutationFn: (id: number) => batchApi.enableJob(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["batch-jobs"] });
    },
  });

  const disableMut = useMutation({
    mutationFn: (id: number) => batchApi.disableJob(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["batch-jobs"] });
    },
  });

  const columnDefs = useMemo<ColDef<BatchJob>[]>(() => [
    {
      field: "jobCode",
      headerName: t("batch.jobCode", "Job Code"),
      flex: 1,
      cellRenderer: (p: { value: string }) => <span className="font-mono text-brand-700">{p.value}</span>,
    },
    {
      field: "jobName",
      headerName: t("batch.jobName"),
      flex: 1.4,
      cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span>,
    },
    { field: "jobType", headerName: t("batch.jobType"), flex: 1 },
    {
      headerName: t("common.scope", "Scope"),
      flex: 1.2,
      valueGetter: (p) => scopeLabel(p.data as BatchJob),
    },
    { field: "cronExpression", headerName: t("batch.cron"), flex: 1 },
    {
      field: "enabled",
      headerName: t("batch.enabled"),
      flex: 0.7,
      cellRenderer: (p: { value: boolean }) => p.value
        ? <span className="badge-success">{t("common.active")}</span>
        : <span className="badge bg-slate-100 text-slate-500">{t("common.inactive")}</span>,
    },
    { field: "lastRunAt", headerName: t("batch.lastRun"), flex: 1.2 },
    { field: "nextRunAt", headerName: t("batch.nextRun"), flex: 1.2 },
  ], [t]);

  const historyColDefs = useMemo<ColDef<BatchExecution>[]>(() => [
    { field: "executionNo", headerName: t("batch.executionNo", "Execution No"), flex: 1 },
    { field: "startedAt", headerName: t("batch.startTime"), flex: 1.2 },
    { field: "completedAt", headerName: t("batch.endTime"), flex: 1.2 },
    {
      field: "status",
      headerName: t("common.status"),
      flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span>,
    },
    { field: "processedRecords", headerName: t("batch.records"), flex: 0.8, type: "numericColumn" },
    { field: "errorMessage", headerName: t("batch.error"), flex: 2 },
  ], [t]);

  const history: BatchExecution[] = historyQuery.data || [];

  if (mode === "list") {
    return (
      <div>
        <PageHeader
          title={t("batch.title")}
          description={t("batch.description")}
          breadcrumbs={[{ label: t("nav.admin") }, { label: t("nav.batch") }]}
          actions={
            <button
              className="btn-primary"
              onClick={() => {
                setForm({
                  jobCode: "",
                  jobName: "",
                  jobType: "GL_POSTING",
                  companyCode: "",
                  departmentCode: "",
                  plantCode: "",
                  cronExpression: "",
                  description: "",
                });
                setMode("create");
              }}
            >
              <Plus size={16} /> {t("batch.newJob")}
            </button>
          }
        />
        <div className="card overflow-hidden">
          <DataGrid<BatchJob>
            rowData={data || []}
            columnDefs={columnDefs}
            loading={isLoading}
            onRowClicked={(row) => {
              setSelected(row);
              setMode("detail");
            }}
          />
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
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mt-4">
            <div>
              <label className="field-label">{t("batch.jobCode", "Job Code")}</label>
              <input className="input" value={form.jobCode} onChange={e => setForm(p => ({ ...p, jobCode: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("batch.jobName")}</label>
              <input className="input" value={form.jobName} onChange={e => setForm(p => ({ ...p, jobName: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("batch.jobType")}</label>
              <select className="input" value={form.jobType} onChange={e => setForm(p => ({ ...p, jobType: e.target.value as BatchJobType }))}>
                {jobTypes.map((jobType) => <option key={jobType} value={jobType}>{jobType}</option>)}
              </select>
            </div>
            <div>
              <label className="field-label">{t("common.company", "Company")}</label>
              <input className="input" value={form.companyCode ?? ""} onChange={e => setForm(p => ({ ...p, companyCode: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("common.plant", "Plant")}</label>
              <input className="input" value={form.plantCode ?? ""} onChange={e => setForm(p => ({ ...p, plantCode: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("common.department", "Department")}</label>
              <input className="input" value={form.departmentCode ?? ""} onChange={e => setForm(p => ({ ...p, departmentCode: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("batch.cron")}</label>
              <input className="input" placeholder="0 0 2 * * ?" value={form.cronExpression ?? ""} onChange={e => setForm(p => ({ ...p, cronExpression: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("common.description")}</label>
              <input className="input" value={form.description ?? ""} onChange={e => setForm(p => ({ ...p, description: e.target.value }))} />
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
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("batch.jobCode", "Job Code")}</span>
            <span className="font-mono font-medium text-slate-900">{selected?.jobCode}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("batch.enabled")}</span>
            <span className="font-medium text-slate-900">{selected?.enabled ? t("common.yes") : t("common.no")}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("common.scope", "Scope")}</span>
            <span className="font-medium text-slate-900">{selected ? scopeLabel(selected) : "-"}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("batch.cron")}</span>
            <span className="font-mono font-medium text-slate-900">{selected?.cronExpression}</span>
          </div>
          <div className="stat-tile md:col-span-2">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("batch.lastRun")}</span>
            <span className="font-medium text-slate-900">{selected?.lastRunAt || "-"}</span>
          </div>
        </div>
      </div>

      <div className="section-card mt-6">
        <p className="section-kicker">Execution History</p>
        <h3 className="section-title">{t("batch.history")}</h3>
        <div className="mt-4">
          <DataGrid<BatchExecution> rowData={history} columnDefs={historyColDefs} loading={historyQuery.isLoading} height="400px" />
        </div>
      </div>
    </div>
  );
}
