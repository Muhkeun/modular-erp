import api, { type ApiResponse } from './client';

// ── Workflow Types ──

export type WorkflowStepType = 'APPROVAL' | 'NOTIFICATION' | 'CONDITION' | 'PARALLEL';
export type ApproverType = 'SPECIFIC_USER' | 'ROLE' | 'DEPARTMENT_HEAD' | 'MANAGER_LEVEL' | 'REQUESTER_MANAGER';

export interface WorkflowStep {
  id: number;
  stepOrder: number;
  name: string;
  stepType: WorkflowStepType;
  approverType: ApproverType;
  approverValue: string;
  condition: string | null;
  autoApproveHours: number;
}

export interface Workflow {
  id: number;
  documentType: string;
  name: string;
  description: string | null;
  version: number;
  isCurrent: boolean;
  steps: WorkflowStep[];
  designerLayout: string | null;
}

// ── Tenant Types ──

export type TenantPlan = 'FREE' | 'STARTER' | 'PROFESSIONAL' | 'ENTERPRISE';
export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';

export interface Tenant {
  id: number;
  tenantId: string;
  name: string;
  description: string | null;
  plan: TenantPlan;
  maxUsers: number;
  maxStorageMb: number;
  status: TenantStatus;
  currentUsers: number;
  activatedAt: string | null;
  createdAt: string;
}

// ── ApiKey Types ──

export type ApiKeyStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';

export interface ApiKeyInfo {
  id: number;
  name: string;
  keyPrefix: string;
  description: string | null;
  allowedResources: string | null;
  rateLimit: number | null;
  status: ApiKeyStatus;
  expiresAt: string | null;
  lastUsedAt: string | null;
  createdAt: string;
}

export interface ApiKeyCreateResult {
  id: number;
  name: string;
  rawKey: string;
  keyPrefix: string;
  expiresAt: string | null;
}

// ── APIs ──

export const workflowApi = {
  getAll: () => api.get<ApiResponse<Workflow[]>>('/api/v1/admin/workflows').then(r => r.data.data ?? []),
  get: (id: number) => api.get<ApiResponse<Workflow>>(`/api/v1/admin/workflows/${id}`).then(r => r.data.data!),
  create: (data: any) => api.post<ApiResponse<Workflow>>('/api/v1/admin/workflows', data).then(r => r.data.data!),
  update: (id: number, data: any) => api.put<ApiResponse<Workflow>>(`/api/v1/admin/workflows/${id}`, data).then(r => r.data.data!),
  activate: (id: number) => api.post<ApiResponse<Workflow>>(`/api/v1/admin/workflows/${id}/activate`).then(r => r.data.data!),
  delete: (id: number) => api.delete(`/api/v1/admin/workflows/${id}`),
};

export const tenantApi = {
  getAll: () => api.get<ApiResponse<Tenant[]>>('/api/v1/admin/tenants').then(r => r.data.data ?? []),
  get: (tenantId: string) => api.get<ApiResponse<Tenant>>(`/api/v1/admin/tenants/${tenantId}`).then(r => r.data.data!),
  create: (data: any) => api.post<ApiResponse<Tenant>>('/api/v1/admin/tenants', data).then(r => r.data.data!),
  update: (tenantId: string, data: any) => api.put<ApiResponse<Tenant>>(`/api/v1/admin/tenants/${tenantId}`, data).then(r => r.data.data!),
  suspend: (tenantId: string) => api.post(`/api/v1/admin/tenants/${tenantId}/suspend`),
  activate: (tenantId: string) => api.post(`/api/v1/admin/tenants/${tenantId}/activate`),
};

export const apiKeyApi = {
  getAll: () => api.get<ApiResponse<ApiKeyInfo[]>>('/api/v1/admin/api-keys').then(r => r.data.data ?? []),
  create: (data: any) => api.post<ApiResponse<ApiKeyCreateResult>>('/api/v1/admin/api-keys', data).then(r => r.data.data!),
  update: (id: number, data: any) => api.put<ApiResponse<ApiKeyInfo>>(`/api/v1/admin/api-keys/${id}`, data).then(r => r.data.data!),
  revoke: (id: number) => api.delete(`/api/v1/admin/api-keys/${id}`),
};
