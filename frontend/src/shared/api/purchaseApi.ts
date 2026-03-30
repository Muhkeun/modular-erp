import api, { type ApiResponse } from "./client";

const REQUEST_BASE = "/api/v1/purchase/requests";
const ORDER_BASE = "/api/v1/purchase/orders";

export type PrStatus = "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | "CLOSED" | "CANCELLED";
export type PrType = "STANDARD" | "URGENT" | "PROJECT" | "INVESTMENT";
export type PoStatus = "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | "SENT" | "COMPLETED" | "CANCELLED";

export interface PurchaseRequestLine {
  id?: number;
  lineNo?: number;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  totalPrice?: number;
  openQuantity?: number;
  specification?: string | null;
  remark?: string | null;
}

export interface PurchaseRequest {
  id: number;
  documentNo: string;
  companyCode: string;
  plantCode: string;
  departmentCode: string | null;
  requestDate: string;
  deliveryDate: string | null;
  status: PrStatus;
  prType: PrType;
  requestedBy: string | null;
  totalAmount: number;
  description: string | null;
  lines: PurchaseRequestLine[];
}

export interface CreatePurchaseRequestRequest {
  companyCode: string;
  plantCode: string;
  departmentCode?: string | null;
  prType: PrType;
  deliveryDate?: string | null;
  description?: string | null;
  lines: PurchaseRequestLine[];
}

export interface PurchaseOrderLine {
  id?: number;
  lineNo?: number;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  taxRate: number;
  totalPrice?: number;
  taxAmount?: number;
  receivedQuantity?: number;
  openQuantity?: number;
  specification?: string | null;
  prDocumentNo?: string | null;
  prLineNo?: number | null;
}

export interface PurchaseOrder {
  id: number;
  documentNo: string;
  companyCode: string;
  plantCode: string;
  createdBy: string | null;
  vendorCode: string;
  vendorName: string;
  orderDate: string;
  deliveryDate: string | null;
  status: PoStatus;
  currencyCode: string;
  totalAmount: number;
  taxAmount: number;
  grandTotal: number;
  paymentTerms: string | null;
  remark: string | null;
  lines: PurchaseOrderLine[];
}

export interface CreatePurchaseOrderRequest {
  companyCode: string;
  plantCode: string;
  vendorCode: string;
  vendorName: string;
  deliveryDate?: string | null;
  currencyCode?: string;
  paymentTerms?: string | null;
  deliveryTerms?: string | null;
  remark?: string | null;
  lines: PurchaseOrderLine[];
}

export interface CreatePurchaseOrderFromPrRequest {
  vendorCode: string;
  vendorName: string;
  deliveryDate?: string | null;
  currencyCode?: string;
  paymentTerms?: string | null;
}

export interface SearchParams {
  page?: number;
  size?: number;
}

export const purchaseApi = {
  getRequests: (params: SearchParams = {}) =>
    api.get<ApiResponse<PurchaseRequest[]>>(REQUEST_BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getRequest: (id: number) =>
    api.get<ApiResponse<PurchaseRequest>>(`${REQUEST_BASE}/${id}`).then((r) => r.data.data!),

  createRequest: (data: CreatePurchaseRequestRequest) =>
    api.post<ApiResponse<PurchaseRequest>>(REQUEST_BASE, data).then((r) => r.data.data!),

  submitRequest: (id: number) =>
    api.post<ApiResponse<PurchaseRequest>>(`${REQUEST_BASE}/${id}/submit`).then((r) => r.data.data!),

  approveRequest: (id: number) =>
    api.post<ApiResponse<PurchaseRequest>>(`${REQUEST_BASE}/${id}/approve`).then((r) => r.data.data!),

  rejectRequest: (id: number) =>
    api.post<ApiResponse<PurchaseRequest>>(`${REQUEST_BASE}/${id}/reject`).then((r) => r.data.data!),

  deleteRequest: (id: number) =>
    api.delete(`${REQUEST_BASE}/${id}`),

  getOrders: (params: SearchParams = {}) =>
    api.get<ApiResponse<PurchaseOrder[]>>(ORDER_BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getOrder: (id: number) =>
    api.get<ApiResponse<PurchaseOrder>>(`${ORDER_BASE}/${id}`).then((r) => r.data.data!),

  createOrder: (data: CreatePurchaseOrderRequest) =>
    api.post<ApiResponse<PurchaseOrder>>(ORDER_BASE, data).then((r) => r.data.data!),

  createOrderFromRequest: (prId: number, data: CreatePurchaseOrderFromPrRequest) =>
    api.post<ApiResponse<PurchaseOrder>>(`${ORDER_BASE}/from-pr/${prId}`, data).then((r) => r.data.data!),

  submitOrder: (id: number) =>
    api.post<ApiResponse<PurchaseOrder>>(`${ORDER_BASE}/${id}/submit`).then((r) => r.data.data!),

  approveOrder: (id: number) =>
    api.post<ApiResponse<PurchaseOrder>>(`${ORDER_BASE}/${id}/approve`).then((r) => r.data.data!),

  rejectOrder: (id: number) =>
    api.post<ApiResponse<PurchaseOrder>>(`${ORDER_BASE}/${id}/reject`).then((r) => r.data.data!),
};
