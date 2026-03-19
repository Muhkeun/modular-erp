import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Save, Trash2, CheckCircle } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

/* ── Types ── */
interface GiLine {
  id?: number;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  storageLocation: string;
}

interface GiHeader {
  id?: number;
  documentNo?: string;
  companyCode: string;
  plantCode: string;
  storageLocation: string;
  issueType: string;
  referenceDocNo: string;
  issueDate: string;
  remark: string;
  status?: string;
  lines: GiLine[];
}

interface GiRow {
  id: number;
  documentNo: string;
  issueType: string;
  referenceDocNo: string | null;
  plantCode: string;
  storageLocation: string;
  issueDate: string;
  status: string;
}

const ISSUE_TYPES = ["SALES", "TRANSFER", "PRODUCTION", "SCRAP", "RETURN"] as const;

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  CONFIRMED: "badge-success",
  CANCELLED: "badge-danger",
};

const emptyLine = (): GiLine => ({
  itemCode: "",
  itemName: "",
  quantity: 0,
  unitOfMeasure: "EA",
  storageLocation: "",
});

const defaultHeader = (): GiHeader => ({
  companyCode: "",
  plantCode: "",
  storageLocation: "",
  issueType: "SALES",
  referenceDocNo: "",
  issueDate: new Date().toISOString().slice(0, 10),
  remark: "",
  lines: [emptyLine()],
});

/* ── Component ── */
export default function GoodsIssuePage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const [mode, setMode] = useState<"list" | "create" | "detail">("list");
  const [form, setForm] = useState<GiHeader>(defaultHeader());
  const [detail, setDetail] = useState<GiHeader | null>(null);

  /* ── Queries ── */
  const { data, isLoading } = useQuery({
    queryKey: ["goods-issues"],
    queryFn: async () => (await api.get("/api/v1/logistics/goods-issues?size=100")).data,
    enabled: mode === "list",
  });

  /* ── Mutations ── */
  const saveMutation = useMutation({
    mutationFn: async (payload: GiHeader) => (await api.post("/api/v1/logistics/goods-issues", payload)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["goods-issues"] });
      setMode("list");
    },
  });

  const confirmMutation = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/logistics/goods-issues/${id}/confirm`)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["goods-issues"] });
      setMode("list");
    },
  });

  /* ── Handlers ── */
  const openCreate = useCallback(() => {
    setForm(defaultHeader());
    setMode("create");
  }, []);

  const openDetail = useCallback(async (row: GiRow) => {
    const res = await api.get(`/api/v1/logistics/goods-issues/${row.id}`);
    setDetail(res.data.data ?? res.data);
    setMode("detail");
  }, []);

  const updateField = useCallback(<K extends keyof GiHeader>(key: K, value: GiHeader[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  }, []);

  const updateLine = useCallback((idx: number, key: keyof GiLine, value: string | number) => {
    setForm((prev) => {
      const lines = [...prev.lines];
      lines[idx] = { ...lines[idx], [key]: value };
      return { ...prev, lines };
    });
  }, []);

  const addLine = useCallback(() => {
    setForm((prev) => ({ ...prev, lines: [...prev.lines, emptyLine()] }));
  }, []);

  const removeLine = useCallback((idx: number) => {
    setForm((prev) => ({ ...prev, lines: prev.lines.filter((_, i) => i !== idx) }));
  }, []);

  const handleSave = useCallback(() => {
    saveMutation.mutate(form);
  }, [form, saveMutation]);

  const handleConfirm = useCallback(() => {
    if (!detail?.id) return;
    if (!window.confirm(t("gi.confirmWarning", "확정하면 재고가 차감됩니다. 계속하시겠습니까?"))) return;
    confirmMutation.mutate(detail.id);
  }, [detail, confirmMutation, t]);

  /* ── Column Defs ── */
  const columnDefs = useMemo<ColDef<GiRow>[]>(
    () => [
      {
        field: "documentNo",
        headerName: t("gi.docNo"),
        flex: 1.2,
        cellRenderer: (p: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{p.value}</span>
        ),
      },
      {
        field: "issueType",
        headerName: t("gi.issueType"),
        flex: 0.8,
        valueFormatter: (p: { value: string }) => t("gi.types." + p.value, p.value),
      },
      { field: "referenceDocNo", headerName: t("gi.refDoc"), flex: 1 },
      { field: "plantCode", headerName: t("gr.plant"), flex: 0.7 },
      { field: "storageLocation", headerName: t("gr.storage"), flex: 0.8 },
      { field: "issueDate", headerName: t("gi.issueDate"), flex: 1 },
      {
        field: "status",
        headerName: t("common.status"),
        flex: 0.8,
        cellRenderer: (p: { value: string }) => (
          <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span>
        ),
      },
    ],
    [t]
  );

  /* ── LIST VIEW ── */
  if (mode === "list") {
    return (
      <div>
        <PageHeader
          title={t("gi.title")}
          description={t("gi.description")}
          breadcrumbs={[{ label: t("nav.logistics") }, { label: t("nav.goodsIssue") }]}
          actions={
            <button className="btn-primary" onClick={openCreate}>
              <Plus size={16} /> {t("gi.newGi")}
            </button>
          }
        />
        <div className="card overflow-hidden">
          <DataGrid<GiRow>
            rowData={data?.data || []}
            columnDefs={columnDefs}
            loading={isLoading}
            onRowClicked={openDetail}
          />
        </div>
      </div>
    );
  }

  /* ── CREATE VIEW ── */
  if (mode === "create") {
    return (
      <div>
        <PageHeader
          title={t("gi.newGi")}
          breadcrumbs={[{ label: t("nav.logistics") }, { label: t("nav.goodsIssue") }, { label: t("common.new") }]}
          actions={
            <div className="flex items-center gap-2">
              <button className="btn-secondary" onClick={() => setMode("list")}>
                <ArrowLeft size={16} /> {t("common.back")}
              </button>
              <button className="btn-primary" onClick={handleSave} disabled={saveMutation.isPending}>
                <Save size={16} /> {saveMutation.isPending ? t("common.saving") : t("common.save")}
              </button>
            </div>
          }
        />

        {/* Workspace Hero */}
        <div className="workspace-hero">
          <p className="section-kicker">Logistics Workspace</p>
          <h2 className="section-title">{t("gi.newGi", "출고 전표 등록")}</h2>
          <div className="mt-4 flex flex-wrap gap-3">
            <div className="stat-tile">
              <span className="text-xs text-slate-500">Line Items</span>
              <span className="text-lg font-bold text-slate-800">{form.lines.length}</span>
            </div>
            <div className="stat-tile">
              <span className="text-xs text-slate-500">Total Qty</span>
              <span className="text-lg font-bold text-slate-800">{form.lines.reduce((s, l) => s + l.quantity, 0)}</span>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          {/* Header fields */}
          <div className="section-card">
            <p className="section-kicker">Issue Header</p>
            <h3 className="section-title">{t("gi.title", "헤더 정보")}</h3>
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <div>
                <label className="field-label">Company Code *</label>
                <input className="input mt-2" value={form.companyCode} onChange={(e) => updateField("companyCode", e.target.value)} />
              </div>
              <div>
                <label className="field-label">{t("gr.plant")} *</label>
                <input className="input mt-2" value={form.plantCode} onChange={(e) => updateField("plantCode", e.target.value)} />
              </div>
              <div>
                <label className="field-label">{t("gr.storage")} *</label>
                <input className="input mt-2" value={form.storageLocation} onChange={(e) => updateField("storageLocation", e.target.value)} />
              </div>
              <div>
                <label className="field-label">{t("gi.issueType")} *</label>
                <select className="input mt-2" value={form.issueType} onChange={(e) => updateField("issueType", e.target.value)}>
                  {ISSUE_TYPES.map((it) => (
                    <option key={it} value={it}>{t("gi.types." + it, it)}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="field-label">{t("gi.refDoc")}</label>
                <input className="input mt-2" value={form.referenceDocNo} onChange={(e) => updateField("referenceDocNo", e.target.value)} />
              </div>
              <div>
                <label className="field-label">{t("gi.issueDate")} *</label>
                <input className="input mt-2" type="date" value={form.issueDate} onChange={(e) => updateField("issueDate", e.target.value)} />
              </div>
              <div className="sm:col-span-2 lg:col-span-3">
                <label className="field-label">Remark</label>
                <input className="input mt-2" value={form.remark} onChange={(e) => updateField("remark", e.target.value)} />
              </div>
            </div>
          </div>

          {/* Line items */}
          <div className="section-card">
            <div className="flex items-center justify-between">
              <div>
                <p className="section-kicker">Issue Lines</p>
                <h3 className="section-title">Line Items</h3>
              </div>
              <button className="btn-secondary text-sm" onClick={addLine}>
                <Plus size={14} /> {t("common.new", "행 추가")}
              </button>
            </div>
            <div className="mt-4 space-y-3">
              {form.lines.map((line, idx) => (
                <div key={idx} className="rounded-[26px] border border-slate-200/80 bg-slate-50/70 p-5">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-xs font-semibold text-slate-400">#{idx + 1}</span>
                    {form.lines.length > 1 && (
                      <button className="text-red-400 hover:text-red-600" onClick={() => removeLine(idx)}>
                        <Trash2 size={14} />
                      </button>
                    )}
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                    <div>
                      <label className="field-label">{t("stock.itemCode")}</label>
                      <input className="input mt-2" value={line.itemCode} onChange={(e) => updateLine(idx, "itemCode", e.target.value)} />
                    </div>
                    <div>
                      <label className="field-label">{t("stock.itemName")}</label>
                      <input className="input mt-2" value={line.itemName} onChange={(e) => updateLine(idx, "itemName", e.target.value)} />
                    </div>
                    <div>
                      <label className="field-label">Qty</label>
                      <input className="input mt-2" type="number" value={line.quantity} onChange={(e) => updateLine(idx, "quantity", Number(e.target.value))} />
                    </div>
                    <div>
                      <label className="field-label">{t("item.uom")}</label>
                      <input className="input mt-2" value={line.unitOfMeasure} onChange={(e) => updateLine(idx, "unitOfMeasure", e.target.value)} />
                    </div>
                    <div>
                      <label className="field-label">{t("gr.storage")}</label>
                      <input className="input mt-2" value={line.storageLocation} onChange={(e) => updateLine(idx, "storageLocation", e.target.value)} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {saveMutation.isError && (
            <div className="rounded-[22px] bg-red-50 p-4 text-sm text-red-600">
              {(saveMutation.error as Error)?.message || "Save failed"}
            </div>
          )}
        </div>
      </div>
    );
  }

  /* ── DETAIL VIEW ── */
  if (mode === "detail" && detail) {
    const d = detail;
    const isDraft = d.status === "DRAFT";
    return (
      <div>
        <PageHeader
          title={`${t("gi.title")} - ${d.documentNo || ""}`}
          breadcrumbs={[{ label: t("nav.logistics") }, { label: t("nav.goodsIssue") }, { label: d.documentNo || "" }]}
          actions={
            <div className="flex items-center gap-2">
              <button className="btn-secondary" onClick={() => setMode("list")}>
                <ArrowLeft size={16} /> {t("common.back")}
              </button>
              {isDraft && (
                <button className="btn-primary" onClick={handleConfirm} disabled={confirmMutation.isPending}>
                  <CheckCircle size={16} /> {t("common.confirm", "확정")}
                </button>
              )}
            </div>
          }
        />

        <div className="space-y-6">
          {/* Status & header info */}
          <div className="section-card">
            <p className="section-kicker">Issue Detail</p>
            <h3 className="section-title">{d.documentNo}</h3>

            <div className="mt-4 flex items-center gap-3">
              <span className={statusStyle[d.status || ""] || "badge"}>
                {String(t("status." + d.status, d.status ?? ""))}
              </span>
              {isDraft && (
                <span className="text-sm text-amber-600 bg-amber-50 px-3 py-1 rounded-md">
                  {t("gi.confirmWarning", "확정하면 재고가 차감됩니다")}
                </span>
              )}
            </div>

            <div className="mt-5 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
              <div className="stat-tile">
                <span className="text-xs text-slate-500">{t("gi.docNo")}</span>
                <span className="font-mono font-semibold">{d.documentNo}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs text-slate-500">{t("gi.issueType")}</span>
                <span className="font-semibold">{t("gi.types." + d.issueType, d.issueType)}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs text-slate-500">{t("gr.plant")}</span>
                <span className="font-semibold">{d.plantCode}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs text-slate-500">{t("gr.storage")}</span>
                <span className="font-semibold">{d.storageLocation}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs text-slate-500">{t("gi.refDoc")}</span>
                <span className="font-mono">{d.referenceDocNo || "-"}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs text-slate-500">{t("gi.issueDate")}</span>
                <span className="font-semibold">{d.issueDate}</span>
              </div>
              {d.remark && (
                <div className="stat-tile col-span-2">
                  <span className="text-xs text-slate-500">Remark</span>
                  <span>{d.remark}</span>
                </div>
              )}
            </div>
          </div>

          {/* Lines table */}
          {d.lines && d.lines.length > 0 && (
            <div className="section-card">
              <p className="section-kicker">Issue Lines</p>
              <h3 className="section-title">Line Items</h3>
              <div className="grid-table mt-4">
                <div className="grid-table-row font-semibold text-xs text-slate-500">
                  <span>#</span>
                  <span>{t("stock.itemCode")}</span>
                  <span>{t("stock.itemName")}</span>
                  <span className="text-right">Qty</span>
                  <span>{t("item.uom")}</span>
                  <span>{t("gr.storage")}</span>
                </div>
                {d.lines.map((line, idx) => (
                  <div key={idx} className="grid-table-row">
                    <span className="text-slate-400">{idx + 1}</span>
                    <span className="font-mono">{line.itemCode}</span>
                    <span>{line.itemName}</span>
                    <span className="text-right">{line.quantity}</span>
                    <span>{line.unitOfMeasure}</span>
                    <span>{line.storageLocation}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {confirmMutation.isError && (
            <div className="rounded-[22px] bg-red-50 p-4 text-sm text-red-600">
              {(confirmMutation.error as Error)?.message || "Confirm failed"}
            </div>
          )}
        </div>
      </div>
    );
  }

  return null;
}
