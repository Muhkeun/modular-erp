import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { Play, ShoppingCart, Factory } from "lucide-react";
import PageHeader from "../../../shared/components/PageHeader";
import api from "../../../shared/api/client";

const actionIcon: Record<string, React.ReactNode> = {
  PURCHASE: <ShoppingCart size={16} className="text-blue-600" />,
  PRODUCE: <Factory size={16} className="text-amber-600" />,
  NONE: <span className="text-slate-300">--</span>,
};

export default function MrpPage() {
  const queryClient = useQueryClient();
  const { t } = useTranslation();
  const [plantCode, setPlantCode] = useState("P001");
  const [horizon, setHorizon] = useState(30);

  const { data: recentRuns } = useQuery({
    queryKey: ["mrp-runs"],
    queryFn: async () => (await api.get("/api/v1/planning/mrp?size=5")).data,
  });

  const runMrp = useMutation({
    mutationFn: async () =>
      (await api.post("/api/v1/planning/mrp/run", { plantCode, planningHorizonDays: horizon })).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["mrp-runs"] }),
  });

  const latestRun = recentRuns?.data?.[0];

  return (
    <div>
      <PageHeader
        title={t("mrp.title")}
        description={t("mrp.description")}
        breadcrumbs={[{ label: t("nav.planning") }, { label: t("nav.mrp") }]}
      />

      {/* Run MRP */}
      <div className="workspace-hero mb-6">
        <p className="section-kicker">Planning Workspace</p>
        <h3 className="section-title">{t("mrp.runMrp")}</h3>
        <div className="flex items-end gap-4 mt-4">
          <div>
            <label className="field-label">
              {t("mrp.plantCode")}
            </label>
            <input
              className="input w-32"
              value={plantCode}
              onChange={(e) => setPlantCode(e.target.value)}
            />
          </div>
          <div>
            <label className="field-label">
              {t("mrp.horizon")}
            </label>
            <input
              className="input w-24"
              type="number"
              value={horizon}
              onChange={(e) => setHorizon(+e.target.value)}
            />
          </div>
          <button
            className="btn-primary"
            onClick={() => runMrp.mutate()}
            disabled={runMrp.isPending}
          >
            <Play size={16} /> {runMrp.isPending ? t("mrp.running") : t("mrp.execute")}
          </button>
        </div>
        {runMrp.isError && (
          <p className="mt-3 text-sm text-red-600">
            MRP execution failed. Please try again.
          </p>
        )}
      </div>

      {/* Recent Runs List */}
      {recentRuns?.data && recentRuns.data.length > 0 && (
        <div className="section-card mb-6">
          <p className="section-kicker">History</p>
          <h3 className="section-title">{t("mrp.latestResults")}</h3>
          <div className="divide-y divide-slate-50 mt-4">
            {recentRuns.data.map((run: any) => (
              <div key={run.id} className="py-3 flex items-center justify-between text-sm">
                <div>
                  <span className="font-mono font-semibold text-brand-700">#{run.id}</span>
                  <span className="ml-3 text-slate-500">
                    {t("gr.plant")}: {run.plantCode}
                  </span>
                  <span className="ml-3 text-slate-400">{run.executedAt || "--"}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-slate-500">
                    {run.results?.length || 0} items
                  </span>
                  <span className="badge-info text-xs">
                    {String(t("status." + run.status, run.status))}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Latest Run Results Table */}
      {latestRun && (
        <div className="section-card">
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="section-kicker">Run #{latestRun.id}</p>
              <h3 className="section-title">
                {t("mrp.latestResults")}
              </h3>
              <p className="text-sm text-slate-500 mt-1">
                {t("gr.plant")}: {latestRun.plantCode} | {latestRun.executedAt || "--"}
              </p>
            </div>
            <span className="badge-info">{String(t("status." + latestRun.status, latestRun.status))}</span>
          </div>

          {!latestRun.results || latestRun.results.length === 0 ? (
            <p className="py-12 text-center text-slate-400">{t("mrp.noShortage")}</p>
          ) : (
            <div className="grid-table mt-4" style={{ gridTemplateColumns: "2fr 1fr 1fr 1fr 1fr 1fr 1fr" }}>
              <div className="grid-table-row font-semibold text-slate-600 bg-slate-50/50">
                <span>{t("item.name")}</span>
                <span className="text-right">{t("mrp.grossReq")}</span>
                <span className="text-right">{t("mrp.onHand")}</span>
                <span className="text-right">{t("mrp.netReq")}</span>
                <span className="text-right">{t("mrp.plannedQty")}</span>
                <span className="text-center">{t("mrp.action")}</span>
                <span>{t("mrp.requiredBy")}</span>
              </div>
              {latestRun.results.map((r: any) => (
                <div key={r.id} className="grid-table-row">
                  <span>
                    <p className="font-medium text-slate-900">{r.itemName}</p>
                    <p className="text-xs text-slate-500 font-mono">{r.itemCode}</p>
                  </span>
                  <span className="text-right">{r.grossRequirement}</span>
                  <span className="text-right">{r.onHandStock}</span>
                  <span className="text-right">
                    {r.netRequirement > 0 ? (
                      <span className="text-red-600 font-semibold">{r.netRequirement}</span>
                    ) : (
                      <span className="text-emerald-600">0</span>
                    )}
                  </span>
                  <span className="text-right font-semibold">
                    {r.plannedOrderQty > 0 ? r.plannedOrderQty : "--"}
                  </span>
                  <span className="text-center">
                    <span className="inline-flex items-center gap-1.5">
                      {actionIcon[r.actionType]}
                      <span className="text-xs">
                        {String(t("mrp.actionTypes." + r.actionType, r.actionType))}
                      </span>
                    </span>
                  </span>
                  <span>{r.requiredDate || "--"}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
