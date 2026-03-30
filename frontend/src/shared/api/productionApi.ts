import api, { type ApiResponse } from "./client";

const WORK_CENTER_BASE = "/api/v1/production/work-centers";
const ROUTING_BASE = "/api/v1/production/routings";
const WORK_ORDER_BASE = "/api/v1/production/work-orders";

export type WorkCenterType = "MACHINE" | "LABOR" | "ASSEMBLY_LINE" | "INSPECTION";
export type WorkCenterStatus = "ACTIVE" | "MAINTENANCE" | "INACTIVE";
export type RoutingStatus = "DRAFT" | "RELEASED" | "OBSOLETE";
export type WoStatus = "PLANNED" | "RELEASED" | "IN_PROGRESS" | "COMPLETED" | "CLOSED" | "CANCELLED";
export type WoType = "STANDARD" | "REWORK" | "PROTOTYPE" | "MAINTENANCE";
export type WoPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";
export type OpStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED";

export interface WorkCenter {
  id: number;
  code: string;
  name: string;
  plantCode: string;
  centerType: WorkCenterType;
  capacityPerDay: number;
  resourceCount: number;
  totalDailyCapacity: number;
  costPerHour: number;
  setupCost: number;
  status: WorkCenterStatus;
  description: string | null;
}

export interface CreateWorkCenterRequest {
  code: string;
  name: string;
  plantCode: string;
  centerType?: WorkCenterType;
  capacityPerDay?: number;
  resourceCount?: number;
  costPerHour?: number;
  setupCost?: number;
  description?: string | null;
}

export interface WorkCenterSearchParams {
  plantCode?: string;
  page?: number;
  size?: number;
}

export interface RoutingOperationInput {
  operationNo: number;
  operationName: string;
  workCenterCode: string;
  setupTime?: number;
  runTimePerUnit: number;
  description?: string | null;
}

export interface RoutingOperation {
  id: number;
  operationNo: number;
  operationName: string;
  workCenterCode: string;
  setupTime: number;
  runTimePerUnit: number;
  description: string | null;
}

export interface Routing {
  id: number;
  productCode: string;
  productName: string;
  plantCode: string;
  revision: string;
  status: RoutingStatus;
  totalStandardTime: number;
  description: string | null;
  operations: RoutingOperation[];
}

export interface CreateRoutingRequest {
  productCode: string;
  productName: string;
  plantCode: string;
  revision?: string;
  description?: string | null;
  operations?: RoutingOperationInput[];
}

export interface RoutingSearchParams {
  productCode?: string;
  page?: number;
  size?: number;
}

export interface WorkOrderOperation {
  id: number;
  operationNo: number;
  operationName: string;
  workCenterCode: string;
  setupTime: number;
  runTimePerUnit: number;
  status: OpStatus;
  completedQuantity: number;
  scrapQuantity: number;
  actualSetupTime: number | null;
  actualRunTime: number | null;
  startedAt: string | null;
  completedAt: string | null;
}

export interface WorkOrderMaterial {
  id: number;
  itemCode: string;
  itemName: string;
  requiredQuantity: number;
  issuedQuantity: number;
  shortageQuantity: number;
  unitOfMeasure: string;
  operationNo: number | null;
}

export interface WorkOrder {
  id: number;
  documentNo: string;
  companyCode: string;
  plantCode: string;
  createdBy: string | null;
  productCode: string;
  productName: string;
  plannedQuantity: number;
  completedQuantity: number;
  scrapQuantity: number;
  remainingQuantity: number;
  yieldRate: number;
  unitOfMeasure: string;
  status: WoStatus;
  orderType: WoType;
  priority: WoPriority;
  salesOrderNo: string | null;
  plannedStartDate: string | null;
  plannedEndDate: string | null;
  actualStartDate: string | null;
  actualEndDate: string | null;
  remark: string | null;
  operations: WorkOrderOperation[];
  materials: WorkOrderMaterial[];
}

export interface CreateWorkOrderRequest {
  companyCode: string;
  plantCode: string;
  productCode: string;
  productName: string;
  plannedQuantity: number;
  unitOfMeasure?: string;
  orderType?: WoType;
  priority?: WoPriority;
  salesOrderNo?: string | null;
  plannedStartDate?: string | null;
  plannedEndDate?: string | null;
  remark?: string | null;
  autoPopulate?: boolean;
}

export interface WorkOrderSearchParams {
  status?: WoStatus;
  plantCode?: string;
  productCode?: string;
  documentNo?: string;
  page?: number;
  size?: number;
}

export interface ReportProductionRequest {
  operationNo?: number | null;
  goodQuantity: number;
  scrapQuantity?: number;
  actualSetupTime?: number | null;
  actualRunTime?: number | null;
}

export interface IssueMaterialRequest {
  itemCode: string;
  quantity: number;
}

export const productionApi = {
  getWorkCenters: (params: WorkCenterSearchParams = {}) =>
    api.get<ApiResponse<WorkCenter[]>>(WORK_CENTER_BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  createWorkCenter: (data: CreateWorkCenterRequest) =>
    api.post<ApiResponse<WorkCenter>>(WORK_CENTER_BASE, data).then((r) => r.data.data!),

  getRoutings: (params: RoutingSearchParams = {}) =>
    api.get<ApiResponse<Routing[]>>(ROUTING_BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  createRouting: (data: CreateRoutingRequest) =>
    api.post<ApiResponse<Routing>>(ROUTING_BASE, data).then((r) => r.data.data!),

  releaseRouting: (id: number) =>
    api.post<ApiResponse<Routing>>(`${ROUTING_BASE}/${id}/release`).then((r) => r.data.data!),

  getWorkOrders: (params: WorkOrderSearchParams = {}) =>
    api.get<ApiResponse<WorkOrder[]>>(WORK_ORDER_BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getWorkOrder: (id: number) =>
    api.get<ApiResponse<WorkOrder>>(`${WORK_ORDER_BASE}/${id}`).then((r) => r.data.data!),

  createWorkOrder: (data: CreateWorkOrderRequest) =>
    api.post<ApiResponse<WorkOrder>>(WORK_ORDER_BASE, data).then((r) => r.data.data!),

  releaseWorkOrder: (id: number) =>
    api.post<ApiResponse<WorkOrder>>(`${WORK_ORDER_BASE}/${id}/release`).then((r) => r.data.data!),

  startWorkOrder: (id: number) =>
    api.post<ApiResponse<WorkOrder>>(`${WORK_ORDER_BASE}/${id}/start`).then((r) => r.data.data!),

  reportWorkOrder: (id: number, data: ReportProductionRequest) =>
    api.post<ApiResponse<WorkOrder>>(`${WORK_ORDER_BASE}/${id}/report`, data).then((r) => r.data.data!),

  issueMaterial: (id: number, data: IssueMaterialRequest) =>
    api.post<ApiResponse<WorkOrder>>(`${WORK_ORDER_BASE}/${id}/issue-material`, data).then((r) => r.data.data!),

  completeWorkOrder: (id: number) =>
    api.post<ApiResponse<WorkOrder>>(`${WORK_ORDER_BASE}/${id}/complete`).then((r) => r.data.data!),

  closeWorkOrder: (id: number) =>
    api.post<ApiResponse<WorkOrder>>(`${WORK_ORDER_BASE}/${id}/close`).then((r) => r.data.data!),
};
