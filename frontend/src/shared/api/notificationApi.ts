import api from "./client";

const BASE = "/api/v1/notification";

export const notificationApi = {
  getAll: (size = 100) => api.get(`${BASE}/notifications?size=${size}`),
  markRead: (id: number) => api.post(`${BASE}/notifications/${id}/read`),
  markUnread: (id: number) => api.post(`${BASE}/notifications/${id}/unread`),
  markAllRead: () => api.post(`${BASE}/notifications/read-all`),
  getTemplates: (size = 100) => api.get(`${BASE}/templates?size=${size}`),
  createTemplate: (data: Record<string, unknown>) => api.post(`${BASE}/templates`, data),
  updateTemplate: (id: number, data: Record<string, unknown>) => api.put(`${BASE}/templates/${id}`, data),
  getPreferences: () => api.get(`${BASE}/preferences`),
  updatePreferences: (data: Record<string, unknown>) => api.put(`${BASE}/preferences`, data),
};
