import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Trash2 } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

/* ── types ─────────────────────────────────────────── */
interface JeLine {
  id?: number;
  accountCode: string;
  accountName: string;
  debitAmount: number;
  creditAmount: number;
  costCenter: string;
  description: string;
}

interface JeRow {
  id: number;
  documentNo: string;
  companyCode: string;
  postingDate: string;
  entryType: string;
  status: string;
  totalDebit: number;
  totalCredit: number;
  isBalanced: boolean;
  description: string | null;
  referenceDocNo: string | null;
  currencyCode: string;
  lines: JeLine[];
}

type Mode = "list" | "create" | "detail";

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  POSTED: "badge-success",
  REVERSED: "badge-danger",
};

const ENTRY_TYPES = ["MANUAL", "GOODS_RECEIPT", "GOODS_ISSUE", "INVOICE", "PAYMENT"] as const;

const emptyLine = (): JeLine => ({
  accountCode: "", accountName: "", debitAmount: 0, creditAmount: 0, costCenter: "", description: "",
});

const fmt = (v: number) =>
  v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

/* ── component ─────────────────────────────────────── */
export default function JournalEntryPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [mode, setMode] = useState<Mode>("list");
  const [selected, setSelected] = useState<JeRow | null>(null);

  /* form state */
  const [form, setForm] = useState({
    companyCode: "", postingDate: "", entryType: "MANUAL" as string,
    referenceDocNo: "", description: "", currencyCode: "KRW",
  });
  const [lines, setLines] = useState<JeLine[]>([emptyLine(), emptyLine()]);

  /* queries */
  const { data, isLoading } = useQuery({
    queryKey: ["journal-entries"],
    queryFn: async () => (await api.get("/api/v1/account/journal-entries?size=100")).data,
  });

  const detailQuery = useQuery({
    queryKey: ["journal-entry", selected?.id],
    queryFn: async () => (await api.get(`/api/v1/account/journal-entries/${selected!.id}`)).data,
    enabled: mode === "detail" && !!selected?.id,
  });

  /* mutations */
  const createMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/account/journal-entries", body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["journal-entries"] }); setMode("list"); },
  });

  const postMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/account/journal-entries/${id}/post`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["journal-entries"] }); qc.invalidateQueries({ queryKey: ["journal-entry", selected?.id] }); },
  });

  const reverseMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/account/journal-entries/${id}/reverse`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["journal-entries"] }); qc.invalidateQueries({ queryKey: ["journal-entry", selected?.id] }); },
  });

  /* column defs */
  const columnDefs = useMemo<ColDef<JeRow>[]>(() => [
    { field: "documentNo", headerName: t("je.docNo"), flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "postingDate", headerName: t("je.postingDate"), flex: 1 },
    { field: "entryType", headerName: t("je.entryType"), flex: 1 },
    { field: "referenceDocNo", headerName: t("je.refDoc"), flex: 1 },
    { field: "description", headerName: t("je.description_"), flex: 2 },
    { field: "totalDebit", headerName: t("je.debit"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "totalCredit", headerName: t("je.credit"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "isBalanced", headerName: t("je.balanced"), flex: 0.7,
      cellRenderer: (p: { value: boolean }) => p.value ? <span className="badge-success">{t("common.yes")}</span> : <span className="badge-danger">{t("common.no")}</span> },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  /* handlers */
  const onRowClicked = useCallback((data: JeRow) => {
    setSelected(data); setMode("detail");
  }, []);

  const openCreate = () => {
    setForm({ companyCode: "", postingDate: "", entryType: "MANUAL", referenceDocNo: "", description: "", currencyCode: "KRW" });
    setLines([emptyLine(), emptyLine()]);
    setMode("create");
  };

  const updateLine = (idx: number, field: keyof JeLine, value: string | number) => {
    setLines(prev => prev.map((l, i) => i === idx ? { ...l, [field]: value } : l));
  };

  const handleSave = () => {
    createMut.mutate({ ...form, lines });
  };

  const detail: JeRow | undefined = detailQuery.data?.data;

  /* running totals for create form */
  const totalDebit = lines.reduce((s, l) => s + (l.debitAmount || 0), 0);
  const totalCredit = lines.reduce((s, l) => s + (l.creditAmount || 0), 0);
  const isBalanced = totalDebit === totalCredit && totalDebit > 0;

  const handlePost = () => {
    if (!isBalanced && mode === "create") return;
    const d = detail || selected;
    if (d && (d.isBalanced || d.totalDebit === d.totalCredit)) {
      postMut.mutate(d.id);
    }
  };

  /* ── LIST ─────────────────────────────────────────── */
  if (mode === "list") {
    return (
      <div>
        <PageHeader title={t("je.title")} description={t("je.description")}
          breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.journalEntries") }]}
          actions={<button className="btn-primary" onClick={openCreate}><Plus size={16} /> {t("je.newJe")}</button>} />
        <div className="card overflow-hidden">
          <DataGrid<JeRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading}
            onRowClicked={onRowClicked} />
        </div>
      </div>
    );
  }

  /* ── CREATE ──────────────────────────────────────── */
  if (mode === "create") {
    return (
      <div>
        <PageHeader title={t("je.newJe")}
          breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.journalEntries") }, { label: t("common.create") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />

        <div className="card p-6 mb-6">
          <h3 className="text-base font-semibold text-slate-900 mb-4">{t("common.basicInfo")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="label">{t("je.companyCode")}</label>
              <input className="input" value={form.companyCode} onChange={e => setForm(p => ({ ...p, companyCode: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t("je.postingDate")}</label>
              <input className="input" type="date" value={form.postingDate} onChange={e => setForm(p => ({ ...p, postingDate: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t("je.entryType")}</label>
              <select className="input" value={form.entryType} onChange={e => setForm(p => ({ ...p, entryType: e.target.value }))}>
                {ENTRY_TYPES.map(et => <option key={et} value={et}>{et}</option>)}
              </select>
            </div>
            <div>
              <label className="label">{t("je.refDoc")}</label>
              <input className="input" value={form.referenceDocNo} onChange={e => setForm(p => ({ ...p, referenceDocNo: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t("je.currencyCode")}</label>
              <input className="input" value={form.currencyCode} onChange={e => setForm(p => ({ ...p, currencyCode: e.target.value }))} />
            </div>
            <div className="md:col-span-3">
              <label className="label">{t("je.description_")}</label>
              <textarea className="input" rows={2} value={form.description} onChange={e => setForm(p => ({ ...p, description: e.target.value }))} />
            </div>
          </div>
        </div>

        {/* lines */}
        <div className="card p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-base font-semibold text-slate-900">{t("common.lines")}</h3>
            <button className="btn-ghost text-sm" onClick={() => setLines(p => [...p, emptyLine()])}><Plus size={14} /> {t("common.addLine")}</button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-left text-slate-500">
                  <th className="pb-2 pr-2">{t("je.accountCode")}</th>
                  <th className="pb-2 pr-2">{t("je.accountName")}</th>
                  <th className="pb-2 pr-2">{t("je.debit")}</th>
                  <th className="pb-2 pr-2">{t("je.credit")}</th>
                  <th className="pb-2 pr-2">{t("je.costCenter")}</th>
                  <th className="pb-2 pr-2">{t("je.lineDescription")}</th>
                  <th className="pb-2 w-10"></th>
                </tr>
              </thead>
              <tbody>
                {lines.map((line, idx) => (
                  <tr key={idx} className="border-b border-slate-100">
                    <td className="py-2 pr-2"><input className="input input-sm" value={line.accountCode} onChange={e => updateLine(idx, "accountCode", e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm" value={line.accountName} onChange={e => updateLine(idx, "accountName", e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm w-28" type="number" value={line.debitAmount} onChange={e => updateLine(idx, "debitAmount", +e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm w-28" type="number" value={line.creditAmount} onChange={e => updateLine(idx, "creditAmount", +e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm" value={line.costCenter} onChange={e => updateLine(idx, "costCenter", e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm" value={line.description} onChange={e => updateLine(idx, "description", e.target.value)} /></td>
                    <td className="py-2">
                      {lines.length > 2 && (
                        <button className="text-slate-400 hover:text-red-500" onClick={() => setLines(p => p.filter((_, i) => i !== idx))}><Trash2 size={14} /></button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* running totals */}
          <div className="flex justify-end mt-4 pt-4 border-t border-slate-100 text-sm space-x-6">
            <span className="text-slate-500">{t("je.debit")}: <strong>{fmt(totalDebit)}</strong></span>
            <span className="text-slate-500">{t("je.credit")}: <strong>{fmt(totalCredit)}</strong></span>
            <span className={isBalanced ? "text-emerald-600 font-semibold" : "text-red-600 font-semibold"}>
              {t("je.balanced")}: {isBalanced ? t("common.yes") : t("common.no")}
            </span>
          </div>
        </div>

        <div className="flex justify-end gap-3">
          <button className="btn-ghost" onClick={() => setMode("list")}>{t("common.cancel")}</button>
          <button className="btn-primary" onClick={handleSave} disabled={createMut.isPending}>
            {createMut.isPending ? t("common.saving") : t("common.save")}
          </button>
        </div>
      </div>
    );
  }

  /* ── DETAIL ──────────────────────────────────────── */
  const detailStatus = detail?.status || selected?.status;

  return (
    <div>
      <PageHeader title={detail?.documentNo || selected?.documentNo || ""}
        breadcrumbs={[{ label: t("nav.finance") }, { label: t("nav.journalEntries") }, { label: t("common.detail") }]}
        actions={
          <div className="flex gap-2">
            <button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>
            {detailStatus === "DRAFT" && (
              <button className="btn-primary" onClick={handlePost} disabled={postMut.isPending}>
                {t("je.post")}
              </button>
            )}
            {detailStatus === "POSTED" && (
              <button className="btn-danger" onClick={() => reverseMut.mutate(selected!.id)} disabled={reverseMut.isPending}>
                {t("je.reverse")}
              </button>
            )}
          </div>
        } />

      {detailQuery.isLoading ? (
        <div className="card p-12 text-center text-slate-400">{t("common.loading")}</div>
      ) : detail ? (
        <>
          <div className="card p-6 mb-6">
            <h3 className="text-base font-semibold text-slate-900 mb-4">{t("common.basicInfo")}</h3>
            <dl className="grid grid-cols-2 md:grid-cols-4 gap-y-4 gap-x-6 text-sm">
              <div><dt className="text-slate-500">{t("common.status")}</dt><dd><span className={statusStyle[detail.status] || "badge"}>{t("status." + detail.status, detail.status)}</span></dd></div>
              <div><dt className="text-slate-500">{t("je.companyCode")}</dt><dd className="font-medium">{detail.companyCode}</dd></div>
              <div><dt className="text-slate-500">{t("je.postingDate")}</dt><dd className="font-medium">{detail.postingDate}</dd></div>
              <div><dt className="text-slate-500">{t("je.entryType")}</dt><dd className="font-medium">{detail.entryType}</dd></div>
              <div><dt className="text-slate-500">{t("je.refDoc")}</dt><dd className="font-medium">{detail.referenceDocNo || "-"}</dd></div>
              <div><dt className="text-slate-500">{t("je.currencyCode")}</dt><dd className="font-medium">{detail.currencyCode}</dd></div>
              <div className="md:col-span-2"><dt className="text-slate-500">{t("je.description_")}</dt><dd className="font-medium">{detail.description || "-"}</dd></div>
            </dl>
          </div>

          <div className="card p-6">
            <h3 className="text-base font-semibold text-slate-900 mb-4">{t("common.lines")}</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-slate-500">
                    <th className="pb-2 pr-2">#</th>
                    <th className="pb-2 pr-2">{t("je.accountCode")}</th>
                    <th className="pb-2 pr-2">{t("je.accountName")}</th>
                    <th className="pb-2 pr-2 text-right">{t("je.debit")}</th>
                    <th className="pb-2 pr-2 text-right">{t("je.credit")}</th>
                    <th className="pb-2 pr-2">{t("je.costCenter")}</th>
                    <th className="pb-2">{t("je.lineDescription")}</th>
                  </tr>
                </thead>
                <tbody>
                  {detail.lines?.map((l: JeLine, idx: number) => (
                    <tr key={idx} className="border-b border-slate-100">
                      <td className="py-2 pr-2 text-slate-400">{idx + 1}</td>
                      <td className="py-2 pr-2 font-mono">{l.accountCode}</td>
                      <td className="py-2 pr-2">{l.accountName}</td>
                      <td className="py-2 pr-2 text-right">{fmt(l.debitAmount)}</td>
                      <td className="py-2 pr-2 text-right">{fmt(l.creditAmount)}</td>
                      <td className="py-2 pr-2">{l.costCenter || "-"}</td>
                      <td className="py-2">{l.description || "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-end mt-4 pt-4 border-t border-slate-100 text-sm space-x-6">
              <span className="text-slate-500">{t("je.debit")}: <strong>{fmt(detail.totalDebit)}</strong></span>
              <span className="text-slate-500">{t("je.credit")}: <strong>{fmt(detail.totalCredit)}</strong></span>
              <span className={detail.isBalanced ? "text-emerald-600 font-semibold" : "text-red-600 font-semibold"}>
                {t("je.balanced")}: {detail.isBalanced ? t("common.yes") : t("common.no")}
              </span>
            </div>
          </div>
        </>
      ) : (
        <div className="card p-12 text-center text-slate-400">{t("common.noData")}</div>
      )}
    </div>
  );
}
