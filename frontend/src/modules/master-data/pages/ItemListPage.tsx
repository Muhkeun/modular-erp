import { useState, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
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

const itemTypeLabels: Record<string, { label: string; class: string }> = {
  MATERIAL: { label: "Material", class: "badge-info" },
  PRODUCT: { label: "Product", class: "badge-success" },
  SEMI_PRODUCT: { label: "Semi Product", class: "badge-warning" },
  SERVICE: { label: "Service", class: "bg-purple-50 text-purple-700 ring-1 ring-inset ring-purple-600/20" },
  ASSET: { label: "Asset", class: "bg-slate-50 text-slate-700 ring-1 ring-inset ring-slate-600/20" },
};

export default function ItemListPage() {
  const navigate = useNavigate();
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
        headerName: "Item Code",
        flex: 1.2,
        cellRenderer: (params: { value: string }) => (
          <span className="font-mono text-sm font-semibold text-brand-700">{params.value}</span>
        ),
      },
      { field: "name", headerName: "Item Name", flex: 2 },
      {
        field: "itemType",
        headerName: "Type",
        flex: 1,
        cellRenderer: (params: { value: string }) => {
          const t = itemTypeLabels[params.value] || { label: params.value, class: "badge" };
          return <span className={`badge ${t.class}`}>{t.label}</span>;
        },
      },
      { field: "itemGroup", headerName: "Group", flex: 1 },
      { field: "unitOfMeasure", headerName: "UoM", flex: 0.6, maxWidth: 80 },
      { field: "specification", headerName: "Spec", flex: 1.5 },
      { field: "makerName", headerName: "Maker", flex: 1 },
      {
        field: "qualityInspectionRequired",
        headerName: "QI",
        flex: 0.5,
        maxWidth: 70,
        cellRenderer: (params: { value: boolean }) =>
          params.value ? <span className="badge-warning">Y</span> : <span className="text-slate-300">-</span>,
      },
      {
        field: "active",
        headerName: "Status",
        flex: 0.7,
        maxWidth: 100,
        cellRenderer: (params: { value: boolean }) =>
          params.value ? <span className="badge-success">Active</span> : <span className="badge-danger">Inactive</span>,
      },
    ],
    []
  );

  const handleRowClick = useCallback(
    (row: ItemRow) => navigate(`/master-data/items/${row.id}`),
    [navigate]
  );

  return (
    <div>
      <PageHeader
        title="Items"
        description="Manage item master data across your organization"
        breadcrumbs={[
          { label: "Master Data", path: "/master-data" },
          { label: "Items" },
        ]}
        actions={
          <>
            <button className="btn-secondary" onClick={() => setShowFilters(!showFilters)}>
              <Filter size={16} /> Filters
            </button>
            <button className="btn-secondary"><Upload size={16} /> Import</button>
            <button className="btn-secondary"><Download size={16} /> Export</button>
            <button className="btn-primary" onClick={() => navigate("/master-data/items/new")}>
              <Plus size={16} /> New Item
            </button>
          </>
        }
      />

      {/* Filter bar */}
      {showFilters && (
        <div className="card p-4 mb-4">
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">Item Code</label>
              <input className="input" placeholder="Search code..."
                value={filters.code} onChange={(e) => setFilters({ ...filters, code: e.target.value })} />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">Type</label>
              <select className="input" value={filters.itemType}
                onChange={(e) => setFilters({ ...filters, itemType: e.target.value })}>
                <option value="">All</option>
                <option value="MATERIAL">Material</option>
                <option value="PRODUCT">Product</option>
                <option value="SEMI_PRODUCT">Semi Product</option>
                <option value="SERVICE">Service</option>
                <option value="ASSET">Asset</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">Group</label>
              <input className="input" placeholder="Filter group..."
                value={filters.itemGroup} onChange={(e) => setFilters({ ...filters, itemGroup: e.target.value })} />
            </div>
            <div className="flex items-end gap-2">
              <button className="btn-secondary" onClick={() => setFilters({ code: "", itemType: "", itemGroup: "" })}>
                Clear
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
