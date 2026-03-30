import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import type { ColDef } from "ag-grid-community";
import { Plus, ArrowLeft, Mail, MailOpen, CheckCheck } from "lucide-react";
import DataGrid from "../../../shared/components/DataGrid";
import PageHeader from "../../../shared/components/PageHeader";
import { useAuth } from "../../../shared/hooks/useAuth";
import {
  notificationApi,
  type CreateNotificationTemplateRequest,
  type NotificationStatus,
  type NotificationTemplate,
} from "../../../shared/api/notificationApi";

type TemplateRow = NotificationTemplate;

type Tab = "inbox" | "templates";

const statusStyle: Record<NotificationStatus, string> = {
  PENDING: "badge bg-amber-50 text-amber-700",
  SENT: "badge bg-blue-50 text-blue-700",
  READ: "badge bg-slate-100 text-slate-600",
  FAILED: "badge-danger",
};

export default function NotificationPage() {
  const { t } = useTranslation();
  const { userId } = useAuth();
  const qc = useQueryClient();

  const [tab, setTab] = useState<Tab>("inbox");
  const [templateMode, setTemplateMode] = useState<"list" | "create">("list");
  const [templateForm, setTemplateForm] = useState<CreateNotificationTemplateRequest>({
    templateCode: "",
    templateName: "",
    eventType: "GENERAL",
    channel: "IN_APP",
    subject: "",
    body: "",
    enabled: true,
    language: "ko",
  });

  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ["notifications", userId],
    queryFn: () => notificationApi.getAll(userId!, 100),
    enabled: !!userId,
  });

  const templatesQuery = useQuery<TemplateRow[]>({
    queryKey: ["notification-templates"],
    queryFn: () => notificationApi.getTemplates(100),
    enabled: tab === "templates",
  });

  const markReadMut = useMutation({
    mutationFn: (id: number) => notificationApi.markRead(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["notifications", userId] });
    },
  });

  const markAllReadMut = useMutation({
    mutationFn: () => notificationApi.markAllRead(userId!),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["notifications", userId] });
    },
  });

  const createTemplateMut = useMutation({
    mutationFn: (body: CreateNotificationTemplateRequest) => notificationApi.createTemplate(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["notification-templates"] });
      setTemplateMode("list");
      setTemplateForm({
        templateCode: "",
        templateName: "",
        eventType: "GENERAL",
        channel: "IN_APP",
        subject: "",
        body: "",
        enabled: true,
        language: "ko",
      });
    },
  });

  const templates = templatesQuery.data ?? [];
  const unreadCount = notifications.filter((notification) => notification.status !== "READ").length;

  const templateColDefs = useMemo<ColDef<TemplateRow>[]>(() => [
    { field: "templateName", headerName: t("notification.templateName"), flex: 1.5,
      cellRenderer: (p: { value: string }) => <span className="font-semibold text-brand-700">{p.value}</span> },
    { field: "eventType", headerName: t("common.type"), flex: 0.8 },
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
              <button className="btn-secondary" onClick={() => markAllReadMut.mutate()} disabled={markAllReadMut.isPending || !userId}>
                <CheckCheck size={16} /> {t("notification.markAllRead")}
              </button>
            )}
            {tab === "templates" && templateMode === "list" && (
              <button className="btn-primary" onClick={() => {
                setTemplateForm({
                  templateCode: "",
                  templateName: "",
                  eventType: "GENERAL",
                  channel: "IN_APP",
                  subject: "",
                  body: "",
                  enabled: true,
                  language: "ko",
                });
                setTemplateMode("create");
              }}>
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
              <div key={n.id} className={`flex items-start justify-between rounded-[22px] border p-4 ${n.status === "READ" ? "border-slate-200/80 bg-white" : "border-brand-200 bg-brand-50/50"}`}>
                <div className="flex items-start gap-3 flex-1">
                  <div className="mt-1">
                    {n.status === "READ" ? <MailOpen size={18} className="text-slate-400" /> : <Mail size={18} className="text-brand-600" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className={`text-sm font-medium ${n.status === "READ" ? "text-slate-600" : "text-slate-900"}`}>{n.subject}</span>
                      <span className={statusStyle[n.status]}>{n.status}</span>
                      <span className="badge bg-slate-100 text-slate-500">{n.channel}</span>
                    </div>
                    <p className="mt-1 text-sm text-slate-500 line-clamp-2">{n.body}</p>
                    <div className="mt-2 flex items-center gap-3 text-xs text-slate-400">
                      <span>{n.readAt ?? n.sentAt ?? "-"}</span>
                      {n.referenceType && <span>{n.referenceType}: {n.referenceId}</span>}
                      {n.errorMessage && <span className="text-red-500">{n.errorMessage}</span>}
                    </div>
                  </div>
                </div>
                <div>
                  {n.status !== "READ" && (
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
                <label className="field-label">{t("notification.templateCode", "템플릿 코드")}</label>
                <input className="input" value={templateForm.templateCode} onChange={e => setTemplateForm((p) => ({ ...p, templateCode: e.target.value }))} />
              </div>
              <div>
                <label className="field-label">{t("notification.templateName")}</label>
                <input className="input" value={templateForm.templateName} onChange={e => setTemplateForm((p) => ({ ...p, templateName: e.target.value }))} />
              </div>
              <div>
                <label className="field-label">{t("common.type")}</label>
                <input className="input" value={templateForm.eventType} onChange={e => setTemplateForm((p) => ({ ...p, eventType: e.target.value }))} />
              </div>
              <div>
                <label className="field-label">{t("notification.channel")}</label>
                <select className="input" value={templateForm.channel} onChange={e => setTemplateForm((p) => ({ ...p, channel: e.target.value as CreateNotificationTemplateRequest["channel"] }))}>
                  {["IN_APP", "EMAIL", "SMS", "PUSH"].map((channel) => <option key={channel} value={channel}>{channel}</option>)}
                </select>
              </div>
              <div className="md:col-span-3">
                <label className="field-label">{t("notification.subject")}</label>
                <input className="input" value={templateForm.subject} onChange={e => setTemplateForm((p) => ({ ...p, subject: e.target.value }))} />
              </div>
              <div className="md:col-span-3">
                <label className="field-label">{t("notification.body")}</label>
                <textarea className="input" rows={4} value={templateForm.body} onChange={e => setTemplateForm((p) => ({ ...p, body: e.target.value }))} />
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
