import { useCallback, useEffect, useRef } from 'react';
import { gridPreferenceApi } from '../api/preferenceApi';

/**
 * AG Grid 상태 자동 저장/복원 훅.
 * gridId별로 컬럼 순서, 너비, 정렬, 필터를 서버에 저장.
 */
export function useGridPreference(gridId: string) {
  const gridApiRef = useRef<any>(null);
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  // 그리드 상태 복원
  const restoreState = useCallback(async () => {
    if (!gridApiRef.current) return;
    try {
      const pref = await gridPreferenceApi.get(gridId);
      if (!pref) return;

      if (pref.columnState) {
        const columnState: any[] = JSON.parse(pref.columnState);
        gridApiRef.current.applyColumnState({ state: columnState, applyOrder: true });
      }
      if (pref.filterModel) {
        gridApiRef.current.setFilterModel(JSON.parse(pref.filterModel));
      }
    } catch {
      // 저장된 설정 없으면 무시
    }
  }, [gridId]);

  // 그리드 상태 저장 (디바운스 1초)
  const saveState = useCallback(() => {
    if (!gridApiRef.current) return;

    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    saveTimeoutRef.current = setTimeout(async () => {
      try {
        const columnState = gridApiRef.current.getColumnState();
        const filterModel = gridApiRef.current.getFilterModel();
        const sortModel = columnState
          .filter((c: any) => c.sort)
          .map((c: any) => ({ colId: c.colId, sort: c.sort, sortIndex: c.sortIndex }));

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
  const onGridReady = useCallback((params: any) => {
    gridApiRef.current = params.api;
    restoreState();
  }, [restoreState]);

  // 리셋
  const resetGridPreference = useCallback(async () => {
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
    onSortChanged: saveState,
    onFilterChanged: saveState,
    resetGridPreference,
  };
}
