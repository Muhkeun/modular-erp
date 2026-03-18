import { useState, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, Download, Upload, Filter } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api, { type ApiResponse } from "../../../shared/api/client";

interface ItemRow {
  id: number;
  code: string;
  name: string;
  itemType: string;
  itemGroup: string | null;
  unitOfMeasure: string;
  specification: string | null;
  makerName: string | null;
  qualityInspectionRequired: boolean;
  active: boolean;
}

export default function ItemListPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [filters, setFilters] = useState({ code: "", itemType: "", itemGroup: "" });
  const [showFilters, setShowFilters] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ["items", filters],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filters.code) params.set("code", filters.code);
      if (filters.itemType) params.set("itemType", filters.itemType);
      if (filters.itemGroup) params.set("itemGroup", filters.itemGroup);
      params.set("size", "100");
      const res = await api.get<ApiResponse<ItemRow[]>>(`/api/v1/master-data/items?${params}`);
      return res.data;
    },
  });

  const columnDefs = useMemo<ColDef<ItemRow>[]>(
    () => [
      {
        field: "code",
        headerName: t("item.code"),
        flex: 1.2,
        cellRenderer: (params: { value: string }) => (
          <span className="font-mono text-sm font-semibold text-brand-700">{params.value}</span>
        ),
      },
      { field: "name", headerName: t("item.name"), flex: 2 },
      {
        field: "itemType",
        headerName: t("item.type"),
        flex: 1,
        cellRenderer: (params: { value: string }) => {
          const typeClasses: Record<string, string> = {
            MATERIAL: "badge-info",
            PRODUCT: "badge-success",
            SEMI_PRODUCT: "badge-warning",
            SERVICE: "bg-purple-50 text-purple-700 ring-1 ring-inset ring-purple-600/20",
            ASSET: "bg-slate-50 text-slate-700 ring-1 ring-inset ring-slate-600/20",
          };
          const cls = typeClasses[params.value] || "badge";
          return <span className={`badge ${cls}`}>{t("item.types." + params.value, params.value)}</span>;
        },
      },
      { field: "itemGroup", headerName: t("item.group"), flex: 1 },
      { field: "unitOfMeasure", headerName: t("item.uom"), flex: 0.6, maxWidth: 80 },
      { field: "specification", headerName: t("item.spec"), flex: 1.5 },
      { field: "makerName", headerName: t("item.maker"), flex: 1 },
      {
        field: "qualityInspectionRequired",
        headerName: t("item.qi"),
        flex: 0.5,
        maxWidth: 70,
        cellRenderer: (params: { value: boolean }) =>
          params.value ? <span className="badge-warning">Y</span> : <span className="text-slate-300">-</span>,
      },
      {
        field: "active",
        headerName: t("common.status"),
        flex: 0.7,
        maxWidth: 100,
        cellRenderer: (params: { value: boolean }) =>
          params.value ? <span className="badge-success">{t("common.active")}</span> : <span className="badge-danger">{t("common.inactive")}</span>,
      },
    ],
    [t]
  );

  const handleRowClick = useCallback(
    (row: ItemRow) => navigate(`/master-data/items/${row.id}`),
    [navigate]
  );

  return (
    <div>
      <PageHeader
        title={t("item.title")}
        description={t("item.description")}
        breadcrumbs={[
          { label: t("nav.masterData"), path: "/master-data" },
          { label: t("nav.items") },
        ]}
        actions={
          <>
            <button className="btn-secondary" onClick={() => setShowFilters(!showFilters)}>
              <Filter size={16} /> {t("common.filters")}
            </button>
            <button className="btn-secondary"><Upload size={16} /> {t("common.import")}</button>
            <button className="btn-secondary"><Download size={16} /> {t("common.export")}</button>
            <button className="btn-primary" onClick={() => navigate("/master-data/items/new")}>
              <Plus size={16} /> {t("item.newItem")}
            </button>
          </>
        }
      />

      {/* Filter bar */}
      {showFilters && (
        <div className="card p-4 mb-4">
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">{t("item.code")}</label>
              <input className="input" placeholder={t("common.search") + "..."}
                value={filters.code} onChange={(e) => setFilters({ ...filters, code: e.target.value })} />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">{t("item.type")}</label>
              <select className="input" value={filters.itemType}
                onChange={(e) => setFilters({ ...filters, itemType: e.target.value })}>
                <option value="">{t("common.all")}</option>
                <option value="MATERIAL">{t("item.types.MATERIAL")}</option>
                <option value="PRODUCT">{t("item.types.PRODUCT")}</option>
                <option value="SEMI_PRODUCT">{t("item.types.SEMI_PRODUCT")}</option>
                <option value="SERVICE">{t("item.types.SERVICE")}</option>
                <option value="ASSET">{t("item.types.ASSET")}</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">{t("item.group")}</label>
              <input className="input" placeholder={t("common.filter") + "..."}
                value={filters.itemGroup} onChange={(e) => setFilters({ ...filters, itemGroup: e.target.value })} />
            </div>
            <div className="flex items-end gap-2">
              <button className="btn-secondary" onClick={() => setFilters({ code: "", itemType: "", itemGroup: "" })}>
                {t("common.clear")}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Grid */}
      <div className="card overflow-hidden">
        <DataGrid<ItemRow>
          rowData={data?.data || []}
          columnDefs={columnDefs}
          loading={isLoading}
          onRowClicked={handleRowClick}
        />
      </div>
    </div>
  );
}
