import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/account/journal-entries";

export type JournalEntryType = "MANUAL" | "GOODS_RECEIPT" | "GOODS_ISSUE" | "INVOICE" | "PAYMENT";
export type JeStatus = "DRAFT" | "POSTED" | "REVERSED";

export interface JournalEntryLineInput {
  accountCode: string;
  accountName: string;
  debitAmount?: number;
  creditAmount?: number;
  costCenter?: string | null;
  description?: string | null;
}

export interface JournalEntryLine extends JournalEntryLineInput {
  id: number;
  lineNo: number;
  debitAmount: number;
  creditAmount: number;
}

export interface JournalEntry {
  id: number;
  documentNo: string;
  companyCode: string;
  postingDate: string;
  documentDate: string;
  entryType: JournalEntryType;
  status: JeStatus;
  referenceDocNo: string | null;
  description: string | null;
  currencyCode: string;
  totalDebit: number;
  totalCredit: number;
  isBalanced: boolean;
  lines: JournalEntryLine[];
}

export interface CreateJournalEntryRequest {
  companyCode: string;
  postingDate?: string;
  entryType?: JournalEntryType;
  referenceDocNo?: string | null;
  referenceDocType?: string | null;
  description?: string | null;
  currencyCode?: string;
  lines: JournalEntryLineInput[];
}

export interface JournalEntrySearchParams {
  page?: number;
  size?: number;
}

export const accountApi = {
  getJournalEntries: (params: JournalEntrySearchParams = {}) =>
    api.get<ApiResponse<JournalEntry[]>>(BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getJournalEntry: (id: number) =>
    api.get<ApiResponse<JournalEntry>>(`${BASE}/${id}`).then((r) => r.data.data!),

  createJournalEntry: (data: CreateJournalEntryRequest) =>
    api.post<ApiResponse<JournalEntry>>(BASE, data).then((r) => r.data.data!),

  postJournalEntry: (id: number) =>
    api.post<ApiResponse<JournalEntry>>(`${BASE}/${id}/post`).then((r) => r.data.data!),

  reverseJournalEntry: (id: number) =>
    api.post<ApiResponse<JournalEntry>>(`${BASE}/${id}/reverse`).then((r) => r.data.data!),
};
