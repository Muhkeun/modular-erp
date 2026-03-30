import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/supply-chain/evaluations";

export type SupplierGrade = "A" | "B" | "C" | "D" | "F";

export interface SupplierEvaluation {
  id: number;
  vendorCode: string;
  vendorName: string;
  evaluationPeriod: string;
  qualityScore: number;
  deliveryScore: number;
  priceScore: number;
  serviceScore: number;
  totalScore: number;
  grade: SupplierGrade;
  evaluationDate: string;
  remarks: string | null;
}

export interface CreateSupplierEvaluationRequest {
  vendorCode: string;
  vendorName: string;
  evaluationPeriod: string;
  qualityScore: number;
  deliveryScore: number;
  priceScore: number;
  serviceScore: number;
  remarks?: string | null;
}

export interface SupplierEvaluationSearchParams {
  vendorCode?: string;
  period?: string;
  page?: number;
  size?: number;
}

export const supplyChainApi = {
  search: (params: SupplierEvaluationSearchParams = {}) =>
    api.get<ApiResponse<SupplierEvaluation[]>>(BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getById: (id: number) =>
    api.get<ApiResponse<SupplierEvaluation>>(`${BASE}/${id}`).then((r) => r.data.data!),

  create: (data: CreateSupplierEvaluationRequest) =>
    api.post<ApiResponse<SupplierEvaluation>>(BASE, data).then((r) => r.data.data!),
};
