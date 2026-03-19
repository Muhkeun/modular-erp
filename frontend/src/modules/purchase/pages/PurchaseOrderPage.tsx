import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Trash2, Building2, HandCoins, Truck } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import SearchSelect, { type SearchOption } from "../../../shared/components/SearchSelect";
import {
  COMPANY_OPTIONS,
  CURRENCY_OPTIONS,
  PAYMENT_TERM_OPTIONS,
  PLANT_OPTIONS,
  UNIT_OPTIONS,
  VENDOR_OPTIONS,
} from "../../../shared/data/lookups";
import api, { type ApiResponse } from "../../../shared/api/client";

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

interface ItemLookupRow {
  id: number;
  code: string;
  name: string;
  itemType: string;
  itemGroup: string | null;
  unitOfMeasure: string;
  specification: string | null;
  makerName: string | null;
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

  const { data, isLoading } = useQuery({
    queryKey: ["purchase-orders"],
    queryFn: async () => (await api.get("/api/v1/purchase/orders?size=100")).data,
  });

  const { data: detailData, isLoading: detailLoading } = useQuery({
    queryKey: ["purchase-order", selectedId],
    queryFn: async () => (await api.get(`/api/v1/purchase/orders/${selectedId}`)).data,
    enabled: mode === "detail" && selectedId !== null,
  });

  const { data: itemLookupData } = useQuery({
    queryKey: ["items", "lookup", "purchase-order"],
    queryFn: async () => {
      const res = await api.get<ApiResponse<ItemLookupRow[]>>("/api/v1/master-data/items?size=100");
      return res.data;
    },
  });

  const detail: PoRow | undefined = detailData?.data;

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

  const itemRows = itemLookupData?.data ?? [];
  const itemOptions = useMemo<SearchOption[]>(
    () =>
      itemRows.map((item) => ({
        value: item.code,
        label: item.name,
        description: [t(`item.types.${item.itemType}`, item.itemType), item.specification]
          .filter(Boolean)
          .join(" · "),
        meta: item.unitOfMeasure,
        keywords: [item.itemGroup ?? "", item.makerName ?? ""],
      })),
    [itemRows, t]
  );

  const filteredPlants = form.companyCode
    ? PLANT_OPTIONS.filter((option) => option.meta === form.companyCode)
    : PLANT_OPTIONS;

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

  const patchLine = (idx: number, patch: Partial<PoLine>) => {
    setForm((prev) => {
      const lines = [...prev.lines];
      lines[idx] = { ...lines[idx], ...patch };
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

  const fmtCurrency = (value: number) =>
    value?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

  const calcLineAmount = (line: PoLine) => line.quantity * line.unitPrice;
  const calcLineTax = (line: PoLine) => calcLineAmount(line) * (line.taxRate / 100);

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

  if (mode === "create") {
    const subtotal = form.lines.reduce((sum, line) => sum + calcLineAmount(line), 0);
    const taxTotal = form.lines.reduce((sum, line) => sum + calcLineTax(line), 0);
    const grandTotal = subtotal + taxTotal;

    return (
      <div className="space-y-6">
        <PageHeader
          title={t("po.newPo")}
          description="공급업체와 기준조건을 검색형 lookup으로 선택하고, 품목 라인은 자동완성으로 빠르게 작성합니다."
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

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.75fr)_360px]">
          <div className="space-y-6">
            <section className="workspace-hero">
              <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <p className="section-kicker">Supplier Collaboration</p>
                  <h2 className="section-title">발주서 입력을 검색형 workspace로 전환</h2>
                  <p className="mt-3 max-w-2xl text-sm text-slate-600">
                    공급업체, 통화, 결제조건까지 lookup 패턴으로 통일해 ERP 입력 속도를 높이고,
                    품목 라인은 인라인 자동완성과 전체 검색 모달을 함께 제공합니다.
                  </p>
                </div>
                <div className="grid w-full gap-3 sm:grid-cols-3 lg:w-[420px]">
                  <div className="stat-tile">
                    <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Vendor</div>
                    <div className="mt-2 text-sm font-semibold text-slate-900">
                      {VENDOR_OPTIONS.find((option) => option.value === form.vendorCode)?.label ?? "선택 대기"}
                    </div>
                  </div>
                  <div className="stat-tile">
                    <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Currency</div>
                    <div className="mt-2 text-sm font-semibold text-slate-900">{form.currencyCode}</div>
                  </div>
                  <div className="stat-tile">
                    <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Grand Total</div>
                    <div className="mt-2 text-sm font-semibold text-slate-900">{fmtCurrency(grandTotal)}</div>
                  </div>
                </div>
              </div>
            </section>

            <section className="section-card">
              <p className="section-kicker">Order Header</p>
              <h3 className="section-title">발주 조건</h3>
              <div className="mt-6 grid grid-cols-1 gap-5 md:grid-cols-2">
                <SearchSelect
                  label={t("nav.companies")}
                  value={form.companyCode}
                  options={COMPANY_OPTIONS}
                  placeholder="회사 검색"
                  searchTitle="회사 선택"
                  onSelect={(option) =>
                    setForm((prev) => ({
                      ...prev,
                      companyCode: option.value,
                      plantCode:
                        prev.plantCode && !PLANT_OPTIONS.some((plant) => plant.value === prev.plantCode && plant.meta === option.value)
                          ? ""
                          : prev.plantCode,
                    }))
                  }
                  onClear={() => updateForm("companyCode", "")}
                />
                <SearchSelect
                  label={t("gr.plant")}
                  value={form.plantCode}
                  options={filteredPlants}
                  placeholder="플랜트 검색"
                  searchTitle="플랜트 선택"
                  onSelect={(option) => updateForm("plantCode", option.value)}
                  onClear={() => updateForm("plantCode", "")}
                />
                <SearchSelect
                  label={t("po.vendor")}
                  value={form.vendorCode}
                  options={VENDOR_OPTIONS}
                  placeholder="공급업체 코드 또는 이름 검색"
                  searchTitle="공급업체 선택"
                  searchDescription="Preferred, Strategic, Global 분류와 함께 검색할 수 있습니다."
                  onSelect={(option) =>
                    setForm((prev) => ({ ...prev, vendorCode: option.value, vendorName: option.label }))
                  }
                  onClear={() =>
                    setForm((prev) => ({ ...prev, vendorCode: "", vendorName: "" }))
                  }
                />
                <SearchSelect
                  label="통화"
                  value={form.currencyCode}
                  options={CURRENCY_OPTIONS}
                  placeholder="통화 검색"
                  searchTitle="통화 선택"
                  onSelect={(option) => updateForm("currencyCode", option.value)}
                />
                <SearchSelect
                  label="결제조건"
                  value={form.paymentTerms}
                  options={PAYMENT_TERM_OPTIONS}
                  placeholder="결제조건 검색"
                  searchTitle="결제조건 선택"
                  onSelect={(option) => updateForm("paymentTerms", option.value)}
                  onClear={() => updateForm("paymentTerms", "")}
                />
                <div className="space-y-2">
                  <label className="field-label">{t("po.deliveryDate")}</label>
                  <input
                    className="input"
                    type="date"
                    value={form.deliveryDate}
                    onChange={(event) => updateForm("deliveryDate", event.target.value)}
                  />
                </div>
                <div className="space-y-2 md:col-span-2">
                  <label className="field-label">비고</label>
                  <textarea
                    className="input min-h-[116px] resize-none"
                    value={form.remark}
                    onChange={(event) => updateForm("remark", event.target.value)}
                    placeholder="납품 조건, 선적 조건, 단가 협의 메모"
                  />
                </div>
              </div>
            </section>

            <section className="section-card">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="section-kicker">Order Lines</p>
                  <h3 className="section-title">품목 라인</h3>
                </div>
                <button className="btn-secondary" onClick={addLine}>
                  <Plus size={16} /> 행 추가
                </button>
              </div>

              <div className="mt-5 space-y-4">
                {form.lines.map((line, idx) => {
                  const amount = calcLineAmount(line);
                  const tax = calcLineTax(line);

                  return (
                    <div key={idx} className="rounded-[26px] border border-slate-200/80 bg-slate-50/70 p-5">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <div className="flex items-center gap-3">
                          <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500 ring-1 ring-inset ring-slate-200">
                            Line {idx + 1}
                          </span>
                          <span className="text-sm text-slate-500">
                            {line.itemCode ? `${line.itemCode} · ${line.itemName}` : "품목 미선택"}
                          </span>
                        </div>
                        <div className="flex items-center gap-3">
                          <div className="rounded-2xl bg-white px-4 py-2 text-right shadow-sm">
                            <div className="text-[11px] uppercase tracking-[0.18em] text-slate-400">Amount</div>
                            <div className="mt-1 text-sm font-semibold text-slate-900">{fmtCurrency(amount + tax)}</div>
                          </div>
                          {form.lines.length > 1 && (
                            <button className="btn-ghost" onClick={() => removeLine(idx)}>
                              <Trash2 size={16} />
                            </button>
                          )}
                        </div>
                      </div>

                      <div className="mt-5 grid gap-4 lg:grid-cols-12">
                        <div className="lg:col-span-5">
                          <SearchSelect
                            label="품목 검색"
                            value={line.itemCode}
                            options={itemOptions}
                            placeholder="품목코드 또는 품목명"
                            searchTitle="품목 선택"
                            searchDescription="품목 마스터에서 검색 후 단위까지 자동 반영합니다."
                            onSelect={(option) => {
                              const item = itemRows.find((candidate) => candidate.code === option.value);
                              patchLine(idx, {
                                itemCode: option.value,
                                itemName: option.label,
                                unitOfMeasure: item?.unitOfMeasure ?? line.unitOfMeasure,
                              });
                            }}
                            onClear={() => patchLine(idx, { itemCode: "", itemName: "" })}
                          />
                        </div>
                        <div className="lg:col-span-3">
                          <label className="field-label">품목명</label>
                          <input
                            className="input mt-2"
                            value={line.itemName}
                            onChange={(event) => updateLine(idx, "itemName", event.target.value)}
                          />
                        </div>
                        <div className="lg:col-span-2">
                          <label className="field-label">수량</label>
                          <input
                            className="input mt-2 text-right"
                            type="number"
                            value={line.quantity}
                            onChange={(event) => updateLine(idx, "quantity", Number(event.target.value))}
                          />
                        </div>
                        <div className="lg:col-span-2">
                          <SearchSelect
                            label="단위"
                            value={line.unitOfMeasure}
                            options={UNIT_OPTIONS}
                            placeholder="단위 검색"
                            onSelect={(option) => updateLine(idx, "unitOfMeasure", option.value)}
                          />
                        </div>
                        <div className="lg:col-span-3">
                          <label className="field-label">단가</label>
                          <input
                            className="input mt-2 text-right"
                            type="number"
                            value={line.unitPrice}
                            onChange={(event) => updateLine(idx, "unitPrice", Number(event.target.value))}
                          />
                        </div>
                        <div className="lg:col-span-3">
                          <label className="field-label">세율(%)</label>
                          <input
                            className="input mt-2 text-right"
                            type="number"
                            value={line.taxRate}
                            onChange={(event) => updateLine(idx, "taxRate", Number(event.target.value))}
                          />
                        </div>
                        <div className="lg:col-span-3">
                          <div className="field-label">공급가액</div>
                          <div className="mt-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-right text-sm font-semibold text-slate-900">
                            {fmtCurrency(amount)}
                          </div>
                        </div>
                        <div className="lg:col-span-3">
                          <div className="field-label">세액</div>
                          <div className="mt-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-right text-sm font-semibold text-slate-900">
                            {fmtCurrency(tax)}
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>
          </div>

          <aside className="space-y-6">
            <div className="section-card xl:sticky xl:top-4">
              <p className="section-kicker">Order Summary</p>
              <h3 className="section-title">발주 요약</h3>
              <div className="mt-5 space-y-3">
                <div className="stat-tile">
                  <div className="flex items-center gap-3">
                    <Building2 size={18} className="text-brand-600" />
                    <div>
                      <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Company / Plant</div>
                      <div className="mt-1 text-sm font-semibold text-slate-900">
                        {form.companyCode || "-"} / {form.plantCode || "-"}
                      </div>
                    </div>
                  </div>
                </div>
                <div className="stat-tile">
                  <div className="flex items-center gap-3">
                    <Truck size={18} className="text-brand-600" />
                    <div>
                      <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Vendor</div>
                      <div className="mt-1 text-sm font-semibold text-slate-900">
                        {form.vendorName || "선택 필요"}
                      </div>
                    </div>
                  </div>
                </div>
                <div className="stat-tile">
                  <div className="flex items-center gap-3">
                    <HandCoins size={18} className="text-brand-600" />
                    <div>
                      <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Terms</div>
                      <div className="mt-1 text-sm font-semibold text-slate-900">
                        {form.currencyCode} · {form.paymentTerms || "미지정"}
                      </div>
                    </div>
                  </div>
                </div>
                <div className="rounded-[24px] bg-slate-950 p-5 text-white">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Financials</div>
                  <div className="mt-4 space-y-2 text-sm">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">공급가액</span>
                      <span>{fmtCurrency(subtotal)}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">세액</span>
                      <span>{fmtCurrency(taxTotal)}</span>
                    </div>
                    <div className="mt-3 flex items-center justify-between border-t border-white/10 pt-3 text-base font-semibold">
                      <span>총액</span>
                      <span>{fmtCurrency(grandTotal)}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="mt-5 space-y-3">
                <button className="btn-primary w-full" onClick={handleSave} disabled={createMutation.isPending}>
                  {createMutation.isPending ? t("common.saving") : t("common.save")}
                </button>
                <button className="btn-secondary w-full" onClick={backToList}>
                  {t("common.cancel")}
                </button>
              </div>
            </div>
          </aside>
        </div>
      </div>
    );
  }

  if (mode === "detail") {
    if (detailLoading || !detail) {
      return <div className="flex h-64 items-center justify-center text-slate-400">{t("common.loading")}</div>;
    }

    const lines: PoLine[] = detail.lines || [];
    const subtotal = lines.reduce((sum, line) => sum + (line.quantity || 0) * (line.unitPrice || 0), 0);
    const taxTotal = lines.reduce(
      (sum, line) => sum + (line.quantity || 0) * (line.unitPrice || 0) * ((line.taxRate || 0) / 100),
      0
    );
    const grandTotal = subtotal + taxTotal;

    return (
      <div className="space-y-6">
        <PageHeader
          title={`${t("po.docNo")}: ${detail.documentNo}`}
          description="발주 상세, 승인, 발송 상태를 한 화면에서 관리합니다."
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

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.75fr)_340px]">
          <div className="space-y-6">
            <section className="section-card">
              <p className="section-kicker">Order Overview</p>
              <h3 className="section-title">헤더 정보</h3>
              <div className="mt-5 grid grid-cols-2 gap-4 lg:grid-cols-4">
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("common.status")}</div>
                  <div className="mt-2">
                    <span className={statusStyle[detail.status] || "badge"}>
                      {t("status." + detail.status, detail.status)}
                    </span>
                  </div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("po.vendor")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">
                    {detail.vendorName} ({detail.vendorCode})
                  </div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("nav.companies")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{detail.companyCode}</div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("gr.plant")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{detail.plantCode}</div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("po.orderDate")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{detail.orderDate}</div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("po.deliveryDate")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{detail.deliveryDate || "-"}</div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">통화</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{detail.currencyCode}</div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">결제조건</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{detail.paymentTerms || "-"}</div>
                </div>
              </div>
              {detail.remark && (
                <div className="mt-5 rounded-[24px] bg-slate-50 p-4 text-sm text-slate-600">{detail.remark}</div>
              )}
            </section>

            <section className="section-card">
              <p className="section-kicker">Order Lines</p>
              <h3 className="section-title">품목 목록</h3>
              <div className="grid-table mt-5">
                <div className="grid grid-cols-[80px_1.1fr_1.2fr_100px_90px_120px_120px_90px_120px] gap-3 border-b border-slate-200 bg-slate-50/80 px-4 py-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">
                  <span>#</span>
                  <span>품목코드</span>
                  <span>품목명</span>
                  <span className="text-right">수량</span>
                  <span>단위</span>
                  <span className="text-right">단가</span>
                  <span className="text-right">금액</span>
                  <span className="text-right">세율</span>
                  <span className="text-right">세액</span>
                </div>
                {lines.map((line, idx) => {
                  const amount = (line.quantity || 0) * (line.unitPrice || 0);
                  const tax = amount * ((line.taxRate || 0) / 100);

                  return (
                    <div
                      key={line.id || idx}
                      className="grid grid-cols-[80px_1.1fr_1.2fr_100px_90px_120px_120px_90px_120px] gap-3 px-4 py-4 text-sm text-slate-700 grid-table-row"
                    >
                      <span className="text-slate-400">{idx + 1}</span>
                      <span className="font-mono">{line.itemCode}</span>
                      <span>{line.itemName}</span>
                      <span className="text-right">{line.quantity?.toLocaleString("ko-KR")}</span>
                      <span>{line.unitOfMeasure}</span>
                      <span className="text-right">{line.unitPrice?.toLocaleString("ko-KR")}</span>
                      <span className="text-right font-semibold">{amount.toLocaleString("ko-KR")}</span>
                      <span className="text-right">{line.taxRate}%</span>
                      <span className="text-right">{tax.toLocaleString("ko-KR")}</span>
                    </div>
                  );
                })}
              </div>
            </section>
          </div>

          <aside className="space-y-6">
            <div className="section-card xl:sticky xl:top-4">
              <p className="section-kicker">Workflow</p>
              <h3 className="section-title">승인 및 발송</h3>
              <div className="mt-5 rounded-[24px] bg-slate-950 p-5 text-white">
                <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("po.grandTotal")}</div>
                <div className="mt-2 text-2xl font-semibold">{fmtCurrency(grandTotal)}</div>
                <div className="mt-4 space-y-2 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">공급가액</span>
                    <span>{fmtCurrency(subtotal)}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">세액</span>
                    <span>{fmtCurrency(taxTotal)}</span>
                  </div>
                </div>
              </div>

              <div className="mt-5 space-y-3">
                {detail.status === "DRAFT" && (
                  <button
                    className="btn-primary w-full"
                    onClick={() => submitMutation.mutate(detail.id)}
                    disabled={submitMutation.isPending}
                  >
                    {t("common.submit")}
                  </button>
                )}
                {detail.status === "SUBMITTED" && (
                  <>
                    <button
                      className="btn-danger w-full"
                      onClick={() => rejectMutation.mutate(detail.id)}
                      disabled={rejectMutation.isPending}
                    >
                      {t("common.reject")}
                    </button>
                    <button
                      className="btn-primary w-full"
                      onClick={() => approveMutation.mutate(detail.id)}
                      disabled={approveMutation.isPending}
                    >
                      {t("common.approve")}
                    </button>
                  </>
                )}
                {detail.status === "APPROVED" && (
                  <button
                    className="btn-primary w-full"
                    onClick={() => sendMutation.mutate(detail.id)}
                    disabled={sendMutation.isPending}
                  >
                    {t("status.SENT", "발송")}
                  </button>
                )}
              </div>
            </div>
          </aside>
        </div>
      </div>
    );
  }

  return null;
}
