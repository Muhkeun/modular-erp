import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Mail, MailOpen, CheckCheck } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

interface NotificationRow {
  id: number;
  title: string;
  message: string;
  type: string;
  channel: string;
  isRead: boolean;
  createdAt: string;
  referenceType: string | null;
  referenceId: string | null;
}

interface TemplateRow {
  id: number;
  name: string;
  type: string;
  channel: string;
  subject: string;
  bodyTemplate: string;
  enabled: boolean;
}

type Tab = "inbox" | "templates";

const typeStyle: Record<string, string> = {
  INFO: "badge bg-blue-50 text-blue-600",
  WARNING: "badge-warning",
  ERROR: "badge-danger",
  SUCCESS: "badge-success",
};

export default function NotificationPage() {
  const { t } = useTranslation();
  const qc = useQueryClient();

  const [tab, setTab] = useState<Tab>("inbox");
  const [templateMode, setTemplateMode] = useState<"list" | "create">("list");
  const [templateForm, setTemplateForm] = useState({
    name: "", type: "INFO", channel: "EMAIL", subject: "", bodyTemplate: "",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["notifications"],
    queryFn: async () => (await api.get("/api/v1/notification/notifications?size=100")).data,
  });

  const templatesQuery = useQuery({
    queryKey: ["notification-templates"],
    queryFn: async () => (await api.get("/api/v1/notification/templates?size=100")).data,
    enabled: tab === "templates",
  });

  const markReadMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/notification/notifications/${id}/read`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["notifications"] }); },
  });

  const markUnreadMut = useMutation({
    mutationFn: async (id: number) => (await api.post(`/api/v1/notification/notifications/${id}/unread`)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["notifications"] }); },
  });

  const markAllReadMut = useMutation({
    mutationFn: async () => (await api.post("/api/v1/notification/notifications/read-all")).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["notifications"] }); },
  });

  const createTemplateMut = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post("/api/v1/notification/templates", body)).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["notification-templates"] }); setTemplateMode("list"); },
  });

  const notifications: NotificationRow[] = data?.data || [];
  const templates: TemplateRow[] = templatesQuery.data?.data || [];
  const unreadCount = notifications.filter(n => !n.isRead).length;

  const templateColDefs = useMemo<ColDef<TemplateRow>[]>(() => [
    { field: "name", headerName: t("notification.templateName"), flex: 1.5,
      cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span> },
    { field: "type", headerName: t("common.type"), flex: 0.8 },
    { field: "channel", headerName: t("notification.channel"), flex: 0.8 },
    { field: "subject", headerName: t("notification.subject"), flex: 2 },
    { field: "enabled", headerName: t("batch.enabled"), flex: 0.6,
      cellRenderer: (p: { value: boolean }) => p.value
        ? <span className="badge-success">{t("common.active")}</span>
        : <span className="badge bg-slate-100 text-slate-500">{t("common.inactive")}</span> },
  ], [t]);

  return (
    <div>
      <PageHeader title={t("notification.title")} description={t("notification.description")}
        breadcrumbs={[{ label: t("notification.title") }]}
        actions={
          <div className="flex gap-2">
            {tab === "inbox" && unreadCount > 0 && (
              <button className="btn-secondary" onClick={() => markAllReadMut.mutate()} disabled={markAllReadMut.isPending}>
                <CheckCheck size={16} /> {t("notification.markAllRead")}
              </button>
            )}
            {tab === "templates" && templateMode === "list" && (
              <button className="btn-primary" onClick={() => { setTemplateForm({ name: "", type: "INFO", channel: "EMAIL", subject: "", bodyTemplate: "" }); setTemplateMode("create"); }}>
                <Plus size={16} /> {t("notification.newTemplate")}
              </button>
            )}
          </div>
        } />

      {/* Tabs */}
      <div className="flex gap-2 mb-4">
        <button className={tab === "inbox" ? "btn-primary" : "btn-ghost"} onClick={() => setTab("inbox")}>
          <Mail size={16} /> {t("notification.inbox")} {unreadCount > 0 && <span className="ml-1 rounded-full bg-red-500 px-2 py-0.5 text-xs text-white">{unreadCount}</span>}
        </button>
        <button className={tab === "templates" ? "btn-primary" : "btn-ghost"} onClick={() => { setTab("templates"); setTemplateMode("list"); }}>
          {t("notification.templates")}
        </button>
      </div>

      {tab === "inbox" && (
        <div className="space-y-2">
          {isLoading ? (
            <div className="section-card text-center text-slate-400">{t("common.loading")}</div>
          ) : notifications.length === 0 ? (
            <div className="section-card text-center text-slate-400">{t("common.noData")}</div>
          ) : (
            notifications.map(n => (
              <div key={n.id} className={`flex items-start justify-between rounded-[22px] border p-4 ${n.isRead ? "border-slate-200/80 bg-white" : "border-brand-200 bg-brand-50/50"}`}>
                <div className="flex items-start gap-3 flex-1">
                  <div className="mt-1">
                    {n.isRead ? <MailOpen size={18} className="text-slate-400" /> : <Mail size={18} className="text-brand-600" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className={`text-sm font-medium ${n.isRead ? "text-slate-600" : "text-slate-900"}`}>{n.title}</span>
                      <span className={typeStyle[n.type] || "badge"}>{n.type}</span>
                      <span className="badge bg-slate-100 text-slate-500">{n.channel}</span>
                    </div>
                    <p className="mt-1 text-sm text-slate-500 line-clamp-2">{n.message}</p>
                    <div className="mt-2 flex items-center gap-3 text-xs text-slate-400">
                      <span>{n.createdAt}</span>
                      {n.referenceType && <span>{n.referenceType}: {n.referenceId}</span>}
                    </div>
                  </div>
                </div>
                <div>
                  {n.isRead ? (
                    <button className="btn-ghost text-xs" onClick={() => markUnreadMut.mutate(n.id)}>{t("notification.markUnread")}</button>
                  ) : (
                    <button className="btn-ghost text-xs" onClick={() => markReadMut.mutate(n.id)}>{t("notification.markRead")}</button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === "templates" && templateMode === "list" && (
        <div className="card overflow-hidden">
          <DataGrid<TemplateRow> rowData={templates} columnDefs={templateColDefs} loading={templatesQuery.isLoading} />
        </div>
      )}

      {tab === "templates" && templateMode === "create" && (
        <div>
          <div className="flex items-center gap-2 mb-4">
            <button className="btn-ghost" onClick={() => setTemplateMode("list")}><ArrowLeft size={16} /> {t("common.back")}</button>
          </div>
          <div className="section-card">
            <p className="section-kicker">Template</p>
            <h3 className="section-title">{t("notification.newTemplate")}</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
              <div>
                <label className="field-label">{t("notification.templateName")}</label>
                <input className="input" value={templateForm.name} onChange={e => setTemplateForm(p => ({ ...p, name: e.target.value }))} />
              </div>
              <div>
                <label className="field-label">{t("common.type")}</label>
                <select className="input" value={templateForm.type} onChange={e => setTemplateForm(p => ({ ...p, type: e.target.value }))}>
                  {["INFO", "WARNING", "ERROR", "SUCCESS"].map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <label className="field-label">{t("notification.channel")}</label>
                <select className="input" value={templateForm.channel} onChange={e => setTemplateForm(p => ({ ...p, channel: e.target.value }))}>
                  {["EMAIL", "IN_APP", "SMS", "PUSH"].map(c => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div className="md:col-span-3">
                <label className="field-label">{t("notification.subject")}</label>
                <input className="input" value={templateForm.subject} onChange={e => setTemplateForm(p => ({ ...p, subject: e.target.value }))} />
              </div>
              <div className="md:col-span-3">
                <label className="field-label">{t("notification.body")}</label>
                <textarea className="input" rows={4} value={templateForm.bodyTemplate} onChange={e => setTemplateForm(p => ({ ...p, bodyTemplate: e.target.value }))} />
              </div>
            </div>
          </div>
          <div className="flex justify-end gap-3 mt-6">
            <button className="btn-ghost" onClick={() => setTemplateMode("list")}>{t("common.cancel")}</button>
            <button className="btn-primary" onClick={() => createTemplateMut.mutate(templateForm)} disabled={createTemplateMut.isPending}>
              {createTemplateMut.isPending ? t("common.saving") : t("common.save")}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
