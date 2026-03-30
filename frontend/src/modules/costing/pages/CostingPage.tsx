import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { ArrowLeft, Calculator, Plus, Search } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import { useToast } from "../../../shared/components/useToast";
import {
  costingApi,
  type CalculateProductCostRequest,
  type CostAllocation,
  type CostCenter,
  type CreateCostAllocationRequest,
  type CreateCostCenterRequest,
  type CreateStandardCostRequest,
  type ProductCost,
  type StandardCost,
  type VarianceRow,
} from "../../../shared/api/costingApi";

type Tab = "centers" | "standard" | "products" | "allocations" | "variance";
type Mode = "list" | "create";

const today = new Date().toISOString().slice(0, 10);
const currentYear = new Date().getFullYear();
const currentPeriod = new Date().getMonth() + 1;

const emptyCenterForm = (): CreateCostCenterRequest => ({
  costCenterCode: "",
  costCenterName: "",
  parentCode: "",
  departmentCode: "",
  managerName: "",
  status: "ACTIVE",
});

const emptyStandardForm = (): CreateStandardCostRequest => ({
  itemCode: "",
  costCenterCode: "",
  costType: "MATERIAL",
  standardRate: 0,
  effectiveFrom: today,
  effectiveTo: "",
  currency: "KRW",
  notes: "",
});

const emptyAllocationForm = (): CreateCostAllocationRequest => ({
  allocationDate: today,
  fromCostCenter: "",
  toCostCenter: "",
  allocationType: "DIRECT",
  amount: 0,
  allocationBasis: "",
  percentage: undefined,
  description: "",
  fiscalYear: currentYear,
  period: currentPeriod,
});

const emptyCalcForm = (): CalculateProductCostRequest => ({
  itemCode: "",
  costCenterCode: "",
  fiscalYear: currentYear,
  period: currentPeriod,
  quantity: 1,
});

const costTypeOptions = ["MATERIAL", "LABOR", "OVERHEAD", "SUBCONTRACTING"];
const allocationTypeOptions = ["DIRECT", "STEP_DOWN", "ACTIVITY_BASED"];
const centerStatusOptions = ["ACTIVE", "INACTIVE"];

const formatCurrency = (value: number | string | null | undefined, currency = "KRW") =>
  Number(value ?? 0).toLocaleString("ko-KR", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  });

const formatNumber = (value: number | string | null | undefined) =>
  Number(value ?? 0).toLocaleString("ko-KR");

const formatDate = (value: string | null | undefined) => value || "-";

export default function CostingPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();

  const [tab, setTab] = useState<Tab>("centers");
  const [mode, setMode] = useState<Mode>("list");
  const [calcDialog, setCalcDialog] = useState(false);
  const [calcForm, setCalcForm] = useState<CalculateProductCostRequest>(emptyCalcForm());
  const [calcResult, setCalcResult] = useState<ProductCost | null>(null);
  const [varianceInput, setVarianceInput] = useState("");
  const [varianceSearchCode, setVarianceSearchCode] = useState("");

  const [centerForm, setCenterForm] = useState<CreateCostCenterRequest>(emptyCenterForm());
  const [standardForm, setStandardForm] = useState<CreateStandardCostRequest>(emptyStandardForm());
  const [allocationForm, setAllocationForm] = useState<CreateCostAllocationRequest>(emptyAllocationForm());

  const centersQ = useQuery({
    queryKey: ["cost-centers"],
    queryFn: () => costingApi.getCostCenters({ size: 100 }),
  });

  const standardQ = useQuery({
    queryKey: ["standard-costs"],
    queryFn: () => costingApi.getStandardCosts({ size: 100 }),
    enabled: tab === "standard",
  });

  const productQ = useQuery({
    queryKey: ["product-costs"],
    queryFn: () => costingApi.getProductCosts({ size: 100 }),
    enabled: tab === "products",
  });

  const allocationQ = useQuery({
    queryKey: ["cost-allocations"],
    queryFn: () => costingApi.getAllocations({ size: 100 }),
    enabled: tab === "allocations",
  });

  const varianceQ = useQuery({
    queryKey: ["variance-analysis", varianceSearchCode],
    queryFn: () => costingApi.getVarianceAnalysis(varianceSearchCode),
    enabled: tab === "variance" && varianceSearchCode.trim().length > 0,
  });

  const createCenterMutation = useMutation({
    mutationFn: (payload: CreateCostCenterRequest) => costingApi.createCostCenter(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cost-centers"] });
      setCenterForm(emptyCenterForm());
      setMode("list");
      toast.success(t("common.save", "Saved"));
    },
    onError: () => toast.error("Cost center creation failed"),
  });

  const createStandardMutation = useMutation({
    mutationFn: (payload: CreateStandardCostRequest) => costingApi.createStandardCost(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["standard-costs"] });
      setStandardForm(emptyStandardForm());
      setMode("list");
      toast.success(t("common.save", "Saved"));
    },
    onError: () => toast.error("Standard cost creation failed"),
  });

  const createAllocationMutation = useMutation({
    mutationFn: (payload: CreateCostAllocationRequest) => costingApi.createAllocation(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cost-allocations"] });
      setAllocationForm(emptyAllocationForm());
      setMode("list");
      toast.success(t("common.save", "Saved"));
    },
    onError: () => toast.error("Allocation creation failed"),
  });

  const calculateMutation = useMutation({
    mutationFn: (payload: CalculateProductCostRequest) => costingApi.calculateProductCost(payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["product-costs"] });
      setCalcResult(data);
      toast.success(t("costing.calculate", "Calculate"));
    },
    onError: () => toast.error("Product cost calculation failed"),
  });

  const costCenters = centersQ.data?.data ?? [];
  const standardCosts = standardQ.data?.data ?? [];
  const productCosts = productQ.data?.data ?? [];
  const allocations = allocationQ.data?.data ?? [];
  const varianceRows = varianceQ.data ?? [];

  const centerColumns = useMemo<ColDef<CostCenter>[]>(
    () => [
      {
        field: "costCenterCode",
        headerName: t("common.code"),
        flex: 1,
        cellRenderer: (params: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{params.value}</span>
        ),
      },
      { field: "costCenterName", headerName: t("common.name"), flex: 1.5 },
      { field: "departmentCode", headerName: t("nav.departments", "Department"), flex: 1 },
      { field: "parentCode", headerName: t("costing.parent", "Parent"), flex: 1 },
      { field: "managerName", headerName: t("costing.manager", "Manager"), flex: 1 },
      {
        field: "status",
        headerName: t("common.status"),
        flex: 0.8,
        cellRenderer: (params: { value: string }) => (
          <span className={params.value === "ACTIVE" ? "badge-success" : "badge bg-slate-100 text-slate-600"}>
            {params.value}
          </span>
        ),
      },
    ],
    [t]
  );

  const standardColumns = useMemo<ColDef<StandardCost>[]>(
    () => [
      {
        field: "itemCode",
        headerName: t("item.code"),
        flex: 1,
        cellRenderer: (params: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{params.value}</span>
        ),
      },
      { field: "costCenterCode", headerName: t("costing.costCenters", "Cost Center"), flex: 1 },
      { field: "costType", headerName: t("costing.costType", "Cost Type"), flex: 1 },
      {
        field: "standardRate",
        headerName: t("costing.standardRate", "Standard Rate"),
        flex: 1.1,
        type: "numericColumn",
        valueFormatter: (params: { value: number; data?: StandardCost }) =>
          formatCurrency(params.value, params.data?.currency),
      },
      { field: "effectiveFrom", headerName: t("costing.effectiveFrom", "Effective From"), flex: 1 },
      {
        field: "effectiveTo",
        headerName: t("costing.effectiveTo", "Effective To"),
        flex: 1,
        valueFormatter: (params: { value: string | null }) => formatDate(params.value),
      },
      { field: "currency", headerName: t("common.currency", "Currency"), flex: 0.8 },
      {
        field: "notes",
        headerName: t("common.note", "Notes"),
        flex: 1.4,
        valueFormatter: (params: { value: string | null }) => params.value || "-",
      },
    ],
    [t]
  );

  const productColumns = useMemo<ColDef<ProductCost>[]>(
    () => [
      {
        field: "itemCode",
        headerName: t("item.code"),
        flex: 1,
        cellRenderer: (params: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{params.value}</span>
        ),
      },
      { field: "costCenterCode", headerName: t("costing.costCenters", "Cost Center"), flex: 1 },
      { field: "fiscalYear", headerName: t("accounting.fiscalYear", "FY"), flex: 0.7 },
      { field: "period", headerName: t("costing.period", "Period"), flex: 0.7 },
      {
        field: "totalCost",
        headerName: t("costing.totalCost", "Total Cost"),
        flex: 1.1,
        type: "numericColumn",
        valueFormatter: (params: { value: number; data?: ProductCost }) =>
          formatCurrency(params.value, params.data?.currency),
      },
      {
        field: "unitCost",
        headerName: t("costing.unitCost", "Unit Cost"),
        flex: 1,
        type: "numericColumn",
        valueFormatter: (params: { value: number; data?: ProductCost }) =>
          formatCurrency(params.value, params.data?.currency),
      },
      {
        field: "quantity",
        headerName: t("common.quantity", "Qty"),
        flex: 0.8,
        type: "numericColumn",
        valueFormatter: (params: { value: number }) => formatNumber(params.value),
      },
      { field: "currency", headerName: t("common.currency", "Currency"), flex: 0.8 },
      {
        field: "calculated",
        headerName: t("common.status"),
        flex: 0.9,
        cellRenderer: (params: { value: boolean }) => (
          <span className={params.value ? "badge-success" : "badge bg-slate-100 text-slate-600"}>
            {params.value ? t("common.complete", "Calculated") : t("common.pending", "Pending")}
          </span>
        ),
      },
    ],
    [t]
  );

  const allocationColumns = useMemo<ColDef<CostAllocation>[]>(
    () => [
      {
        field: "documentNo",
        headerName: t("costing.allocNo", "Document No"),
        flex: 1.1,
        cellRenderer: (params: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{params.value}</span>
        ),
      },
      { field: "fromCostCenter", headerName: t("costing.from", "From"), flex: 1 },
      { field: "toCostCenter", headerName: t("costing.to", "To"), flex: 1 },
      {
        field: "amount",
        headerName: t("costing.amount", "Amount"),
        flex: 1,
        type: "numericColumn",
        valueFormatter: (params: { value: number }) => formatCurrency(params.value),
      },
      { field: "allocationBasis", headerName: t("costing.base", "Basis"), flex: 1 },
      { field: "fiscalYear", headerName: t("accounting.fiscalYear", "FY"), flex: 0.7 },
      { field: "period", headerName: t("costing.period", "Period"), flex: 0.7 },
      {
        field: "status",
        headerName: t("common.status"),
        flex: 0.8,
        cellRenderer: (params: { value: string }) => (
          <span className={params.value === "POSTED" ? "badge-success" : "badge-info"}>{params.value}</span>
        ),
      },
    ],
    [t]
  );

  const varianceColumns = useMemo<ColDef<VarianceRow>[]>(
    () => [
      {
        field: "itemCode",
        headerName: t("item.code"),
        flex: 1,
        cellRenderer: (params: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{params.value}</span>
        ),
      },
      { field: "costType", headerName: t("costing.costType", "Cost Type"), flex: 1 },
      {
        field: "standardRate",
        headerName: t("costing.standardRate", "Standard Rate"),
        flex: 1,
        type: "numericColumn",
        valueFormatter: (params: { value: number }) => formatCurrency(params.value),
      },
      {
        field: "actualRate",
        headerName: t("costing.actualCost", "Actual Rate"),
        flex: 1,
        type: "numericColumn",
        valueFormatter: (params: { value: number }) => formatCurrency(params.value),
      },
      {
        field: "variance",
        headerName: t("budget.variance", "Variance"),
        flex: 1,
        type: "numericColumn",
        cellRenderer: (params: { value: number }) => (
          <span className={params.value >= 0 ? "text-emerald-600 font-semibold" : "text-red-600 font-semibold"}>
            {formatCurrency(params.value)}
          </span>
        ),
      },
      {
        field: "variancePercentage",
        headerName: t("costing.variancePct", "Variance %"),
        flex: 0.9,
        valueFormatter: (params: { value: number }) => `${Number(params.value ?? 0).toFixed(1)}%`,
      },
    ],
    [t]
  );

  const canCreate = tab === "centers" || tab === "standard" || tab === "allocations";
  const createPending =
    createCenterMutation.isPending || createStandardMutation.isPending || createAllocationMutation.isPending;

  const openCreate = () => {
    if (tab === "centers") setCenterForm(emptyCenterForm());
    if (tab === "standard") setStandardForm(emptyStandardForm());
    if (tab === "allocations") setAllocationForm(emptyAllocationForm());
    setMode("create");
  };

  const handleCreate = () => {
    if (tab === "centers") {
      createCenterMutation.mutate({
        ...centerForm,
        parentCode: centerForm.parentCode?.trim() || null,
        departmentCode: centerForm.departmentCode?.trim() || null,
        managerName: centerForm.managerName?.trim() || null,
      });
      return;
    }

    if (tab === "standard") {
      createStandardMutation.mutate({
        ...standardForm,
        costCenterCode: standardForm.costCenterCode?.trim() || null,
        effectiveTo: standardForm.effectiveTo?.trim() || null,
        notes: standardForm.notes?.trim() || null,
      });
      return;
    }

    if (tab === "allocations") {
      createAllocationMutation.mutate({
        ...allocationForm,
        allocationBasis: allocationForm.allocationBasis?.trim() || null,
        description: allocationForm.description?.trim() || null,
        percentage:
          allocationForm.percentage === undefined || allocationForm.percentage === null
            ? undefined
            : Number(allocationForm.percentage),
      });
    }
  };

  const openCalculateDialog = () => {
    setCalcForm(emptyCalcForm());
    setCalcResult(null);
    setCalcDialog(true);
  };

  const executeVarianceSearch = () => {
    setVarianceSearchCode(varianceInput.trim());
  };

  if (mode === "create") {
    return (
      <div>
        <PageHeader
          title={t("common.create")}
          breadcrumbs={[{ label: t("nav.costing") }, { label: t("common.create") }]}
          actions={
            <button className="btn-ghost" onClick={() => setMode("list")}>
              <ArrowLeft size={16} /> {t("common.back")}
            </button>
          }
        />

        <div className="section-card">
          <p className="section-kicker">{t("nav.costing")}</p>
          <h3 className="section-title">
            {tab === "centers" && t("costing.costCenters", "Cost Centers")}
            {tab === "standard" && t("costing.standardCosts", "Standard Costs")}
            {tab === "allocations" && t("costing.allocations", "Allocations")}
          </h3>

          <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
            {tab === "centers" && (
              <>
                <div>
                  <label className="field-label">{t("common.code")}</label>
                  <input
                    className="input"
                    value={centerForm.costCenterCode}
                    onChange={(event) => setCenterForm((prev) => ({ ...prev, costCenterCode: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="field-label">{t("common.name")}</label>
                  <input
                    className="input"
                    value={centerForm.costCenterName}
                    onChange={(event) => setCenterForm((prev) => ({ ...prev, costCenterName: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="field-label">{t("nav.departments", "Department")}</label>
                  <input
                    className="input"
                    value={centerForm.departmentCode ?? ""}
                    onChange={(event) => setCenterForm((prev) => ({ ...prev, departmentCode: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="field-label">{t("costing.parent", "Parent")}</label>
                  <input
                    className="input"
                    value={centerForm.parentCode ?? ""}
                    onChange={(event) => setCenterForm((prev) => ({ ...prev, parentCode: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="field-label">{t("costing.manager", "Manager")}</label>
                  <input
                    className="input"
                    value={centerForm.managerName ?? ""}
                    onChange={(event) => setCenterForm((prev) => ({ ...prev, managerName: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="field-label">{t("common.status")}</label>
                  <select
                    className="input"
                    value={centerForm.status}
                    onChange={(event) => setCenterForm((prev) => ({ ...prev, status: event.target.value }))}
                  >
                    {centerStatusOptions.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                </div>
              </>
            )}

            {tab === "standard" && (
              <>
                <div>
                  <label className="field-label">{t("item.code")}</label>
                  <input
                    className="input"
                    value={standardForm.itemCode}
                    onChange={(event) => setStandardForm((prev) => ({ ...prev, itemCode: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="field-label">{t("costing.costCenters", "Cost Center")}</label>
                  <select
                    className="input"
                    value={standardForm.costCenterCode ?? ""}
                    onChange={(event) => setStandardForm((prev) => ({ ...prev, costCenterCode: event.target.value }))}
                  >
                    <option value="">{t("common.all", "All")}</option>
                    {costCenters.map((center) => (
                      <option key={center.id} value={center.costCenterCode}>
                        {center.costCenterCode} · {center.costCenterName}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t("costing.costType", "Cost Type")}</label>
                  <select
                    className="input"
                    value={standardForm.costType}
                    onChange={(event) => setStandardForm((prev) => ({ ...prev, costType: event.target.value }))}
                  >
                    {costTypeOptions.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t("costing.standardRate", "Standard Rate")}</label>
                  <input
                    className="input"
                    type="number"
                    min="0"
                    value={standardForm.standardRate}
                    onChange={(event) =>
                      setStandardForm((prev) => ({ ...prev, standardRate: Number(event.target.value) }))
                    }
                  />
                </div>
                <div>
                  <label className="field-label">{t("costing.effectiveFrom", "Effective From")}</label>
                  <input
                    className="input"
                    type="date"
                    value={standardForm.effectiveFrom}
                    onChange={(event) =>
                      setStandardForm((prev) => ({ ...prev, effectiveFrom: event.target.value }))
                    }
                  />
                </div>
                <div>
                  <label className="field-label">{t("costing.effectiveTo", "Effective To")}</label>
                  <input
                    className="input"
                    type="date"
                    value={standardForm.effectiveTo ?? ""}
                    onChange={(event) => setStandardForm((prev) => ({ ...prev, effectiveTo: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="field-label">{t("common.currency", "Currency")}</label>
                  <input
                    className="input"
                    value={standardForm.currency ?? "KRW"}
                    onChange={(event) => setStandardForm((prev) => ({ ...prev, currency: event.target.value }))}
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="field-label">{t("common.note", "Notes")}</label>
                  <input
                    className="input"
                    value={standardForm.notes ?? ""}
                    onChange={(event) => setStandardForm((prev) => ({ ...prev, notes: event.target.value }))}
                  />
                </div>
              </>
            )}

            {tab === "allocations" && (
              <>
                <div>
                  <label className="field-label">{t("costing.from", "From")}</label>
                  <select
                    className="input"
                    value={allocationForm.fromCostCenter}
                    onChange={(event) =>
                      setAllocationForm((prev) => ({ ...prev, fromCostCenter: event.target.value }))
                    }
                  >
                    <option value="">{t("common.select", "Select")}</option>
                    {costCenters.map((center) => (
                      <option key={center.id} value={center.costCenterCode}>
                        {center.costCenterCode} · {center.costCenterName}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t("costing.to", "To")}</label>
                  <select
                    className="input"
                    value={allocationForm.toCostCenter}
                    onChange={(event) =>
                      setAllocationForm((prev) => ({ ...prev, toCostCenter: event.target.value }))
                    }
                  >
                    <option value="">{t("common.select", "Select")}</option>
                    {costCenters.map((center) => (
                      <option key={center.id} value={center.costCenterCode}>
                        {center.costCenterCode} · {center.costCenterName}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t("costing.amount", "Amount")}</label>
                  <input
                    className="input"
                    type="number"
                    min="0"
                    value={allocationForm.amount}
                    onChange={(event) =>
                      setAllocationForm((prev) => ({ ...prev, amount: Number(event.target.value) }))
                    }
                  />
                </div>
                <div>
                  <label className="field-label">{t("costing.base", "Basis")}</label>
                  <input
                    className="input"
                    value={allocationForm.allocationBasis ?? ""}
                    onChange={(event) =>
                      setAllocationForm((prev) => ({ ...prev, allocationBasis: event.target.value }))
                    }
                  />
                </div>
                <div>
                  <label className="field-label">{t("costing.allocationType", "Allocation Type")}</label>
                  <select
                    className="input"
                    value={allocationForm.allocationType}
                    onChange={(event) =>
                      setAllocationForm((prev) => ({ ...prev, allocationType: event.target.value }))
                    }
                  >
                    {allocationTypeOptions.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t("accounting.fiscalYear", "FY")}</label>
                  <input
                    className="input"
                    type="number"
                    value={allocationForm.fiscalYear}
                    onChange={(event) =>
                      setAllocationForm((prev) => ({ ...prev, fiscalYear: Number(event.target.value) }))
                    }
                  />
                </div>
                <div>
                  <label className="field-label">{t("costing.period", "Period")}</label>
                  <input
                    className="input"
                    type="number"
                    min="1"
                    max="12"
                    value={allocationForm.period}
                    onChange={(event) =>
                      setAllocationForm((prev) => ({ ...prev, period: Number(event.target.value) }))
                    }
                  />
                </div>
                <div>
                  <label className="field-label">{t("common.date", "Date")}</label>
                  <input
                    className="input"
                    type="date"
                    value={allocationForm.allocationDate ?? today}
                    onChange={(event) =>
                      setAllocationForm((prev) => ({ ...prev, allocationDate: event.target.value }))
                    }
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="field-label">{t("common.description", "Description")}</label>
                  <input
                    className="input"
                    value={allocationForm.description ?? ""}
                    onChange={(event) =>
                      setAllocationForm((prev) => ({ ...prev, description: event.target.value }))
                    }
                  />
                </div>
              </>
            )}
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button className="btn-ghost" onClick={() => setMode("list")}>
            {t("common.cancel")}
          </button>
          <button className="btn-primary" onClick={handleCreate} disabled={createPending}>
            {createPending ? t("common.saving") : t("common.save")}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title={t("costing.title")}
        description={t("costing.description")}
        breadcrumbs={[{ label: t("nav.costing") }]}
        actions={
          <div className="flex gap-2">
            <button className="btn-secondary" onClick={openCalculateDialog}>
              <Calculator size={16} /> {t("costing.calculate", "Calculate")}
            </button>
            {canCreate && (
              <button className="btn-primary" onClick={openCreate}>
                <Plus size={16} /> {t("common.new")}
              </button>
            )}
          </div>
        }
      />

      <div className="mb-4 flex flex-wrap gap-2">
        <button className={tab === "centers" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("centers")}>
          {t("costing.costCenters", "Cost Centers")}
        </button>
        <button className={tab === "standard" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("standard")}>
          {t("costing.standardCosts", "Standard Costs")}
        </button>
        <button className={tab === "products" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("products")}>
          {t("costing.productCosts", "Product Costs")}
        </button>
        <button className={tab === "allocations" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("allocations")}>
          {t("costing.allocations", "Allocations")}
        </button>
        <button className={tab === "variance" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("variance")}>
          {t("costing.varianceAnalysis", "Variance")}
        </button>
      </div>

      {tab === "variance" && (
        <div className="card mb-4 p-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-end">
            <div className="flex-1">
              <label className="field-label">{t("item.code")}</label>
              <input
                className="input"
                value={varianceInput}
                onChange={(event) => setVarianceInput(event.target.value)}
                placeholder={t("common.search", "Search")}
              />
            </div>
            <button className="btn-secondary" onClick={executeVarianceSearch}>
              <Search size={16} /> {t("common.search", "Search")}
            </button>
          </div>
        </div>
      )}

      <div className="card overflow-hidden">
        {tab === "centers" && (
          <DataGrid<CostCenter>
            gridId="costing.cost-centers"
            rowData={costCenters}
            columnDefs={centerColumns}
            loading={centersQ.isLoading}
          />
        )}

        {tab === "standard" && (
          <DataGrid<StandardCost>
            gridId="costing.standard-costs"
            rowData={standardCosts}
            columnDefs={standardColumns}
            loading={standardQ.isLoading}
          />
        )}

        {tab === "products" && (
          <DataGrid<ProductCost>
            gridId="costing.product-costs"
            rowData={productCosts}
            columnDefs={productColumns}
            loading={productQ.isLoading}
          />
        )}

        {tab === "allocations" && (
          <DataGrid<CostAllocation>
            gridId="costing.allocations"
            rowData={allocations}
            columnDefs={allocationColumns}
            loading={allocationQ.isLoading}
          />
        )}

        {tab === "variance" && (
          <DataGrid<VarianceRow>
            gridId="costing.variance"
            rowData={varianceRows}
            columnDefs={varianceColumns}
            loading={varianceQ.isLoading}
            emptyTitle={varianceSearchCode ? t("common.noData", "No data") : t("common.search", "Search")}
            emptyDescription={
              varianceSearchCode
                ? t("costing.noVarianceData", "No variance rows matched the selected item.")
                : t("costing.enterVarianceItem", "Enter an item code to load variance rows.")
            }
          />
        )}
      </div>

      {calcDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 px-4 backdrop-blur-sm">
          <div className="w-full max-w-2xl rounded-[28px] border border-white/70 bg-white/95 p-6 shadow-[0_32px_80px_rgba(15,23,42,0.28)]">
            <p className="section-kicker">{t("costing.productCosts", "Product Costs")}</p>
            <h3 className="section-title">{t("costing.calculate", "Calculate")}</h3>

            <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <label className="field-label">{t("item.code")}</label>
                <input
                  className="input"
                  value={calcForm.itemCode}
                  onChange={(event) => setCalcForm((prev) => ({ ...prev, itemCode: event.target.value }))}
                />
              </div>
              <div>
                <label className="field-label">{t("costing.costCenters", "Cost Center")}</label>
                <select
                  className="input"
                  value={calcForm.costCenterCode ?? ""}
                  onChange={(event) => setCalcForm((prev) => ({ ...prev, costCenterCode: event.target.value }))}
                >
                  <option value="">{t("common.all", "All")}</option>
                  {costCenters.map((center) => (
                    <option key={center.id} value={center.costCenterCode}>
                      {center.costCenterCode} · {center.costCenterName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="field-label">{t("accounting.fiscalYear", "FY")}</label>
                <input
                  className="input"
                  type="number"
                  value={calcForm.fiscalYear}
                  onChange={(event) =>
                    setCalcForm((prev) => ({ ...prev, fiscalYear: Number(event.target.value) }))
                  }
                />
              </div>
              <div>
                <label className="field-label">{t("costing.period", "Period")}</label>
                <input
                  className="input"
                  type="number"
                  min="1"
                  max="12"
                  value={calcForm.period}
                  onChange={(event) => setCalcForm((prev) => ({ ...prev, period: Number(event.target.value) }))}
                />
              </div>
              <div>
                <label className="field-label">{t("common.quantity", "Qty")}</label>
                <input
                  className="input"
                  type="number"
                  min="0"
                  step="0.01"
                  value={calcForm.quantity ?? 1}
                  onChange={(event) => setCalcForm((prev) => ({ ...prev, quantity: Number(event.target.value) }))}
                />
              </div>
            </div>

            {calcResult && (
              <div className="mt-5 grid grid-cols-1 gap-3 rounded-[22px] bg-slate-50 p-4 md:grid-cols-3">
                <div className="rounded-2xl bg-white p-4">
                  <p className="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">
                    {t("costing.totalCost", "Total Cost")}
                  </p>
                  <p className="mt-2 text-xl font-semibold text-slate-900">
                    {formatCurrency(calcResult.totalCost, calcResult.currency)}
                  </p>
                </div>
                <div className="rounded-2xl bg-white p-4">
                  <p className="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">
                    {t("costing.unitCost", "Unit Cost")}
                  </p>
                  <p className="mt-2 text-xl font-semibold text-slate-900">
                    {formatCurrency(calcResult.unitCost, calcResult.currency)}
                  </p>
                </div>
                <div className="rounded-2xl bg-white p-4">
                  <p className="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">
                    {t("common.quantity", "Qty")}
                  </p>
                  <p className="mt-2 text-xl font-semibold text-slate-900">{formatNumber(calcResult.quantity)}</p>
                </div>
                <div className="rounded-2xl bg-white p-4">
                  <p className="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">Material</p>
                  <p className="mt-2 text-base font-semibold text-slate-900">
                    {formatCurrency(calcResult.materialCost, calcResult.currency)}
                  </p>
                </div>
                <div className="rounded-2xl bg-white p-4">
                  <p className="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">Labor</p>
                  <p className="mt-2 text-base font-semibold text-slate-900">
                    {formatCurrency(calcResult.laborCost, calcResult.currency)}
                  </p>
                </div>
                <div className="rounded-2xl bg-white p-4">
                  <p className="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">Overhead</p>
                  <p className="mt-2 text-base font-semibold text-slate-900">
                    {formatCurrency(calcResult.overheadCost, calcResult.currency)}
                  </p>
                </div>
              </div>
            )}

            <div className="mt-6 flex justify-end gap-3">
              <button className="btn-ghost" onClick={() => setCalcDialog(false)}>
                {t("common.cancel")}
              </button>
              <button
                className="btn-primary"
                onClick={() =>
                  calculateMutation.mutate({
                    ...calcForm,
                    costCenterCode: calcForm.costCenterCode?.trim() || null,
                  })
                }
                disabled={calculateMutation.isPending}
              >
                {calculateMutation.isPending ? t("common.loading") : t("costing.calculate", "Calculate")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
