import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/sales/orders";

export type SoStatus = "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | "SHIPPED" | "COMPLETED" | "CANCELLED";

export interface SalesOrderLineInput {
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  taxRate: number;
  specification?: string | null;
}

export interface SalesOrderLine extends SalesOrderLineInput {
  id: number;
  lineNo: number;
  totalPrice: number;
  taxAmount: number;
  shippedQuantity: number;
  openQuantity: number;
}

export interface SalesOrder {
  id: number;
  documentNo: string;
  companyCode: string;
  plantCode: string;
  createdBy: string | null;
  customerCode: string;
  customerName: string;
  orderDate: string;
  deliveryDate: string | null;
  status: SoStatus;
  currencyCode: string;
  totalAmount: number;
  taxAmount: number;
  grandTotal: number;
  paymentTerms: string | null;
  shippingAddress: string | null;
  remark: string | null;
  lines: SalesOrderLine[];
}

export interface CreateSalesOrderRequest {
  companyCode: string;
  plantCode: string;
  customerCode: string;
  customerName: string;
  deliveryDate?: string | null;
  currencyCode?: string;
  paymentTerms?: string | null;
  shippingAddress?: string | null;
  remark?: string | null;
  lines: SalesOrderLineInput[];
}

export interface SalesOrderSearchParams {
  page?: number;
  size?: number;
}

export const salesApi = {
  getOrders: (params: SalesOrderSearchParams = {}) =>
    api.get<ApiResponse<SalesOrder[]>>(BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getOrder: (id: number) =>
    api.get<ApiResponse<SalesOrder>>(`${BASE}/${id}`).then((r) => r.data.data!),

  createOrder: (data: CreateSalesOrderRequest) =>
    api.post<ApiResponse<SalesOrder>>(BASE, data).then((r) => r.data.data!),

  submitOrder: (id: number) =>
    api.post<ApiResponse<SalesOrder>>(`${BASE}/${id}/submit`).then((r) => r.data.data!),

  approveOrder: (id: number) =>
    api.post<ApiResponse<SalesOrder>>(`${BASE}/${id}/approve`).then((r) => r.data.data!),

  rejectOrder: (id: number) =>
    api.post<ApiResponse<SalesOrder>>(`${BASE}/${id}/reject`).then((r) => r.data.data!),
};
