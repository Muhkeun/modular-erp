import { useCallback, useMemo, useRef } from "react";
import { AgGridReact } from "ag-grid-react";
import type { ColDef, GridReadyEvent } from "ag-grid-community";
import "ag-grid-community/styles/ag-grid.css";
import "ag-grid-community/styles/ag-theme-alpine.css";

export interface DataGridProps<T> {
  rowData: T[];
  columnDefs: ColDef<T>[];
  loading?: boolean;
  onRowClicked?: (data: T) => void;
  pagination?: boolean;
  pageSize?: number;
  totalRows?: number;
  currentPage?: number;
  onPageChange?: (page: number) => void;
  height?: string;
  domLayout?: "normal" | "autoHeight";
}

export default function DataGrid<T>({
  rowData,
  columnDefs,
  loading = false,
  onRowClicked,
  pagination = true,
  pageSize = 20,
  height = "calc(100vh - 320px)",
  domLayout = "normal",
}: DataGridProps<T>) {
  const gridRef = useRef<AgGridReact<T>>(null);

  const defaultColDef = useMemo<ColDef>(
    () => ({
      sortable: true,
      resizable: true,
      filter: true,
      minWidth: 80,
      flex: 1,
    }),
    []
  );

  const onGridReady = useCallback((_params: GridReadyEvent) => {
    // Auto-size on ready
  }, []);

  const handleRowClicked = useCallback(
    (event: { data: T | undefined }) => {
      if (event.data && onRowClicked) onRowClicked(event.data);
    },
    [onRowClicked]
  );

  return (
    <div className="ag-theme-custom" style={{ height, width: "100%" }}>
      <AgGridReact<T>
        ref={gridRef}
        rowData={rowData}
        columnDefs={columnDefs}
        defaultColDef={defaultColDef}
        onGridReady={onGridReady}
        onRowClicked={handleRowClicked}
        pagination={pagination}
        paginationPageSize={pageSize}
        paginationPageSizeSelector={[10, 20, 50, 100]}
        rowSelection="single"
        animateRows
        suppressCellFocus
        domLayout={domLayout}
        overlayLoadingTemplate='<div class="flex items-center gap-2 text-sm text-slate-500"><span class="animate-spin">⟳</span> Loading...</div>'
        overlayNoRowsTemplate='<div class="text-sm text-slate-400 py-8">No data found</div>'
        loading={loading}
      />
    </div>
  );
}
