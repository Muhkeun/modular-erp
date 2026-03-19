import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Play } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface AssetRow {
  id: number;
  assetNo: string;
  name: string;
  companyCode: string;
  category: string;
  acquisitionDate: string;
  acquisitionCost: number;
  bookValue: number;
  accumulatedDepreciation: number;
  usefulLife: number;
  depreciationMethod: string;
  status: string;
  location: string | null;
}

interface DepreciationEntry {
  id: number;
  period: string;
  depreciationAmount: number;
  accumulatedAmount: number;
  bookValue: number;
}

type Mode = "list" | "create" | "detail";

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  ACTIVE: "badge-success",
  DISPOSED: "badge bg-slate-100 text-slate-500",
  FULLY_DEPRECIATED: "badge-warning",
};

const DEPRECIATION_METHODS = ["STRAIGHT_LINE", "DECLINING_BALANCE", "UNITS_OF_PRODUCTION"] as const;

const fmt = (v: number) =>
  v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

export default function AssetPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [mode, setMode] = useState<Mode>("list");
  const [selected, setSelected] = useState<AssetRow | null>(null);
  const [depRunDialog, setDepRunDialog] = useState(false);
  const [depRunForm, setDepRunForm] = useState({ period: "", companyCode: "" });
  const [disposeDialog, setDisposeDialog] = useState(false);
  const [disposeForm, setDisposeForm] = useState({ disposalDate: "", disposalAmount: 0, reason: "" });
  const [form, setForm] = useState({
    assetNo: "", name: "", companyCode: "", category: "", acquisitionDate: "",
    acquisitionCost: 0, usefulLife: 5, depreciationMethod: "STRAIGHT_LINE" as string,
    location: "", description: "",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["assets"],
    queryFn: async () => (await api.get("/api/v1/asset/assets?size=100")).data,
  });

  const detailQuery = useQuery({
    queryKey: ["asset", selected?.id],
    queryFn: async () => (await api.get(`/api/v1/asset/assets/${selected!.id}`)).data,
    enabled: mode === "detail" && !!selected?.id,
  });

  const scheduleQuery = useQuery({
    queryKey: ["asset-schedule", selected?.id],
    queryFn: async () => (await api.get(`/api/v1/asset/assets/${selected!.id}/depreciation-schedule`)).data,
    enabled: mode === "detail" && !!selected?.id,
  });

  const createMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/asset/assets", body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["assets"] }); setMode("list"); },
  });

  const activateMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/asset/assets/${id}/activate`)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["assets"] });
      qc.invalidateQueries({ queryKey: ["asset", selected?.id] });
    },
  });

  const disposeMut = useMutation({
    mutationFn: async ({ id, body }: { id: number; body: Record<string, unknown> }) =>
      (await api.post(`/api/v1/asset/assets/${id}/dispose`, body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["assets"] });
      qc.invalidateQueries({ queryKey: ["asset", selected?.id] });
      setDisposeDialog(false);
    },
  });

  const depRunMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/asset/depreciation/run", body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["assets"] });
      setDepRunDialog(false);
    },
  });

  const columnDefs = useMemo<ColDef<AssetRow>[]>(() => [
    { field: "assetNo", headerName: t("asset.assetNo"), flex: 1,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "name", headerName: t("asset.name"), flex: 1.5 },
    { field: "category", headerName: t("asset.category"), flex: 1 },
    { field: "acquisitionDate", headerName: t("asset.acquisitionDate"), flex: 1 },
    { field: "acquisitionCost", headerName: t("asset.acquisitionCost"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "bookValue", headerName: t("asset.bookValue"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "depreciationMethod", headerName: t("asset.method"), flex: 1 },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  const scheduleColDefs = useMemo<ColDef<DepreciationEntry>[]>(() => [
    { field: "period", headerName: t("asset.period"), flex: 1 },
    { field: "depreciationAmount", headerName: t("asset.depAmount"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "accumulatedAmount", headerName: t("asset.accDepreciation"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "bookValue", headerName: t("asset.bookValue"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
  ], [t]);

  const onRowClicked = useCallback((data: AssetRow) => {
    setSelected(data); setMode("detail");
  }, []);

  const openCreate = () => {
    setForm({ assetNo: "", name: "", companyCode: "", category: "", acquisitionDate: "",
      acquisitionCost: 0, usefulLife: 5, depreciationMethod: "STRAIGHT_LINE", location: "", description: "" });
    setMode("create");
  };

  const detail: AssetRow | undefined = detailQuery.data?.data ?? selected;
  const schedule: DepreciationEntry[] = scheduleQuery.data?.data || [];

  if (mode === "list") {
    return (
      <div>
        <PageHeader title={t("asset.title")} description={t("asset.description")}
          breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.assets") }]}
          actions={
            <div className="flex gap-2">
              <button className="btn-secondary" onClick={() => { setDepRunForm({ period: "", companyCode: "" }); setDepRunDialog(true); }}>
                <Play size={16} /> {t("asset.runDepreciation")}
              </button>
              <button className="btn-primary" onClick={openCreate}><Plus size={16} /> {t("asset.newAsset")}</button>
            </div>
          } />
        <div className="card overflow-hidden">
          <DataGrid<AssetRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} onRowClicked={onRowClicked} />
        </div>

        {depRunDialog && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 px-4 backdrop-blur-sm">
            <div className="w-full max-w-md rounded-[28px] border border-white/70 bg-white/95 p-6 shadow-[0_32px_80px_rgba(15,23,42,0.28)]">
              <p className="section-kicker">Depreciation</p>
              <h3 className="section-title">{t("asset.runDepreciation")}</h3>
              <div className="mt-4 space-y-4">
                <div>
                  <label className="field-label">{t("asset.period")}</label>
                  <input className="input" placeholder="2024-01" value={depRunForm.period} onChange={e => setDepRunForm(p => ({ ...p, period: e.target.value }))} />
                </div>
                <div>
                  <label className="field-label">{t("nav.companies")}</label>
                  <input className="input" value={depRunForm.companyCode} onChange={e => setDepRunForm(p => ({ ...p, companyCode: e.target.value }))} />
                </div>
              </div>
              <div className="mt-6 flex justify-end gap-3">
                <button className="btn-ghost" onClick={() => setDepRunDialog(false)}>{t("common.cancel")}</button>
                <button className="btn-primary" onClick={() => depRunMut.mutate(depRunForm)} disabled={depRunMut.isPending}>
                  {depRunMut.isPending ? t("common.saving") : t("asset.execute")}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  if (mode === "create") {
    return (
      <div>
        <PageHeader title={t("asset.newAsset")}
          breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.assets") }, { label: t("common.create") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />
        <div className="workspace-hero">
          <p className="section-kicker">Asset Workspace</p>
          <h3 className="section-title">{t("asset.newAsset")}</h3>
        </div>
        <div className="section-card">
          <p className="section-kicker">Asset Info</p>
          <h3 className="section-title">{t("common.basicInfo")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            <div>
              <label className="field-label">{t("asset.assetNo")}</label>
              <input className="input" value={form.assetNo} onChange={e => setForm(p => ({ ...p, assetNo: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("asset.name")}</label>
              <input className="input" value={form.name} onChange={e => setForm(p => ({ ...p, name: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("nav.companies")}</label>
              <input className="input" value={form.companyCode} onChange={e => setForm(p => ({ ...p, companyCode: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("asset.category")}</label>
              <input className="input" value={form.category} onChange={e => setForm(p => ({ ...p, category: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("asset.acquisitionDate")}</label>
              <input className="input" type="date" value={form.acquisitionDate} onChange={e => setForm(p => ({ ...p, acquisitionDate: e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("asset.acquisitionCost")}</label>
              <input className="input" type="number" value={form.acquisitionCost} onChange={e => setForm(p => ({ ...p, acquisitionCost: +e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("asset.usefulLife")}</label>
              <input className="input" type="number" value={form.usefulLife} onChange={e => setForm(p => ({ ...p, usefulLife: +e.target.value }))} />
            </div>
            <div>
              <label className="field-label">{t("asset.method")}</label>
              <select className="input" value={form.depreciationMethod} onChange={e => setForm(p => ({ ...p, depreciationMethod: e.target.value }))}>
                {DEPRECIATION_METHODS.map(m => <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <div>
              <label className="field-label">{t("asset.location")}</label>
              <input className="input" value={form.location} onChange={e => setForm(p => ({ ...p, location: e.target.value }))} />
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
      <PageHeader title={detail?.assetNo ? `${detail.assetNo} - ${detail.name}` : ""}
        breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.assets") }, { label: t("common.detail") }]}
        actions={
          <div className="flex gap-2">
            <button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>
            {detail?.status === "DRAFT" && (
              <button className="btn-primary" onClick={() => activateMut.mutate(detail.id)} disabled={activateMut.isPending}>
                {t("asset.activate")}
              </button>
            )}
            {detail?.status === "ACTIVE" && (
              <button className="btn-danger" onClick={() => { setDisposeForm({ disposalDate: "", disposalAmount: 0, reason: "" }); setDisposeDialog(true); }}>
                {t("asset.dispose")}
              </button>
            )}
          </div>
        } />

      {detailQuery.isLoading ? (
        <div className="section-card text-center text-slate-400">{t("common.loading")}</div>
      ) : detail ? (
        <>
          <div className="section-card">
            <p className="section-kicker">Asset Overview</p>
            <h3 className="section-title">{t("common.basicInfo")}</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("common.status")}</span>
                <span className={statusStyle[detail.status] || "badge"}>{t("status." + detail.status, detail.status)}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("asset.category")}</span>
                <span className="font-medium text-slate-900">{detail.category}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("asset.acquisitionCost")}</span>
                <span className="font-medium text-slate-900">{fmt(detail.acquisitionCost)}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("asset.bookValue")}</span>
                <span className="font-medium text-slate-900">{fmt(detail.bookValue)}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("asset.accDepreciation")}</span>
                <span className="font-medium text-slate-900">{fmt(detail.accumulatedDepreciation)}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("asset.method")}</span>
                <span className="font-medium text-slate-900">{detail.depreciationMethod}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("asset.usefulLife")}</span>
                <span className="font-medium text-slate-900">{detail.usefulLife} {t("asset.years")}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("asset.location")}</span>
                <span className="font-medium text-slate-900">{detail.location || "-"}</span>
              </div>
            </div>
          </div>

          <div className="section-card mt-6">
            <p className="section-kicker">Depreciation Schedule</p>
            <h3 className="section-title">{t("asset.depSchedule")}</h3>
            <div className="mt-4">
              <DataGrid<DepreciationEntry> rowData={schedule} columnDefs={scheduleColDefs} loading={scheduleQuery.isLoading} height="400px" />
            </div>
          </div>
        </>
      ) : (
        <div className="section-card text-center text-slate-400">{t("common.noData")}</div>
      )}

      {disposeDialog && detail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 px-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-[28px] border border-white/70 bg-white/95 p-6 shadow-[0_32px_80px_rgba(15,23,42,0.28)]">
            <p className="section-kicker">Asset Disposal</p>
            <h3 className="section-title">{t("asset.dispose")}</h3>
            <div className="mt-4 space-y-4">
              <div>
                <label className="field-label">{t("asset.disposalDate")}</label>
                <input className="input" type="date" value={disposeForm.disposalDate} onChange={e => setDisposeForm(p => ({ ...p, disposalDate: e.target.value }))} />
              </div>
              <div>
                <label className="field-label">{t("asset.disposalAmount")}</label>
                <input className="input" type="number" value={disposeForm.disposalAmount} onChange={e => setDisposeForm(p => ({ ...p, disposalAmount: +e.target.value }))} />
              </div>
              <div>
                <label className="field-label">{t("budget.reason")}</label>
                <input className="input" value={disposeForm.reason} onChange={e => setDisposeForm(p => ({ ...p, reason: e.target.value }))} />
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button className="btn-ghost" onClick={() => setDisposeDialog(false)}>{t("common.cancel")}</button>
              <button className="btn-danger" onClick={() => disposeMut.mutate({ id: detail.id, body: disposeForm })} disabled={disposeMut.isPending}>
                {disposeMut.isPending ? t("common.saving") : t("asset.dispose")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
