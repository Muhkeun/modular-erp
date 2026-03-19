import api from "./client";

const BASE = "/api/v1/crm";

export const crmApi = {
  getCustomers: (size = 100) => api.get(`${BASE}/customers?size=${size}`),
  createCustomer: (data: Record<string, unknown>) => api.post(`${BASE}/customers`, data),
  getLeads: (size = 100) => api.get(`${BASE}/leads?size=${size}`),
  createLead: (data: Record<string, unknown>) => api.post(`${BASE}/leads`, data),
  convertLead: (id: number) => api.post(`${BASE}/leads/${id}/convert`),
  getOpportunities: (size = 100) => api.get(`${BASE}/opportunities?size=${size}`),
  createOpportunity: (data: Record<string, unknown>) => api.post(`${BASE}/opportunities`, data),
  updateStage: (id: number, stage: string) => api.post(`${BASE}/opportunities/${id}/stage`, { stage }),
  getActivities: (size = 100) => api.get(`${BASE}/activities?size=${size}`),
  createActivity: (data: Record<string, unknown>) => api.post(`${BASE}/activities`, data),
  getPipelineSummary: () => api.get(`${BASE}/pipeline/summary`),
};
