import { useState, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, X, Factory, Clock, CheckCircle } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface WoRow {
  id: number;
  documentNo: string;
  productCode: string;
  productName: string;
  plannedQuantity: number;
  completedQuantity: number;
  scrapQuantity: number;
  yieldRate: number;
  status: string;
  priority: string;
  orderType: string;
  plantCode: string;
  plannedStartDate: string | null;
  plannedEndDate: string | null;
}

interface CreateForm {
  companyCode: string;
  plantCode: string;
  productCode: string;
  productName: string;
  plannedQuantity: number;
  unitOfMeasure: string;
  orderType: string;
  priority: string;
  salesOrderNo: string;
  plannedStartDate: string;
  plannedEndDate: string;
  remark: string;
  autoPopulate: boolean;
}

const statusStyle: Record<string, string> = {
  PLANNED: "badge bg-slate-100 text-slate-600",
  RELEASED: "badge-info",
  IN_PROGRESS: "badge-warning",
  COMPLETED: "badge-success",
  CLOSED: "badge bg-slate-100 text-slate-500",
};

const priorityStyle: Record<string, string> = {
  LOW: "text-slate-400",
  NORMAL: "text-slate-600",
  HIGH: "text-amber-600 font-semibold",
  URGENT: "text-red-600 font-bold",
};

const emptyForm: CreateForm = {
  companyCode: "",
  plantCode: "",
  productCode: "",
  productName: "",
  plannedQuantity: 0,
  unitOfMeasure: "EA",
  orderType: "STANDARD",
  priority: "NORMAL",
  salesOrderNo: "",
  plannedStartDate: "",
  plannedEndDate: "",
  remark: "",
  autoPopulate: true,
};

export default function WorkOrderPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<CreateForm>({ ...emptyForm });
  const [statusFilter, setStatusFilter] = useState("");
  const [plantFilter, setPlantFilter] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["work-orders"],
    queryFn: async () => (await api.get("/api/v1/production/work-orders?size=100")).data,
  });

  const createMutation = useMutation({
    mutationFn: async (payload: CreateForm) =>
      (await api.post("/api/v1/production/work-orders", payload)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["work-orders"] });
      setShowCreate(false);
      setForm({ ...emptyForm });
    },
  });

  const allRows: WoRow[] = data?.data || [];

  const rows = useMemo(() => {
    let filtered = allRows;
    if (statusFilter) filtered = filtered.filter((r) => r.status === statusFilter);
    if (plantFilter) filtered = filtered.filter((r) => r.plantCode === plantFilter);
    return filtered;
  }, [allRows, statusFilter, plantFilter]);

  const inProgress = allRows.filter((r) => r.status === "IN_PROGRESS").length;
  const planned = allRows.filter((r) => r.status === "PLANNED" || r.status === "RELEASED").length;
  const completed = allRows.filter((r) => r.status === "COMPLETED").length;

  const columnDefs = useMemo<ColDef<WoRow>[]>(
    () => [
      {
        field: "documentNo",
        headerName: t("wo.docNo"),
        flex: 1.2,
        cellRenderer: (p: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{p.value}</span>
        ),
      },
      { field: "productName", headerName: t("wo.product"), flex: 2 },
      { field: "plannedQuantity", headerName: t("wo.planned"), flex: 0.8, type: "numericColumn" },
      { field: "completedQuantity", headerName: t("wo.completed"), flex: 0.8, type: "numericColumn" },
      {
        field: "scrapQuantity",
        headerName: t("wo.scrap"),
        flex: 0.7,
        type: "numericColumn",
        cellRenderer: (p: { value: number }) =>
          p.value > 0 ? <span className="text-red-600">{p.value}</span> : <span className="text-slate-300">0</span>,
      },
      {
        field: "yieldRate",
        headerName: t("wo.yield"),
        flex: 0.7,
        valueFormatter: (p: { value: number }) => `${(p.value * 100).toFixed(1)}%`,
        cellClass: (p: { value: number }) => (p.value < 0.95 ? "text-red-600" : "text-emerald-600"),
      },
      {
        field: "priority",
        headerName: t("wo.priority"),
        flex: 0.7,
        cellRenderer: (p: { value: string }) => (
          <span className={priorityStyle[p.value] || ""}>{p.value}</span>
        ),
      },
      { field: "plannedStartDate", headerName: t("wo.startDate"), flex: 1 },
      {
        field: "status",
        headerName: t("common.status"),
        flex: 0.9,
        cellRenderer: (p: { value: string }) => (
          <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span>
        ),
      },
    ],
    [t]
  );

  const handleRowClick = useCallback(
    (row: WoRow) => navigate(`/production/work-orders/${row.id}`),
    [navigate]
  );

  const handleOpenCreate = () => {
    setForm({ ...emptyForm });
    setShowCreate(true);
  };

  const handleSave = () => {
    createMutation.mutate(form);
  };

  const updateField = <K extends keyof CreateForm>(key: K, value: CreateForm[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const statuses = ["PLANNED", "RELEASED", "IN_PROGRESS", "COMPLETED", "CLOSED"];
  const plants = [...new Set(allRows.map((r) => r.plantCode).filter(Boolean))];

  return (
    <div>
      <PageHeader
        title={t("wo.title")}
        description={t("wo.description")}
        breadcrumbs={[{ label: t("nav.production") }, { label: t("nav.workOrders") }]}
        actions={
          <button className="btn-primary" onClick={handleOpenCreate}>
            <Plus size={16} /> {t("wo.newWo")}
          </button>
        }
      />

      {/* Stats */}
      <div className="workspace-hero mb-6">
        <p className="section-kicker">Production Workspace</p>
        <h3 className="section-title">{t("wo.title")}</h3>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-4">
          <div className="stat-tile">
            <span className="field-label"><Factory size={16} className="inline mr-1 text-amber-600" />{t("wo.inProgress")}</span>
            <span className="text-2xl font-bold text-slate-900">{inProgress}</span>
          </div>
          <div className="stat-tile">
            <span className="field-label"><Clock size={16} className="inline mr-1 text-blue-600" />{t("wo.plannedReleased")}</span>
            <span className="text-2xl font-bold text-slate-900">{planned}</span>
          </div>
          <div className="stat-tile">
            <span className="field-label"><CheckCircle size={16} className="inline mr-1 text-emerald-600" />{t("wo.completedWo")}</span>
            <span className="text-2xl font-bold text-slate-900">{completed}</span>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-3 mb-4">
        <div className="relative">
          <select
            className="input w-44 appearance-none pr-8"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">{t("wo.filterByStatus")}: {t("common.all")}</option>
            {statuses.map((s) => (
              <option key={s} value={s}>
                {String(t("status." + s, s))}
              </option>
            ))}
          </select>
        </div>
        <div className="relative">
          <select
            className="input w-36 appearance-none pr-8"
            value={plantFilter}
            onChange={(e) => setPlantFilter(e.target.value)}
          >
            <option value="">{t("wo.filterByPlant")}: {t("common.all")}</option>
            {plants.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Grid */}
      <div className="card overflow-hidden">
        <DataGrid<WoRow>
          rowData={rows}
          columnDefs={columnDefs}
          loading={isLoading}
          onRowClicked={handleRowClick}
        />
      </div>

      {/* Create Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 backdrop-blur-sm">
          <div className="rounded-[28px] border border-white/70 bg-white/95 p-6 shadow-[0_32px_80px_rgba(15,23,42,0.28)] w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-5">
              <div>
                <p className="section-kicker">{t("wo.title")}</p>
                <h3 className="section-title">{t("wo.newWo")}</h3>
              </div>
              <button className="text-slate-400 hover:text-slate-600" onClick={() => setShowCreate(false)}>
                <X size={20} />
              </button>
            </div>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="field-label">{t("wo.companyCode")}</label>
                  <input className="input w-full" value={form.companyCode} onChange={(e) => updateField("companyCode", e.target.value)} />
                </div>
                <div>
                  <label className="field-label">{t("wo.plantCode")}</label>
                  <input className="input w-full" value={form.plantCode} onChange={(e) => updateField("plantCode", e.target.value)} />
                </div>
                <div>
                  <label className="field-label">{t("wo.productCode")}</label>
                  <input className="input w-full" value={form.productCode} onChange={(e) => updateField("productCode", e.target.value)} />
                </div>
                <div>
                  <label className="field-label">{t("wo.productName")}</label>
                  <input className="input w-full" value={form.productName} onChange={(e) => updateField("productName", e.target.value)} />
                </div>
                <div>
                  <label className="field-label">{t("wo.plannedQty")}</label>
                  <input className="input w-full" type="number" value={form.plannedQuantity} onChange={(e) => updateField("plannedQuantity", +e.target.value)} />
                </div>
                <div>
                  <label className="field-label">{t("wo.uom")}</label>
                  <input className="input w-full" value={form.unitOfMeasure} onChange={(e) => updateField("unitOfMeasure", e.target.value)} />
                </div>
                <div>
                  <label className="field-label">{t("wo.orderType")}</label>
                  <select className="input w-full" value={form.orderType} onChange={(e) => updateField("orderType", e.target.value)}>
                    <option value="STANDARD">Standard</option>
                    <option value="REWORK">Rework</option>
                    <option value="PROTOTYPE">Prototype</option>
                  </select>
                </div>
                <div>
                  <label className="field-label">{t("wo.priority")}</label>
                  <select className="input w-full" value={form.priority} onChange={(e) => updateField("priority", e.target.value)}>
                    <option value="LOW">LOW</option>
                    <option value="NORMAL">NORMAL</option>
                    <option value="HIGH">HIGH</option>
                    <option value="URGENT">URGENT</option>
                  </select>
                </div>
                <div>
                  <label className="field-label">{t("wo.salesOrderNo")}</label>
                  <input className="input w-full" value={form.salesOrderNo} onChange={(e) => updateField("salesOrderNo", e.target.value)} placeholder="Optional" />
                </div>
                <div>
                  <label className="field-label">{t("wo.startDate")}</label>
                  <input className="input w-full" type="date" value={form.plannedStartDate} onChange={(e) => updateField("plannedStartDate", e.target.value)} />
                </div>
                <div>
                  <label className="field-label">{t("wo.endDate")}</label>
                  <input className="input w-full" type="date" value={form.plannedEndDate} onChange={(e) => updateField("plannedEndDate", e.target.value)} />
                </div>
              </div>
              <div>
                <label className="field-label">{t("wo.remark")}</label>
                <textarea className="input w-full" rows={2} value={form.remark} onChange={(e) => updateField("remark", e.target.value)} />
              </div>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input type="checkbox" checked={form.autoPopulate} onChange={(e) => updateField("autoPopulate", e.target.checked)} className="rounded border-slate-300" />
                {t("wo.autoPopulate")}
              </label>
            </div>
            <div className="flex justify-end gap-3 mt-6 pt-5 border-t border-slate-100">
              <button className="btn-secondary" onClick={() => setShowCreate(false)}>
                {t("common.cancel")}
              </button>
              <button className="btn-primary" onClick={handleSave} disabled={createMutation.isPending}>
                {createMutation.isPending ? t("common.saving") : t("common.save")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
