import api, { type ApiResponse } from './client';

// ── Types ──

export type ApprovalStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'RETURNED' | 'CANCELLED';
export type ApprovalDecision = 'APPROVED' | 'REJECTED' | 'RETURNED';
export type StepStatus = 'WAITING' | 'ACTIVE' | 'COMPLETED';

export interface ApprovalStep {
  id: number;
  stepOrder: number;
  approverRole: string;
  approverId: string | null;
  decision: ApprovalDecision | null;
  stepStatus: StepStatus;
  decidedBy: string | null;
  decidedAt: string | null;
  comment: string | null;
}

export interface ApprovalComment {
  id: number;
  stepSequence: number | null;
  commentBy: string;
  comment: string;
  commentAt: string;
}

export interface ApprovalRequest {
  id: number;
  documentType: string;
  documentId: number;
  documentNo: string | null;
  requestedBy: string;
  status: ApprovalStatus;
  steps: ApprovalStep[];
  comments: ApprovalComment[];
  createdAt: string | null;
  completedAt: string | null;
}

export interface Delegation {
  id: number;
  fromUserId: string;
  toUserId: string;
  startDate: string;
  endDate: string;
  isActive: boolean;
  reason: string | null;
  createdAt: string | null;
}

export interface ApprovalDashboard {
  pendingCount: number;
  submittedCount: number;
  recentActions: ApprovalRequest[];
}

// ── APIs ──

export const approvalApi = {
  // Inbox
  getMyPending: () =>
    api.get<ApiResponse<ApprovalRequest[]>>('/api/v1/approvals/my-pending').then(r => r.data.data ?? []),

  getMySubmitted: () =>
    api.get<ApiResponse<ApprovalRequest[]>>('/api/v1/approvals/my-submitted').then(r => r.data.data ?? []),

  getById: (id: number) =>
    api.get<ApiResponse<ApprovalRequest>>(`/api/v1/approvals/${id}`).then(r => r.data.data!),

  // Actions
  submit: (data: { documentType: string; documentId: number; documentNo: string }) =>
    api.post<ApiResponse<number>>('/api/v1/approvals', data).then(r => r.data.data!),

  approveStep: (id: number, stepId: number, comment?: string) =>
    api.post<ApiResponse<ApprovalRequest>>(`/api/v1/approvals/${id}/steps/${stepId}/approve`, { comment }).then(r => r.data.data!),

  rejectStep: (id: number, stepId: number, comment?: string) =>
    api.post<ApiResponse<ApprovalRequest>>(`/api/v1/approvals/${id}/steps/${stepId}/reject`, { comment }).then(r => r.data.data!),

  returnStep: (id: number, stepId: number, comment?: string) =>
    api.post<ApiResponse<ApprovalRequest>>(`/api/v1/approvals/${id}/steps/${stepId}/return`, { comment }).then(r => r.data.data!),

  addComment: (id: number, comment: string, stepSequence?: number) =>
    api.post<ApiResponse<ApprovalRequest>>(`/api/v1/approvals/${id}/comments`, { comment, stepSequence }).then(r => r.data.data!),

  // Delegations
  getDelegations: () =>
    api.get<ApiResponse<Delegation[]>>('/api/v1/approvals/delegations').then(r => r.data.data ?? []),

  createDelegation: (data: { toUserId: string; startDate: string; endDate: string; reason?: string }) =>
    api.post<ApiResponse<Delegation>>('/api/v1/approvals/delegations', data).then(r => r.data.data!),

  deleteDelegation: (id: number) =>
    api.delete(`/api/v1/approvals/delegations/${id}`),

  // Dashboard
  getDashboard: () =>
    api.get<ApiResponse<ApprovalDashboard>>('/api/v1/approvals/dashboard').then(r => r.data.data!),
};
