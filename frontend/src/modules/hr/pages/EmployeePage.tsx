import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

/* ── types ─────────────────────────────────────────── */
interface EmpRow {
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
  status: string;
}

type Mode = "list" | "create";

const statusStyle: Record<string, string> = {
  ACTIVE: "badge-success",
  ON_LEAVE: "badge-warning",
  RESIGNED: "badge bg-slate-100 text-slate-500",
  TERMINATED: "badge-danger",
};

/* ── component ─────────────────────────────────────── */
export default function EmployeePage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [mode, setMode] = useState<Mode>("list");
  const [form, setForm] = useState({
    employeeNo: "", name: "", companyCode: "", departmentCode: "", departmentName: "",
    positionTitle: "", jobTitle: "", email: "", phone: "", hireDate: "",
  });

  /* queries */
  const { data, isLoading } = useQuery({
    queryKey: ["employees"],
    queryFn: async () => (await api.get("/api/v1/hr/employees?size=100")).data,
  });

  /* mutations */
  const createMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/hr/employees", body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["employees"] }); setMode("list"); },
  });

  /* column defs */
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

  /* handlers */
  const openCreate = () => {
    setForm({ employeeNo: "", name: "", companyCode: "", departmentCode: "", departmentName: "",
      positionTitle: "", jobTitle: "", email: "", phone: "", hireDate: "" });
    setMode("create");
  };

  const handleSave = () => {
    createMut.mutate(form);
  };

  /* ── LIST ─────────────────────────────────────────── */
  if (mode === "list") {
    return (
      <div>
        <PageHeader title={t("emp.title")} description={t("emp.description")}
          breadcrumbs={[{ label: t("nav.hr") }, { label: t("emp.title") }]}
          actions={<button className="btn-primary" onClick={openCreate}><Plus size={16} /> {t("emp.newEmp")}</button>} />
        <div className="card overflow-hidden">
          <DataGrid<EmpRow> rowData={data?.data || []} columnDefs={columnDefs} loading={isLoading} />
        </div>
      </div>
    );
  }

  /* ── CREATE ──────────────────────────────────────── */
  return (
    <div>
      <PageHeader title={t("emp.newEmp")}
        breadcrumbs={[{ label: t("nav.hr") }, { label: t("emp.title") }, { label: t("common.create") }]}
        actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />

      {/* workspace hero */}
      <div className="workspace-hero">
        <p className="section-kicker">HR Workspace</p>
        <h3 className="section-title">{t("emp.newEmp")}</h3>
      </div>

      <div className="section-card">
        <p className="section-kicker">Employee Info</p>
        <h3 className="section-title">{t("common.basicInfo")}</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
          <div>
            <label className="field-label">{t("emp.empNo")}</label>
            <input className="input" value={form.employeeNo} onChange={e => setForm(p => ({ ...p, employeeNo: e.target.value }))} />
          </div>
          <div>
            <label className="field-label">{t("emp.name")}</label>
            <input className="input" value={form.name} onChange={e => setForm(p => ({ ...p, name: e.target.value }))} />
          </div>
          <div>
            <label className="field-label">{t("emp.companyCode")}</label>
            <input className="input" value={form.companyCode} onChange={e => setForm(p => ({ ...p, companyCode: e.target.value }))} />
          </div>
          <div>
            <label className="field-label">{t("emp.departmentCode")}</label>
            <input className="input" value={form.departmentCode} onChange={e => setForm(p => ({ ...p, departmentCode: e.target.value }))} />
          </div>
          <div>
            <label className="field-label">{t("emp.department")}</label>
            <input className="input" value={form.departmentName} onChange={e => setForm(p => ({ ...p, departmentName: e.target.value }))} />
          </div>
          <div>
            <label className="field-label">{t("emp.position")}</label>
            <input className="input" value={form.positionTitle} onChange={e => setForm(p => ({ ...p, positionTitle: e.target.value }))} />
          </div>
          <div>
            <label className="field-label">{t("emp.jobTitle")}</label>
            <input className="input" value={form.jobTitle} onChange={e => setForm(p => ({ ...p, jobTitle: e.target.value }))} />
          </div>
          <div>
            <label className="field-label">{t("emp.email")}</label>
            <input className="input" type="email" value={form.email} onChange={e => setForm(p => ({ ...p, email: e.target.value }))} />
          </div>
          <div>
            <label className="field-label">{t("emp.phone")}</label>
            <input className="input" value={form.phone} onChange={e => setForm(p => ({ ...p, phone: e.target.value }))} />
          </div>
          <div>
            <label className="field-label">{t("emp.hireDate")}</label>
            <input className="input" type="date" value={form.hireDate} onChange={e => setForm(p => ({ ...p, hireDate: e.target.value }))} />
          </div>
        </div>
      </div>

      <div className="flex justify-end gap-3 mt-6">
        <button className="btn-ghost" onClick={() => setMode("list")}>{t("common.cancel")}</button>
        <button className="btn-primary" onClick={handleSave} disabled={createMut.isPending}>
          {createMut.isPending ? t("common.saving") : t("common.save")}
        </button>
      </div>
    </div>
  );
}
