import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({
    queryKey: ["employees"],
    queryFn: async () => (await api.get("/api/v1/hr/employees?size=100")).data,
  });

  const columnDefs = useMemo<ColDef<EmpRow>[]>(() => [
    { field: "employeeNo", headerName: t("emp.empNo"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className="font-mono font-semibold text-brand-700">{p.value}</span> },
    { field: "name", headerName: t("emp.name"), flex: 1.5 },
    { field: "departmentName", headerName: t("emp.department"), flex: 1.2 },
    { field: "positionTitle", headerName: t("emp.position"), flex: 1 },
    { field: "email", headerName: t("emp.email"), flex: 1.5 },
    { field: "phone", headerName: t("emp.phone"), flex: 1 },
    { field: "hireDate", headerName: t("emp.hireDate"), flex: 1 },
    { field: "status", headerName: t("common.status"), flex: 0.8,
      cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{t("status." + p.value, p.value)}</span> },
  ], [t]);

  return (
    <div>
      <PageHeader title={t("emp.title")} description={t("emp.description")}
        breadcrumbs={[{ label: t("nav.hr") }, { label: t("emp.title") }]}
        actions={<button className="btn-primary"><Plus size={16} /> {t("emp.newEmp")}</button>} />
      <div className="card overflow-hidden">
        <DataGrid<EmpRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
      </div>
    </div>
  );
}
