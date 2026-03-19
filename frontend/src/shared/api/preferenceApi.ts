import api, { type ApiResponse } from './client';

// ── Types ──

export type PreferenceCategory = 'DISPLAY' | 'FORMAT' | 'DEFAULT' | 'DASHBOARD' | 'GRID';

export interface PreferenceItem {
  id: number;
  category: PreferenceCategory;
  key: string;
  value: string;
}

export interface GridPreference {
  id: number;
  gridId: string;
  columnState: string | null;
  sortModel: string | null;
  filterModel: string | null;
  pageSize: number;
}

// ── Preference API ──

export const preferenceApi = {
  getAll: () =>
    api.get<ApiResponse<PreferenceItem[]>>('/api/v1/preferences').then(r => r.data.data ?? []),

  getByCategory: (category: PreferenceCategory) =>
    api.get<ApiResponse<PreferenceItem[]>>(`/api/v1/preferences/category/${category}`).then(r => r.data.data ?? []),

  save: (category: PreferenceCategory, key: string, value: string) =>
    api.put<ApiResponse<PreferenceItem>>('/api/v1/preferences', { category, key, value }).then(r => r.data.data!),

  saveBatch: (preferences: { category: PreferenceCategory; key: string; value: string }[]) =>
    api.put<ApiResponse<PreferenceItem[]>>('/api/v1/preferences/batch', { preferences }).then(r => r.data.data ?? []),

  delete: (category: PreferenceCategory, key: string) =>
    api.delete(`/api/v1/preferences/${category}/${key}`),
};

// ── Grid Preference API ──

export const gridPreferenceApi = {
  getAll: () =>
    api.get<ApiResponse<GridPreference[]>>('/api/v1/preferences/grids').then(r => r.data.data ?? []),

  get: (gridId: string) =>
    api.get<ApiResponse<GridPreference | null>>(`/api/v1/preferences/grids/${gridId}`).then(r => r.data.data),

  save: (data: { gridId: string; columnState?: string; sortModel?: string; filterModel?: string; pageSize?: number }) =>
    api.put<ApiResponse<GridPreference>>('/api/v1/preferences/grids', data).then(r => r.data.data!),

  delete: (gridId: string) =>
    api.delete(`/api/v1/preferences/grids/${gridId}`),
};
