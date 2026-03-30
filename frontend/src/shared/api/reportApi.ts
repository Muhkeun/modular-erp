import api, { type ApiResponse } from "./client";

const EXPORT_BASE = "/api/v1/export";
const REPORT_BASE = "/api/v1/reports";

export type ReportType = "TABLE" | "SUMMARY" | "CHART" | "CUSTOM";
export type OutputFormat = "EXCEL" | "PDF" | "CSV" | "HTML";
export type ExecutionStatus = "QUEUED" | "GENERATING" | "COMPLETED" | "FAILED";
export type ReportAlign = "left" | "center" | "right";

export interface ReportColumn {
  field: string;
  header: string;
  width?: number | null;
  align?: ReportAlign | null;
}

export interface GenericExportRequest {
  moduleName: string;
  columns: ReportColumn[];
  rows: Array<Record<string, unknown>>;
  title?: string | null;
  fileName?: string | null;
  landscape?: boolean;
}

export interface PdfTableRequest {
  title: string;
  subtitle?: string | null;
  columns: ReportColumn[];
  rows: Array<Record<string, unknown>>;
  landscape?: boolean;
  footer?: string | null;
}

export interface PdfHtmlRequest {
  html: string;
  landscape?: boolean;
}

export interface ExcelExportRequest {
  sheetName?: string | null;
  columns: ReportColumn[];
  rows: Array<Record<string, unknown>>;
  fileName?: string | null;
}

export interface SheetData {
  sheetName: string;
  columns: ReportColumn[];
  rows: Array<Record<string, unknown>>;
}

export interface MultiSheetExcelRequest {
  sheets: SheetData[];
  fileName?: string | null;
}

export interface CreateReportTemplateRequest {
  templateCode: string;
  templateName: string;
  reportType?: ReportType;
  outputFormat?: OutputFormat;
  moduleCode: string;
  queryDefinition?: string;
  layoutDefinition?: string | null;
  description?: string | null;
}

export interface UpdateReportTemplateRequest {
  templateName?: string | null;
  reportType?: ReportType | null;
  outputFormat?: OutputFormat | null;
  queryDefinition?: string | null;
  layoutDefinition?: string | null;
  enabled?: boolean | null;
  description?: string | null;
}

export interface ReportTemplate {
  id: number;
  templateCode: string;
  templateName: string;
  reportType: ReportType;
  outputFormat: OutputFormat;
  moduleCode: string;
  queryDefinition: string;
  layoutDefinition: string | null;
  enabled: boolean;
  description: string | null;
}

export interface GenerateReportRequest {
  templateId: number;
  outputFormat?: OutputFormat | null;
  parameters?: string | null;
  columns?: ReportColumn[] | null;
  rows?: Array<Record<string, unknown>> | null;
}

export interface ReportExecution {
  id: number;
  executionNo: string;
  templateId: number | null;
  templateName: string | null;
  outputFormat: OutputFormat;
  status: ExecutionStatus;
  fileSize: number | null;
  generatedAt: string | null;
  errorMessage: string | null;
  requestedBy: string;
}

function extractFilename(contentDisposition: string | undefined, fallback: string) {
  const match = contentDisposition?.match(/filename="?([^"]+)"?/i);
  return match?.[1] ?? fallback;
}

async function exportFile(
  path: string,
  body: unknown,
  fallbackFilename: string
) {
  const response = await api.post<Blob>(path, body, { responseType: "blob" });
  return {
    blob: response.data,
    fileName: extractFilename(response.headers["content-disposition"], fallbackFilename),
  };
}

export const exportApi = {
  excel: (request: GenericExportRequest) =>
    exportFile(`${EXPORT_BASE}/excel`, request, request.fileName ?? "export.xlsx"),

  pdf: (request: GenericExportRequest) =>
    exportFile(`${EXPORT_BASE}/pdf`, request, request.fileName ?? "export.pdf"),

  csv: (request: GenericExportRequest) =>
    exportFile(`${EXPORT_BASE}/csv`, request, request.fileName ?? "export.csv"),
};

export const reportApi = {
  tablePdf: (request: PdfTableRequest) =>
    exportFile(`${REPORT_BASE}/pdf/table`, request, `${request.title || "report"}.pdf`),

  htmlPdf: (request: PdfHtmlRequest) =>
    exportFile(`${REPORT_BASE}/pdf/html`, request, "report.pdf"),

  excel: (request: ExcelExportRequest) =>
    exportFile(`${REPORT_BASE}/excel`, request, request.fileName ?? "report.xlsx"),

  multiSheetExcel: (request: MultiSheetExcelRequest) =>
    exportFile(`${REPORT_BASE}/excel/multi-sheet`, request, request.fileName ?? "report.xlsx"),

  listTemplates: (page = 0, size = 20) =>
    api.get<ApiResponse<ReportTemplate[]>>(`${REPORT_BASE}/templates`, { params: { page, size } }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  getTemplate: (id: number) =>
    api.get<ApiResponse<ReportTemplate>>(`${REPORT_BASE}/templates/${id}`).then((r) => r.data.data!),

  createTemplate: (data: CreateReportTemplateRequest) =>
    api.post<ApiResponse<ReportTemplate>>(`${REPORT_BASE}/templates`, data).then((r) => r.data.data!),

  updateTemplate: (id: number, data: UpdateReportTemplateRequest) =>
    api.put<ApiResponse<ReportTemplate>>(`${REPORT_BASE}/templates/${id}`, data).then((r) => r.data.data!),

  deleteTemplate: (id: number) =>
    api.delete<ApiResponse<void>>(`${REPORT_BASE}/templates/${id}`),

  generate: (data: GenerateReportRequest) =>
    api.post<ApiResponse<ReportExecution>>(`${REPORT_BASE}/generate`, data).then((r) => r.data.data!),

  downloadExecution: async (id: number, fallbackFilename = `report-${id}`) => {
    const response = await api.get<Blob>(`${REPORT_BASE}/${id}/download`, { responseType: "blob" });
    return {
      blob: response.data,
      fileName: extractFilename(response.headers["content-disposition"], fallbackFilename),
    };
  },
};
