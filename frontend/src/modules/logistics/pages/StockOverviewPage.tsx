import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColDef } from "ag-grid-community";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import StatsCard from "../../../shared/components/StatsCard";
import api from "../../../shared/api/client";
import { Package, AlertTriangle, TrendingUp } from "lucide-react";

interface StockRow {
  id: number; itemCode: string; itemName: string; plantCode: string;
  storageLocation: string; unitOfMeasure: string;
  quantityOnHand: number; quantityReserved: number; availableQuantity: number; totalValue: number;
}

export default function StockOverviewPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["stock"],
    queryFn: async () => (await api.get("/api/v1/logistics/stock?size=200")).data,
  });

  const rows: StockRow[] = data?.data || [];
  const totalItems = rows.length;
  const lowStock = rows.filter(r => r.availableQuantity <= 0).length;
  const totalValue = rows.reduce((sum, r) => sum + (r.totalValue || 0), 0);

  const columnDefs = useMemo<ColDef<StockRow>[]>(() => [
    { field: "itemCode", headerName: "Item Code", flex: 1,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "itemName", headerName: "Item Name", flex: 2 },
    { field: "plantCode", headerName: "Plant", flex: 0.7 },
    { field: "storageLocation", headerName: "Location", flex: 0.8 },
    { field: "unitOfMeasure", headerName: "UoM", flex: 0.5 },
    { field: "quantityOnHand", headerName: "On Hand", flex: 0.8, type: "numericColumn" },
    { field: "quantityReserved", headerName: "Reserved", flex: 0.8, type: "numericColumn" },
    { field: "availableQuantity", headerName: "Available", flex: 0.8, type: "numericColumn",
      cellRenderer: (p: { value: number }) =>
        <span className={p.value <= 0 ? "text-red-600 font-bold" : "text-emerald-600 font-semibold"}>{p.value}</span> },
    { field: "totalValue", headerName: "Value", flex: 1.2, type: "numericColumn",
      valueFormatter: (p: { value: number }) => p.value?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 }) },
  ], []);

  return (
    <div>
      <PageHeader title="Stock Overview" description="Real-time inventory levels across all locations"
        breadcrumbs={[{ label: "Logistics" }, { label: "Stock Overview" }]} />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <StatsCard title="Total Items" value={totalItems} icon={<Package size={20} />} iconColor="bg-blue-50 text-blue-600" />
        <StatsCard title="Out of Stock" value={lowStock} change={lowStock > 0 ? "Action required" : "All stocked"} changeType={lowStock > 0 ? "negative" : "positive"} icon={<AlertTriangle size={20} />} iconColor="bg-red-50 text-red-600" />
        <StatsCard title="Total Value" value={totalValue.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 })} icon={<TrendingUp size={20} />} iconColor="bg-emerald-50 text-emerald-600" />
      </div>

      <div className="card overflow-hidden">
        <DataGrid<StockRow> rowData={rows} columnDefs={columnDefs} loading={isLoading} pageSize={50} />
      </div>
    </div>
  );
}
