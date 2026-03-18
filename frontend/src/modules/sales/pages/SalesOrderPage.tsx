import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Trash2 } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

/* ── types ─────────────────────────────────────────── */
interface SoLine {
  id?: number;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  taxRate: number;
  specification: string;
}

interface SoRow {
  id: number;
  documentNo: string;
  companyCode: string;
  plantCode: string;
  customerCode: string;
  customerName: string;
  orderDate: string;
  deliveryDate: string | null;
  currencyCode: string;
  paymentTerms: string | null;
  shippingAddress: string | null;
  remark: string | null;
  status: string;
  totalAmount: number;
  taxAmount: number;
  grandTotal: number;
  lines: SoLine[];
}

type Mode = "list" | "create" | "detail";

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  SUBMITTED: "badge-info",
  APPROVED: "badge-success",
  REJECTED: "badge-danger",
  SHIPPED: "badge bg-purple-50 text-purple-700 ring-1 ring-inset ring-purple-600/20",
  COMPLETED: "badge bg-slate-100 text-slate-500",
};

const emptyLine = (): SoLine => ({
  itemCode: "", itemName: "", quantity: 1, unitOfMeasure: "EA", unitPrice: 0, taxRate: 10, specification: "",
});

const fmt = (v: number) =>
  v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

/* ── component ─────────────────────────────────────── */
export default function SalesOrderPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [mode, setMode] = useState<Mode>("list");
  const [selected, setSelected] = useState<SoRow | null>(null);

  /* form state */
  const [form, setForm] = useState({
    companyCode: "", plantCode: "", customerCode: "", customerName: "",
    deliveryDate: "", currencyCode: "KRW", paymentTerms: "", shippingAddress: "", remark: "",
  });
  const [lines, setLines] = useState<SoLine[]>([emptyLine()]);

  /* queries */
  const { data, isLoading } = useQuery({
    queryKey: ["sales-orders"],
    queryFn: async () => (await api.get("/api/v1/sales/orders?size=100")).data,
  });

  const detailQuery = useQuery({
    queryKey: ["sales-order", selected?.id],
    queryFn: async () => (await api.get(`/api/v1/sales/orders/${selected!.id}`)).data,
    enabled: mode === "detail" && !!selected?.id,
  });

  /* mutations */
  const createMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/sales/orders", body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["sales-orders"] }); setMode("list"); },
  });

  const submitMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/sales/orders/${id}/submit`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["sales-orders"] }); qc.invalidateQueries({ queryKey: ["sales-order", selected?.id] }); },
  });

  const approveMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/sales/orders/${id}/approve`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["sales-orders"] }); qc.invalidateQueries({ queryKey: ["sales-order", selected?.id] }); },
  });

  const rejectMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/sales/orders/${id}/reject`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["sales-orders"] }); qc.invalidateQueries({ queryKey: ["sales-order", selected?.id] }); },
  });

  /* column defs */
  const columnDefs = useMemo<ColDef<SoRow>[]>(() => [
    { field: "documentNo", headerName: t("so.docNo"), flex: 1.2,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "customerName", headerName: t("so.customer"), flex: 2 },
    { field: "orderDate", headerName: t("so.orderDate"), flex: 1 },
    { field: "deliveryDate", headerName: t("so.deliveryDate"), flex: 1 },
    { field: "grandTotal", headerName: t("so.grandTotal"), flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  /* handlers */
  const onRowClicked = useCallback((data: SoRow) => {
    setSelected(data); setMode("detail");
  }, []);

  const openCreate = () => {
    setForm({ companyCode: "", plantCode: "", customerCode: "", customerName: "",
      deliveryDate: "", currencyCode: "KRW", paymentTerms: "", shippingAddress: "", remark: "" });
    setLines([emptyLine()]);
    setMode("create");
  };

  const updateLine = (idx: number, field: keyof SoLine, value: string | number) => {
    setLines(prev => prev.map((l, i) => i === idx ? { ...l, [field]: value } : l));
  };

  const handleSave = () => {
    createMut.mutate({ ...form, lines });
  };

  const detail: SoRow | undefined = detailQuery.data?.data;

  /* ── LIST ─────────────────────────────────────────── */
  if (mode === "list") {
    return (
      <div>
        <PageHeader title={t("so.title")} description={t("so.description")}
          breadcrumbs={[{ label: t("nav.sales") }, { label: t("nav.salesOrders") }]}
          actions={<button className="btn-primary" onClick={openCreate}><Plus size={16} /> {t("so.newSo")}</button>} />
        <div className="card overflow-hidden">
          <DataGrid<SoRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading}
            onRowClicked={onRowClicked} />
        </div>
      </div>
    );
  }

  /* ── CREATE ──────────────────────────────────────── */
  if (mode === "create") {
    const totals = lines.reduce((acc, l) => {
      const amt = l.quantity * l.unitPrice;
      const tax = amt * (l.taxRate / 100);
      return { totalAmount: acc.totalAmount + amt, taxAmount: acc.taxAmount + tax };
    }, { totalAmount: 0, taxAmount: 0 });

    return (
      <div>
        <PageHeader title={t("so.newSo")}
          breadcrumbs={[{ label: t("nav.sales") }, { label: t("nav.salesOrders") }, { label: t("common.create") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />

        <div className="card p-6 mb-6">
          <h3 className="text-base font-semibold text-slate-900 mb-4">{t("common.basicInfo")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="label">{t("so.companyCode")}</label>
              <input className="input" value={form.companyCode} onChange={e => setForm(p => ({ ...p, companyCode: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t("so.plantCode")}</label>
              <input className="input" value={form.plantCode} onChange={e => setForm(p => ({ ...p, plantCode: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t("so.customerCode")}</label>
              <input className="input" value={form.customerCode} onChange={e => setForm(p => ({ ...p, customerCode: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t("so.customerName")}</label>
              <input className="input" value={form.customerName} onChange={e => setForm(p => ({ ...p, customerName: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t("so.deliveryDate")}</label>
              <input className="input" type="date" value={form.deliveryDate} onChange={e => setForm(p => ({ ...p, deliveryDate: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t("so.currencyCode")}</label>
              <input className="input" value={form.currencyCode} onChange={e => setForm(p => ({ ...p, currencyCode: e.target.value }))} />
            </div>
            <div>
              <label className="label">{t("so.paymentTerms")}</label>
              <input className="input" value={form.paymentTerms} onChange={e => setForm(p => ({ ...p, paymentTerms: e.target.value }))} />
            </div>
            <div className="md:col-span-2">
              <label className="label">{t("so.shippingAddress")}</label>
              <input className="input" value={form.shippingAddress} onChange={e => setForm(p => ({ ...p, shippingAddress: e.target.value }))} />
            </div>
            <div className="md:col-span-3">
              <label className="label">{t("so.remark")}</label>
              <textarea className="input" rows={2} value={form.remark} onChange={e => setForm(p => ({ ...p, remark: e.target.value }))} />
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
                  <th className="pb-2 pr-2">{t("so.itemCode")}</th>
                  <th className="pb-2 pr-2">{t("so.itemName")}</th>
                  <th className="pb-2 pr-2">{t("so.quantity")}</th>
                  <th className="pb-2 pr-2">{t("so.uom")}</th>
                  <th className="pb-2 pr-2">{t("so.unitPrice")}</th>
                  <th className="pb-2 pr-2">{t("so.taxRate")}</th>
                  <th className="pb-2 pr-2">{t("so.specification")}</th>
                  <th className="pb-2 w-10"></th>
                </tr>
              </thead>
              <tbody>
                {lines.map((line, idx) => (
                  <tr key={idx} className="border-b border-slate-100">
                    <td className="py-2 pr-2"><input className="input input-sm" value={line.itemCode} onChange={e => updateLine(idx, "itemCode", e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm" value={line.itemName} onChange={e => updateLine(idx, "itemName", e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm w-20" type="number" value={line.quantity} onChange={e => updateLine(idx, "quantity", +e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm w-16" value={line.unitOfMeasure} onChange={e => updateLine(idx, "unitOfMeasure", e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm w-28" type="number" value={line.unitPrice} onChange={e => updateLine(idx, "unitPrice", +e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm w-16" type="number" value={line.taxRate} onChange={e => updateLine(idx, "taxRate", +e.target.value)} /></td>
                    <td className="py-2 pr-2"><input className="input input-sm" value={line.specification} onChange={e => updateLine(idx, "specification", e.target.value)} /></td>
                    <td className="py-2">
                      {lines.length > 1 && (
                        <button className="text-slate-400 hover:text-red-500" onClick={() => setLines(p => p.filter((_, i) => i !== idx))}><Trash2 size={14} /></button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* totals */}
          <div className="flex justify-end mt-4 text-sm space-x-6">
            <span className="text-slate-500">{t("so.totalAmount")}: <strong>{fmt(totals.totalAmount)}</strong></span>
            <span className="text-slate-500">{t("so.taxAmount")}: <strong>{fmt(totals.taxAmount)}</strong></span>
            <span className="text-slate-900 font-semibold">{t("so.grandTotal")}: {fmt(totals.totalAmount + totals.taxAmount)}</span>
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
  return (
    <div>
      <PageHeader title={detail?.documentNo || selected?.documentNo || ""}
        breadcrumbs={[{ label: t("nav.sales") }, { label: t("nav.salesOrders") }, { label: t("common.detail") }]}
        actions={
          <div className="flex gap-2">
            <button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>
            {(detail?.status || selected?.status) === "DRAFT" && (
              <button className="btn-primary" onClick={() => submitMut.mutate(selected!.id)} disabled={submitMut.isPending}>
                {t("so.submit")}
              </button>
            )}
            {(detail?.status || selected?.status) === "SUBMITTED" && (
              <>
                <button className="btn-primary" onClick={() => approveMut.mutate(selected!.id)} disabled={approveMut.isPending}>
                  {t("so.approve")}
                </button>
                <button className="btn-danger" onClick={() => rejectMut.mutate(selected!.id)} disabled={rejectMut.isPending}>
                  {t("so.reject")}
                </button>
              </>
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
              <div><dt className="text-slate-500">{t("so.companyCode")}</dt><dd className="font-medium">{detail.companyCode}</dd></div>
              <div><dt className="text-slate-500">{t("so.plantCode")}</dt><dd className="font-medium">{detail.plantCode}</dd></div>
              <div><dt className="text-slate-500">{t("so.customerCode")}</dt><dd className="font-medium">{detail.customerCode}</dd></div>
              <div><dt className="text-slate-500">{t("so.customer")}</dt><dd className="font-medium">{detail.customerName}</dd></div>
              <div><dt className="text-slate-500">{t("so.orderDate")}</dt><dd className="font-medium">{detail.orderDate}</dd></div>
              <div><dt className="text-slate-500">{t("so.deliveryDate")}</dt><dd className="font-medium">{detail.deliveryDate || "-"}</dd></div>
              <div><dt className="text-slate-500">{t("so.currencyCode")}</dt><dd className="font-medium">{detail.currencyCode}</dd></div>
              <div><dt className="text-slate-500">{t("so.paymentTerms")}</dt><dd className="font-medium">{detail.paymentTerms || "-"}</dd></div>
              <div className="md:col-span-2"><dt className="text-slate-500">{t("so.shippingAddress")}</dt><dd className="font-medium">{detail.shippingAddress || "-"}</dd></div>
              <div className="md:col-span-4"><dt className="text-slate-500">{t("so.remark")}</dt><dd className="font-medium">{detail.remark || "-"}</dd></div>
            </dl>
          </div>

          <div className="card p-6">
            <h3 className="text-base font-semibold text-slate-900 mb-4">{t("common.lines")}</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-slate-500">
                    <th className="pb-2 pr-2">#</th>
                    <th className="pb-2 pr-2">{t("so.itemCode")}</th>
                    <th className="pb-2 pr-2">{t("so.itemName")}</th>
                    <th className="pb-2 pr-2 text-right">{t("so.quantity")}</th>
                    <th className="pb-2 pr-2">{t("so.uom")}</th>
                    <th className="pb-2 pr-2 text-right">{t("so.unitPrice")}</th>
                    <th className="pb-2 pr-2 text-right">{t("so.taxRate")}</th>
                    <th className="pb-2 pr-2">{t("so.specification")}</th>
                    <th className="pb-2 text-right">{t("common.amount")}</th>
                  </tr>
                </thead>
                <tbody>
                  {detail.lines?.map((l: SoLine, idx: number) => (
                    <tr key={idx} className="border-b border-slate-100">
                      <td className="py-2 pr-2 text-slate-400">{idx + 1}</td>
                      <td className="py-2 pr-2 font-mono">{l.itemCode}</td>
                      <td className="py-2 pr-2">{l.itemName}</td>
                      <td className="py-2 pr-2 text-right">{l.quantity}</td>
                      <td className="py-2 pr-2">{l.unitOfMeasure}</td>
                      <td className="py-2 pr-2 text-right">{fmt(l.unitPrice)}</td>
                      <td className="py-2 pr-2 text-right">{l.taxRate}%</td>
                      <td className="py-2 pr-2">{l.specification || "-"}</td>
                      <td className="py-2 text-right font-medium">{fmt(l.quantity * l.unitPrice)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-end mt-4 pt-4 border-t border-slate-100 text-sm space-x-6">
              <span className="text-slate-500">{t("so.totalAmount")}: <strong>{fmt(detail.totalAmount)}</strong></span>
              <span className="text-slate-500">{t("so.taxAmount")}: <strong>{fmt(detail.taxAmount)}</strong></span>
              <span className="text-slate-900 font-bold text-base">{t("so.grandTotal")}: {fmt(detail.grandTotal)}</span>
            </div>
          </div>
        </>
      ) : (
        <div className="card p-12 text-center text-slate-400">{t("common.noData")}</div>
      )}
    </div>
  );
}
