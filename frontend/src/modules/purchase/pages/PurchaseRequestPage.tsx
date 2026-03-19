import { useState, useMemo, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, X, ArrowLeft, Trash2, Building2, Factory, Package, CalendarDays } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import SearchSelect, { type SearchOption } from "../../../shared/components/SearchSelect";
import {
  COMPANY_OPTIONS,
  PLANT_OPTIONS,
  UNIT_OPTIONS,
  VENDOR_OPTIONS,
} from "../../../shared/data/lookups";
import api, { type ApiResponse } from "../../../shared/api/client";

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
  CLOSED: "badge bg-slate-100 text-slate-500",
};

const PR_TYPES = ["STANDARD", "URGENT", "PROJECT", "INVESTMENT"] as const;

const PR_TYPE_DESCRIPTIONS: Record<(typeof PR_TYPES)[number], string> = {
  STANDARD: "정규 구매 사이클에 맞춰 진행되는 일반 요청",
  URGENT: "리드타임 단축이 필요한 긴급 보충 요청",
  PROJECT: "프로젝트 단위 비용 추적이 필요한 구매",
  INVESTMENT: "설비, 금형, 고정자산 관련 투자성 구매",
};

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

  const { data, isLoading } = useQuery({
    queryKey: ["purchase-requests"],
    queryFn: async () => (await api.get("/api/v1/purchase/requests?size=100")).data,
  });

  const { data: detailData, isLoading: detailLoading } = useQuery({
    queryKey: ["purchase-request", selectedId],
    queryFn: async () => (await api.get(`/api/v1/purchase/requests/${selectedId}`)).data,
    enabled: mode === "detail" && selectedId !== null,
  });

  const { data: itemLookupData } = useQuery({
    queryKey: ["items", "lookup", "purchase-request"],
    queryFn: async () => {
      const res = await api.get<ApiResponse<ItemLookupRow[]>>("/api/v1/master-data/items?size=100");
      return res.data;
    },
  });

  const detail: PrRow | undefined = detailData?.data;

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

  const prTypeOptions = useMemo<SearchOption[]>(
    () =>
      PR_TYPES.map((prType) => ({
        value: prType,
        label: t(`pr.types.${prType}`, prType),
        description: PR_TYPE_DESCRIPTIONS[prType],
        meta: "PR",
      })),
    [t]
  );

  const selectedCompany = COMPANY_OPTIONS.find((option) => option.value === form.companyCode);
  const filteredPlants = form.companyCode
    ? PLANT_OPTIONS.filter((option) => option.meta === form.companyCode)
    : PLANT_OPTIONS;
  const lineTotal = form.lines.reduce((sum, line) => sum + line.quantity * line.unitPrice, 0);
  const itemCount = form.lines.filter((line) => line.itemCode).length;

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

  const patchLine = (idx: number, patch: Partial<PrLine>) => {
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

  if (mode === "create") {
    return (
      <div className="space-y-6">
        <PageHeader
          title={t("pr.newPr")}
          description="드롭다운 대신 검색형 필드와 자동완성으로 구매요청을 빠르게 구성합니다."
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

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.75fr)_360px]">
          <div className="space-y-6">
            <section className="workspace-hero">
              <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <p className="section-kicker">Procurement Workspace</p>
                  <h2 className="section-title">검색 중심으로 구매 요청을 구성합니다</h2>
                  <p className="mt-3 max-w-2xl text-sm text-slate-600">
                    SAP Fiori의 value help dialog와 Dynamics 365의 contextual lookup 패턴을 참고해,
                    회사/공장/품목을 코드 대신 검색 결과로 바로 선택하도록 구성했습니다.
                  </p>
                </div>
                <div className="grid w-full gap-3 sm:grid-cols-3 lg:w-[420px]">
                  <div className="stat-tile">
                    <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Company</div>
                    <div className="mt-2 text-sm font-semibold text-slate-900">
                      {selectedCompany?.label ?? "선택 대기"}
                    </div>
                  </div>
                  <div className="stat-tile">
                    <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Lines</div>
                    <div className="mt-2 text-sm font-semibold text-slate-900">{itemCount} selected</div>
                  </div>
                  <div className="stat-tile">
                    <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Amount</div>
                    <div className="mt-2 text-sm font-semibold text-slate-900">{fmtCurrency(lineTotal)}</div>
                  </div>
                </div>
              </div>
            </section>

            <section className="section-card">
              <p className="section-kicker">Request Header</p>
              <h3 className="section-title">헤더 정보</h3>
              <div className="mt-6 grid grid-cols-1 gap-5 md:grid-cols-2">
                <SearchSelect
                  label={t("nav.companies")}
                  value={form.companyCode}
                  options={COMPANY_OPTIONS}
                  placeholder="회사 검색"
                  searchTitle="회사 선택"
                  searchDescription="회사코드 또는 운영 단위로 검색해 선택합니다."
                  helper="회사 선택 시 연결된 플랜트만 추천됩니다."
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
                  searchDescription="회사 기준으로 운영 가능한 플랜트를 검색합니다."
                  helper="회사 선택 전에도 전체 플랜트 검색은 가능합니다."
                  onSelect={(option) => updateForm("plantCode", option.value)}
                  onClear={() => updateForm("plantCode", "")}
                />
                <SearchSelect
                  label={t("common.type")}
                  value={form.prType}
                  options={prTypeOptions}
                  placeholder="요청 유형 검색"
                  searchTitle="구매요청 유형"
                  searchDescription="업무 성격에 따라 유형을 검색 후 선택합니다."
                  onSelect={(option) => updateForm("prType", option.value)}
                />
                <div className="space-y-2">
                  <label className="field-label">{t("pr.deliveryDate")}</label>
                  <div className="lookup-shell">
                    <CalendarDays size={18} className="text-slate-400" />
                    <input
                      className="lookup-input"
                      type="date"
                      value={form.deliveryDate}
                      onChange={(event) => updateForm("deliveryDate", event.target.value)}
                    />
                  </div>
                  <p className="field-helper">납기일이 없으면 공급업체와 협의 후 상세 단계에서 확정할 수 있습니다.</p>
                </div>
                <div className="space-y-2 md:col-span-2">
                  <label className="field-label">설명</label>
                  <textarea
                    className="input min-h-[116px] resize-none"
                    value={form.description}
                    onChange={(event) => updateForm("description", event.target.value)}
                    placeholder="예: 신규 프로젝트 조립 자재 선행 확보"
                  />
                </div>
              </div>
            </section>

            <section className="section-card">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="section-kicker">Line Builder</p>
                  <h3 className="section-title">품목 라인</h3>
                </div>
                <button className="btn-secondary" onClick={addLine}>
                  <Plus size={16} /> 행 추가
                </button>
              </div>

              <div className="mt-5 space-y-4">
                {form.lines.map((line, idx) => {
                  const lineAmount = line.quantity * line.unitPrice;

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
                            <div className="text-[11px] uppercase tracking-[0.18em] text-slate-400">Line Amount</div>
                            <div className="mt-1 text-sm font-semibold text-slate-900">
                              {fmtCurrency(lineAmount)}
                            </div>
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
                            searchDescription="품목 마스터 기준으로 검색해 라인을 채웁니다."
                            emptyText="등록된 품목이 없습니다."
                            onSelect={(option) => {
                              const item = itemRows.find((candidate) => candidate.code === option.value);
                              patchLine(idx, {
                                itemCode: option.value,
                                itemName: option.label,
                                unitOfMeasure: item?.unitOfMeasure ?? line.unitOfMeasure,
                                specification: item?.specification ?? line.specification,
                              });
                            }}
                            onClear={() =>
                              patchLine(idx, {
                                itemCode: "",
                                itemName: "",
                              })
                            }
                          />
                        </div>
                        <div className="lg:col-span-3">
                          <label className="field-label">품목명</label>
                          <input
                            className="input mt-2"
                            value={line.itemName}
                            onChange={(event) => updateLine(idx, "itemName", event.target.value)}
                            placeholder="자동 채움 또는 직접 수정"
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
                        <div className="lg:col-span-4">
                          <label className="field-label">규격</label>
                          <input
                            className="input mt-2"
                            value={line.specification}
                            onChange={(event) => updateLine(idx, "specification", event.target.value)}
                            placeholder="규격 또는 옵션"
                          />
                        </div>
                        <div className="lg:col-span-5">
                          <label className="field-label">비고</label>
                          <input
                            className="input mt-2"
                            value={line.remark}
                            onChange={(event) => updateLine(idx, "remark", event.target.value)}
                            placeholder="현장 메모, 요청 사유, 긴급도"
                          />
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
              <p className="section-kicker">Request Summary</p>
              <h3 className="section-title">요청 요약</h3>
              <div className="mt-5 space-y-3">
                <div className="stat-tile">
                  <div className="flex items-center gap-3">
                    <Building2 size={18} className="text-brand-600" />
                    <div>
                      <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Company</div>
                      <div className="mt-1 text-sm font-semibold text-slate-900">
                        {selectedCompany?.label ?? "선택 필요"}
                      </div>
                    </div>
                  </div>
                </div>
                <div className="stat-tile">
                  <div className="flex items-center gap-3">
                    <Factory size={18} className="text-brand-600" />
                    <div>
                      <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Plant</div>
                      <div className="mt-1 text-sm font-semibold text-slate-900">
                        {filteredPlants.find((option) => option.value === form.plantCode)?.label ?? "선택 필요"}
                      </div>
                    </div>
                  </div>
                </div>
                <div className="stat-tile">
                  <div className="flex items-center gap-3">
                    <Package size={18} className="text-brand-600" />
                    <div>
                      <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Total</div>
                      <div className="mt-1 text-lg font-semibold text-slate-950">{fmtCurrency(lineTotal)}</div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="mt-5 rounded-[24px] bg-slate-950 p-5 text-white">
                <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Preview</div>
                <div className="mt-3 text-sm text-slate-300">
                  {prTypeOptions.find((option) => option.value === form.prType)?.label ?? "일반"} 요청으로
                  {itemCount > 0 ? ` ${itemCount}개 품목` : " 품목"}을 등록합니다.
                </div>
                <div className="mt-4 space-y-2 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">납기일</span>
                    <span>{form.deliveryDate || "-"}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">라인 수</span>
                    <span>{form.lines.length}</span>
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

    const lines: PrLine[] = detail.lines || [];
    const detailLineTotal = lines.reduce((sum, line) => sum + (line.quantity || 0) * (line.unitPrice || 0), 0);

    return (
      <div className="space-y-6">
        <PageHeader
          title={`${t("pr.docNo")}: ${detail.documentNo}`}
          description="구매요청 상세와 승인/발주 전환을 한 화면에서 처리합니다."
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

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.75fr)_340px]">
          <div className="space-y-6">
            <section className="section-card">
              <p className="section-kicker">Request Overview</p>
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
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("common.type")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">
                    {t(`pr.types.${detail.prType}`, detail.prType)}
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
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("pr.requestDate")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{detail.requestDate}</div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("pr.deliveryDate")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{detail.deliveryDate || "-"}</div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("pr.requestedBy")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{detail.requestedBy || "-"}</div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">{t("pr.totalAmount")}</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{fmtCurrency(detail.totalAmount)}</div>
                </div>
              </div>
              {detail.description && (
                <div className="mt-5 rounded-[24px] bg-slate-50 p-4 text-sm text-slate-600">{detail.description}</div>
              )}
            </section>

            <section className="section-card">
              <p className="section-kicker">Request Lines</p>
              <h3 className="section-title">품목 목록</h3>
              <div className="grid-table mt-5">
                <div className="grid grid-cols-[80px_1.2fr_1.2fr_100px_90px_120px_120px_1fr_1fr] gap-3 border-b border-slate-200 bg-slate-50/80 px-4 py-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">
                  <span>#</span>
                  <span>품목코드</span>
                  <span>품목명</span>
                  <span className="text-right">수량</span>
                  <span>단위</span>
                  <span className="text-right">단가</span>
                  <span className="text-right">금액</span>
                  <span>규격</span>
                  <span>비고</span>
                </div>
                {lines.map((line, idx) => (
                  <div
                    key={line.id || idx}
                    className="grid grid-cols-[80px_1.2fr_1.2fr_100px_90px_120px_120px_1fr_1fr] gap-3 px-4 py-4 text-sm text-slate-700 grid-table-row"
                  >
                    <span className="text-slate-400">{idx + 1}</span>
                    <span className="font-mono">{line.itemCode}</span>
                    <span>{line.itemName}</span>
                    <span className="text-right">{line.quantity?.toLocaleString("ko-KR")}</span>
                    <span>{line.unitOfMeasure}</span>
                    <span className="text-right">{line.unitPrice?.toLocaleString("ko-KR")}</span>
                    <span className="text-right font-semibold">
                      {((line.quantity || 0) * (line.unitPrice || 0)).toLocaleString("ko-KR")}
                    </span>
                    <span>{line.specification}</span>
                    <span>{line.remark}</span>
                  </div>
                ))}
              </div>
            </section>
          </div>

          <aside className="space-y-6">
            <div className="section-card xl:sticky xl:top-4">
              <p className="section-kicker">Workflow</p>
              <h3 className="section-title">승인 및 전환</h3>
              <div className="mt-5 rounded-[24px] bg-slate-950 p-5 text-white">
                <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Current Amount</div>
                <div className="mt-2 text-2xl font-semibold">{fmtCurrency(detailLineTotal)}</div>
                <div className="mt-4 text-sm text-slate-300">
                  승인 완료 후 공급업체를 검색 모달로 선택해 발주서로 전환할 수 있습니다.
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
                    onClick={() => {
                      setVendorCode("");
                      setVendorName("");
                      setVendorDialog(true);
                    }}
                  >
                    발주 전환
                  </button>
                )}
              </div>
            </div>
          </aside>
        </div>

        {vendorDialog && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 px-4 backdrop-blur-sm">
            <div className="w-full max-w-2xl rounded-[28px] border border-white/70 bg-white/95 p-6 shadow-[0_32px_80px_rgba(15,23,42,0.28)]">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="section-kicker">Convert To PO</p>
                  <h3 className="section-title">공급업체 선택</h3>
                  <p className="mt-2 text-sm text-slate-500">검색 모달 또는 자동완성으로 공급업체를 찾은 뒤 발주서로 전환합니다.</p>
                </div>
                <button onClick={() => setVendorDialog(false)} className="btn-ghost">
                  <X size={18} />
                </button>
              </div>

              <div className="mt-6">
                <SearchSelect
                  label="공급업체"
                  value={vendorCode}
                  options={VENDOR_OPTIONS}
                  placeholder="공급업체 코드 또는 이름 검색"
                  searchTitle="공급업체 선택"
                  searchDescription="코드, 이름, 카테고리 키워드로 검색해 발주처를 선택합니다."
                  onSelect={(option) => {
                    setVendorCode(option.value);
                    setVendorName(option.label);
                  }}
                  onClear={() => {
                    setVendorCode("");
                    setVendorName("");
                  }}
                />
              </div>

              {vendorCode && (
                <div className="mt-4 rounded-[22px] bg-slate-50 p-4 text-sm text-slate-600">
                  선택된 공급업체: <span className="font-semibold text-slate-900">{vendorName}</span> ({vendorCode})
                </div>
              )}

              <div className="mt-6 flex justify-end gap-3">
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
