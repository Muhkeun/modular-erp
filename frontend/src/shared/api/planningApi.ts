import api, { type ApiResponse } from "./client";

const MRP_BASE = "/api/v1/planning/mrp";
const CAPACITY_BASE = "/api/v1/planning/capacity";
const SCHEDULE_BASE = "/api/v1/planning/schedule";

export type MrpStatus = "PLANNED" | "EXECUTED" | "CONFIRMED";
export type MrpActionType = "PURCHASE" | "PRODUCE" | "NONE";
export type ScheduleStatus = "PLANNED" | "CONFIRMED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface MrpResult {
  id: number;
  itemCode: string;
  itemName: string;
  grossRequirement: number;
  onHandStock: number;
  scheduledReceipts: number;
  netRequirement: number;
  plannedOrderQty: number;
  unitOfMeasure: string;
  actionType: MrpActionType;
  requiredDate: string | null;
  generatedDocNo: string | null;
}

export interface MrpRun {
  id: number;
  plantCode: string;
  planningHorizonDays: number;
  status: MrpStatus;
  executedAt: string | null;
  executedBy: string | null;
  results: MrpResult[];
}

export interface RunMrpRequest {
  plantCode: string;
  planningHorizonDays?: number;
}

export interface MrpSearchParams {
  page?: number;
  size?: number;
}

export interface CapacityPlan {
  id: number;
  plantCode: string;
  workCenterCode: string;
  workCenterName: string;
  planDate: string;
  availableHours: number;
  plannedLoadHours: number;
  actualHours: number;
  remainingCapacity: number;
  utilizationRate: number;
  isOverloaded: boolean;
}

export interface CapacityPlanParams {
  plantCode: string;
  fromDate: string;
  toDate: string;
}

export interface ProductionSchedule {
  id: number;
  plantCode: string;
  workCenterCode: string;
  scheduleDate: string;
  workOrderNo: string | null;
  productCode: string;
  productName: string;
  plannedQuantity: number;
  plannedHours: number;
  status: ScheduleStatus;
  sequenceNo: number;
}

export interface ProductionScheduleParams {
  plantCode: string;
  fromDate: string;
  toDate: string;
  workCenterCode?: string;
}

export const planningApi = {
  listMrpRuns: (params: MrpSearchParams = {}) =>
    api.get<ApiResponse<MrpRun[]>>(MRP_BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getMrpRun: (id: number) =>
    api.get<ApiResponse<MrpRun>>(`${MRP_BASE}/${id}`).then((r) => r.data.data!),

  runMrp: (data: RunMrpRequest) =>
    api.post<ApiResponse<MrpRun>>(`${MRP_BASE}/run`, data).then((r) => r.data.data!),

  getCapacityPlan: (params: CapacityPlanParams) =>
    api.get<ApiResponse<CapacityPlan[]>>(CAPACITY_BASE, { params }).then((r) => r.data.data ?? []),

  getProductionSchedule: (params: ProductionScheduleParams) =>
    api.get<ApiResponse<ProductionSchedule[]>>(SCHEDULE_BASE, { params }).then((r) => r.data.data ?? []),
};
