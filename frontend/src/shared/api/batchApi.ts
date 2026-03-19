import api from "./client";

const BASE = "/api/v1/batch";

export const batchApi = {
  getJobs: (size = 100) => api.get(`${BASE}/jobs?size=${size}`),
  getJob: (id: number) => api.get(`${BASE}/jobs/${id}`),
  createJob: (data: Record<string, unknown>) => api.post(`${BASE}/jobs`, data),
  updateJob: (id: number, data: Record<string, unknown>) => api.put(`${BASE}/jobs/${id}`, data),
  executeJob: (id: number) => api.post(`${BASE}/jobs/${id}/execute`),
  enableJob: (id: number) => api.post(`${BASE}/jobs/${id}/enable`),
  disableJob: (id: number) => api.post(`${BASE}/jobs/${id}/disable`),
  getHistory: (id: number) => api.get(`${BASE}/jobs/${id}/history?size=50`),
};
