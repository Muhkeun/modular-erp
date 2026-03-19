import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Package, AlertTriangle, TrendingUp, Search } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
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

      {/* Workspace Hero with stat tiles */}
      <div className="workspace-hero">
        <p className="section-kicker">Logistics Workspace</p>
        <h2 className="section-title">{t("stock.title", "재고 현황")}</h2>
        <div className="mt-4 flex flex-wrap gap-3">
          <div className="stat-tile">
            <span className="text-xs text-slate-500"><Package size={14} className="inline mr-1" />{t("stock.totalItems")}</span>
            <span className="text-lg font-bold text-slate-800">{totalItems}</span>
          </div>
          <div className="stat-tile">
            <span className="text-xs text-slate-500"><AlertTriangle size={14} className="inline mr-1" />{t("stock.outOfStock")}</span>
            <span className={`text-lg font-bold ${outOfStock > 0 ? "text-red-600" : "text-emerald-600"}`}>{outOfStock}</span>
            <span className={`text-xs ${outOfStock > 0 ? "text-red-500" : "text-emerald-500"}`}>
              {outOfStock > 0 ? t("stock.actionRequired") : t("stock.allStocked")}
            </span>
          </div>
          <div className="stat-tile">
            <span className="text-xs text-slate-500"><TrendingUp size={14} className="inline mr-1" />{t("stock.totalValue")}</span>
            <span className="text-lg font-bold text-slate-800">{totalValue.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 })}</span>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-3 mb-4">
        <div className="lookup-shell">
          <Search size={15} className="text-slate-400" />
          <input
            className="lookup-input"
            placeholder={`${t("gr.plant")} ${t("common.filter")}`}
            value={filterPlant}
            onChange={(e) => setFilterPlant(e.target.value)}
          />
        </div>
        <div className="lookup-shell">
          <Search size={15} className="text-slate-400" />
          <input
            className="lookup-input"
            placeholder={`${t("stock.itemCode")} / ${t("stock.itemName")} ${t("common.search")}`}
            value={filterItem}
            onChange={(e) => setFilterItem(e.target.value)}
          />
        </div>
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
