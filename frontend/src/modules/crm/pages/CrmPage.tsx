import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, ArrowUpRight, Users, Target, Activity } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface CustomerRow { id: number; name: string; email: string | null; phone: string | null; company: string | null; segment: string; status: string; totalRevenue: number; }
interface LeadRow { id: number; name: string; email: string | null; phone: string | null; company: string | null; source: string; status: string; score: number; assignedTo: string | null; createdAt: string; }
interface OpportunityRow { id: number; name: string; customerName: string; stage: string; amount: number; probability: number; expectedCloseDate: string; assignedTo: string | null; }
interface ActivityRow { id: number; type: string; subject: string; relatedTo: string; relatedName: string; dueDate: string | null; status: string; assignedTo: string | null; }

type Tab = "customers" | "leads" | "opportunities" | "activities";
type Mode = "list" | "create";

const statusStyle: Record<string, string> = {
  ACTIVE: "badge-success", NEW: "badge-info", QUALIFIED: "badge-success", CONTACTED: "badge-warning",
  CONVERTED: "badge bg-blue-50 text-blue-600", LOST: "badge-danger", WON: "badge-success",
  OPEN: "badge-info", COMPLETED: "badge-success", CANCELLED: "badge bg-slate-100 text-slate-500",
};

const stageStyle: Record<string, string> = {
  PROSPECT: "badge bg-slate-100 text-slate-600", QUALIFICATION: "badge-info",
  PROPOSAL: "badge-warning", NEGOTIATION: "badge bg-orange-50 text-orange-600",
  CLOSED_WON: "badge-success", CLOSED_LOST: "badge-danger",
};

const fmt = (v: number) => v?.toLocaleString("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });

const STAGES = ["PROSPECT", "QUALIFICATION", "PROPOSAL", "NEGOTIATION", "CLOSED_WON", "CLOSED_LOST"] as const;

export default function CrmPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [tab, setTab] = useState<Tab>("customers");
  const [mode, setMode] = useState<Mode>("list");
  const [stageDialog, setStageDialog] = useState<{ id: number; stage: string } | null>(null);

  // Forms
  const [customerForm, setCustomerForm] = useState({ name: "", email: "", phone: "", company: "", segment: "" });
  const [leadForm, setLeadForm] = useState({ name: "", email: "", phone: "", company: "", source: "" });
  const [oppForm, setOppForm] = useState({ name: "", customerName: "", amount: 0, probability: 50, expectedCloseDate: "" });
  const [actForm, setActForm] = useState({ type: "CALL", subject: "", relatedTo: "", relatedName: "", dueDate: "" });

  // Queries
  const customersQ = useQuery({ queryKey: ["crm-customers"], queryFn: async () => (await api.get("/api/v1/crm/customers?size=100")).data, enabled: tab === "customers" });
  const leadsQ = useQuery({ queryKey: ["crm-leads"], queryFn: async () => (await api.get("/api/v1/crm/leads?size=100")).data, enabled: tab === "leads" });
  const oppsQ = useQuery({ queryKey: ["crm-opportunities"], queryFn: async () => (await api.get("/api/v1/crm/opportunities?size=100")).data, enabled: tab === "opportunities" });
  const actsQ = useQuery({ queryKey: ["crm-activities"], queryFn: async () => (await api.get("/api/v1/crm/activities?size=100")).data, enabled: tab === "activities" });
  const pipelineQ = useQuery({ queryKey: ["crm-pipeline"], queryFn: async () => (await api.get("/api/v1/crm/pipeline/summary")).data });

  // Mutations
  const createCustomerMut = useMutation({
    mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/crm/customers", b)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["crm-customers"] }); setMode("list"); },
  });
  const createLeadMut = useMutation({
    mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/crm/leads", b)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["crm-leads"] }); setMode("list"); },
  });
  const convertLeadMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/crm/leads/${id}/convert`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["crm-leads"] }); qc.invalidateQueries({ queryKey: ["crm-customers"] }); },
  });
  const createOppMut = useMutation({
    mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/crm/opportunities", b)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["crm-opportunities"] }); setMode("list"); },
  });
  const updateStageMut = useMutation({
    mutationFn: async ({ id, stage }: { id: number; stage: string }) => (await api.post(`/api/v1/crm/opportunities/${id}/stage`, { stage })).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["crm-opportunities"] }); qc.invalidateQueries({ queryKey: ["crm-pipeline"] }); setStageDialog(null); },
  });
  const createActMut = useMutation({
    mutationFn: async (b: Record<string, unknown>) => (await api.post("/api/v1/crm/activities", b)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["crm-activities"] }); setMode("list"); },
  });

  // Column defs
  const custCols = useMemo<ColDef<CustomerRow>[]>(() => [
    { field: "name", headerName: t("crm.customerName"), flex: 1.5, cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span> },
    { field: "company", headerName: t("crm.company"), flex: 1.2 },
    { field: "email", headerName: t("emp.email"), flex: 1.5 },
    { field: "phone", headerName: t("emp.phone"), flex: 1 },
    { field: "segment", headerName: t("crm.segment"), flex: 0.8 },
    { field: "totalRevenue", headerName: t("crm.revenue"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "status", headerName: t("common.status"), flex: 0.8, cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], [t]);

  const leadCols = useMemo<ColDef<LeadRow>[]>(() => [
    { field: "name", headerName: t("crm.leadName"), flex: 1.5, cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span> },
    { field: "company", headerName: t("crm.company"), flex: 1.2 },
    { field: "source", headerName: t("crm.source"), flex: 0.8 },
    { field: "score", headerName: t("crm.score"), flex: 0.6, type: "numericColumn" },
    { field: "assignedTo", headerName: t("crm.assignedTo"), flex: 1 },
    { field: "createdAt", headerName: t("common.datetime"), flex: 1 },
    { field: "status", headerName: t("common.status"), flex: 0.8, cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], [t]);

  const oppCols = useMemo<ColDef<OpportunityRow>[]>(() => [
    { field: "name", headerName: t("crm.oppName"), flex: 1.5, cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span> },
    { field: "customerName", headerName: t("crm.customerName"), flex: 1.2 },
    { field: "stage", headerName: t("crm.stage"), flex: 1, cellRenderer: (p: { value: string }) => <span className={stageStyle[p.value] || "badge"}>{p.value}</span> },
    { field: "amount", headerName: t("crm.amount"), flex: 1.2, type: "numericColumn", valueFormatter: (p: { value: number }) => fmt(p.value) },
    { field: "probability", headerName: t("crm.probability"), flex: 0.7, valueFormatter: (p: { value: number }) => `${p.value}%` },
    { field: "expectedCloseDate", headerName: t("crm.expectedClose"), flex: 1 },
    { field: "assignedTo", headerName: t("crm.assignedTo"), flex: 1 },
  ], [t]);

  const actCols = useMemo<ColDef<ActivityRow>[]>(() => [
    { field: "type", headerName: t("common.type"), flex: 0.7 },
    { field: "subject", headerName: t("crm.subject"), flex: 2, cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span> },
    { field: "relatedTo", headerName: t("crm.relatedTo"), flex: 0.8 },
    { field: "relatedName", headerName: t("common.name"), flex: 1.2 },
    { field: "dueDate", headerName: t("crm.dueDate"), flex: 1 },
    { field: "assignedTo", headerName: t("crm.assignedTo"), flex: 1 },
    { field: "status", headerName: t("common.status"), flex: 0.8, cellRenderer: (p: { value: string }) => <span className={statusStyle[p.value] || "badge"}>{p.value}</span> },
  ], [t]);

  const pipeline = pipelineQ.data?.data;

  const handleCreate = () => {
    if (tab === "customers") createCustomerMut.mutate(customerForm);
    else if (tab === "leads") createLeadMut.mutate(leadForm);
    else if (tab === "opportunities") createOppMut.mutate(oppForm);
    else if (tab === "activities") createActMut.mutate(actForm);
  };

  const isPending = createCustomerMut.isPending || createLeadMut.isPending || createOppMut.isPending || createActMut.isPending;

  if (mode === "create") {
    return (
      <div>
        <PageHeader title={t(`crm.new${tab.charAt(0).toUpperCase() + tab.slice(1, -1)}`)}
          breadcrumbs={[{ label: t("nav.crm") }, { label: t("common.create") }]}
          actions={<button className="btn-ghost" onClick={() => setMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>} />
        <div className="section-card">
          <p className="section-kicker">CRM</p>
          <h3 className="section-title">{t("common.basicInfo")}</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            {tab === "customers" && <>
              <div><label className="field-label">{t("common.name")}</label><input className="input" value={customerForm.name} onChange={e => setCustomerForm(p => ({ ...p, name: e.target.value }))} /></div>
              <div><label className="field-label">{t("emp.email")}</label><input className="input" type="email" value={customerForm.email} onChange={e => setCustomerForm(p => ({ ...p, email: e.target.value }))} /></div>
              <div><label className="field-label">{t("emp.phone")}</label><input className="input" value={customerForm.phone} onChange={e => setCustomerForm(p => ({ ...p, phone: e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.company")}</label><input className="input" value={customerForm.company} onChange={e => setCustomerForm(p => ({ ...p, company: e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.segment")}</label><input className="input" value={customerForm.segment} onChange={e => setCustomerForm(p => ({ ...p, segment: e.target.value }))} /></div>
            </>}
            {tab === "leads" && <>
              <div><label className="field-label">{t("common.name")}</label><input className="input" value={leadForm.name} onChange={e => setLeadForm(p => ({ ...p, name: e.target.value }))} /></div>
              <div><label className="field-label">{t("emp.email")}</label><input className="input" type="email" value={leadForm.email} onChange={e => setLeadForm(p => ({ ...p, email: e.target.value }))} /></div>
              <div><label className="field-label">{t("emp.phone")}</label><input className="input" value={leadForm.phone} onChange={e => setLeadForm(p => ({ ...p, phone: e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.company")}</label><input className="input" value={leadForm.company} onChange={e => setLeadForm(p => ({ ...p, company: e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.source")}</label><input className="input" value={leadForm.source} onChange={e => setLeadForm(p => ({ ...p, source: e.target.value }))} /></div>
            </>}
            {tab === "opportunities" && <>
              <div><label className="field-label">{t("crm.oppName")}</label><input className="input" value={oppForm.name} onChange={e => setOppForm(p => ({ ...p, name: e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.customerName")}</label><input className="input" value={oppForm.customerName} onChange={e => setOppForm(p => ({ ...p, customerName: e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.amount")}</label><input className="input" type="number" value={oppForm.amount} onChange={e => setOppForm(p => ({ ...p, amount: +e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.probability")}</label><input className="input" type="number" value={oppForm.probability} onChange={e => setOppForm(p => ({ ...p, probability: +e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.expectedClose")}</label><input className="input" type="date" value={oppForm.expectedCloseDate} onChange={e => setOppForm(p => ({ ...p, expectedCloseDate: e.target.value }))} /></div>
            </>}
            {tab === "activities" && <>
              <div><label className="field-label">{t("common.type")}</label>
                <select className="input" value={actForm.type} onChange={e => setActForm(p => ({ ...p, type: e.target.value }))}>
                  {["CALL", "EMAIL", "MEETING", "TASK", "NOTE"].map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div><label className="field-label">{t("crm.subject")}</label><input className="input" value={actForm.subject} onChange={e => setActForm(p => ({ ...p, subject: e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.relatedTo")}</label><input className="input" value={actForm.relatedTo} onChange={e => setActForm(p => ({ ...p, relatedTo: e.target.value }))} /></div>
              <div><label className="field-label">{t("common.name")}</label><input className="input" value={actForm.relatedName} onChange={e => setActForm(p => ({ ...p, relatedName: e.target.value }))} /></div>
              <div><label className="field-label">{t("crm.dueDate")}</label><input className="input" type="date" value={actForm.dueDate} onChange={e => setActForm(p => ({ ...p, dueDate: e.target.value }))} /></div>
            </>}
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <button className="btn-ghost" onClick={() => setMode("list")}>{t("common.cancel")}</button>
          <button className="btn-primary" onClick={handleCreate} disabled={isPending}>
            {isPending ? t("common.saving") : t("common.save")}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <PageHeader title={t("crm.title")} description={t("crm.description")}
        breadcrumbs={[{ label: t("nav.crm") }]}
        actions={<button className="btn-primary" onClick={() => setMode("create")}><Plus size={16} /> {t("common.new")}</button>} />

      {/* Pipeline Summary */}
      {pipeline && (
        <div className="workspace-hero mb-6">
          <p className="section-kicker">Pipeline Summary</p>
          <h3 className="section-title">{t("crm.pipeline")}</h3>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-5">
            <div className="stat-tile">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("crm.totalLeads")}</span>
              <span className="text-xl font-bold text-slate-900">{pipeline.totalLeads ?? 0}</span>
            </div>
            <div className="stat-tile">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("crm.openOpps")}</span>
              <span className="text-xl font-bold text-slate-900">{pipeline.openOpportunities ?? 0}</span>
            </div>
            <div className="stat-tile">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("crm.pipelineValue")}</span>
              <span className="text-xl font-bold text-slate-900">{fmt(pipeline.totalPipelineValue ?? 0)}</span>
            </div>
            <div className="stat-tile">
              <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">{t("crm.wonDeals")}</span>
              <span className="text-xl font-bold text-emerald-600">{pipeline.wonDeals ?? 0}</span>
            </div>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-2 mb-4">
        <button className={tab === "customers" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("customers")}><Users size={16} /> {t("crm.customers")}</button>
        <button className={tab === "leads" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("leads")}><ArrowUpRight size={16} /> {t("crm.leads")}</button>
        <button className={tab === "opportunities" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("opportunities")}><Target size={16} /> {t("crm.opportunities")}</button>
        <button className={tab === "activities" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("activities")}><Activity size={16} /> {t("crm.activities")}</button>
      </div>

      <div className="card overflow-hidden">
        {tab === "customers" && (
          <DataGrid<CustomerRow> rowData={customersQ.data?.data || []} columnDefs={custCols} loading={customersQ.isLoading} />
        )}
        {tab === "leads" && (
          <DataGrid<LeadRow> rowData={leadsQ.data?.data || []} columnDefs={leadCols} loading={leadsQ.isLoading}
            onRowClicked={(row) => {
              if (row.status === "QUALIFIED" || row.status === "NEW") {
                if (confirm(t("crm.confirmConvert"))) convertLeadMut.mutate(row.id);
              }
            }} />
        )}
        {tab === "opportunities" && (
          <DataGrid<OpportunityRow> rowData={oppsQ.data?.data || []} columnDefs={oppCols} loading={oppsQ.isLoading}
            onRowClicked={(row) => setStageDialog({ id: row.id, stage: row.stage })} />
        )}
        {tab === "activities" && (
          <DataGrid<ActivityRow> rowData={actsQ.data?.data || []} columnDefs={actCols} loading={actsQ.isLoading} />
        )}
      </div>

      {/* Stage update dialog */}
      {stageDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 px-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-[28px] border border-white/70 bg-white/95 p-6 shadow-[0_32px_80px_rgba(15,23,42,0.28)]">
            <p className="section-kicker">Opportunity</p>
            <h3 className="section-title">{t("crm.updateStage")}</h3>
            <div className="mt-4">
              <label className="field-label">{t("crm.stage")}</label>
              <select className="input" value={stageDialog.stage} onChange={e => setStageDialog(p => p ? { ...p, stage: e.target.value } : null)}>
                {STAGES.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button className="btn-ghost" onClick={() => setStageDialog(null)}>{t("common.cancel")}</button>
              <button className="btn-primary" onClick={() => updateStageMut.mutate(stageDialog)} disabled={updateStageMut.isPending}>
                {updateStageMut.isPending ? t("common.saving") : t("common.save")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
