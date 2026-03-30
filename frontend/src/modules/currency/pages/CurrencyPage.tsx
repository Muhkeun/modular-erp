import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { ArrowLeft, ArrowRightLeft, Plus, RefreshCw } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import { useToast } from "../../../shared/components/useToast";
import {
  currencyApi,
  type ConvertRequest,
  type ConvertResponse,
  type CreateCurrencyRequest,
  type CreateExchangeRateRequest,
  type CreateRevaluationRequest,
  type Currency,
  type ExchangeRate,
  type Revaluation,
} from "../../../shared/api/currencyApi";

type Tab = "currencies" | "rates" | "convert" | "revaluation";
type Mode = "list" | "create";

const today = new Date().toISOString().slice(0, 10);
const currentYear = new Date().getFullYear();
const currentPeriod = new Date().getMonth() + 1;

const formatNumber = (value: number | string | null | undefined) =>
  Number(value ?? 0).toLocaleString("ko-KR", { maximumFractionDigits: 4 });

const formatCurrency = (value: number | string | null | undefined, currency = "KRW") =>
  Number(value ?? 0).toLocaleString("ko-KR", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  });

export default function CurrencyPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();

  const [tab, setTab] = useState<Tab>("currencies");
  const [mode, setMode] = useState<Mode>("list");
  const [convertResult, setConvertResult] = useState<ConvertResponse | null>(null);

  const [currencyForm, setCurrencyForm] = useState<CreateCurrencyRequest>({
    currencyCode: "",
    currencyName: "",
    symbol: "",
    decimalPlaces: 2,
    isBaseCurrency: false,
    status: "ACTIVE",
  });
  const [rateForm, setRateForm] = useState<CreateExchangeRateRequest>({
    fromCurrency: "",
    toCurrency: "",
    rateDate: today,
    exchangeRate: 0,
    rateType: "SPOT",
    source: "",
  });
  const [convertForm, setConvertForm] = useState<ConvertRequest>({
    fromCurrency: "USD",
    toCurrency: "KRW",
    amount: 0,
    date: today,
  });
  const [revaluationForm, setRevaluationForm] = useState<CreateRevaluationRequest>({
    companyCode: "",
    revaluationDate: today,
    fiscalYear: currentYear,
    period: currentPeriod,
    fromCurrency: "USD",
    toCurrency: "KRW",
    originalRate: 0,
    revaluationRate: 0,
    unrealizedGainLoss: 0,
  });

  const currenciesQ = useQuery({
    queryKey: ["currencies"],
    queryFn: () => currencyApi.getCurrencies({ size: 100 }),
    enabled: tab === "currencies",
  });

  const ratesQ = useQuery({
    queryKey: ["exchange-rates"],
    queryFn: () => currencyApi.getRates({ size: 100 }),
    enabled: tab === "rates",
  });

  const revaluationsQ = useQuery({
    queryKey: ["revaluations"],
    queryFn: () => currencyApi.getRevaluations({ size: 100 }),
    enabled: tab === "revaluation",
  });

  const createCurrencyMutation = useMutation({
    mutationFn: (payload: CreateCurrencyRequest) => currencyApi.createCurrency(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["currencies"] });
      setMode("list");
      toast.success(t("common.save", "Saved"));
    },
    onError: () => toast.error("Currency creation failed"),
  });

  const createRateMutation = useMutation({
    mutationFn: (payload: CreateExchangeRateRequest) => currencyApi.createRate(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["exchange-rates"] });
      setMode("list");
      toast.success(t("common.save", "Saved"));
    },
    onError: () => toast.error("Exchange rate creation failed"),
  });

  const convertMutation = useMutation({
    mutationFn: (payload: ConvertRequest) => currencyApi.convert(payload),
    onSuccess: (data) => setConvertResult(data),
    onError: () => toast.error("Currency conversion failed"),
  });

  const revaluationMutation = useMutation({
    mutationFn: (payload: CreateRevaluationRequest) => currencyApi.runRevaluation(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["revaluations"] });
      toast.success(t("currency.runRevaluation", "Run Revaluation"));
    },
    onError: () => toast.error("Revaluation creation failed"),
  });

  const currencyColumns = useMemo<ColDef<Currency>[]>(
    () => [
      {
        field: "currencyCode",
        headerName: t("currency.code"),
        flex: 0.8,
        cellRenderer: (params: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{params.value}</span>
        ),
      },
      { field: "currencyName", headerName: t("currency.name"), flex: 1.5 },
      { field: "symbol", headerName: t("currency.symbol"), flex: 0.5 },
      { field: "decimalPlaces", headerName: t("currency.decimals"), flex: 0.6 },
      {
        field: "isBaseCurrency",
        headerName: t("currency.baseCurrency"),
        flex: 0.8,
        cellRenderer: (params: { value: boolean }) =>
          params.value ? <span className="badge-success">{t("common.yes")}</span> : <span className="text-slate-400">-</span>,
      },
      {
        field: "status",
        headerName: t("common.status"),
        flex: 0.8,
        cellRenderer: (params: { value: string }) => (
          <span className={params.value === "ACTIVE" ? "badge-success" : "badge bg-slate-100 text-slate-500"}>
            {params.value}
          </span>
        ),
      },
    ],
    [t]
  );

  const rateColumns = useMemo<ColDef<ExchangeRate>[]>(
    () => [
      { field: "fromCurrency", headerName: t("currency.from"), flex: 0.8 },
      { field: "toCurrency", headerName: t("currency.to"), flex: 0.8 },
      {
        field: "exchangeRate",
        headerName: t("currency.rate"),
        flex: 1.2,
        type: "numericColumn",
        valueFormatter: (params: { value: number }) => formatNumber(params.value),
      },
      { field: "rateDate", headerName: t("currency.effectiveDate"), flex: 1 },
      { field: "rateType", headerName: t("currency.rateType", "Rate Type"), flex: 0.8 },
      { field: "source", headerName: t("currency.source"), flex: 1 },
    ],
    [t]
  );

  const revaluationColumns = useMemo<ColDef<Revaluation>[]>(
    () => [
      {
        field: "documentNo",
        headerName: t("currency.revalNo"),
        flex: 1,
        cellRenderer: (params: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{params.value}</span>
        ),
      },
      { field: "companyCode", headerName: t("nav.companies"), flex: 0.8 },
      { field: "fiscalYear", headerName: t("accounting.fiscalYear", "FY"), flex: 0.7 },
      { field: "period", headerName: t("currency.period"), flex: 0.7 },
      {
        field: "unrealizedGainLoss",
        headerName: t("currency.gainLoss"),
        flex: 1.2,
        type: "numericColumn",
        cellRenderer: (params: { value: number }) => (
          <span className={params.value >= 0 ? "text-emerald-600" : "text-red-600 font-semibold"}>
            {formatCurrency(params.value)}
          </span>
        ),
      },
      {
        field: "status",
        headerName: t("common.status"),
        flex: 0.8,
        cellRenderer: (params: { value: string }) => (
          <span className={params.value === "POSTED" ? "badge-success" : "badge-info"}>{params.value}</span>
        ),
      },
      { field: "postedAt", headerName: t("common.datetime"), flex: 1.2 },
    ],
    [t]
  );

  const openCreate = () => {
    if (tab === "currencies") {
      setCurrencyForm({
        currencyCode: "",
        currencyName: "",
        symbol: "",
        decimalPlaces: 2,
        isBaseCurrency: false,
        status: "ACTIVE",
      });
    }

    if (tab === "rates") {
      setRateForm({
        fromCurrency: "",
        toCurrency: "",
        rateDate: today,
        exchangeRate: 0,
        rateType: "SPOT",
        source: "",
      });
    }

    setMode("create");
  };

  const handleCreate = () => {
    if (tab === "currencies") {
      createCurrencyMutation.mutate(currencyForm);
      return;
    }

    if (tab === "rates") {
      createRateMutation.mutate({
        ...rateForm,
        source: rateForm.source?.trim() || null,
      });
    }
  };

  const createPending = createCurrencyMutation.isPending || createRateMutation.isPending;

  if (mode === "create") {
    return (
      <div>
        <PageHeader
          title={t("common.create")}
          breadcrumbs={[{ label: t("nav.currency") }, { label: t("common.create") }]}
          actions={
            <button className="btn-ghost" onClick={() => setMode("list")}>
              <ArrowLeft size={16} /> {t("common.back")}
            </button>
          }
        />
        <div className="section-card">
          <p className="section-kicker">Currency</p>
          <h3 className="section-title">{t("common.basicInfo")}</h3>
          <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
            {tab === "currencies" && (
              <>
                <div>
                  <label className="field-label">{t("currency.code")}</label>
                  <input
                    className="input"
                    value={currencyForm.currencyCode}
                    onChange={(event) => setCurrencyForm((prev) => ({ ...prev, currencyCode: event.target.value }))}
                    placeholder="USD"
                  />
                </div>
                <div>
                  <label className="field-label">{t("currency.name")}</label>
                  <input
                    className="input"
                    value={currencyForm.currencyName}
                    onChange={(event) => setCurrencyForm((prev) => ({ ...prev, currencyName: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="field-label">{t("currency.symbol")}</label>
                  <input
                    className="input"
                    value={currencyForm.symbol}
                    onChange={(event) => setCurrencyForm((prev) => ({ ...prev, symbol: event.target.value }))}
                    placeholder="$"
                  />
                </div>
                <div>
                  <label className="field-label">{t("currency.decimals")}</label>
                  <input
                    className="input"
                    type="number"
                    value={currencyForm.decimalPlaces ?? 2}
                    onChange={(event) =>
                      setCurrencyForm((prev) => ({ ...prev, decimalPlaces: Number(event.target.value) }))
                    }
                  />
                </div>
                <div>
                  <label className="field-label">{t("currency.baseCurrency")}</label>
                  <select
                    className="input"
                    value={currencyForm.isBaseCurrency ? "Y" : "N"}
                    onChange={(event) =>
                      setCurrencyForm((prev) => ({ ...prev, isBaseCurrency: event.target.value === "Y" }))
                    }
                  >
                    <option value="N">{t("common.no")}</option>
                    <option value="Y">{t("common.yes")}</option>
                  </select>
                </div>
                <div>
                  <label className="field-label">{t("common.status")}</label>
                  <select
                    className="input"
                    value={currencyForm.status}
                    onChange={(event) => setCurrencyForm((prev) => ({ ...prev, status: event.target.value as "ACTIVE" | "INACTIVE" }))}
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="INACTIVE">INACTIVE</option>
                  </select>
                </div>
              </>
            )}

            {tab === "rates" && (
              <>
                <div>
                  <label className="field-label">{t("currency.from")}</label>
                  <input
                    className="input"
                    value={rateForm.fromCurrency}
                    onChange={(event) => setRateForm((prev) => ({ ...prev, fromCurrency: event.target.value }))}
                    placeholder="USD"
                  />
                </div>
                <div>
                  <label className="field-label">{t("currency.to")}</label>
                  <input
                    className="input"
                    value={rateForm.toCurrency}
                    onChange={(event) => setRateForm((prev) => ({ ...prev, toCurrency: event.target.value }))}
                    placeholder="KRW"
                  />
                </div>
                <div>
                  <label className="field-label">{t("currency.rate")}</label>
                  <input
                    className="input"
                    type="number"
                    step="0.0001"
                    value={rateForm.exchangeRate}
                    onChange={(event) =>
                      setRateForm((prev) => ({ ...prev, exchangeRate: Number(event.target.value) }))
                    }
                  />
                </div>
                <div>
                  <label className="field-label">{t("currency.effectiveDate")}</label>
                  <input
                    className="input"
                    type="date"
                    value={rateForm.rateDate ?? today}
                    onChange={(event) => setRateForm((prev) => ({ ...prev, rateDate: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="field-label">{t("currency.rateType", "Rate Type")}</label>
                  <select
                    className="input"
                    value={rateForm.rateType}
                    onChange={(event) =>
                      setRateForm((prev) => ({ ...prev, rateType: event.target.value as "SPOT" | "AVERAGE" | "CLOSING" }))
                    }
                  >
                    <option value="SPOT">SPOT</option>
                    <option value="AVERAGE">AVERAGE</option>
                    <option value="CLOSING">CLOSING</option>
                  </select>
                </div>
                <div>
                  <label className="field-label">{t("currency.source")}</label>
                  <input
                    className="input"
                    value={rateForm.source ?? ""}
                    onChange={(event) => setRateForm((prev) => ({ ...prev, source: event.target.value }))}
                  />
                </div>
              </>
            )}
          </div>
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <button className="btn-ghost" onClick={() => setMode("list")}>
            {t("common.cancel")}
          </button>
          <button className="btn-primary" onClick={handleCreate} disabled={createPending}>
            {createPending ? t("common.saving") : t("common.save")}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title={t("currency.title")}
        description={t("currency.description")}
        breadcrumbs={[{ label: t("nav.currency") }]}
        actions={
          <div className="flex gap-2">
            {(tab === "currencies" || tab === "rates") && (
              <button className="btn-primary" onClick={openCreate}>
                <Plus size={16} /> {t("common.new")}
              </button>
            )}
            {tab === "revaluation" && (
              <button className="btn-primary" onClick={() => revaluationMutation.mutate(revaluationForm)} disabled={revaluationMutation.isPending}>
                <RefreshCw size={16} /> {t("currency.runRevaluation")}
              </button>
            )}
          </div>
        }
      />

      <div className="mb-4 flex gap-2">
        <button className={tab === "currencies" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("currencies")}>
          {t("currency.currencies")}
        </button>
        <button className={tab === "rates" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("rates")}>
          {t("currency.exchangeRates")}
        </button>
        <button className={tab === "convert" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("convert")}>
          <ArrowRightLeft size={16} /> {t("currency.convert")}
        </button>
        <button className={tab === "revaluation" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("revaluation")}>
          {t("currency.revaluation")}
        </button>
      </div>

      {tab === "currencies" && (
        <div className="card overflow-hidden">
          <DataGrid<Currency> rowData={currenciesQ.data?.data ?? []} columnDefs={currencyColumns} loading={currenciesQ.isLoading} />
        </div>
      )}

      {tab === "rates" && (
        <div className="card overflow-hidden">
          <DataGrid<ExchangeRate> rowData={ratesQ.data?.data ?? []} columnDefs={rateColumns} loading={ratesQ.isLoading} />
        </div>
      )}

      {tab === "convert" && (
        <div className="section-card max-w-lg">
          <p className="section-kicker">Currency Conversion</p>
          <h3 className="section-title">{t("currency.convert")}</h3>
          <div className="mt-4 grid grid-cols-1 gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="field-label">{t("currency.from")}</label>
                <input
                  className="input"
                  value={convertForm.fromCurrency}
                  onChange={(event) => setConvertForm((prev) => ({ ...prev, fromCurrency: event.target.value }))}
                />
              </div>
              <div>
                <label className="field-label">{t("currency.to")}</label>
                <input
                  className="input"
                  value={convertForm.toCurrency}
                  onChange={(event) => setConvertForm((prev) => ({ ...prev, toCurrency: event.target.value }))}
                />
              </div>
            </div>
            <div>
              <label className="field-label">{t("currency.amount")}</label>
              <input
                className="input"
                type="number"
                value={convertForm.amount}
                onChange={(event) => setConvertForm((prev) => ({ ...prev, amount: Number(event.target.value) }))}
              />
            </div>
            <div>
              <label className="field-label">{t("common.date", "Date")}</label>
              <input
                className="input"
                type="date"
                value={convertForm.date ?? today}
                onChange={(event) => setConvertForm((prev) => ({ ...prev, date: event.target.value }))}
              />
            </div>
            <button className="btn-primary" onClick={() => convertMutation.mutate(convertForm)} disabled={convertMutation.isPending}>
              {convertMutation.isPending ? t("common.loading") : t("currency.convert")}
            </button>
            {convertResult && (
              <div className="rounded-[22px] bg-slate-50 p-4">
                <div className="text-xs uppercase tracking-wider text-slate-400">Result</div>
                <div className="mt-2 text-2xl font-bold text-slate-900">
                  {convertResult.toCurrency} {Number(convertResult.toAmount).toLocaleString("ko-KR", { maximumFractionDigits: 2 })}
                </div>
                <div className="mt-1 text-sm text-slate-500">
                  Rate: {formatNumber(convertResult.exchangeRate)} @ {convertResult.rateDate}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {tab === "revaluation" && (
        <div>
          <div className="section-card mb-4">
            <p className="section-kicker">Revaluation Parameters</p>
            <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
              <div>
                <label className="field-label">{t("nav.companies")}</label>
                <input
                  className="input"
                  value={revaluationForm.companyCode}
                  onChange={(event) => setRevaluationForm((prev) => ({ ...prev, companyCode: event.target.value }))}
                />
              </div>
              <div>
                <label className="field-label">{t("accounting.fiscalYear", "FY")}</label>
                <input
                  className="input"
                  type="number"
                  value={revaluationForm.fiscalYear}
                  onChange={(event) => setRevaluationForm((prev) => ({ ...prev, fiscalYear: Number(event.target.value) }))}
                />
              </div>
              <div>
                <label className="field-label">{t("currency.period")}</label>
                <input
                  className="input"
                  type="number"
                  min="1"
                  max="12"
                  value={revaluationForm.period}
                  onChange={(event) => setRevaluationForm((prev) => ({ ...prev, period: Number(event.target.value) }))}
                />
              </div>
              <div>
                <label className="field-label">{t("currency.from")}</label>
                <input
                  className="input"
                  value={revaluationForm.fromCurrency}
                  onChange={(event) => setRevaluationForm((prev) => ({ ...prev, fromCurrency: event.target.value }))}
                />
              </div>
              <div>
                <label className="field-label">{t("currency.to")}</label>
                <input
                  className="input"
                  value={revaluationForm.toCurrency}
                  onChange={(event) => setRevaluationForm((prev) => ({ ...prev, toCurrency: event.target.value }))}
                />
              </div>
              <div>
                <label className="field-label">{t("common.date", "Date")}</label>
                <input
                  className="input"
                  type="date"
                  value={revaluationForm.revaluationDate ?? today}
                  onChange={(event) => setRevaluationForm((prev) => ({ ...prev, revaluationDate: event.target.value }))}
                />
              </div>
              <div>
                <label className="field-label">{t("currency.originalRate", "Original Rate")}</label>
                <input
                  className="input"
                  type="number"
                  step="0.0001"
                  value={revaluationForm.originalRate}
                  onChange={(event) => setRevaluationForm((prev) => ({ ...prev, originalRate: Number(event.target.value) }))}
                />
              </div>
              <div>
                <label className="field-label">{t("currency.revaluationRate", "Revaluation Rate")}</label>
                <input
                  className="input"
                  type="number"
                  step="0.0001"
                  value={revaluationForm.revaluationRate}
                  onChange={(event) => setRevaluationForm((prev) => ({ ...prev, revaluationRate: Number(event.target.value) }))}
                />
              </div>
              <div>
                <label className="field-label">{t("currency.gainLoss")}</label>
                <input
                  className="input"
                  type="number"
                  value={revaluationForm.unrealizedGainLoss}
                  onChange={(event) =>
                    setRevaluationForm((prev) => ({ ...prev, unrealizedGainLoss: Number(event.target.value) }))
                  }
                />
              </div>
            </div>
          </div>
          <div className="card overflow-hidden">
            <DataGrid<Revaluation>
              rowData={revaluationsQ.data?.data ?? []}
              columnDefs={revaluationColumns}
              loading={revaluationsQ.isLoading}
            />
          </div>
        </div>
      )}
    </div>
  );
}
