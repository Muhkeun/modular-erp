import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/currencies";

export type CurrencyStatus = "ACTIVE" | "INACTIVE";
export type RateType = "SPOT" | "AVERAGE" | "CLOSING";
export type RevaluationStatus = "DRAFT" | "POSTED" | "REVERSED";

export type Currency = {
  id: number;
  currencyCode: string;
  currencyName: string;
  symbol: string;
  decimalPlaces: number;
  isBaseCurrency: boolean;
  status: CurrencyStatus;
};

export type CreateCurrencyRequest = {
  currencyCode: string;
  currencyName: string;
  symbol: string;
  decimalPlaces?: number;
  isBaseCurrency?: boolean;
  status?: CurrencyStatus;
};

export type ExchangeRate = {
  id: number;
  fromCurrency: string;
  toCurrency: string;
  rateDate: string;
  exchangeRate: number;
  rateType: RateType;
  source: string | null;
};

export type CreateExchangeRateRequest = {
  fromCurrency: string;
  toCurrency: string;
  rateDate?: string;
  exchangeRate: number;
  rateType?: RateType;
  source?: string | null;
};

export type ConvertRequest = {
  amount: number;
  fromCurrency: string;
  toCurrency: string;
  date?: string;
};

export type ConvertResponse = {
  fromAmount: number;
  fromCurrency: string;
  toAmount: number;
  toCurrency: string;
  exchangeRate: number;
  rateDate: string;
};

export type Revaluation = {
  id: number;
  documentNo: string;
  companyCode: string | null;
  revaluationDate: string;
  fiscalYear: number;
  period: number;
  fromCurrency: string;
  toCurrency: string;
  originalRate: number;
  revaluationRate: number;
  unrealizedGainLoss: number;
  status: RevaluationStatus;
  postedBy: string | null;
  postedAt: string | null;
};

export type CreateRevaluationRequest = {
  companyCode: string;
  revaluationDate?: string;
  fiscalYear: number;
  period: number;
  fromCurrency: string;
  toCurrency: string;
  originalRate: number;
  revaluationRate: number;
  unrealizedGainLoss: number;
};

export type SearchParams = {
  page?: number;
  size?: number;
  status?: string;
  fiscalYear?: number;
  fromCurrency?: string;
  toCurrency?: string;
};

export const currencyApi = {
  getCurrencies: (params: SearchParams = {}) =>
    api.get<ApiResponse<Currency[]>>(BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  createCurrency: (data: CreateCurrencyRequest) =>
    api.post<ApiResponse<Currency>>(BASE, data).then((r) => r.data.data!),

  updateCurrency: (id: number, data: CreateCurrencyRequest) =>
    api.put<ApiResponse<Currency>>(`${BASE}/${id}`, data).then((r) => r.data.data!),

  getRates: (params: SearchParams = {}) =>
    api.get<ApiResponse<ExchangeRate[]>>(`${BASE}/exchange-rates`, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  createRate: (data: CreateExchangeRateRequest) =>
    api.post<ApiResponse<ExchangeRate>>(`${BASE}/exchange-rates`, data).then((r) => r.data.data!),

  convert: (data: ConvertRequest) =>
    api.post<ApiResponse<ConvertResponse>>(`${BASE}/convert`, data).then((r) => r.data.data!),

  getRevaluations: (params: SearchParams = {}) =>
    api.get<ApiResponse<Revaluation[]>>(`${BASE}/revaluations`, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  runRevaluation: (data: CreateRevaluationRequest) =>
    api.post<ApiResponse<Revaluation>>(`${BASE}/revaluations`, data).then((r) => r.data.data!),
};
