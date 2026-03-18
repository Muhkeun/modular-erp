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

        <div className="card p-6 space-y-6">
          {/* Header fields */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <label className="form-group">
              <span className="form-label">Company Code *</span>
              <input className="form-input" value={form.companyCode} onChange={(e) => updateField("companyCode", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">{t("gr.plant")} *</span>
              <input className="form-input" value={form.plantCode} onChange={(e) => updateField("plantCode", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">{t("gr.storage")} *</span>
              <input className="form-input" value={form.storageLocation} onChange={(e) => updateField("storageLocation", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">{t("gi.issueType")} *</span>
              <select className="form-input" value={form.issueType} onChange={(e) => updateField("issueType", e.target.value)}>
                {ISSUE_TYPES.map((it) => (
                  <option key={it} value={it}>{t("gi.types." + it, it)}</option>
                ))}
              </select>
            </label>
            <label className="form-group">
              <span className="form-label">{t("gi.refDoc")}</span>
              <input className="form-input" value={form.referenceDocNo} onChange={(e) => updateField("referenceDocNo", e.target.value)} />
            </label>
            <label className="form-group">
              <span className="form-label">{t("gi.issueDate")} *</span>
              <input className="form-input" type="date" value={form.issueDate} onChange={(e) => updateField("issueDate", e.target.value)} />
            </label>
            <label className="form-group sm:col-span-2 lg:col-span-3">
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

        <div className="card p-6 space-y-6">
          {/* Status badge */}
          <div className="flex items-center gap-3">
            <span className={statusStyle[d.status || ""] || "badge"}>
              {String(t("status." + d.status, d.status ?? ""))}
            </span>
            {isDraft && (
              <span className="text-sm text-amber-600 bg-amber-50 px-3 py-1 rounded-md">
                {t("gi.confirmWarning", "확정하면 재고가 차감됩니다")}
              </span>
            )}
          </div>

          {/* Header info */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 text-sm">
            <div>
              <span className="text-slate-400">{t("gi.docNo")}</span>
              <p className="font-mono font-semibold">{d.documentNo}</p>
            </div>
            <div>
              <span className="text-slate-400">{t("gi.issueType")}</span>
              <p className="font-semibold">{t("gi.types." + d.issueType, d.issueType)}</p>
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
              <span className="text-slate-400">{t("gi.refDoc")}</span>
              <p className="font-mono">{d.referenceDocNo || "-"}</p>
            </div>
            <div>
              <span className="text-slate-400">{t("gi.issueDate")}</span>
              <p className="font-semibold">{d.issueDate}</p>
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
