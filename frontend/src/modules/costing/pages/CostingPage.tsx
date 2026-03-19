import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Calculator } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface CostCenter { id: number; code: string; name: string; companyCode: string; parentCode: string | null; managerName: string | null; status: string; }
interface StandardCost { id: number; itemCode: string; itemName: string; materialCost: number; laborCost: number; overheadCost: number; totalCost: number; effectiveDate: string; status: string; }
interface Allocation { id: number; allocationNo: string; fromCostCenter: string; toCostCenter: string; amount: number; allocationBase: string; period: string; status: string; }
interface VarianceRow { id: number; itemCode: string; itemName: string; standardCost: number; actualCost: number; variance: number; variancePercent: number; period: string; }

type Tab = "centers" | "standard" | "allocations" | "variance";
type Mode = "list" | "create";

const fmt = (v: number) => v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

export default function CostingPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [tab, setTab] = useState<Tab>("centers");
  const [mode, setMode] = useState<Mode>("list");
  const [calcDialog, setCalcDialog] = useState(false);
  const [calcForm, setCalcForm] = useState({ itemCode: "", effectiveDate: "" });
  const [calcResult, setCalcResult] = useState<Record<string, unknown> | null>(null);

  const [centerForm, setCenterForm] = useState({ code: "", name: "", companyCode: "", parentCode: "" });
  const [stdForm, setStdForm] = useState({ itemCode: "", itemName: "", materialCost: 0, laborCost: 0, overheadCost: 0, effectiveDate: "" });
  const [allocForm, setAllocForm] = useState({ fromCostCenter: "", toCostCenter: "", amount: 0, allocationBase: "", period: "" });

  const centersQ = useQuery({ queryKey: ["cost-centers"], queryFn: async () => (await api.get("/api/v1/costing/cost-centers?size=100")).data, enabled: tab === "centers" });
  const stdQ = useQuery({ queryKey: ["standard-costs"], queryFn: async () => (await api.get("/api/v1/costing/standard-costs?size=100")).data, enabled: tab === "standard" });
  const allocQ = useQuery({ queryKey: ["cost-allocations"], queryFn: async () => (await api.get("/api/v1/costing/allocations?size=100")).data, enabled: tab === "allocations" });
  const varQ = useQuery({ queryKey: ["variance-analysis"], queryFn: async () => (await api.get("/api/v1/costing/variance-analysis")).data, enabled: tab === "variance" });

  const createCenterMut = useMutation({ mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/costing/cost-centers", b)).data, onSuccess: () => { qc.invalidateQueries({ queryKey: ["cost-centers"] }); setMode("list"); } });
  const createStdMut = useMutation({ mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/costing/standard-costs", b)).data, onSuccess: () => { qc.invalidateQueries({ queryKey: ["standard-costs"] }); setMode("list"); } });
  const createAllocMut = useMutation({ mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/costing/allocations", b)).data, onSuccess: () => { qc.invalidateQueries({ queryKey: ["cost-allocations"] }); setMode("list"); } });
  const calcMut = useMutation({
    mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/costing/product-cost/calculate", b)).data,
    onSuccess: (data) => { setCalcResult(data?.data || data); },
  });

  const centerCols = useMemo<ColDef<CostCenter>[]>(() => [
    { field: "code", headerName: t("common.code"), flex: 1, cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "name", headerName: t("common.name"), flex: 1.5 },
    { field: "companyCode", headerName: t("nav.companies"), flex: 0.8 },
    { field: "parentCode", headerName: t("costing.parent"), flex: 1 },
    { field: "managerName", headerName: t("costing.manager"), flex: 1 },
    { field: "status", headerName: t("common.status"), flex: 0.8, cellRenderer: (p: { value: string }) => <span className="badge-success">{p.value}</span> },
  ], [t]);

  const stdCols = useMemo<ColDef<StandardCost>[]>(() => [
    { field: "itemCode", headerName: t("item.code"), flex: 1, cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "itemName", headerName: t("item.name"), flex: 1.5 },
    { field: "materialCost", headerName: t("costing.materialCost"), flex: 1, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "laborCost", headerName: t("costing.laborCost"), flex: 1, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "overheadCost", headerName: t("costing.overheadCost"), flex: 1, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "totalCost", headerName: t("costing.totalCost"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "effectiveDate", headerName: t("costing.effectiveDate"), flex: 1 },
  ], [t]);

  const allocCols = useMemo<ColDef<Allocation>[]>(() => [
    { field: "allocationNo", headerName: t("costing.allocNo"), flex: 1, cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "fromCostCenter", headerName: t("costing.from"), flex: 1 },
    { field: "toCostCenter", headerName: t("costing.to"), flex: 1 },
    { field: "amount", headerName: t("costing.amount"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "allocationBase", headerName: t("costing.base"), flex: 1 },
    { field: "period", headerName: t("costing.period"), flex: 0.8 },
    { field: "status", headerName: t("common.status"), flex: 0.8, cellRenderer: (p: { value: string }) => <span className="badge-success">{p.value}</span> },
  ], [t]);

  const varCols = useMemo<ColDef<VarianceRow>[]>(() => [
    { field: "itemCode", headerName: t("item.code"), flex: 1 },
    { field: "itemName", headerName: t("item.name"), flex: 1.5 },
    { field: "standardCost", headerName: t("costing.standardCost"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "actualCost", headerName: t("costing.actualCost"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "variance", headerName: t("budget.variance"), flex: 1, type: "numericColumn",
      cellRenderer: (p: { value: number }) => <span className={p.value >= 0 ? "text-emerald-600" : "text-red-600 font-semibold"}>{fmt(p.value)}</span> },
    { field: "variancePercent", headerName: t("costing.variancePct"), flex: 0.8, valueFormatter: (p: { value: number }) => `${p.value?.toFixed(1)}%` },
    { field: "period", headerName: t("costing.period"), flex: 0.8 },
  ], [t]);

  const handleCreate = () => {
    if (tab === "centers") createCenterMut.mutate(centerForm);
    else if (tab === "standard") createStdMut.mutate(stdForm);
    else if (tab === "allocations") createAllocMut.mutate(allocForm);
  };
  const isPending = createCenterMut.isPending || createStdMut.isPending || createAllocMut.isPending;

  if (mode === "create") {
    return (
      <div>
        <PageHeader title={t("common.create")}
          breadcrumbs={[{ label: t("nav.costing") }, { label: t("common.create") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />
        <div className="section-card">
          <p className="section-kicker">Costing</p>
          <h3 className="section-title">{t("common.basicInfo")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            {tab === "centers" && <>
              <div><label className="field-label">{t("common.code")}</label><input className="input" value={centerForm.code} onChange={e => setCenterForm(p => ({ ...p, code: e.target.value }))} /></div>
              <div><label className="field-label">{t("common.name")}</label><input className="input" value={centerForm.name} onChange={e => setCenterForm(p => ({ ...p, name: e.target.value }))} /></div>
              <div><label className="field-label">{t("nav.companies")}</label><input className="input" value={centerForm.companyCode} onChange={e => setCenterForm(p => ({ ...p, companyCode: e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.parent")}</label><input className="input" value={centerForm.parentCode} onChange={e => setCenterForm(p => ({ ...p, parentCode: e.target.value }))} /></div>
            </>}
            {tab === "standard" && <>
              <div><label className="field-label">{t("item.code")}</label><input className="input" value={stdForm.itemCode} onChange={e => setStdForm(p => ({ ...p, itemCode: e.target.value }))} /></div>
              <div><label className="field-label">{t("item.name")}</label><input className="input" value={stdForm.itemName} onChange={e => setStdForm(p => ({ ...p, itemName: e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.materialCost")}</label><input className="input" type="number" value={stdForm.materialCost} onChange={e => setStdForm(p => ({ ...p, materialCost: +e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.laborCost")}</label><input className="input" type="number" value={stdForm.laborCost} onChange={e => setStdForm(p => ({ ...p, laborCost: +e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.overheadCost")}</label><input className="input" type="number" value={stdForm.overheadCost} onChange={e => setStdForm(p => ({ ...p, overheadCost: +e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.effectiveDate")}</label><input className="input" type="date" value={stdForm.effectiveDate} onChange={e => setStdForm(p => ({ ...p, effectiveDate: e.target.value }))} /></div>
            </>}
            {tab === "allocations" && <>
              <div><label className="field-label">{t("costing.from")}</label><input className="input" value={allocForm.fromCostCenter} onChange={e => setAllocForm(p => ({ ...p, fromCostCenter: e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.to")}</label><input className="input" value={allocForm.toCostCenter} onChange={e => setAllocForm(p => ({ ...p, toCostCenter: e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.amount")}</label><input className="input" type="number" value={allocForm.amount} onChange={e => setAllocForm(p => ({ ...p, amount: +e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.base")}</label><input className="input" value={allocForm.allocationBase} onChange={e => setAllocForm(p => ({ ...p, allocationBase: e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.period")}</label><input className="input" value={allocForm.period} onChange={e => setAllocForm(p => ({ ...p, period: e.target.value }))} /></div>
            </>}
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button className="btn-ghost" onClick={() => setMode("list")}>{t("common.cancel")}</button>
          <button className="btn-primary" onClick={handleCreate} disabled={isPending}>
            {isPending ? t("common.saving") : t("common.save")}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <PageHeader title={t("costing.title")} description={t("costing.description")}
        breadcrumbs={[{ label: t("nav.costing") }]}
        actions={
          <div className="flex gap-2">
            <button className="btn-secondary" onClick={() => { setCalcForm({ itemCode: "", effectiveDate: "" }); setCalcResult(null); setCalcDialog(true); }}>
              <Calculator size={16} /> {t("costing.calculate")}
            </button>
            {tab !== "variance" && <button className="btn-primary" onClick={() => setMode("create")}><Plus size={16} /> {t("common.new")}</button>}
          </div>
        } />

      <div className="flex gap-2 mb-4">
        <button className={tab === "centers" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("centers")}>{t("costing.costCenters")}</button>
        <button className={tab === "standard" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("standard")}>{t("costing.standardCosts")}</button>
        <button className={tab === "allocations" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("allocations")}>{t("costing.allocations")}</button>
        <button className={tab === "variance" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("variance")}>{t("costing.varianceAnalysis")}</button>
      </div>

      <div className="card overflow-hidden">
        {tab === "centers" && <DataGrid<CostCenter> rowData={centersQ.data?.data || []} columnDefs={centerCols} loading={centersQ.isLoading} />}
        {tab === "standard" && <DataGrid<StandardCost> rowData={stdQ.data?.data || []} columnDefs={stdCols} loading={stdQ.isLoading} />}
        {tab === "allocations" && <DataGrid<Allocation> rowData={allocQ.data?.data || []} columnDefs={allocCols} loading={allocQ.isLoading} />}
        {tab === "variance" && <DataGrid<VarianceRow> rowData={varQ.data?.data || []} columnDefs={varCols} loading={varQ.isLoading} />}
      </div>

      {calcDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 px-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-[28px] border border-white/70 bg-white/95 p-6 shadow-[0_32px_80px_rgba(15,23,42,0.28)]">
            <p className="section-kicker">Product Cost</p>
            <h3 className="section-title">{t("costing.calculate")}</h3>
            <div className="mt-4 space-y-4">
              <div><label className="field-label">{t("item.code")}</label><input className="input" value={calcForm.itemCode} onChange={e => setCalcForm(p => ({ ...p, itemCode: e.target.value }))} /></div>
              <div><label className="field-label">{t("costing.effectiveDate")}</label><input className="input" type="date" value={calcForm.effectiveDate} onChange={e => setCalcForm(p => ({ ...p, effectiveDate: e.target.value }))} /></div>
            </div>
            {calcResult && (
              <div className="mt-4 rounded-[22px] bg-slate-50 p-4 text-sm">
                <pre className="text-slate-600 whitespace-pre-wrap">{JSON.stringify(calcResult, null, 2)}</pre>
              </div>
            )}
            <div className="mt-6 flex justify-end gap-3">
              <button className="btn-ghost" onClick={() => setCalcDialog(false)}>{t("common.cancel")}</button>
              <button className="btn-primary" onClick={() => calcMut.mutate(calcForm)} disabled={calcMut.isPending}>
                {calcMut.isPending ? t("common.loading") : t("costing.calculate")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
