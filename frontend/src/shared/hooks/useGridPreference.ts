import { useCallback, useEffect, useRef } from 'react';
import type { ColumnState, GridApi, GridReadyEvent } from 'ag-grid-community';
import { gridPreferenceApi } from '../api/preferenceApi';

type SortableColumnState = ColumnState & {
  sort?: 'asc' | 'desc' | null;
  sortIndex?: number | null;
};

/**
 * AG Grid 상태 자동 저장/복원 훅.
 * gridId별로 컬럼 순서, 너비, 정렬, 필터를 서버에 저장.
 */
export function useGridPreference(gridId: string) {
  const gridApiRef = useRef<GridApi | null>(null);
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 그리드 상태 복원
  const restoreState = useCallback(async () => {
    if (!gridId || !gridApiRef.current) return;
    try {
      const pref = await gridPreferenceApi.get(gridId);
      if (!pref) return;

      if (pref.columnState) {
        const parsedState: unknown = JSON.parse(pref.columnState);
        if (Array.isArray(parsedState)) {
          gridApiRef.current.applyColumnState({ state: parsedState as ColumnState[], applyOrder: true });
        }
      }
      if (pref.filterModel) {
        const parsedFilterModel: unknown = JSON.parse(pref.filterModel);
        gridApiRef.current.setFilterModel(parsedFilterModel as Record<string, unknown>);
      }
    } catch {
      // 저장된 설정 없으면 무시
    }
  }, [gridId]);

  // 그리드 상태 저장 (디바운스 1초)
  const saveState = useCallback(() => {
    if (!gridId || !gridApiRef.current) return;

    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    saveTimeoutRef.current = setTimeout(async () => {
      try {
        const gridApi = gridApiRef.current;
        if (!gridApi) return;

        const columnState = gridApi.getColumnState();
        const filterModel = gridApi.getFilterModel();
        const sortModel = columnState
          .filter((column): column is SortableColumnState => !!column.sort)
          .map((column) => ({ colId: column.colId, sort: column.sort, sortIndex: column.sortIndex }));

        await gridPreferenceApi.save({
          gridId,
          columnState: JSON.stringify(columnState),
          sortModel: JSON.stringify(sortModel),
          filterModel: JSON.stringify(filterModel),
        });
      } catch {
        // 저장 실패 시 무시 (UX 영향 없음)
      }
    }, 1000);
  }, [gridId]);

  // 초기화 시 복원
  const onGridReady = useCallback((params: GridReadyEvent) => {
    gridApiRef.current = params.api;
    void restoreState();
  }, [restoreState]);

  // 리셋
  const resetGridPreference = useCallback(async () => {
    if (!gridId) return;
    try {
      await gridPreferenceApi.delete(gridId);
      if (gridApiRef.current) {
        gridApiRef.current.resetColumnState();
        gridApiRef.current.setFilterModel(null);
      }
    } catch {
      // ignore
    }
  }, [gridId]);

  useEffect(() => {
    return () => {
      if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    };
  }, []);

  return {
    onGridReady,
    onColumnMoved: saveState,
    onColumnResized: saveState,
    onColumnVisible: saveState,
    onColumnPinned: saveState,
    onSortChanged: saveState,
    onFilterChanged: saveState,
    resetGridPreference,
  };
}
