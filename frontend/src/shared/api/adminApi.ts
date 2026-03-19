import api, { type ApiResponse } from './client';

// ── Role Types ──

export type ActionType = 'READ' | 'CREATE' | 'UPDATE' | 'DELETE' | 'EXPORT' | 'IMPORT' | 'APPROVE';

export interface Permission {
  id: number;
  resource: string;
  actions: ActionType[];
}

export interface Role {
  id: number;
  code: string;
  name: string;
  description: string | null;
  isSystem: boolean;
  permissions: Permission[];
}

// ── MenuProfile Types ──

export interface MenuProfileItem {
  id: number;
  menuCode: string;
  sortOrder: number;
  visible: boolean;
}

export interface MenuProfile {
  id: number;
  code: string;
  name: string;
  description: string | null;
  menuItems: MenuProfileItem[];
}

// ── SystemCode Types ──

export interface SystemCodeItem {
  id: number;
  code: string;
  name: string;
  sortOrder: number;
  extra: string | null;
}

export interface SystemCode {
  id: number;
  groupCode: string;
  groupName: string;
  description: string | null;
  isSystem: boolean;
  items: SystemCodeItem[];
}

// ── Organization Types ──

export type OrgType = 'COMPANY' | 'OPERATING_UNIT' | 'PLANT' | 'DEPARTMENT';

export interface Organization {
  id: number;
  code: string;
  name: string;
  orgType: OrgType;
  parentId: number | null;
  sortOrder: number;
  description: string | null;
  children: Organization[];
}

// ── AuditLog Types ──

export type AuditAction = 'CREATE' | 'READ' | 'UPDATE' | 'DELETE' | 'LOGIN' | 'LOGOUT' | 'EXPORT' | 'IMPORT' | 'APPROVE' | 'REJECT';

export interface AuditLog {
  id: number;
  userId: string;
  action: AuditAction;
  entityType: string;
  entityId: string | null;
  oldValue: string | null;
  newValue: string | null;
  ipAddress: string | null;
  metadata: string | null;
  createdAt: string;
}

// ── APIs ──

export const roleApi = {
  getAll: () => api.get<ApiResponse<Role[]>>('/api/v1/admin/roles').then(r => r.data.data ?? []),
  get: (code: string) => api.get<ApiResponse<Role>>(`/api/v1/admin/roles/${code}`).then(r => r.data.data!),
  create: (data: { code: string; name: string; description?: string; permissions?: { resource: string; actions: ActionType[] }[] }) =>
    api.post<ApiResponse<Role>>('/api/v1/admin/roles', data).then(r => r.data.data!),
  update: (code: string, data: { name: string; description?: string; permissions?: { resource: string; actions: ActionType[] }[] }) =>
    api.put<ApiResponse<Role>>(`/api/v1/admin/roles/${code}`, data).then(r => r.data.data!),
  delete: (code: string) => api.delete(`/api/v1/admin/roles/${code}`),
};

export const menuProfileApi = {
  getAll: () => api.get<ApiResponse<MenuProfile[]>>('/api/v1/admin/menu-profiles').then(r => r.data.data ?? []),
  get: (code: string) => api.get<ApiResponse<MenuProfile>>(`/api/v1/admin/menu-profiles/${code}`).then(r => r.data.data!),
  create: (data: { code: string; name: string; description?: string; menuItems?: { menuCode: string; sortOrder: number; visible: boolean }[] }) =>
    api.post<ApiResponse<MenuProfile>>('/api/v1/admin/menu-profiles', data).then(r => r.data.data!),
  update: (code: string, data: { name: string; description?: string; menuItems?: { menuCode: string; sortOrder: number; visible: boolean }[] }) =>
    api.put<ApiResponse<MenuProfile>>(`/api/v1/admin/menu-profiles/${code}`, data).then(r => r.data.data!),
  delete: (code: string) => api.delete(`/api/v1/admin/menu-profiles/${code}`),
};

export const systemCodeApi = {
  getAll: () => api.get<ApiResponse<SystemCode[]>>('/api/v1/admin/system-codes').then(r => r.data.data ?? []),
  get: (groupCode: string) => api.get<ApiResponse<SystemCode>>(`/api/v1/admin/system-codes/${groupCode}`).then(r => r.data.data!),
  create: (data: { groupCode: string; groupName: string; description?: string; items?: { code: string; name: string; sortOrder: number; extra?: string }[] }) =>
    api.post<ApiResponse<SystemCode>>('/api/v1/admin/system-codes', data).then(r => r.data.data!),
  update: (groupCode: string, data: { groupName: string; description?: string; items?: { code: string; name: string; sortOrder: number; extra?: string }[] }) =>
    api.put<ApiResponse<SystemCode>>(`/api/v1/admin/system-codes/${groupCode}`, data).then(r => r.data.data!),
  delete: (groupCode: string) => api.delete(`/api/v1/admin/system-codes/${groupCode}`),
};

export const organizationApi = {
  getTree: () => api.get<ApiResponse<Organization[]>>('/api/v1/admin/organizations/tree').then(r => r.data.data ?? []),
  getAll: () => api.get<ApiResponse<Organization[]>>('/api/v1/admin/organizations').then(r => r.data.data ?? []),
  create: (data: { code: string; name: string; orgType: OrgType; parentId?: number; sortOrder?: number; description?: string }) =>
    api.post<ApiResponse<Organization>>('/api/v1/admin/organizations', data).then(r => r.data.data!),
  update: (id: number, data: { name: string; sortOrder?: number; description?: string }) =>
    api.put<ApiResponse<Organization>>(`/api/v1/admin/organizations/${id}`, data).then(r => r.data.data!),
  delete: (id: number) => api.delete(`/api/v1/admin/organizations/${id}`),
};

export const auditLogApi = {
  search: (params: { userId?: string; action?: AuditAction; entityType?: string; from?: string; to?: string; page?: number; size?: number }) =>
    api.get<ApiResponse<AuditLog[]>>('/api/v1/admin/audit-logs', { params }).then(r => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),
  getEntityHistory: (entityType: string, entityId: string) =>
    api.get<ApiResponse<AuditLog[]>>(`/api/v1/admin/audit-logs/entity/${entityType}/${entityId}`).then(r => r.data.data ?? []),
};
