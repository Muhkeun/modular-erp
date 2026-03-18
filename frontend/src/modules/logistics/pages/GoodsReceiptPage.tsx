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

        <div className="card p-6 space-y-6">
          {/* Header fields */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <label className="form-group">
              <span className="form-label">{t("gr.plant")} *</span>
              <input className="form-input" value={form.plantCode} onChange={(e) => updateField("plantCode", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">Company Code *</span>
              <input className="form-input" value={form.companyCode} onChange={(e) => updateField("companyCode", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">{t("gr.storage")} *</span>
              <input className="form-input" value={form.storageLocation} onChange={(e) => updateField("storageLocation", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">{t("gr.vendor")} Code *</span>
              <input className="form-input" value={form.vendorCode} onChange={(e) => updateField("vendorCode", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">{t("gr.vendor")} *</span>
              <input className="form-input" value={form.vendorName} onChange={(e) => updateField("vendorName", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">{t("gr.poRef")}</span>
              <input className="form-input" value={form.poDocumentNo} onChange={(e) => updateField("poDocumentNo", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">{t("gr.receiptDate")} *</span>
              <input className="form-input" type="date" value={form.receiptDate} onChange={(e) => updateField("receiptDate", e.target.value)} />
            </label>
            <label className="form-group sm:col-span-2">
              <span className="form-label">Remark</span>
              <input className="form-input" value={form.remark} onChange={(e) => updateField("remark", e.target.value)} />
            </label>
          </div>

          {/* Line items */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-semibold text-slate-700">Line Items</h3>
              <button className="btn-secondary text-sm" onClick={addLine}>
                <Plus size={14} /> {t("common.new", "행 추가")}
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="table w-full text-sm">
                <thead>
                  <tr>
                    <th className="w-8">#</th>
                    <th>{t("stock.itemCode")}</th>
                    <th>{t("stock.itemName")}</th>
                    <th className="w-24">Qty</th>
                    <th className="w-20">{t("item.uom")}</th>
                    <th className="w-28">Unit Price</th>
                    <th className="w-24">PO Line</th>
                    <th className="w-28">{t("gr.storage")}</th>
                    <th className="w-12"></th>
                  </tr>
                </thead>
                <tbody>
                  {form.lines.map((line, idx) => (
                    <tr key={idx}>
                      <td className="text-slate-400">{idx + 1}</td>
                      <td><input className="form-input form-input-sm" value={line.itemCode} onChange={(e) => updateLine(idx, "itemCode", e.target.value)} /></td>
                      <td><input className="form-input form-input-sm" value={line.itemName} onChange={(e) => updateLine(idx, "itemName", e.target.value)} /></td>
                      <td><input className="form-input form-input-sm" type="number" value={line.quantity} onChange={(e) => updateLine(idx, "quantity", Number(e.target.value))} /></td>
                      <td><input className="form-input form-input-sm" value={line.unitOfMeasure} onChange={(e) => updateLine(idx, "unitOfMeasure", e.target.value)} /></td>
                      <td><input className="form-input form-input-sm" type="number" value={line.unitPrice} onChange={(e) => updateLine(idx, "unitPrice", Number(e.target.value))} /></td>
                      <td><input className="form-input form-input-sm" value={line.poLineNo} onChange={(e) => updateLine(idx, "poLineNo", e.target.value)} /></td>
                      <td><input className="form-input form-input-sm" value={line.storageLocation} onChange={(e) => updateLine(idx, "storageLocation", e.target.value)} /></td>
                      <td>
                        {form.lines.length > 1 && (
                          <button className="text-red-400 hover:text-red-600" onClick={() => removeLine(idx)}>
                            <Trash2 size={14} />
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {saveMutation.isError && (
            <div className="text-sm text-red-600 bg-red-50 rounded-lg px-4 py-2">
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

        <div className="card p-6 space-y-6">
          {/* Status badge */}
          <div className="flex items-center gap-3">
            <span className={statusStyle[d.status || ""] || "badge"}>
              {String(t("status." + d.status, d.status ?? ""))}
            </span>
            {isDraft && (
              <span className="text-sm text-amber-600 bg-amber-50 px-3 py-1 rounded-md">
                {t("gr.confirmWarning", "확정하면 재고가 증가합니다")}
              </span>
            )}
          </div>

          {/* Header info */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 text-sm">
            <div>
              <span className="text-slate-400">{t("gr.docNo")}</span>
              <p className="font-mono font-semibold">{d.documentNo}</p>
            </div>
            <div>
              <span className="text-slate-400">{t("gr.plant")}</span>
              <p className="font-semibold">{d.plantCode}</p>
            </div>
            <div>
              <span className="text-slate-400">{t("gr.storage")}</span>
              <p className="font-semibold">{d.storageLocation}</p>
            </div>
            <div>
              <span className="text-slate-400">{t("gr.vendor")}</span>
              <p className="font-semibold">{d.vendorName} ({d.vendorCode})</p>
            </div>
            <div>
              <span className="text-slate-400">{t("gr.poRef")}</span>
              <p className="font-mono">{d.poDocumentNo || "-"}</p>
            </div>
            <div>
              <span className="text-slate-400">{t("gr.receiptDate")}</span>
              <p className="font-semibold">{d.receiptDate}</p>
            </div>
            {d.remark && (
              <div className="col-span-2">
                <span className="text-slate-400">Remark</span>
                <p>{d.remark}</p>
              </div>
            )}
          </div>

          {/* Lines table */}
          {d.lines && d.lines.length > 0 && (
            <div>
              <h3 className="text-sm font-semibold text-slate-700 mb-3">Line Items</h3>
              <div className="overflow-x-auto">
                <table className="table w-full text-sm">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>{t("stock.itemCode")}</th>
                      <th>{t("stock.itemName")}</th>
                      <th>Qty</th>
                      <th>{t("item.uom")}</th>
                      <th>Unit Price</th>
                      <th>PO Line</th>
                      <th>{t("gr.storage")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {d.lines.map((line, idx) => (
                      <tr key={idx}>
                        <td className="text-slate-400">{idx + 1}</td>
                        <td className="font-mono">{line.itemCode}</td>
                        <td>{line.itemName}</td>
                        <td className="text-right">{line.quantity}</td>
                        <td>{line.unitOfMeasure}</td>
                        <td className="text-right">{line.unitPrice?.toLocaleString()}</td>
                        <td>{line.poLineNo || "-"}</td>
                        <td>{line.storageLocation}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {confirmMutation.isError && (
            <div className="text-sm text-red-600 bg-red-50 rounded-lg px-4 py-2">
              {(confirmMutation.error as Error)?.message || "Confirm failed"}
            </div>
          )}
        </div>
      </div>
    );
  }

  return null;
}
