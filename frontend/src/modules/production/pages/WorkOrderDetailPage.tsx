import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { ArrowLeft, Play, CheckCircle, Package, Clock } from "lucide-react";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

const statusColors: Record<string, string> = {
  PLANNED: "bg-slate-100 text-slate-700", RELEASED: "bg-blue-100 text-blue-700",
  IN_PROGRESS: "bg-amber-100 text-amber-700", COMPLETED: "bg-emerald-100 text-emerald-700",
  CLOSED: "bg-slate-100 text-slate-500", PENDING: "bg-slate-50 text-slate-500",
};

export default function WorkOrderDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const { data } = useQuery({
    queryKey: ["work-order", id],
    queryFn: async () => (await api.get(`/api/v1/production/work-orders/${id}`)).data.data,
    enabled: !!id,
  });

  if (!data) return <div className="flex items-center justify-center h-64"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-600" /></div>;

  const wo = data;
  const progressPct = wo.plannedQuantity > 0 ? Math.min(100, (wo.completedQuantity / wo.plannedQuantity) * 100) : 0;

  return (
    <div>
      <PageHeader
        title={`${t("wo.title")} — ${wo.documentNo}`}
        breadcrumbs={[{ label: t("nav.production") }, { label: t("nav.workOrders"), path: "/production/work-orders" }, { label: wo.documentNo }]}
        actions={
          <>
            <button className="btn-secondary" onClick={() => navigate("/production/work-orders")}><ArrowLeft size={16} /> {t("common.back")}</button>
            {wo.status === "RELEASED" && <button className="btn-primary"><Play size={16} /> {t("wo.startProduction")}</button>}
            {wo.status === "IN_PROGRESS" && <button className="btn-primary"><CheckCircle size={16} /> {t("wo.reportOutput")}</button>}
          </>
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
          <p className="mt-1 text-lg font-bold text-slate-900">{wo.completedQuantity} / {wo.plannedQuantity} {wo.unitOfMeasure}</p>
          <div className="mt-2 h-2 w-full rounded-full bg-slate-100 overflow-hidden">
            <div className="h-full rounded-full bg-brand-500 transition-all" style={{ width: `${progressPct}%` }} />
          </div>
        </div>
        <div className="card p-5">
          <p className="text-xs font-medium text-slate-500 uppercase">{t("wo.yield")}</p>
          <p className={`mt-1 text-lg font-bold ${wo.yieldRate >= 0.95 ? "text-emerald-600" : "text-red-600"}`}>
            {(wo.yieldRate * 100).toFixed(1)}%
          </p>
          <p className="text-sm text-slate-500">{t("wo.scrap")}: {wo.scrapQuantity}</p>
        </div>
        <div className="card p-5">
          <p className="text-xs font-medium text-slate-500 uppercase">{t("common.status")}</p>
          <span className={`mt-2 inline-block rounded-lg px-3 py-1.5 text-sm font-semibold ${statusColors[wo.status] || ""}`}>
            {String(t("status." + wo.status, wo.status))}
          </span>
          <p className="mt-1 text-xs text-slate-400">{t("wo.priority")}: {wo.priority}</p>
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
            {wo.operations.length === 0 ? (
              <p className="px-6 py-8 text-sm text-slate-400 text-center">{t("common.noData")}</p>
            ) : wo.operations.map((op: any) => (
              <div key={op.operationNo} className="px-6 py-4 flex items-center gap-4">
                <div className={`flex h-10 w-10 items-center justify-center rounded-lg text-sm font-bold ${statusColors[op.status] || "bg-slate-50"}`}>
                  {op.operationNo}
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-slate-900">{op.operationName}</p>
                  <p className="text-xs text-slate-500">WC: {op.workCenterCode} | Setup: {op.setupTime}h | Run: {op.runTimePerUnit}h/ea</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-semibold">{op.completedQuantity} done</p>
                  {op.scrapQuantity > 0 && <p className="text-xs text-red-500">{op.scrapQuantity} {t("wo.scrap").toLowerCase()}</p>}
                </div>
                <span className={`badge ${statusColors[op.status] || ""}`}>{String(t("status." + op.status, op.status))}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Materials */}
        <div className="card">
          <div className="border-b border-slate-100 px-6 py-4 flex items-center gap-2">
            <Package size={18} className="text-slate-400" />
            <h3 className="text-base font-semibold text-slate-900">{t("wo.materials")}</h3>
          </div>
          <div className="divide-y divide-slate-50">
            {wo.materials.length === 0 ? (
              <p className="px-6 py-8 text-sm text-slate-400 text-center">{t("common.noData")}</p>
            ) : wo.materials.map((mat: any) => (
              <div key={mat.itemCode} className="px-6 py-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-slate-900">{mat.itemName}</p>
                    <p className="text-xs text-slate-500 font-mono">{mat.itemCode}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm"><span className="font-semibold">{mat.issuedQuantity}</span> / {mat.requiredQuantity} {mat.unitOfMeasure}</p>
                    {mat.shortageQuantity > 0 && (
                      <p className="text-xs text-red-600 font-medium">{t("wo.shortage")}: {mat.shortageQuantity}</p>
                    )}
                  </div>
                </div>
                <div className="mt-2 h-1.5 w-full rounded-full bg-slate-100 overflow-hidden">
                  <div className={`h-full rounded-full transition-all ${mat.shortageQuantity > 0 ? "bg-amber-400" : "bg-emerald-400"}`}
                    style={{ width: `${Math.min(100, (mat.issuedQuantity / mat.requiredQuantity) * 100)}%` }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
