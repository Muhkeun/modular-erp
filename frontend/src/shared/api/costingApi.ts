import api from "./client";

const BASE = "/api/v1/costing";

export const costingApi = {
  getCostCenters: (size = 100) => api.get(`${BASE}/cost-centers?size=${size}`),
  createCostCenter: (data: Record<string, unknown>) => api.post(`${BASE}/cost-centers`, data),
  getStandardCosts: (size = 100) => api.get(`${BASE}/standard-costs?size=${size}`),
  createStandardCost: (data: Record<string, unknown>) => api.post(`${BASE}/standard-costs`, data),
  calculateProductCost: (data: Record<string, unknown>) => api.post(`${BASE}/product-cost/calculate`, data),
  getAllocations: (size = 100) => api.get(`${BASE}/allocations?size=${size}`),
  createAllocation: (data: Record<string, unknown>) => api.post(`${BASE}/allocations`, data),
  getVarianceAnalysis: (params?: string) => api.get(`${BASE}/variance-analysis${params ? `?${params}` : ""}`),
};
