import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Trash2 } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface PoLine {
  id?: number;
  lineNo?: number;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  taxRate: number;
}

interface PoRow {
  id: number;
  documentNo: string;
  companyCode: string;
  plantCode: string;
  vendorCode: string;
  vendorName: string;
  orderDate: string;
  deliveryDate: string | null;
  status: string;
  currencyCode: string;
  paymentTerms: string | null;
  remark: string | null;
  grandTotal: number;
  lines?: PoLine[];
}

interface PoForm {
  companyCode: string;
  plantCode: string;
  vendorCode: string;
  vendorName: string;
  deliveryDate: string;
  currencyCode: string;
  paymentTerms: string;
  remark: string;
  lines: PoLine[];
}

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  SUBMITTED: "badge-info",
  APPROVED: "badge-success",
  REJECTED: "badge-danger",
  SENT: "badge bg-purple-50 text-purple-700 ring-1 ring-inset ring-purple-600/20",
  COMPLETED: "badge bg-slate-100 text-slate-500",
};

const emptyLine = (): PoLine => ({
  itemCode: "",
  itemName: "",
  quantity: 0,
  unitOfMeasure: "EA",
  unitPrice: 0,
  taxRate: 10,
});

const emptyForm = (): PoForm => ({
  companyCode: "",
  plantCode: "",
  vendorCode: "",
  vendorName: "",
  deliveryDate: "",
  currencyCode: "KRW",
  paymentTerms: "",
  remark: "",
  lines: [emptyLine()],
});

export default function PurchaseOrderPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [mode, setMode] = useState<"list" | "create" | "detail">("list");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [form, setForm] = useState<PoForm>(emptyForm());

  // ── Queries ──
  const { data, isLoading } = useQuery({
    queryKey: ["purchase-orders"],
    queryFn: async () => (await api.get("/api/v1/purchase/orders?size=100")).data,
  });

  const { data: detailData, isLoading: detailLoading } = useQuery({
    queryKey: ["purchase-order", selectedId],
    queryFn: async () => (await api.get(`/api/v1/purchase/orders/${selectedId}`)).data,
    enabled: mode === "detail" && selectedId !== null,
  });

  const detail: PoRow | undefined = detailData?.data;

  // ── Mutations ──
  const createMutation = useMutation({
    mutationFn: (payload: PoForm) => api.post("/api/v1/purchase/orders", payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-orders"] });
      setMode("list");
    },
  });

  const submitMutation = useMutation({
    mutationFn: (id: number) => api.post(`/api/v1/purchase/orders/${id}/submit`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-orders"] });
      queryClient.invalidateQueries({ queryKey: ["purchase-order", selectedId] });
    },
  });

  const approveMutation = useMutation({
    mutationFn: (id: number) => api.post(`/api/v1/purchase/orders/${id}/approve`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-orders"] });
      queryClient.invalidateQueries({ queryKey: ["purchase-order", selectedId] });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: (id: number) => api.post(`/api/v1/purchase/orders/${id}/reject`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-orders"] });
      queryClient.invalidateQueries({ queryKey: ["purchase-order", selectedId] });
    },
  });

  const sendMutation = useMutation({
    mutationFn: (id: number) => api.post(`/api/v1/purchase/orders/${id}/send`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-orders"] });
      queryClient.invalidateQueries({ queryKey: ["purchase-order", selectedId] });
    },
  });

  // ── Handlers ──
  const openCreate = () => {
    setForm(emptyForm());
    setMode("create");
  };

  const openDetail = useCallback((row: PoRow) => {
    setSelectedId(row.id);
    setMode("detail");
  }, []);

  const backToList = () => {
    setMode("list");
    setSelectedId(null);
  };

  const updateForm = (field: keyof PoForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const updateLine = (idx: number, field: keyof PoLine, value: string | number) => {
    setForm((prev) => {
      const lines = [...prev.lines];
      lines[idx] = { ...lines[idx], [field]: value };
      return { ...prev, lines };
    });
  };

  const addLine = () => {
    setForm((prev) => ({ ...prev, lines: [...prev.lines, emptyLine()] }));
  };

  const removeLine = (idx: number) => {
    setForm((prev) => ({ ...prev, lines: prev.lines.filter((_, i) => i !== idx) }));
  };

  const handleSave = () => {
    createMutation.mutate(form);
  };

  const fmtCurrency = (v: number) =>
    v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

  const calcLineAmount = (line: PoLine) => line.quantity * line.unitPrice;
  const calcLineTax = (line: PoLine) => calcLineAmount(line) * (line.taxRate / 100);

  // ── Column Defs ──
  const columnDefs = useMemo<ColDef<PoRow>[]>(
    () => [
      {
        field: "documentNo",
        headerName: t("po.docNo"),
        flex: 1.2,
        cellRenderer: (p: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{p.value}</span>
        ),
      },
      { field: "vendorName", headerName: t("po.vendor"), flex: 2 },
      { field: "orderDate", headerName: t("po.orderDate"), flex: 1 },
      { field: "deliveryDate", headerName: t("po.deliveryDate"), flex: 1 },
      {
        field: "grandTotal",
        headerName: t("po.grandTotal"),
        flex: 1.2,
        type: "numericColumn",
        valueFormatter: (p: { value: number }) => fmtCurrency(p.value),
      },
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

  // ── LIST VIEW ──
  if (mode === "list") {
    return (
      <div>
        <PageHeader
          title={t("po.title")}
          description={t("po.description")}
          breadcrumbs={[{ label: t("nav.procurement") }, { label: t("nav.purchaseOrders") }]}
          actions={
            <button className="btn-primary" onClick={openCreate}>
              <Plus size={16} /> {t("po.newPo")}
            </button>
          }
        />
        <div className="card overflow-hidden">
          <DataGrid<PoRow>
            rowData={data?.data || []}
            columnDefs={columnDefs}
            loading={isLoading}
            onRowClicked={openDetail}
          />
        </div>
      </div>
    );
  }

  // ── CREATE VIEW ──
  if (mode === "create") {
    const subtotal = form.lines.reduce((s, l) => s + calcLineAmount(l), 0);
    const taxTotal = form.lines.reduce((s, l) => s + calcLineTax(l), 0);
    const grandTotal = subtotal + taxTotal;

    return (
      <div>
        <PageHeader
          title={t("po.newPo")}
          breadcrumbs={[
            { label: t("nav.procurement") },
            { label: t("nav.purchaseOrders") },
            { label: t("po.newPo") },
          ]}
          actions={
            <button className="btn-secondary" onClick={backToList}>
              <ArrowLeft size={16} /> {t("common.back")}
            </button>
          }
        />

        <div className="card p-6 space-y-6">
          {/* Header fields */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">{t("nav.companies")}</label>
              <input
                className="input"
                value={form.companyCode}
                onChange={(e) => updateForm("companyCode", e.target.value)}
                placeholder="Company Code"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">{t("gr.plant")}</label>
              <input
                className="input"
                value={form.plantCode}
                onChange={(e) => updateForm("plantCode", e.target.value)}
                placeholder="Plant Code"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">공급업체 코드</label>
              <input
                className="input"
                value={form.vendorCode}
                onChange={(e) => updateForm("vendorCode", e.target.value)}
                placeholder="Vendor Code"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">공급업체명</label>
              <input
                className="input"
                value={form.vendorName}
                onChange={(e) => updateForm("vendorName", e.target.value)}
                placeholder="Vendor Name"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">{t("po.deliveryDate")}</label>
              <input
                className="input"
                type="date"
                value={form.deliveryDate}
                onChange={(e) => updateForm("deliveryDate", e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">통화</label>
              <input
                className="input"
                value={form.currencyCode}
                onChange={(e) => updateForm("currencyCode", e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">결제조건</label>
              <input
                className="input"
                value={form.paymentTerms}
                onChange={(e) => updateForm("paymentTerms", e.target.value)}
                placeholder="e.g. NET30"
              />
            </div>
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-slate-700 mb-1">비고</label>
              <input
                className="input"
                value={form.remark}
                onChange={(e) => updateForm("remark", e.target.value)}
              />
            </div>
          </div>

          {/* Line items */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-semibold text-slate-700">품목 목록</h3>
              <button className="btn-secondary text-sm" onClick={addLine}>
                <Plus size={14} /> 행 추가
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-slate-500">
                    <th className="pb-2 pr-2">#</th>
                    <th className="pb-2 pr-2">품목코드</th>
                    <th className="pb-2 pr-2">품목명</th>
                    <th className="pb-2 pr-2">수량</th>
                    <th className="pb-2 pr-2">단위</th>
                    <th className="pb-2 pr-2">단가</th>
                    <th className="pb-2 pr-2">금액</th>
                    <th className="pb-2 pr-2">세율(%)</th>
                    <th className="pb-2 pr-2">세액</th>
                    <th className="pb-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {form.lines.map((line, idx) => (
                    <tr key={idx} className="border-b border-slate-100">
                      <td className="py-2 pr-2 text-slate-400">{idx + 1}</td>
                      <td className="py-2 pr-2">
                        <input
                          className="input py-1 px-2 w-24"
                          value={line.itemCode}
                          onChange={(e) => updateLine(idx, "itemCode", e.target.value)}
                        />
                      </td>
                      <td className="py-2 pr-2">
                        <input
                          className="input py-1 px-2 w-32"
                          value={line.itemName}
                          onChange={(e) => updateLine(idx, "itemName", e.target.value)}
                        />
                      </td>
                      <td className="py-2 pr-2">
                        <input
                          className="input py-1 px-2 w-20 text-right"
                          type="number"
                          value={line.quantity}
                          onChange={(e) => updateLine(idx, "quantity", Number(e.target.value))}
                        />
                      </td>
                      <td className="py-2 pr-2">
                        <input
                          className="input py-1 px-2 w-16"
                          value={line.unitOfMeasure}
                          onChange={(e) => updateLine(idx, "unitOfMeasure", e.target.value)}
                        />
                      </td>
                      <td className="py-2 pr-2">
                        <input
                          className="input py-1 px-2 w-24 text-right"
                          type="number"
                          value={line.unitPrice}
                          onChange={(e) => updateLine(idx, "unitPrice", Number(e.target.value))}
                        />
                      </td>
                      <td className="py-2 pr-2 text-right font-medium">
                        {calcLineAmount(line).toLocaleString("ko-KR")}
                      </td>
                      <td className="py-2 pr-2">
                        <input
                          className="input py-1 px-2 w-16 text-right"
                          type="number"
                          value={line.taxRate}
                          onChange={(e) => updateLine(idx, "taxRate", Number(e.target.value))}
                        />
                      </td>
                      <td className="py-2 pr-2 text-right">
                        {calcLineTax(line).toLocaleString("ko-KR")}
                      </td>
                      <td className="py-2">
                        {form.lines.length > 1 && (
                          <button
                            className="text-red-400 hover:text-red-600"
                            onClick={() => removeLine(idx)}
                          >
                            <Trash2 size={14} />
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="text-sm">
                    <td colSpan={6} className="pt-3 text-right text-slate-500">공급가액</td>
                    <td className="pt-3 text-right font-medium">{subtotal.toLocaleString("ko-KR")}</td>
                    <td className="pt-3 text-right text-slate-500">세액</td>
                    <td className="pt-3 text-right font-medium">{taxTotal.toLocaleString("ko-KR")}</td>
                    <td></td>
                  </tr>
                  <tr className="text-sm font-semibold">
                    <td colSpan={6} className="pt-1 text-right text-slate-700">합계 (세금포함)</td>
                    <td colSpan={3} className="pt-1 text-right">{fmtCurrency(grandTotal)}</td>
                    <td></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <button className="btn-secondary" onClick={backToList}>
              {t("common.cancel")}
            </button>
            <button
              className="btn-primary"
              onClick={handleSave}
              disabled={createMutation.isPending}
            >
              {createMutation.isPending ? t("common.saving") : t("common.save")}
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ── DETAIL VIEW ──
  if (mode === "detail") {
    if (detailLoading || !detail) {
      return (
        <div className="flex items-center justify-center h-64 text-slate-400">
          {t("common.loading")}
        </div>
      );
    }

    const lines: PoLine[] = detail.lines || [];
    const subtotal = lines.reduce((s, l) => s + (l.quantity || 0) * (l.unitPrice || 0), 0);
    const taxTotal = lines.reduce(
      (s, l) => s + (l.quantity || 0) * (l.unitPrice || 0) * ((l.taxRate || 0) / 100),
      0
    );
    const grandTotal = subtotal + taxTotal;

    return (
      <div>
        <PageHeader
          title={`${t("po.docNo")}: ${detail.documentNo}`}
          breadcrumbs={[
            { label: t("nav.procurement") },
            { label: t("nav.purchaseOrders") },
            { label: detail.documentNo },
          ]}
          actions={
            <button className="btn-secondary" onClick={backToList}>
              <ArrowLeft size={16} /> {t("common.back")}
            </button>
          }
        />

        <div className="card p-6 space-y-6">
          {/* Header info */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <span className="text-slate-500">{t("common.status")}</span>
              <div className="mt-1">
                <span className={statusStyle[detail.status] || "badge"}>
                  {t("status." + detail.status, detail.status)}
                </span>
              </div>
            </div>
            <div>
              <span className="text-slate-500">{t("po.vendor")}</span>
              <div className="mt-1 font-medium">
                {detail.vendorName} ({detail.vendorCode})
              </div>
            </div>
            <div>
              <span className="text-slate-500">{t("nav.companies")}</span>
              <div className="mt-1 font-medium">{detail.companyCode}</div>
            </div>
            <div>
              <span className="text-slate-500">{t("gr.plant")}</span>
              <div className="mt-1 font-medium">{detail.plantCode}</div>
            </div>
            <div>
              <span className="text-slate-500">{t("po.orderDate")}</span>
              <div className="mt-1 font-medium">{detail.orderDate}</div>
            </div>
            <div>
              <span className="text-slate-500">{t("po.deliveryDate")}</span>
              <div className="mt-1 font-medium">{detail.deliveryDate || "-"}</div>
            </div>
            <div>
              <span className="text-slate-500">통화</span>
              <div className="mt-1 font-medium">{detail.currencyCode}</div>
            </div>
            <div>
              <span className="text-slate-500">결제조건</span>
              <div className="mt-1 font-medium">{detail.paymentTerms || "-"}</div>
            </div>
          </div>

          {detail.remark && (
            <div className="text-sm">
              <span className="text-slate-500">비고</span>
              <div className="mt-1">{detail.remark}</div>
            </div>
          )}

          {/* Line items */}
          <div>
            <h3 className="text-sm font-semibold text-slate-700 mb-3">품목 목록</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-slate-500">
                    <th className="pb-2 pr-2">#</th>
                    <th className="pb-2 pr-2">품목코드</th>
                    <th className="pb-2 pr-2">품목명</th>
                    <th className="pb-2 pr-2 text-right">수량</th>
                    <th className="pb-2 pr-2">단위</th>
                    <th className="pb-2 pr-2 text-right">단가</th>
                    <th className="pb-2 pr-2 text-right">금액</th>
                    <th className="pb-2 pr-2 text-right">세율(%)</th>
                    <th className="pb-2 text-right">세액</th>
                  </tr>
                </thead>
                <tbody>
                  {lines.map((line, idx) => {
                    const amt = (line.quantity || 0) * (line.unitPrice || 0);
                    const tax = amt * ((line.taxRate || 0) / 100);
                    return (
                      <tr key={line.id || idx} className="border-b border-slate-100">
                        <td className="py-2 pr-2 text-slate-400">{idx + 1}</td>
                        <td className="py-2 pr-2 font-mono">{line.itemCode}</td>
                        <td className="py-2 pr-2">{line.itemName}</td>
                        <td className="py-2 pr-2 text-right">{line.quantity?.toLocaleString("ko-KR")}</td>
                        <td className="py-2 pr-2">{line.unitOfMeasure}</td>
                        <td className="py-2 pr-2 text-right">{line.unitPrice?.toLocaleString("ko-KR")}</td>
                        <td className="py-2 pr-2 text-right font-medium">{amt.toLocaleString("ko-KR")}</td>
                        <td className="py-2 pr-2 text-right">{line.taxRate}%</td>
                        <td className="py-2 text-right">{tax.toLocaleString("ko-KR")}</td>
                      </tr>
                    );
                  })}
                </tbody>
                <tfoot>
                  <tr className="text-sm">
                    <td colSpan={6} className="pt-3 text-right text-slate-500">공급가액</td>
                    <td className="pt-3 text-right font-medium">{subtotal.toLocaleString("ko-KR")}</td>
                    <td className="pt-3 text-right text-slate-500">세액</td>
                    <td className="pt-3 text-right font-medium">{taxTotal.toLocaleString("ko-KR")}</td>
                  </tr>
                  <tr className="text-sm font-semibold">
                    <td colSpan={6} className="pt-1 text-right text-slate-700">{t("po.grandTotal")}</td>
                    <td colSpan={3} className="pt-1 text-right text-lg">{fmtCurrency(grandTotal)}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>

          {/* Action buttons based on status */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            {detail.status === "DRAFT" && (
              <button
                className="btn-primary"
                onClick={() => submitMutation.mutate(detail.id)}
                disabled={submitMutation.isPending}
              >
                {t("common.submit")}
              </button>
            )}
            {detail.status === "SUBMITTED" && (
              <>
                <button
                  className="btn-danger"
                  onClick={() => rejectMutation.mutate(detail.id)}
                  disabled={rejectMutation.isPending}
                >
                  {t("common.reject")}
                </button>
                <button
                  className="btn-primary"
                  onClick={() => approveMutation.mutate(detail.id)}
                  disabled={approveMutation.isPending}
                >
                  {t("common.approve")}
                </button>
              </>
            )}
            {detail.status === "APPROVED" && (
              <button
                className="btn-primary"
                onClick={() => sendMutation.mutate(detail.id)}
                disabled={sendMutation.isPending}
              >
                {t("status.SENT", "발송")}
              </button>
            )}
          </div>
        </div>
      </div>
    );
  }

  return null;
}
