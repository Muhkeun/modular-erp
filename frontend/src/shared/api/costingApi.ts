import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/costing";

export type CostCenter = {
  id: number;
  costCenterCode: string;
  costCenterName: string;
  parentCode: string | null;
  departmentCode: string | null;
  managerName: string | null;
  status: string;
};

export type CreateCostCenterRequest = {
  costCenterCode: string;
  costCenterName: string;
  parentCode?: string | null;
  departmentCode?: string | null;
  managerName?: string | null;
  status?: string;
};

export type StandardCost = {
  id: number;
  itemCode: string;
  costCenterCode: string | null;
  costType: string;
  standardRate: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  currency: string;
  notes: string | null;
};

export type CreateStandardCostRequest = {
  itemCode: string;
  costCenterCode?: string | null;
  costType?: string;
  standardRate: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  currency?: string;
  notes?: string | null;
};

export type ProductCost = {
  id: number;
  itemCode: string;
  costCenterCode: string | null;
  fiscalYear: number;
  period: number;
  materialCost: number;
  laborCost: number;
  overheadCost: number;
  totalCost: number;
  unitCost: number;
  quantity: number;
  currency: string;
  calculated: boolean;
  calculatedAt: string | null;
};

export type CalculateProductCostRequest = {
  itemCode: string;
  costCenterCode?: string | null;
  fiscalYear: number;
  period: number;
  quantity?: number;
};

export type CostAllocation = {
  id: number;
  documentNo: string;
  allocationDate: string;
  fromCostCenter: string;
  toCostCenter: string;
  allocationType: string;
  amount: number;
  allocationBasis: string | null;
  percentage: number | null;
  description: string | null;
  status: string;
  fiscalYear: number;
  period: number;
};

export type CreateCostAllocationRequest = {
  allocationDate?: string;
  fromCostCenter: string;
  toCostCenter: string;
  allocationType?: string;
  amount: number;
  allocationBasis?: string | null;
  percentage?: number | null;
  description?: string | null;
  fiscalYear: number;
  period: number;
};

export type VarianceRow = {
  itemCode: string;
  costType: string;
  standardRate: number;
  actualRate: number;
  variance: number;
  variancePercentage: number;
};

export type SearchParams = {
  page?: number;
  size?: number;
  itemCode?: string;
  costType?: string;
  fiscalYear?: number;
  status?: string;
  costCenterCode?: string;
};

export const costingApi = {
  getCostCenters: (params: SearchParams = {}) =>
    api.get<ApiResponse<CostCenter[]>>(`${BASE}/cost-centers`, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  createCostCenter: (data: CreateCostCenterRequest) =>
    api.post<ApiResponse<CostCenter>>(`${BASE}/cost-centers`, data).then((r) => r.data.data!),

  getStandardCosts: (params: SearchParams = {}) =>
    api.get<ApiResponse<StandardCost[]>>(`${BASE}/standard-costs`, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  createStandardCost: (data: CreateStandardCostRequest) =>
    api.post<ApiResponse<StandardCost>>(`${BASE}/standard-costs`, data).then((r) => r.data.data!),

  getProductCosts: (params: SearchParams = {}) =>
    api.get<ApiResponse<ProductCost[]>>(`${BASE}/product-costs`, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  calculateProductCost: (data: CalculateProductCostRequest) =>
    api.post<ApiResponse<ProductCost>>(`${BASE}/product-costs/calculate`, data).then((r) => r.data.data!),

  getAllocations: (params: SearchParams = {}) =>
    api.get<ApiResponse<CostAllocation[]>>(`${BASE}/allocations`, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  createAllocation: (data: CreateCostAllocationRequest) =>
    api.post<ApiResponse<CostAllocation>>(`${BASE}/allocations`, data).then((r) => r.data.data!),

  getVarianceAnalysis: (itemCode: string) =>
    api
      .get<ApiResponse<VarianceRow[]>>(`${BASE}/variance`, { params: { itemCode } })
      .then((r) => r.data.data ?? []),
};
