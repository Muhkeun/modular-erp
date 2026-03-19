import api from "./client";

const BASE = "/api/v1/currency";

export const currencyApi = {
  getCurrencies: (size = 100) => api.get(`${BASE}/currencies?size=${size}`),
  createCurrency: (data: Record<string, unknown>) => api.post(`${BASE}/currencies`, data),
  updateCurrency: (id: number, data: Record<string, unknown>) => api.put(`${BASE}/currencies/${id}`, data),
  getRates: (size = 100) => api.get(`${BASE}/exchange-rates?size=${size}`),
  createRate: (data: Record<string, unknown>) => api.post(`${BASE}/exchange-rates`, data),
  convert: (data: Record<string, unknown>) => api.post(`${BASE}/convert`, data),
  getRevaluations: (size = 100) => api.get(`${BASE}/revaluations?size=${size}`),
  runRevaluation: (data: Record<string, unknown>) => api.post(`${BASE}/revaluations`, data),
};
