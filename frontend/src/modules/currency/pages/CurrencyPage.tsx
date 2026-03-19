import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, ArrowRightLeft, RefreshCw } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface CurrencyRow { id: number; code: string; name: string; symbol: string; decimalPlaces: number; isBaseCurrency: boolean; isActive: boolean; }
interface RateRow { id: number; fromCurrency: string; toCurrency: string; rate: number; effectiveDate: string; source: string; }
interface RevaluationRow { id: number; revaluationNo: string; period: string; companyCode: string; status: string; totalGainLoss: number; createdAt: string; }

type Tab = "currencies" | "rates" | "convert" | "revaluation";
type Mode = "list" | "create";

const fmt = (v: number) => v?.toLocaleString("ko-KR", { maximumFractionDigits: 4 });
const fmtCurrency = (v: number) => v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

export default function CurrencyPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [tab, setTab] = useState<Tab>("currencies");
  const [mode, setMode] = useState<Mode>("list");
  const [convertResult, setConvertResult] = useState<Record<string, unknown> | null>(null);

  const [currForm, setCurrForm] = useState({ code: "", name: "", symbol: "", decimalPlaces: 2 });
  const [rateForm, setRateForm] = useState({ fromCurrency: "", toCurrency: "", rate: 0, effectiveDate: "" });
  const [convertForm, setConvertForm] = useState({ fromCurrency: "USD", toCurrency: "KRW", amount: 0 });
  const [revalForm, setRevalForm] = useState({ period: "", companyCode: "" });

  const currQ = useQuery({ queryKey: ["currencies"], queryFn: async () => (await api.get("/api/v1/currency/currencies?size=100")).data, enabled: tab === "currencies" });
  const ratesQ = useQuery({ queryKey: ["exchange-rates"], queryFn: async () => (await api.get("/api/v1/currency/exchange-rates?size=100")).data, enabled: tab === "rates" });
  const revalQ = useQuery({ queryKey: ["revaluations"], queryFn: async () => (await api.get("/api/v1/currency/revaluations?size=100")).data, enabled: tab === "revaluation" });

  const createCurrMut = useMutation({ mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/currency/currencies", b)).data, onSuccess: () => { qc.invalidateQueries({ queryKey: ["currencies"] }); setMode("list"); } });
  const createRateMut = useMutation({ mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/currency/exchange-rates", b)).data, onSuccess: () => { qc.invalidateQueries({ queryKey: ["exchange-rates"] }); setMode("list"); } });
  const convertMut = useMutation({
    mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/currency/convert", b)).data,
    onSuccess: (data) => { setConvertResult(data?.data || data); },
  });
  const revalMut = useMutation({
    mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/currency/revaluations", b)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["revaluations"] }); },
  });

  const currCols = useMemo<ColDef<CurrencyRow>[]>(() => [
    { field: "code", headerName: t("currency.code"), flex: 0.8, cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "name", headerName: t("currency.name"), flex: 1.5 },
    { field: "symbol", headerName: t("currency.symbol"), flex: 0.5 },
    { field: "decimalPlaces", headerName: t("currency.decimals"), flex: 0.6 },
    { field: "isBaseCurrency", headerName: t("currency.baseCurrency"), flex: 0.8,
      cellRenderer: (p: { value: boolean }) => p.value ? <span className="badge-success">{t("common.yes")}</span> : <span className="text-slate-400">-</span> },
    { field: "isActive", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: boolean }) => p.value ? <span className="badge-success">{t("common.active")}</span> : <span className="badge bg-slate-100 text-slate-500">{t("common.inactive")}</span> },
  ], [t]);

  const rateCols = useMemo<ColDef<RateRow>[]>(() => [
    { field: "fromCurrency", headerName: t("currency.from"), flex: 0.8 },
    { field: "toCurrency", headerName: t("currency.to"), flex: 0.8 },
    { field: "rate", headerName: t("currency.rate"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "effectiveDate", headerName: t("currency.effectiveDate"), flex: 1 },
    { field: "source", headerName: t("currency.source"), flex: 1 },
  ], [t]);

  const revalCols = useMemo<ColDef<RevaluationRow>[]>(() => [
    { field: "revaluationNo", headerName: t("currency.revalNo"), flex: 1, cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "period", headerName: t("currency.period"), flex: 0.8 },
    { field: "companyCode", headerName: t("nav.companies"), flex: 0.8 },
    { field: "totalGainLoss", headerName: t("currency.gainLoss"), flex: 1.2, type: "numericColumn",
      cellRenderer: (p: { value: number }) => <span className={p.value >= 0 ? "text-emerald-600" : "text-red-600 font-semibold"}>{fmtCurrency(p.value)}</span> },
    { field: "status", headerName: t("common.status"), flex: 0.8, cellRenderer: (p: { value: string }) => <span className="badge-success">{p.value}</span> },
    { field: "createdAt", headerName: t("common.datetime"), flex: 1.2 },
  ], [t]);

  const handleCreate = () => {
    if (tab === "currencies") createCurrMut.mutate(currForm);
    else if (tab === "rates") createRateMut.mutate(rateForm);
  };
  const isPending = createCurrMut.isPending || createRateMut.isPending;

  if (mode === "create") {
    return (
      <div>
        <PageHeader title={t("common.create")}
          breadcrumbs={[{ label: t("nav.currency") }, { label: t("common.create") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />
        <div className="section-card">
          <p className="section-kicker">Currency</p>
          <h3 className="section-title">{t("common.basicInfo")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            {tab === "currencies" && <>
              <div><label className="field-label">{t("currency.code")}</label><input className="input" value={currForm.code} onChange={e => setCurrForm(p => ({ ...p, code: e.target.value }))} placeholder="USD" /></div>
              <div><label className="field-label">{t("currency.name")}</label><input className="input" value={currForm.name} onChange={e => setCurrForm(p => ({ ...p, name: e.target.value }))} /></div>
              <div><label className="field-label">{t("currency.symbol")}</label><input className="input" value={currForm.symbol} onChange={e => setCurrForm(p => ({ ...p, symbol: e.target.value }))} placeholder="$" /></div>
              <div><label className="field-label">{t("currency.decimals")}</label><input className="input" type="number" value={currForm.decimalPlaces} onChange={e => setCurrForm(p => ({ ...p, decimalPlaces: +e.target.value }))} /></div>
            </>}
            {tab === "rates" && <>
              <div><label className="field-label">{t("currency.from")}</label><input className="input" value={rateForm.fromCurrency} onChange={e => setRateForm(p => ({ ...p, fromCurrency: e.target.value }))} placeholder="USD" /></div>
              <div><label className="field-label">{t("currency.to")}</label><input className="input" value={rateForm.toCurrency} onChange={e => setRateForm(p => ({ ...p, toCurrency: e.target.value }))} placeholder="KRW" /></div>
              <div><label className="field-label">{t("currency.rate")}</label><input className="input" type="number" step="0.0001" value={rateForm.rate} onChange={e => setRateForm(p => ({ ...p, rate: +e.target.value }))} /></div>
              <div><label className="field-label">{t("currency.effectiveDate")}</label><input className="input" type="date" value={rateForm.effectiveDate} onChange={e => setRateForm(p => ({ ...p, effectiveDate: e.target.value }))} /></div>
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
      <PageHeader title={t("currency.title")} description={t("currency.description")}
        breadcrumbs={[{ label: t("nav.currency") }]}
        actions={
          <div className="flex gap-2">
            {(tab === "currencies" || tab === "rates") && (
              <button className="btn-primary" onClick={() => setMode("create")}><Plus size={16} /> {t("common.new")}</button>
            )}
            {tab === "revaluation" && (
              <button className="btn-primary" onClick={() => revalMut.mutate(revalForm)} disabled={revalMut.isPending}>
                <RefreshCw size={16} /> {t("currency.runRevaluation")}
              </button>
            )}
          </div>
        } />

      <div className="flex gap-2 mb-4">
        <button className={tab === "currencies" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("currencies")}>{t("currency.currencies")}</button>
        <button className={tab === "rates" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("rates")}>{t("currency.exchangeRates")}</button>
        <button className={tab === "convert" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("convert")}><ArrowRightLeft size={16} /> {t("currency.convert")}</button>
        <button className={tab === "revaluation" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("revaluation")}>{t("currency.revaluation")}</button>
      </div>

      {tab === "currencies" && (
        <div className="card overflow-hidden">
          <DataGrid<CurrencyRow> rowData={currQ.data?.data || []} columnDefs={currCols} loading={currQ.isLoading} />
        </div>
      )}

      {tab === "rates" && (
        <div className="card overflow-hidden">
          <DataGrid<RateRow> rowData={ratesQ.data?.data || []} columnDefs={rateCols} loading={ratesQ.isLoading} />
        </div>
      )}

      {tab === "convert" && (
        <div className="section-card max-w-lg">
          <p className="section-kicker">Currency Conversion</p>
          <h3 className="section-title">{t("currency.convert")}</h3>
          <div className="grid grid-cols-1 gap-4 mt-4">
            <div className="grid grid-cols-2 gap-4">
              <div><label className="field-label">{t("currency.from")}</label><input className="input" value={convertForm.fromCurrency} onChange={e => setConvertForm(p => ({ ...p, fromCurrency: e.target.value }))} /></div>
              <div><label className="field-label">{t("currency.to")}</label><input className="input" value={convertForm.toCurrency} onChange={e => setConvertForm(p => ({ ...p, toCurrency: e.target.value }))} /></div>
            </div>
            <div><label className="field-label">{t("currency.amount")}</label><input className="input" type="number" value={convertForm.amount} onChange={e => setConvertForm(p => ({ ...p, amount: +e.target.value }))} /></div>
            <button className="btn-primary" onClick={() => convertMut.mutate(convertForm)} disabled={convertMut.isPending}>
              {convertMut.isPending ? t("common.loading") : t("currency.convert")}
            </button>
            {convertResult && (
              <div className="rounded-[22px] bg-slate-50 p-4">
                <div className="text-xs uppercase tracking-wider text-slate-400">Result</div>
                <div className="mt-2 text-2xl font-bold text-slate-900">
                  {convertResult.convertedAmount !== undefined
                    ? `${convertForm.toCurrency} ${Number(convertResult.convertedAmount).toLocaleString("ko-KR", { maximumFractionDigits: 2 })}`
                    : JSON.stringify(convertResult)}
                </div>
                {convertResult.rate !== undefined && (
                  <div className="mt-1 text-sm text-slate-500">Rate: {fmt(Number(convertResult.rate))}</div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {tab === "revaluation" && (
        <div>
          <div className="section-card mb-4">
            <p className="section-kicker">Revaluation Parameters</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
              <div><label className="field-label">{t("currency.period")}</label><input className="input" placeholder="2024-01" value={revalForm.period} onChange={e => setRevalForm(p => ({ ...p, period: e.target.value }))} /></div>
              <div><label className="field-label">{t("nav.companies")}</label><input className="input" value={revalForm.companyCode} onChange={e => setRevalForm(p => ({ ...p, companyCode: e.target.value }))} /></div>
            </div>
          </div>
          <div className="card overflow-hidden">
            <DataGrid<RevaluationRow> rowData={revalQ.data?.data || []} columnDefs={revalCols} loading={revalQ.isLoading} />
          </div>
        </div>
      )}
    </div>
  );
}
