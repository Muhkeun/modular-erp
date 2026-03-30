import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/quality/inspections";

export type InspectionType = "INCOMING" | "IN_PROCESS" | "FINAL" | "RETURN";
export type QiStatus = "PENDING" | "COMPLETED" | "CANCELLED";
export type QiResult = "PASS" | "FAIL" | "CONDITIONAL";

export interface QualityInspection {
  id: number;
  documentNo: string;
  inspectionType: InspectionType;
  referenceDocNo: string | null;
  itemCode: string;
  itemName: string;
  plantCode: string;
  inspectedQuantity: number;
  acceptedQuantity: number | null;
  rejectedQuantity: number | null;
  inspectionDate: string;
  status: QiStatus;
  result: QiResult | null;
  remarks: string | null;
}

export interface CreateQiRequest {
  inspectionType?: InspectionType;
  referenceDocNo?: string | null;
  itemCode: string;
  itemName: string;
  plantCode: string;
  inspectedQuantity: number;
  inspectionDate?: string;
}

export interface CompleteQiRequest {
  acceptedQuantity: number;
  rejectedQuantity: number;
  result: QiResult;
  remarks?: string | null;
}

export interface QualitySearchParams {
  status?: QiStatus;
  inspectionType?: InspectionType;
  page?: number;
  size?: number;
}

export const qualityApi = {
  search: (params: QualitySearchParams = {}) =>
    api.get<ApiResponse<QualityInspection[]>>(BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getById: (id: number) =>
    api.get<ApiResponse<QualityInspection>>(`${BASE}/${id}`).then((r) => r.data.data!),

  create: (data: CreateQiRequest) =>
    api.post<ApiResponse<QualityInspection>>(BASE, data).then((r) => r.data.data!),

  complete: (id: number, data: CompleteQiRequest) =>
    api.post<ApiResponse<QualityInspection>>(`${BASE}/${id}/complete`, data).then((r) => r.data.data!),
};
