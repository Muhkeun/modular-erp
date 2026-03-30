import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/contracts";

export type ContractType = "PURCHASE" | "SALES" | "SERVICE" | "NDA" | "FRAMEWORK";
export type ContractStatus = "DRAFT" | "ACTIVE" | "EXPIRED" | "TERMINATED" | "CANCELLED";

export interface Contract {
  id: number;
  documentNo: string;
  title: string;
  contractType: ContractType;
  counterpartyCode: string;
  counterpartyName: string;
  startDate: string;
  endDate: string;
  contractAmount: number | null;
  currencyCode: string;
  status: ContractStatus;
  terms: string | null;
  description: string | null;
}

export interface CreateContractRequest {
  title: string;
  contractType: ContractType;
  counterpartyCode: string;
  counterpartyName: string;
  startDate: string;
  endDate: string;
  contractAmount?: number | null;
  currencyCode?: string;
  terms?: string | null;
  description?: string | null;
}

export interface ContractSearchParams {
  status?: ContractStatus;
  contractType?: ContractType;
  page?: number;
  size?: number;
}

export const contractApi = {
  search: (params: ContractSearchParams = {}) =>
    api.get<ApiResponse<Contract[]>>(BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getById: (id: number) =>
    api.get<ApiResponse<Contract>>(`${BASE}/${id}`).then((r) => r.data.data!),

  create: (data: CreateContractRequest) =>
    api.post<ApiResponse<Contract>>(BASE, data).then((r) => r.data.data!),

  activate: (id: number) =>
    api.post<ApiResponse<Contract>>(`${BASE}/${id}/activate`).then((r) => r.data.data!),

  terminate: (id: number) =>
    api.post<ApiResponse<Contract>>(`${BASE}/${id}/terminate`).then((r) => r.data.data!),
};
