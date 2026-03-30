import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/notifications";

export type NotificationChannel = "IN_APP" | "EMAIL" | "SMS" | "PUSH";
export type NotificationStatus = "PENDING" | "SENT" | "READ" | "FAILED";
export type NotificationPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";

export interface NotificationItem {
  id: number;
  templateCode: string | null;
  channel: NotificationChannel;
  recipientId: string;
  recipientEmail: string | null;
  subject: string;
  body: string;
  status: NotificationStatus;
  sentAt: string | null;
  readAt: string | null;
  referenceType: string | null;
  referenceId: number | null;
  errorMessage: string | null;
  priority: NotificationPriority;
}

export interface NotificationTemplate {
  id: number;
  templateCode: string;
  templateName: string;
  channel: NotificationChannel;
  eventType: string;
  subject: string;
  body: string;
  enabled: boolean;
  language: string;
}

export interface CreateNotificationTemplateRequest {
  templateCode: string;
  templateName: string;
  channel: NotificationChannel;
  eventType: string;
  subject: string;
  body: string;
  enabled?: boolean;
  language?: string;
}

export interface UpdateNotificationTemplateRequest {
  templateName?: string;
  subject?: string;
  body?: string;
  enabled?: boolean;
}

export interface NotificationPreference {
  id: number;
  userId: string;
  eventType: string;
  channelInApp: boolean;
  channelEmail: boolean;
  channelSms: boolean;
  channelPush: boolean;
}

export interface UpdateNotificationPreferenceRequest {
  eventType: string;
  channelInApp?: boolean;
  channelEmail?: boolean;
  channelSms?: boolean;
  channelPush?: boolean;
}

export const notificationApi = {
  getAll: (recipientId: string, size = 100) =>
    api.get<ApiResponse<NotificationItem[]>>(BASE, { params: { recipientId, size } })
      .then((r) => r.data.data ?? []),

  markRead: (id: number) =>
    api.put<ApiResponse<NotificationItem>>(`${BASE}/${id}/read`)
      .then((r) => r.data.data!),

  markAllRead: (recipientId: string) =>
    api.put<ApiResponse<string>>(`${BASE}/read-all`, undefined, { params: { recipientId } })
      .then((r) => r.data.data),

  delete: (id: number) =>
    api.delete<ApiResponse<string>>(`${BASE}/${id}`)
      .then((r) => r.data.data),

  getTemplates: (size = 100) =>
    api.get<ApiResponse<NotificationTemplate[]>>(`${BASE}/templates`, { params: { size } })
      .then((r) => r.data.data ?? []),

  createTemplate: (data: CreateNotificationTemplateRequest) =>
    api.post<ApiResponse<NotificationTemplate>>(`${BASE}/templates`, data)
      .then((r) => r.data.data!),

  updateTemplate: (id: number, data: UpdateNotificationTemplateRequest) =>
    api.put<ApiResponse<NotificationTemplate>>(`${BASE}/templates/${id}`, data)
      .then((r) => r.data.data!),

  getPreferences: (userId: string) =>
    api.get<ApiResponse<NotificationPreference[]>>(`${BASE}/preferences`, { params: { userId } })
      .then((r) => r.data.data ?? []),

  updatePreferences: (userId: string, data: UpdateNotificationPreferenceRequest) =>
    api.put<ApiResponse<NotificationPreference>>(`${BASE}/preferences`, data, { params: { userId } })
      .then((r) => r.data.data!),
};
