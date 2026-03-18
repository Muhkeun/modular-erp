import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Package, AlertTriangle, TrendingUp } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import StatsCard from "../../../shared/components/StatsCard";
import api from "../../../shared/api/client";

interface StockRow {
  id: number;
  itemCode: string;
  itemName: string;
  plantCode: string;
  storageLocation: string;
  unitOfMeasure: string;
  quantityOnHand: number;
  quantityReserved: number;
  availableQuantity: number;
  totalValue: number;
}

const LOW_STOCK_THRESHOLD = 10;

export default function StockOverviewPage() {
  const { t } = useTranslation();
  const [filterPlant, setFilterPlant] = useState("");
  const [filterItem, setFilterItem] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["stock"],
    queryFn: async () => (await api.get("/api/v1/logistics/stock?size=200")).data,
  });

  const allRows: StockRow[] = data?.data || [];

  /* ── Filtered rows ── */
  const rows = useMemo(() => {
    let result = allRows;
    if (filterPlant) {
      const q = filterPlant.toLowerCase();
      result = result.filter((r) => r.plantCode.toLowerCase().includes(q));
    }
    if (filterItem) {
      const q = filterItem.toLowerCase();
      result = result.filter(
        (r) => r.itemCode.toLowerCase().includes(q) || r.itemName.toLowerCase().includes(q)
      );
    }
    return result;
  }, [allRows, filterPlant, filterItem]);

  /* ── Summary stats ── */
  const totalItems = rows.length;
  const outOfStock = rows.filter((r) => r.availableQuantity <= 0).length;
  const totalValue = rows.reduce((sum, r) => sum + (r.totalValue || 0), 0);

  /* ── Column Defs ── */
  const columnDefs = useMemo<ColDef<StockRow>[]>(
    () => [
      {
        field: "itemCode",
        headerName: t("stock.itemCode"),
        flex: 1,
        cellRenderer: (p: { value: string }) => (
          <span className="font-mono font-semibold text-brand-700">{p.value}</span>
        ),
      },
      { field: "itemName", headerName: t("stock.itemName"), flex: 2 },
      { field: "plantCode", headerName: t("gr.plant"), flex: 0.7 },
      { field: "storageLocation", headerName: t("stock.location"), flex: 0.8 },
      { field: "unitOfMeasure", headerName: t("item.uom"), flex: 0.5 },
      { field: "quantityOnHand", headerName: t("stock.onHand"), flex: 0.8, type: "numericColumn" },
      { field: "quantityReserved", headerName: t("stock.reserved"), flex: 0.8, type: "numericColumn" },
      {
        field: "availableQuantity",
        headerName: t("stock.available"),
        flex: 0.8,
        type: "numericColumn",
        cellRenderer: (p: { value: number }) => {
          const cls =
            p.value <= 0
              ? "text-red-600 font-bold"
              : p.value <= LOW_STOCK_THRESHOLD
                ? "text-amber-600 font-semibold"
                : "text-emerald-600 font-semibold";
          return <span className={cls}>{p.value}</span>;
        },
      },
      {
        field: "totalValue",
        headerName: t("stock.value"),
        flex: 1.2,
        type: "numericColumn",
        valueFormatter: (p: { value: number }) =>
          p.value?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 }),
      },
    ],
    [t]
  );

  return (
    <div>
      <PageHeader
        title={t("stock.title")}
        description={t("stock.description")}
        breadcrumbs={[{ label: t("nav.logistics") }, { label: t("nav.stockOverview") }]}
      />

      {/* Summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <StatsCard
          title={t("stock.totalItems")}
          value={totalItems}
          icon={<Package size={20} />}
          iconColor="bg-blue-50 text-blue-600"
        />
        <StatsCard
          title={t("stock.outOfStock")}
          value={outOfStock}
          change={outOfStock > 0 ? t("stock.actionRequired") : t("stock.allStocked")}
          changeType={outOfStock > 0 ? "negative" : "positive"}
          icon={<AlertTriangle size={20} />}
          iconColor="bg-red-50 text-red-600"
        />
        <StatsCard
          title={t("stock.totalValue")}
          value={totalValue.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 })}
          icon={<TrendingUp size={20} />}
          iconColor="bg-emerald-50 text-emerald-600"
        />
      </div>

      {/* Filters */}
      <div className="flex items-center gap-3 mb-4">
        <input
          className="form-input w-48"
          placeholder={`${t("gr.plant")} ${t("common.filter")}`}
          value={filterPlant}
          onChange={(e) => setFilterPlant(e.target.value)}
        />
        <input
          className="form-input w-64"
          placeholder={`${t("stock.itemCode")} / ${t("stock.itemName")} ${t("common.search")}`}
          value={filterItem}
          onChange={(e) => setFilterItem(e.target.value)}
        />
        {(filterPlant || filterItem) && (
          <button
            className="btn-secondary text-sm"
            onClick={() => {
              setFilterPlant("");
              setFilterItem("");
            }}
          >
            {t("common.clear")}
          </button>
        )}
      </div>

      {/* Grid */}
      <div className="card overflow-hidden">
        <DataGrid<StockRow> rowData={rows} columnDefs={columnDefs} loading={isLoading} pageSize={50} />
      </div>
    </div>
  );
}
