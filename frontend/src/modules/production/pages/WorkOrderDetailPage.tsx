import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import {
  ArrowLeft,
  Play,
  CheckCircle,
  Package,
  Clock,
  Send,
  Lock,
  FileText,
  X,
} from "lucide-react";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

const statusColors: Record<string, string> = {
  PLANNED: "bg-slate-100 text-slate-700",
  RELEASED: "bg-blue-100 text-blue-700",
  IN_PROGRESS: "bg-amber-100 text-amber-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  CLOSED: "bg-slate-100 text-slate-500",
  PENDING: "bg-slate-50 text-slate-500",
};

export default function WorkOrderDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const [showReport, setShowReport] = useState(false);
  const [reportForm, setReportForm] = useState({ goodQuantity: 0, scrapQuantity: 0, operationNo: 1 });
  const [issueForm, setIssueForm] = useState<{ itemCode: string; quantity: number } | null>(null);

  const { data } = useQuery({
    queryKey: ["work-order", id],
    queryFn: async () => (await api.get(`/api/v1/production/work-orders/${id}`)).data.data,
    enabled: !!id,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["work-order", id] });

  const releaseMutation = useMutation({
    mutationFn: async () => (await api.post(`/api/v1/production/work-orders/${id}/release`)).data,
    onSuccess: invalidate,
  });

  const startMutation = useMutation({
    mutationFn: async () => (await api.post(`/api/v1/production/work-orders/${id}/start`)).data,
    onSuccess: invalidate,
  });

  const reportMutation = useMutation({
    mutationFn: async (payload: { goodQuantity: number; scrapQuantity: number; operationNo: number }) =>
      (await api.post(`/api/v1/production/work-orders/${id}/report`, payload)).data,
    onSuccess: () => {
      invalidate();
      setShowReport(false);
      setReportForm({ goodQuantity: 0, scrapQuantity: 0, operationNo: 1 });
    },
  });

  const completeMutation = useMutation({
    mutationFn: async () => (await api.post(`/api/v1/production/work-orders/${id}/complete`)).data,
    onSuccess: invalidate,
  });

  const closeMutation = useMutation({
    mutationFn: async () => (await api.post(`/api/v1/production/work-orders/${id}/close`)).data,
    onSuccess: invalidate,
  });

  const issueMaterialMutation = useMutation({
    mutationFn: async (payload: { itemCode: string; quantity: number }) =>
      (await api.post(`/api/v1/production/work-orders/${id}/issue-material`, payload)).data,
    onSuccess: () => {
      invalidate();
      setIssueForm(null);
    },
  });

  if (!data)
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-600" />
      </div>
    );

  const wo = data;
  const progressPct =
    wo.plannedQuantity > 0 ? Math.min(100, (wo.completedQuantity / wo.plannedQuantity) * 100) : 0;

  return (
    <div>
      <PageHeader
        title={`${t("wo.title")} — ${wo.documentNo}`}
        breadcrumbs={[
          { label: t("nav.production") },
          { label: t("nav.workOrders"), path: "/production/work-orders" },
          { label: wo.documentNo },
        ]}
        actions={
          <div className="flex items-center gap-2">
            <button className="btn-secondary" onClick={() => navigate("/production/work-orders")}>
              <ArrowLeft size={16} /> {t("common.back")}
            </button>
            {wo.status === "PLANNED" && (
              <button className="btn-primary" onClick={() => releaseMutation.mutate()} disabled={releaseMutation.isPending}>
                <Send size={16} /> {t("wo.release")}
              </button>
            )}
            {wo.status === "RELEASED" && (
              <button className="btn-primary" onClick={() => startMutation.mutate()} disabled={startMutation.isPending}>
                <Play size={16} /> {t("wo.start")}
              </button>
            )}
            {wo.status === "IN_PROGRESS" && (
              <>
                <button className="btn-primary" onClick={() => setShowReport(true)}>
                  <FileText size={16} /> {t("wo.reportOutput")}
                </button>
                <button className="btn-secondary" onClick={() => completeMutation.mutate()} disabled={completeMutation.isPending}>
                  <CheckCircle size={16} /> {t("wo.complete")}
                </button>
              </>
            )}
            {wo.status === "COMPLETED" && (
              <button className="btn-secondary" onClick={() => closeMutation.mutate()} disabled={closeMutation.isPending}>
                <Lock size={16} /> {t("wo.close")}
              </button>
            )}
          </div>
        }
      />

      {/* Summary cards */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4 mb-6">
        <div className="card p-5">
          <p className="text-xs font-medium text-slate-500 uppercase">{t("wo.product")}</p>
          <p className="mt-1 text-lg font-bold text-slate-900">{wo.productName}</p>
          <p className="text-sm text-slate-500 font-mono">{wo.productCode}</p>
        </div>
        <div className="card p-5">
          <p className="text-xs font-medium text-slate-500 uppercase">{t("wo.progress")}</p>
          <p className="mt-1 text-lg font-bold text-slate-900">
            {wo.completedQuantity} / {wo.plannedQuantity} {wo.unitOfMeasure}
          </p>
          <div className="mt-2 h-2 w-full rounded-full bg-slate-100 overflow-hidden">
            <div
              className="h-full rounded-full bg-brand-500 transition-all"
              style={{ width: `${progressPct}%` }}
            />
          </div>
        </div>
        <div className="card p-5">
          <p className="text-xs font-medium text-slate-500 uppercase">{t("wo.yield")}</p>
          <p className={`mt-1 text-lg font-bold ${wo.yieldRate >= 0.95 ? "text-emerald-600" : "text-red-600"}`}>
            {(wo.yieldRate * 100).toFixed(1)}%
          </p>
          <p className="text-sm text-slate-500">
            {t("wo.scrap")}: {wo.scrapQuantity}
          </p>
        </div>
        <div className="card p-5">
          <p className="text-xs font-medium text-slate-500 uppercase">{t("common.status")}</p>
          <span
            className={`mt-2 inline-block rounded-lg px-3 py-1.5 text-sm font-semibold ${statusColors[wo.status] || ""}`}
          >
            {String(t("status." + wo.status, wo.status))}
          </span>
          <p className="mt-1 text-xs text-slate-400">
            {t("wo.priority")}: {wo.priority}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Operations */}
        <div className="card">
          <div className="border-b border-slate-100 px-6 py-4 flex items-center gap-2">
            <Clock size={18} className="text-slate-400" />
            <h3 className="text-base font-semibold text-slate-900">{t("wo.operations")}</h3>
          </div>
          <div className="divide-y divide-slate-50">
            {(!wo.operations || wo.operations.length === 0) ? (
              <p className="px-6 py-8 text-sm text-slate-400 text-center">{t("common.noData")}</p>
            ) : (
              wo.operations.map((op: any) => {
                const opProgress =
                  wo.plannedQuantity > 0
                    ? Math.min(100, (op.completedQuantity / wo.plannedQuantity) * 100)
                    : 0;
                return (
                  <div key={op.operationNo} className="px-6 py-4">
                    <div className="flex items-center gap-4">
                      <div
                        className={`flex h-10 w-10 items-center justify-center rounded-lg text-sm font-bold ${statusColors[op.status] || "bg-slate-50"}`}
                      >
                        {op.operationNo}
                      </div>
                      <div className="flex-1">
                        <p className="text-sm font-medium text-slate-900">{op.operationName}</p>
                        <p className="text-xs text-slate-500">
                          WC: {op.workCenterCode} | Setup: {op.setupTime}h | Run: {op.runTimePerUnit}h/ea
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="text-sm font-semibold">
                          {op.completedQuantity}/{wo.plannedQuantity}
                        </p>
                        {op.scrapQuantity > 0 && (
                          <p className="text-xs text-red-500">
                            {op.scrapQuantity} {t("wo.scrap").toLowerCase()}
                          </p>
                        )}
                      </div>
                      <span className={`badge ${statusColors[op.status] || ""}`}>
                        {String(t("status." + op.status, op.status))}
                      </span>
                    </div>
                    <div className="mt-2 h-1.5 w-full rounded-full bg-slate-100 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-brand-400 transition-all"
                        style={{ width: `${opProgress}%` }}
                      />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Materials */}
        <div className="card">
          <div className="border-b border-slate-100 px-6 py-4 flex items-center gap-2">
            <Package size={18} className="text-slate-400" />
            <h3 className="text-base font-semibold text-slate-900">{t("wo.materials")}</h3>
          </div>
          <div className="divide-y divide-slate-50">
            {(!wo.materials || wo.materials.length === 0) ? (
              <p className="px-6 py-8 text-sm text-slate-400 text-center">{t("common.noData")}</p>
            ) : (
              wo.materials.map((mat: any) => {
                const matProgress =
                  mat.requiredQuantity > 0
                    ? Math.min(100, (mat.issuedQuantity / mat.requiredQuantity) * 100)
                    : 0;
                return (
                  <div key={mat.itemCode} className="px-6 py-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium text-slate-900">{mat.itemName}</p>
                        <p className="text-xs text-slate-500 font-mono">{mat.itemCode}</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <div className="text-right">
                          <p className="text-sm">
                            <span className="font-semibold">{mat.issuedQuantity}</span> /{" "}
                            {mat.requiredQuantity} {mat.unitOfMeasure}
                          </p>
                          {mat.shortageQuantity > 0 && (
                            <p className="text-xs text-red-600 font-medium">
                              {t("wo.shortage")}: {mat.shortageQuantity}
                            </p>
                          )}
                        </div>
                        <button
                          className="btn-secondary text-xs px-2 py-1"
                          onClick={() =>
                            setIssueForm({
                              itemCode: mat.itemCode,
                              quantity: mat.shortageQuantity > 0 ? mat.shortageQuantity : mat.requiredQuantity - mat.issuedQuantity,
                            })
                          }
                        >
                          {t("wo.issueMaterial")}
                        </button>
                      </div>
                    </div>
                    <div className="mt-2 h-1.5 w-full rounded-full bg-slate-100 overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all ${mat.shortageQuantity > 0 ? "bg-amber-400" : "bg-emerald-400"}`}
                        style={{ width: `${matProgress}%` }}
                      />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Report Output Dialog */}
      {showReport && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
              <h2 className="text-lg font-bold text-slate-900">{t("wo.reportOutput")}</h2>
              <button className="text-slate-400 hover:text-slate-600" onClick={() => setShowReport(false)}>
                <X size={20} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t("wo.operationNo")}</label>
                <input
                  className="input w-full"
                  type="number"
                  value={reportForm.operationNo}
                  onChange={(e) => setReportForm((f) => ({ ...f, operationNo: +e.target.value }))}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t("wo.goodQty")}</label>
                <input
                  className="input w-full"
                  type="number"
                  value={reportForm.goodQuantity}
                  onChange={(e) => setReportForm((f) => ({ ...f, goodQuantity: +e.target.value }))}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t("wo.scrapQty")}</label>
                <input
                  className="input w-full"
                  type="number"
                  value={reportForm.scrapQuantity}
                  onChange={(e) => setReportForm((f) => ({ ...f, scrapQuantity: +e.target.value }))}
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 border-t border-slate-100 px-6 py-4">
              <button className="btn-secondary" onClick={() => setShowReport(false)}>
                {t("common.cancel")}
              </button>
              <button
                className="btn-primary"
                onClick={() => reportMutation.mutate(reportForm)}
                disabled={reportMutation.isPending}
              >
                {reportMutation.isPending ? t("common.saving") : t("common.submit")}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Issue Material Dialog */}
      {issueForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm">
            <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
              <h2 className="text-lg font-bold text-slate-900">{t("wo.issueMaterial")}</h2>
              <button className="text-slate-400 hover:text-slate-600" onClick={() => setIssueForm(null)}>
                <X size={20} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t("item.code")}</label>
                <input className="input w-full bg-slate-50" value={issueForm.itemCode} readOnly />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t("wo.plannedQty")}</label>
                <input
                  className="input w-full"
                  type="number"
                  value={issueForm.quantity}
                  onChange={(e) => setIssueForm((f) => f ? { ...f, quantity: +e.target.value } : f)}
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 border-t border-slate-100 px-6 py-4">
              <button className="btn-secondary" onClick={() => setIssueForm(null)}>
                {t("common.cancel")}
              </button>
              <button
                className="btn-primary"
                onClick={() => issueMaterialMutation.mutate(issueForm)}
                disabled={issueMaterialMutation.isPending}
              >
                {issueMaterialMutation.isPending ? t("common.saving") : t("common.confirm")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
