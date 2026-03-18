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
      <div className="card p-6 mb-6">
        <h3 className="text-base font-semibold text-slate-900 mb-4">{t("mrp.runMrp")}</h3>
        <div className="flex items-end gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              {t("mrp.plantCode")}
            </label>
            <input
              className="input w-32"
              value={plantCode}
              onChange={(e) => setPlantCode(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
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
        <div className="card mb-6">
          <div className="border-b border-slate-100 px-6 py-4">
            <h3 className="text-base font-semibold text-slate-900">{t("mrp.latestResults")}</h3>
          </div>
          <div className="divide-y divide-slate-50">
            {recentRuns.data.map((run: any) => (
              <div key={run.id} className="px-6 py-3 flex items-center justify-between text-sm">
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
        <div className="card">
          <div className="border-b border-slate-100 px-6 py-4 flex items-center justify-between">
            <div>
              <h3 className="text-base font-semibold text-slate-900">
                Run #{latestRun.id} {t("mrp.latestResults")}
              </h3>
              <p className="text-sm text-slate-500">
                {t("gr.plant")}: {latestRun.plantCode} | {latestRun.executedAt || "--"}
              </p>
            </div>
            <span className="badge-info">{String(t("status." + latestRun.status, latestRun.status))}</span>
          </div>

          {!latestRun.results || latestRun.results.length === 0 ? (
            <p className="px-6 py-12 text-center text-slate-400">{t("mrp.noShortage")}</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50/50">
                    <th className="px-6 py-3 text-left font-semibold text-slate-600">
                      {t("item.name")}
                    </th>
                    <th className="px-4 py-3 text-right font-semibold text-slate-600">
                      {t("mrp.grossReq")}
                    </th>
                    <th className="px-4 py-3 text-right font-semibold text-slate-600">
                      {t("mrp.onHand")}
                    </th>
                    <th className="px-4 py-3 text-right font-semibold text-slate-600">
                      {t("mrp.netReq")}
                    </th>
                    <th className="px-4 py-3 text-right font-semibold text-slate-600">
                      {t("mrp.plannedQty")}
                    </th>
                    <th className="px-4 py-3 text-center font-semibold text-slate-600">
                      {t("mrp.action")}
                    </th>
                    <th className="px-4 py-3 text-left font-semibold text-slate-600">
                      {t("mrp.requiredBy")}
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {latestRun.results.map((r: any) => (
                    <tr key={r.id} className="hover:bg-slate-50">
                      <td className="px-6 py-3">
                        <p className="font-medium text-slate-900">{r.itemName}</p>
                        <p className="text-xs text-slate-500 font-mono">{r.itemCode}</p>
                      </td>
                      <td className="px-4 py-3 text-right">{r.grossRequirement}</td>
                      <td className="px-4 py-3 text-right">{r.onHandStock}</td>
                      <td className="px-4 py-3 text-right">
                        {r.netRequirement > 0 ? (
                          <span className="text-red-600 font-semibold">{r.netRequirement}</span>
                        ) : (
                          <span className="text-emerald-600">0</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-right font-semibold">
                        {r.plannedOrderQty > 0 ? r.plannedOrderQty : "--"}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className="inline-flex items-center gap-1.5">
                          {actionIcon[r.actionType]}
                          <span className="text-xs">
                            {String(t("mrp.actionTypes." + r.actionType, r.actionType))}
                          </span>
                        </span>
                      </td>
                      <td className="px-4 py-3">{r.requiredDate || "--"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
