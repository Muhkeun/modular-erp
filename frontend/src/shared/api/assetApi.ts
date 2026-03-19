import api from "./client";

const BASE = "/api/v1/asset";

export const assetApi = {
  getAll: (size = 100) => api.get(`${BASE}/assets?size=${size}`),
  get: (id: number) => api.get(`${BASE}/assets/${id}`),
  create: (data: Record<string, unknown>) => api.post(`${BASE}/assets`, data),
  update: (id: number, data: Record<string, unknown>) => api.put(`${BASE}/assets/${id}`, data),
  activate: (id: number) => api.post(`${BASE}/assets/${id}/activate`),
  dispose: (id: number, data: Record<string, unknown>) => api.post(`${BASE}/assets/${id}/dispose`, data),
  runDepreciation: (data: Record<string, unknown>) => api.post(`${BASE}/depreciation/run`, data),
  getSchedule: (id: number) => api.get(`${BASE}/assets/${id}/depreciation-schedule`),
};
