import api, { type ApiResponse } from "./client";

const GOODS_RECEIPT_BASE = "/api/v1/logistics/goods-receipts";
const GOODS_ISSUE_BASE = "/api/v1/logistics/goods-issues";
const STOCK_BASE = "/api/v1/logistics/stock";

export type GrStatus = "DRAFT" | "CONFIRMED" | "CANCELLED";
export type GiType = "SALES" | "TRANSFER" | "PRODUCTION" | "SCRAP" | "RETURN";
export type GiStatus = "DRAFT" | "CONFIRMED" | "CANCELLED";

export interface GoodsReceiptLineInput {
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  poLineNo?: string | number | null;
  storageLocation?: string | null;
}

export interface GoodsReceiptLine {
  id: number;
  lineNo: number;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  totalPrice: number;
  storageLocation: string;
  poLineNo: number | null;
}

export interface GoodsReceipt {
  id: number;
  documentNo: string;
  companyCode: string;
  plantCode: string;
  createdBy: string | null;
  storageLocation: string;
  poDocumentNo: string | null;
  vendorCode: string;
  vendorName: string;
  receiptDate: string;
  status: GrStatus;
  remark: string | null;
  lines: GoodsReceiptLine[];
}

export interface CreateGoodsReceiptRequest {
  companyCode: string;
  plantCode: string;
  storageLocation: string;
  poDocumentNo?: string | null;
  vendorCode: string;
  vendorName: string;
  receiptDate: string;
  remark?: string | null;
  lines: GoodsReceiptLineInput[];
}

export interface GoodsIssueLineInput {
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  storageLocation?: string | null;
}

export interface GoodsIssueLine {
  id: number;
  lineNo: number;
  itemCode: string;
  itemName: string;
  quantity: number;
  unitOfMeasure: string;
  storageLocation: string;
}

export interface GoodsIssue {
  id: number;
  documentNo: string;
  companyCode: string;
  plantCode: string;
  createdBy: string | null;
  storageLocation: string;
  issueType: GiType;
  referenceDocNo: string | null;
  issueDate: string;
  status: GiStatus;
  remark: string | null;
  lines: GoodsIssueLine[];
}

export interface CreateGoodsIssueRequest {
  companyCode: string;
  plantCode: string;
  storageLocation: string;
  issueType?: GiType;
  referenceDocNo?: string | null;
  issueDate: string;
  remark?: string | null;
  lines: GoodsIssueLineInput[];
}

export interface StockRow {
  id: number;
  itemCode: string;
  itemName: string;
  plantCode: string;
  storageLocation: string;
  unitOfMeasure: string;
  quantityOnHand: number;
  quantityReserved: number;
  availableQuantity: number;
  totalValue: number;
}

export interface ListParams {
  page?: number;
  size?: number;
}

export const logisticsApi = {
  getGoodsReceipts: (params: ListParams = {}) =>
    api.get<ApiResponse<GoodsReceipt[]>>(GOODS_RECEIPT_BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getGoodsReceipt: (id: number) =>
    api.get<ApiResponse<GoodsReceipt>>(`${GOODS_RECEIPT_BASE}/${id}`).then((r) => r.data.data!),

  createGoodsReceipt: (data: CreateGoodsReceiptRequest) =>
    api.post<ApiResponse<GoodsReceipt>>(GOODS_RECEIPT_BASE, data).then((r) => r.data.data!),

  confirmGoodsReceipt: (id: number) =>
    api.post<ApiResponse<GoodsReceipt>>(`${GOODS_RECEIPT_BASE}/${id}/confirm`).then((r) => r.data.data!),

  getGoodsIssues: (params: ListParams = {}) =>
    api.get<ApiResponse<GoodsIssue[]>>(GOODS_ISSUE_BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getGoodsIssue: (id: number) =>
    api.get<ApiResponse<GoodsIssue>>(`${GOODS_ISSUE_BASE}/${id}`).then((r) => r.data.data!),

  createGoodsIssue: (data: CreateGoodsIssueRequest) =>
    api.post<ApiResponse<GoodsIssue>>(GOODS_ISSUE_BASE, data).then((r) => r.data.data!),

  confirmGoodsIssue: (id: number) =>
    api.post<ApiResponse<GoodsIssue>>(`${GOODS_ISSUE_BASE}/${id}/confirm`).then((r) => r.data.data!),

  getStock: (params: ListParams = {}) =>
    api.get<ApiResponse<StockRow[]>>(STOCK_BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),
};
