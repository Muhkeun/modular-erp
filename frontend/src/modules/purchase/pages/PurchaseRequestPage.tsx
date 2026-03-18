import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, X, ArrowLeft, Trash2 } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface PrLine {
  id?: number;
  lineNo?: number;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  specification: string;
  remark: string;
}

interface PrRow {
  id: number;
  documentNo: string;
  companyCode: string;
  plantCode: string;
  requestDate: string;
  deliveryDate: string | null;
  status: string;
  prType: string;
  requestedBy: string | null;
  totalAmount: number;
  description: string | null;
  lines?: PrLine[];
}

interface PrForm {
  companyCode: string;
  plantCode: string;
  prType: string;
  deliveryDate: string;
  description: string;
  lines: PrLine[];
}

const statusStyle: Record<string, string> = {
  DRAFT: "badge bg-slate-100 text-slate-600",
  SUBMITTED: "badge-info",
  APPROVED: "badge-success",
  REJECTED: "badge-danger",
  CLOSED: "badge bg-slate-100 text-slate-500",
};

const PR_TYPES = ["STANDARD", "URGENT", "PROJECT", "INVESTMENT"] as const;

const emptyLine = (): PrLine => ({
  itemCode: "",
  itemName: "",
  quantity: 0,
  unitOfMeasure: "EA",
  unitPrice: 0,
  specification: "",
  remark: "",
});

const emptyForm = (): PrForm => ({
  companyCode: "",
  plantCode: "",
  prType: "STANDARD",
  deliveryDate: "",
  description: "",
  lines: [emptyLine()],
});

export default function PurchaseRequestPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [mode, setMode] = useState<"list" | "create" | "detail">("list");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [form, setForm] = useState<PrForm>(emptyForm());
  const [vendorDialog, setVendorDialog] = useState(false);
  const [vendorCode, setVendorCode] = useState("");
  const [vendorName, setVendorName] = useState("");

  // ── Queries ──
  const { data, isLoading } = useQuery({
    queryKey: ["purchase-requests"],
    queryFn: async () => (await api.get("/api/v1/purchase/requests?size=100")).data,
  });

  const { data: detailData, isLoading: detailLoading } = useQuery({
    queryKey: ["purchase-request", selectedId],
    queryFn: async () => (await api.get(`/api/v1/purchase/requests/${selectedId}`)).data,
    enabled: mode === "detail" && selectedId !== null,
  });

  const detail: PrRow | undefined = detailData?.data;

  // ── Mutations ──
  const createMutation = useMutation({
    mutationFn: (payload: PrForm) => api.post("/api/v1/purchase/requests", payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-requests"] });
      setMode("list");
    },
  });

  const submitMutation = useMutation({
    mutationFn: (id: number) => api.post(`/api/v1/purchase/requests/${id}/submit`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-requests"] });
      queryClient.invalidateQueries({ queryKey: ["purchase-request", selectedId] });
    },
  });

  const approveMutation = useMutation({
    mutationFn: (id: number) => api.post(`/api/v1/purchase/requests/${id}/approve`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-requests"] });
      queryClient.invalidateQueries({ queryKey: ["purchase-request", selectedId] });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: (id: number) => api.post(`/api/v1/purchase/requests/${id}/reject`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-requests"] });
      queryClient.invalidateQueries({ queryKey: ["purchase-request", selectedId] });
    },
  });

  const convertToPoMutation = useMutation({
    mutationFn: ({ prId, vendor }: { prId: number; vendor: { vendorCode: string; vendorName: string } }) =>
      api.post(`/api/v1/purchase/orders/from-pr/${prId}`, vendor),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["purchase-requests"] });
      queryClient.invalidateQueries({ queryKey: ["purchase-orders"] });
      queryClient.invalidateQueries({ queryKey: ["purchase-request", selectedId] });
      setVendorDialog(false);
      setMode("list");
    },
  });

  // ── Handlers ──
  const openCreate = () => {
    setForm(emptyForm());
    setMode("create");
  };

  const openDetail = useCallback((row: PrRow) => {
    setSelectedId(row.id);
    setMode("detail");
  }, []);

  const backToList = () => {
    setMode("list");
    setSelectedId(null);
  };

  const updateForm = (field: keyof PrForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const updateLine = (idx: number, field: keyof PrLine, value: string | number) => {
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

  // ── Column Defs ──
  const columnDefs = useMemo<ColDef<PrRow>[]>(
    () => [
      {
        field: "documentNo",
        headerName: t("pr.docNo"),
        flex: 1.2,
        cellRenderer: (p: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{p.value}</span>
        ),
      },
      { field: "prType", headerName: t("common.type"), flex: 0.8 },
      { field: "companyCode", headerName: t("nav.companies"), flex: 0.8 },
      { field: "plantCode", headerName: t("gr.plant"), flex: 0.7 },
      { field: "requestDate", headerName: t("pr.requestDate"), flex: 1 },
      { field: "deliveryDate", headerName: t("pr.deliveryDate"), flex: 1 },
      { field: "requestedBy", headerName: t("pr.requestedBy"), flex: 1 },
      {
        field: "totalAmount",
        headerName: t("pr.totalAmount"),
        flex: 1,
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
          title={t("pr.title")}
          description={t("pr.description")}
          breadcrumbs={[{ label: t("nav.procurement") }, { label: t("nav.purchaseRequests") }]}
          actions={
            <button className="btn-primary" onClick={openCreate}>
              <Plus size={16} /> {t("pr.newPr")}
            </button>
          }
        />
        <div className="card overflow-hidden">
          <DataGrid<PrRow>
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
    const lineTotal = form.lines.reduce((s, l) => s + l.quantity * l.unitPrice, 0);

    return (
      <div>
        <PageHeader
          title={t("pr.newPr")}
          breadcrumbs={[
            { label: t("nav.procurement") },
            { label: t("nav.purchaseRequests") },
            { label: t("pr.newPr") },
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
              <label className="block text-sm font-medium text-slate-700 mb-1">{t("common.type")}</label>
              <select
                className="input"
                value={form.prType}
                onChange={(e) => updateForm("prType", e.target.value)}
              >
                {PR_TYPES.map((pt) => (
                  <option key={pt} value={pt}>
                    {t(`pr.types.${pt}`, pt)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">{t("pr.deliveryDate")}</label>
              <input
                className="input"
                type="date"
                value={form.deliveryDate}
                onChange={(e) => updateForm("deliveryDate", e.target.value)}
              />
            </div>
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-slate-700 mb-1">설명</label>
              <input
                className="input"
                value={form.description}
                onChange={(e) => updateForm("description", e.target.value)}
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
                    <th className="pb-2 pr-2">규격</th>
                    <th className="pb-2 pr-2">비고</th>
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
                        {(line.quantity * line.unitPrice).toLocaleString("ko-KR")}
                      </td>
                      <td className="py-2 pr-2">
                        <input
                          className="input py-1 px-2 w-24"
                          value={line.specification}
                          onChange={(e) => updateLine(idx, "specification", e.target.value)}
                        />
                      </td>
                      <td className="py-2 pr-2">
                        <input
                          className="input py-1 px-2 w-24"
                          value={line.remark}
                          onChange={(e) => updateLine(idx, "remark", e.target.value)}
                        />
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
                  <tr>
                    <td colSpan={6} className="pt-3 text-right font-semibold text-slate-700">
                      합계
                    </td>
                    <td className="pt-3 text-right font-semibold">{lineTotal.toLocaleString("ko-KR")}</td>
                    <td colSpan={3}></td>
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

    const lines: PrLine[] = detail.lines || [];
    const lineTotal = lines.reduce((s, l) => s + (l.quantity || 0) * (l.unitPrice || 0), 0);

    return (
      <div>
        <PageHeader
          title={`${t("pr.docNo")}: ${detail.documentNo}`}
          breadcrumbs={[
            { label: t("nav.procurement") },
            { label: t("nav.purchaseRequests") },
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
              <span className="text-slate-500">{t("common.type")}</span>
              <div className="mt-1 font-medium">{t(`pr.types.${detail.prType}`, detail.prType)}</div>
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
              <span className="text-slate-500">{t("pr.requestDate")}</span>
              <div className="mt-1 font-medium">{detail.requestDate}</div>
            </div>
            <div>
              <span className="text-slate-500">{t("pr.deliveryDate")}</span>
              <div className="mt-1 font-medium">{detail.deliveryDate || "-"}</div>
            </div>
            <div>
              <span className="text-slate-500">{t("pr.requestedBy")}</span>
              <div className="mt-1 font-medium">{detail.requestedBy || "-"}</div>
            </div>
            <div>
              <span className="text-slate-500">{t("pr.totalAmount")}</span>
              <div className="mt-1 font-semibold">{fmtCurrency(detail.totalAmount)}</div>
            </div>
          </div>

          {detail.description && (
            <div className="text-sm">
              <span className="text-slate-500">설명</span>
              <div className="mt-1">{detail.description}</div>
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
                    <th className="pb-2 pr-2">규격</th>
                    <th className="pb-2">비고</th>
                  </tr>
                </thead>
                <tbody>
                  {lines.map((line, idx) => (
                    <tr key={line.id || idx} className="border-b border-slate-100">
                      <td className="py-2 pr-2 text-slate-400">{idx + 1}</td>
                      <td className="py-2 pr-2 font-mono">{line.itemCode}</td>
                      <td className="py-2 pr-2">{line.itemName}</td>
                      <td className="py-2 pr-2 text-right">{line.quantity?.toLocaleString("ko-KR")}</td>
                      <td className="py-2 pr-2">{line.unitOfMeasure}</td>
                      <td className="py-2 pr-2 text-right">{line.unitPrice?.toLocaleString("ko-KR")}</td>
                      <td className="py-2 pr-2 text-right font-medium">
                        {((line.quantity || 0) * (line.unitPrice || 0)).toLocaleString("ko-KR")}
                      </td>
                      <td className="py-2 pr-2">{line.specification}</td>
                      <td className="py-2">{line.remark}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={6} className="pt-3 text-right font-semibold text-slate-700">
                      합계
                    </td>
                    <td className="pt-3 text-right font-semibold">
                      {lineTotal.toLocaleString("ko-KR")}
                    </td>
                    <td colSpan={2}></td>
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
                onClick={() => setVendorDialog(true)}
              >
                발주 전환
              </button>
            )}
          </div>
        </div>

        {/* Vendor Selection Dialog */}
        {vendorDialog && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            <div className="card p-6 w-full max-w-md space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold">공급업체 선택</h3>
                <button onClick={() => setVendorDialog(false)} className="text-slate-400 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">공급업체 코드</label>
                <input
                  className="input"
                  value={vendorCode}
                  onChange={(e) => setVendorCode(e.target.value)}
                  placeholder="Vendor Code"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">공급업체명</label>
                <input
                  className="input"
                  value={vendorName}
                  onChange={(e) => setVendorName(e.target.value)}
                  placeholder="Vendor Name"
                />
              </div>
              <div className="flex justify-end gap-3">
                <button className="btn-secondary" onClick={() => setVendorDialog(false)}>
                  {t("common.cancel")}
                </button>
                <button
                  className="btn-primary"
                  disabled={convertToPoMutation.isPending || !vendorCode}
                  onClick={() =>
                    convertToPoMutation.mutate({
                      prId: detail.id,
                      vendor: { vendorCode, vendorName },
                    })
                  }
                >
                  {convertToPoMutation.isPending ? t("common.saving") : "발주 전환"}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  return null;
}
