import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ColDef } from "ag-grid-community";
import { Plus } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface EmpRow {
  id: number; employeeNo: string; name: string; companyCode: string;
  departmentName: string | null; positionTitle: string | null; email: string | null;
  phone: string | null; hireDate: string | null; status: string;
}

const statusStyle: Record<string, string> = {
  ACTIVE: "badge-success", ON_LEAVE: "badge-warning", RESIGNED: "badge bg-slate-100 text-slate-500", TERMINATED: "badge-danger",
};

export default function EmployeePage() {
  const { data, isLoading } = useQuery({
    queryKey: ["employees"],
    queryFn: async () => (await api.get("/api/v1/hr/employees?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<EmpRow>[]>(() => [
    { field: "employeeNo", headerName: "Emp No.", flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "name", headerName: "Name", flex: 1.5 },
    { field: "departmentName", headerName: "Department", flex: 1.2 },
    { field: "positionTitle", headerName: "Position", flex: 1 },
    { field: "email", headerName: "Email", flex: 1.5 },
    { field: "phone", headerName: "Phone", flex: 1 },
    { field: "hireDate", headerName: "Hire Date", flex: 1 },
    { field: "status", headerName: "Status", flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], []);

  return (
    <div>
      <PageHeader title="Employees" description="Human resources management"
        breadcrumbs={[{ label: "HR" }, { label: "Employees" }]}
        actions={<button className="btn-primary"><Plus size={16} /> New Employee</button>} />
      <div className="card overflow-hidden">
        <DataGrid<EmpRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
