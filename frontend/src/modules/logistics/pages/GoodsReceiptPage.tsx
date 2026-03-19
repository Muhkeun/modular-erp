import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Save, Trash2, CheckCircle } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

/* ── Types ── */
interface GrLine {
  id?: number;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  poLineNo: string;
  storageLocation: string;
}

interface GrHeader {
  id?: number;
  documentNo?: string;
  companyCode: string;
  plantCode: string;
  storageLocation: string;
  vendorCode: string;
  vendorName: string;
  poDocumentNo: string;
  receiptDate: string;
  remark: string;
  status?: string;
  lines: GrLine[];
}

interface GrRow {
  id: number;
  documentNo: string;
  vendorName: string;
  plantCode: string;
  storageLocation: string;
  poDocumentNo: string | null;
  receiptDate: string;
  status: string;
}

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  CONFIRMED: "badge-success",
  CANCELLED: "badge-danger",
};

const emptyLine = (): GrLine => ({
  itemCode: "",
  itemName: "",
  quantity: 0,
  unitOfMeasure: "EA",
  unitPrice: 0,
  poLineNo: "",
  storageLocation: "",
});

const defaultHeader = (): GrHeader => ({
  companyCode: "",
  plantCode: "",
  storageLocation: "",
  vendorCode: "",
  vendorName: "",
  poDocumentNo: "",
  receiptDate: new Date().toISOString().slice(0, 10),
  remark: "",
  lines: [emptyLine()],
});

/* ── Component ── */
export default function GoodsReceiptPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const [mode, setMode] = useState<"list" | "create" | "detail">("list");
  const [form, setForm] = useState<GrHeader>(defaultHeader());
  const [detail, setDetail] = useState<GrHeader | null>(null);

  /* ── Queries ── */
  const { data, isLoading } = useQuery({
    queryKey: ["goods-receipts"],
    queryFn: async () => (await api.get("/api/v1/logistics/goods-receipts?size=100")).data,
    enabled: mode === "list",
  });

  /* ── Mutations ── */
  const saveMutation = useMutation({
    mutationFn: async (payload: GrHeader) => (await api.post("/api/v1/logistics/goods-receipts", payload)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["goods-receipts"] });
      setMode("list");
    },
  });

  const confirmMutation = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/logistics/goods-receipts/${id}/confirm`)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["goods-receipts"] });
      setMode("list");
    },
  });

  /* ── Handlers ── */
  const openCreate = useCallback(() => {
    setForm(defaultHeader());
    setMode("create");
  }, []);

  const openDetail = useCallback(async (row: GrRow) => {
    const res = await api.get(`/api/v1/logistics/goods-receipts/${row.id}`);
    setDetail(res.data.data ?? res.data);
    setMode("detail");
  }, []);

  const updateField = useCallback(<K extends keyof GrHeader>(key: K, value: GrHeader[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  }, []);

  const updateLine = useCallback((idx: number, key: keyof GrLine, value: string | number) => {
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
    if (!window.confirm(t("gr.confirmWarning", "확정하면 재고가 증가합니다. 계속하시겠습니까?"))) return;
    confirmMutation.mutate(detail.id);
  }, [detail, confirmMutation, t]);

  /* ── Column Defs ── */
  const columnDefs = useMemo<ColDef<GrRow>[]>(
    () => [
      {
        field: "documentNo",
        headerName: t("gr.docNo"),
        flex: 1.2,
        cellRenderer: (p: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{p.value}</span>
        ),
      },
      {
        field: "poDocumentNo",
        headerName: t("gr.poRef"),
        flex: 1,
        cellRenderer: (p: { value: string | null }) =>
          p.value ? <span className="font-mono text-slate-600">{p.value}</span> : <span className="text-slate-300">-</span>,
      },
      { field: "vendorName", headerName: t("gr.vendor"), flex: 2 },
      { field: "plantCode", headerName: t("gr.plant"), flex: 0.7 },
      { field: "storageLocation", headerName: t("gr.storage"), flex: 0.8 },
      { field: "receiptDate", headerName: t("gr.receiptDate"), flex: 1 },
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
          title={t("gr.title")}
          description={t("gr.description")}
          breadcrumbs={[{ label: t("nav.logistics") }, { label: t("nav.goodsReceipt") }]}
          actions={
            <button className="btn-primary" onClick={openCreate}>
              <Plus size={16} /> {t("gr.newGr")}
            </button>
          }
        />
        <div className="card overflow-hidden">
          <DataGrid<GrRow>
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
          title={t("gr.newGr")}
          breadcrumbs={[{ label: t("nav.logistics") }, { label: t("nav.goodsReceipt") }, { label: t("common.new") }]}
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
          <h2 className="section-title">{t("gr.newGr", "입고 전표 등록")}</h2>
          <div className="mt-4 flex flex-wrap gap-3">
            <div className="stat-tile">
              <span className="text-xs text-slate-500">Line Items</span>
              <span className="text-lg font-bold text-slate-800">{form.lines.length}</span>
            </div>
            <div className="stat-tile">
              <span className="text-xs text-slate-500">Total Qty</span>
              <span className="text-lg font-bold text-slate-800">{form.lines.reduce((s, l) => s + l.quantity, 0)}</span>
            </div>
            <div className="stat-tile">
              <span className="text-xs text-slate-500">Total Amount</span>
              <span className="text-lg font-bold text-slate-800">{form.lines.reduce((s, l) => s + l.quantity * l.unitPrice, 0).toLocaleString()}</span>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          {/* Header fields */}
          <div className="section-card">
            <p className="section-kicker">Receipt Header</p>
            <h3 className="section-title">{t("gr.title", "헤더 정보")}</h3>
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <div>
                <label className="field-label">{t("gr.plant")} *</label>
                <input className="input mt-2" value={form.plantCode} onChange={(e) => updateField("plantCode", e.target.value)} />
              </div>
              <div>
                <label className="field-label">Company Code *</label>
                <input className="input mt-2" value={form.companyCode} onChange={(e) => updateField("companyCode", e.target.value)} />
              </div>
              <div>
                <label className="field-label">{t("gr.storage")} *</label>
                <input className="input mt-2" value={form.storageLocation} onChange={(e) => updateField("storageLocation", e.target.value)} />
              </div>
              <div>
                <label className="field-label">{t("gr.vendor")} Code *</label>
                <input className="input mt-2" value={form.vendorCode} onChange={(e) => updateField("vendorCode", e.target.value)} />
              </div>
              <div>
                <label className="field-label">{t("gr.vendor")} *</label>
                <input className="input mt-2" value={form.vendorName} onChange={(e) => updateField("vendorName", e.target.value)} />
              </div>
              <div>
                <label className="field-label">{t("gr.poRef")}</label>
                <input className="input mt-2" value={form.poDocumentNo} onChange={(e) => updateField("poDocumentNo", e.target.value)} />
              </div>
              <div>
                <label className="field-label">{t("gr.receiptDate")} *</label>
                <input className="input mt-2" type="date" value={form.receiptDate} onChange={(e) => updateField("receiptDate", e.target.value)} />
              </div>
              <div className="sm:col-span-2">
                <label className="field-label">Remark</label>
                <input className="input mt-2" value={form.remark} onChange={(e) => updateField("remark", e.target.value)} />
              </div>
            </div>
          </div>

          {/* Line items */}
          <div className="section-card">
            <div className="flex items-center justify-between">
              <div>
                <p className="section-kicker">Receipt Lines</p>
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
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
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
                      <label className="field-label">Unit Price</label>
                      <input className="input mt-2" type="number" value={line.unitPrice} onChange={(e) => updateLine(idx, "unitPrice", Number(e.target.value))} />
                    </div>
                    <div>
                      <label className="field-label">PO Line</label>
                      <input className="input mt-2" value={line.poLineNo} onChange={(e) => updateLine(idx, "poLineNo", e.target.value)} />
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
          title={`${t("gr.title")} - ${d.documentNo || ""}`}
          breadcrumbs={[{ label: t("nav.logistics") }, { label: t("nav.goodsReceipt") }, { label: d.documentNo || "" }]}
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
            <p className="section-kicker">Receipt Detail</p>
            <h3 className="section-title">{d.documentNo}</h3>

            <div className="mt-4 flex items-center gap-3">
              <span className={statusStyle[d.status || ""] || "badge"}>
                {String(t("status." + d.status, d.status ?? ""))}
              </span>
              {isDraft && (
                <span className="text-sm text-amber-600 bg-amber-50 px-3 py-1 rounded-md">
                  {t("gr.confirmWarning", "확정하면 재고가 증가합니다")}
                </span>
              )}
            </div>

            <div className="mt-5 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
              <div className="stat-tile">
                <span className="text-xs text-slate-500">{t("gr.docNo")}</span>
                <span className="font-mono font-semibold">{d.documentNo}</span>
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
                <span className="text-xs text-slate-500">{t("gr.vendor")}</span>
                <span className="font-semibold">{d.vendorName} ({d.vendorCode})</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs text-slate-500">{t("gr.poRef")}</span>
                <span className="font-mono">{d.poDocumentNo || "-"}</span>
              </div>
              <div className="stat-tile">
                <span className="text-xs text-slate-500">{t("gr.receiptDate")}</span>
                <span className="font-semibold">{d.receiptDate}</span>
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
              <p className="section-kicker">Receipt Lines</p>
              <h3 className="section-title">Line Items</h3>
              <div className="grid-table mt-4">
                <div className="grid-table-row font-semibold text-xs text-slate-500">
                  <span>#</span>
                  <span>{t("stock.itemCode")}</span>
                  <span>{t("stock.itemName")}</span>
                  <span className="text-right">Qty</span>
                  <span>{t("item.uom")}</span>
                  <span className="text-right">Unit Price</span>
                  <span>PO Line</span>
                  <span>{t("gr.storage")}</span>
                </div>
                {d.lines.map((line, idx) => (
                  <div key={idx} className="grid-table-row">
                    <span className="text-slate-400">{idx + 1}</span>
                    <span className="font-mono">{line.itemCode}</span>
                    <span>{line.itemName}</span>
                    <span className="text-right">{line.quantity}</span>
                    <span>{line.unitOfMeasure}</span>
                    <span className="text-right">{line.unitPrice?.toLocaleString()}</span>
                    <span>{line.poLineNo || "-"}</span>
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
