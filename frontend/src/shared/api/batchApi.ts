import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/batch";

export type BatchJobType =
  | "GL_POSTING"
  | "DEPRECIATION"
  | "MRP_RUN"
  | "STOCK_REVALUATION"
  | "EXCHANGE_RATE_UPDATE"
  | "DATA_IMPORT"
  | "DATA_EXPORT"
  | "REPORT_GENERATION"
  | "EMAIL_SENDING"
  | "CLEANUP";

export type ExecutionStatus = "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED";

export type BatchJob = {
  id: number;
  jobCode: string;
  jobName: string;
  jobType: BatchJobType;
  companyCode: string | null;
  departmentCode: string | null;
  plantCode: string | null;
  cronExpression: string | null;
  enabled: boolean;
  lastRunAt: string | null;
  nextRunAt: string | null;
  description: string | null;
};

export type CreateBatchJobRequest = {
  jobCode: string;
  jobName: string;
  jobType: BatchJobType;
  companyCode?: string | null;
  departmentCode?: string | null;
  plantCode?: string | null;
  cronExpression?: string | null;
  enabled?: boolean;
  description?: string | null;
};

export type UpdateBatchJobRequest = {
  jobName?: string;
  companyCode?: string | null;
  departmentCode?: string | null;
  plantCode?: string | null;
  cronExpression?: string | null;
  description?: string | null;
};

export type BatchExecution = {
  id: number;
  jobId: number;
  jobCode: string;
  companyCode: string | null;
  departmentCode: string | null;
  plantCode: string | null;
  executionNo: string;
  status: ExecutionStatus;
  startedAt: string;
  completedAt: string | null;
  totalRecords: number;
  processedRecords: number;
  failedRecords: number;
  errorMessage: string | null;
  parameters: string | null;
  result: string | null;
  triggeredBy: string;
  executedBy: string | null;
};

export const batchApi = {
  getJobs: (size = 100) =>
    api.get<ApiResponse<BatchJob[]>>(`${BASE}/jobs?size=${size}`).then((r) => r.data.data ?? []),

  getJob: (id: number) =>
    api.get<ApiResponse<BatchJob>>(`${BASE}/jobs/${id}`).then((r) => r.data.data!),

  createJob: (data: CreateBatchJobRequest) =>
    api.post<ApiResponse<BatchJob>>(`${BASE}/jobs`, data).then((r) => r.data.data!),

  updateJob: (id: number, data: UpdateBatchJobRequest) =>
    api.put<ApiResponse<BatchJob>>(`${BASE}/jobs/${id}`, data).then((r) => r.data.data!),

  executeJob: (id: number, parameters?: string) =>
    api
      .post<ApiResponse<BatchExecution>>(`${BASE}/jobs/${id}/execute`, parameters ? { parameters } : undefined)
      .then((r) => r.data.data!),

  enableJob: (id: number) =>
    api.post<ApiResponse<BatchJob>>(`${BASE}/jobs/${id}/enable`).then((r) => r.data.data!),

  disableJob: (id: number) =>
    api.post<ApiResponse<BatchJob>>(`${BASE}/jobs/${id}/disable`).then((r) => r.data.data!),

  getHistory: (id: number, size = 50) =>
    api.get<ApiResponse<BatchExecution[]>>(`${BASE}/jobs/${id}/executions?size=${size}`).then((r) => r.data.data ?? []),
};
