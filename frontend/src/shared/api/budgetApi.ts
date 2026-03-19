import api from "./client";

const BASE = "/api/v1/budget";

export const budgetApi = {
  getPeriods: (size = 100) => api.get(`${BASE}/periods?size=${size}`),
  getPeriod: (id: number) => api.get(`${BASE}/periods/${id}`),
  createPeriod: (data: Record<string, unknown>) => api.post(`${BASE}/periods`, data),
  updatePeriod: (id: number, data: Record<string, unknown>) => api.put(`${BASE}/periods/${id}`, data),
  approvePeriod: (id: number) => api.post(`${BASE}/periods/${id}/approve`),
  closePeriod: (id: number) => api.post(`${BASE}/periods/${id}/close`),
  getItems: (periodId: number) => api.get(`${BASE}/periods/${periodId}/items?size=200`),
  createItem: (periodId: number, data: Record<string, unknown>) => api.post(`${BASE}/periods/${periodId}/items`, data),
  transfer: (data: Record<string, unknown>) => api.post(`${BASE}/transfers`, data),
  getBudgetVsActual: (periodId: number) => api.get(`${BASE}/periods/${periodId}/vs-actual`),
};
