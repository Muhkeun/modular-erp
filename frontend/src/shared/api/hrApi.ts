import api, { type ApiResponse } from "./client";

const BASE = "/api/v1/hr/employees";

export type EmployeeStatus = "ACTIVE" | "ON_LEAVE" | "RESIGNED" | "TERMINATED";

export interface Employee {
  id: number;
  employeeNo: string;
  name: string;
  companyCode: string;
  departmentCode: string | null;
  departmentName: string | null;
  positionTitle: string | null;
  jobTitle: string | null;
  email: string | null;
  phone: string | null;
  hireDate: string | null;
  terminationDate: string | null;
  status: EmployeeStatus;
  active: boolean;
}

export interface CreateEmployeeRequest {
  employeeNo: string;
  name: string;
  companyCode: string;
  departmentCode?: string | null;
  departmentName?: string | null;
  positionTitle?: string | null;
  jobTitle?: string | null;
  email?: string | null;
  phone?: string | null;
  hireDate?: string | null;
}

export interface EmployeeSearchParams {
  page?: number;
  size?: number;
}

export const hrApi = {
  getEmployees: (params: EmployeeSearchParams = {}) =>
    api.get<ApiResponse<Employee[]>>(BASE, { params }).then((r) => ({
      data: r.data.data ?? [],
      meta: r.data.meta,
    })),

  createEmployee: (data: CreateEmployeeRequest) =>
    api.post<ApiResponse<Employee>>(BASE, data).then((r) => r.data.data!),
};
