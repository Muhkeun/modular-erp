import api from "./client";

const BASE = "/api/v1/period-close";

export const periodCloseApi = {
  getPeriods: (size = 100) => api.get(`${BASE}/periods?size=${size}`),
  getPeriod: (id: number) => api.get(`${BASE}/periods/${id}`),
  createPeriod: (data: Record<string, unknown>) => api.post(`${BASE}/periods`, data),
  getChecklist: (id: number) => api.get(`${BASE}/periods/${id}/checklist`),
  executeTask: (periodId: number, taskId: number) => api.post(`${BASE}/periods/${periodId}/checklist/${taskId}/execute`),
  softClose: (id: number) => api.post(`${BASE}/periods/${id}/soft-close`),
  hardClose: (id: number) => api.post(`${BASE}/periods/${id}/hard-close`),
  getClosingEntries: (id: number) => api.get(`${BASE}/periods/${id}/closing-entries`),
};
