import { useState, useEffect, useCallback } from "react";
import { useTranslation } from "react-i18next";
import PageHeader from "../../../shared/components/PageHeader";
import { approvalApi, type ApprovalRequest, type Delegation } from "../../../shared/api/approvalApi";
import { Check, X, RotateCcw, Clock, FileText, ChevronDown, ChevronRight, Plus, Trash2, MessageSquare } from "lucide-react";
import { clsx } from "clsx";

type Tab = "pending" | "submitted" | "delegations";

const STATUS_COLORS: Record<string, string> = {
  PENDING: "bg-amber-100 text-amber-700",
  APPROVED: "bg-emerald-100 text-emerald-700",
  REJECTED: "bg-red-100 text-red-700",
  RETURNED: "bg-orange-100 text-orange-700",
  CANCELLED: "bg-slate-100 text-slate-500",
  DRAFT: "bg-slate-100 text-slate-500",
};

const DOC_TYPE_LABELS: Record<string, string> = {
  PR: "Purchase Request",
  PO: "Purchase Order",
  SO: "Sales Order",
  WO: "Work Order",
  JE: "Journal Entry",
  BUDGET: "Budget",
  ASSET: "Asset",
};

export default function ApprovalInboxPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>("pending");
  const [pending, setPending] = useState<ApprovalRequest[]>([]);
  const [submitted, setSubmitted] = useState<ApprovalRequest[]>([]);
  const [delegations, setDelegations] = useState<Delegation[]>([]);
  const [loading, setLoading] = useState(false);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [commentText, setCommentText] = useState("");
  const [actionComment, setActionComment] = useState("");
  const [showDelegationForm, setShowDelegationForm] = useState(false);
  const [delegationForm, setDelegationForm] = useState({ toUserId: "", startDate: "", endDate: "", reason: "" });

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      if (tab === "pending") setPending(await approvalApi.getMyPending());
      else if (tab === "submitted") setSubmitted(await approvalApi.getMySubmitted());
      else setDelegations(await approvalApi.getDelegations());
    } catch { /* ignore */ }
    setLoading(false);
  }, [tab]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleAction = async (req: ApprovalRequest, action: "approve" | "reject" | "return") => {
    const activeStep = req.steps.find(s => s.stepStatus === "ACTIVE" && !s.decision);
    if (!activeStep) return;
    try {
      const fn = action === "approve" ? approvalApi.approveStep
        : action === "reject" ? approvalApi.rejectStep
        : approvalApi.returnStep;
      await fn(req.id, activeStep.id, actionComment || undefined);
      setActionComment("");
      loadData();
    } catch { /* ignore */ }
  };

  const handleAddComment = async (reqId: number) => {
    if (!commentText.trim()) return;
    try {
      await approvalApi.addComment(reqId, commentText);
      setCommentText("");
      loadData();
    } catch { /* ignore */ }
  };

  const handleCreateDelegation = async () => {
    if (!delegationForm.toUserId || !delegationForm.startDate || !delegationForm.endDate) return;
    try {
      await approvalApi.createDelegation(delegationForm);
      setShowDelegationForm(false);
      setDelegationForm({ toUserId: "", startDate: "", endDate: "", reason: "" });
      loadData();
    } catch { /* ignore */ }
  };

  const handleDeleteDelegation = async (id: number) => {
    try {
      await approvalApi.deleteDelegation(id);
      loadData();
    } catch { /* ignore */ }
  };

  const tabs: { key: Tab; label: string; count?: number }[] = [
    { key: "pending", label: t("approval.pending"), count: pending.length },
    { key: "submitted", label: t("approval.submitted") },
    { key: "delegations", label: t("approval.delegations") },
  ];

  const renderRequestRow = (req: ApprovalRequest, showActions: boolean) => {
    const expanded = expandedId === req.id;
    const activeStep = req.steps.find(s => s.stepStatus === "ACTIVE" && !s.decision);
    return (
      <div key={req.id} className="rounded-2xl border border-slate-200/80 bg-white/90 backdrop-blur-sm overflow-hidden">
        <div
          className="flex items-center gap-4 px-5 py-4 cursor-pointer hover:bg-slate-50/50 transition-colors"
          onClick={() => setExpandedId(expanded ? null : req.id)}
        >
          <span className="text-slate-400">
            {expanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
          </span>
          <FileText size={18} className="text-slate-400 shrink-0" />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <span className="font-semibold text-sm text-slate-900">{req.documentNo || `#${req.documentId}`}</span>
              <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                {DOC_TYPE_LABELS[req.documentType] || req.documentType}
              </span>
            </div>
            <div className="text-xs text-slate-400 mt-0.5">
              {t("approval.requestedBy")}: {req.requestedBy} | {req.createdAt ? new Date(req.createdAt).toLocaleDateString() : ""}
            </div>
          </div>
          <span className={clsx("rounded-full px-3 py-1 text-xs font-semibold", STATUS_COLORS[req.status] || "bg-slate-100 text-slate-500")}>
            {t(`status.${req.status}`)}
          </span>
          {showActions && activeStep && (
            <div className="flex gap-1.5" onClick={e => e.stopPropagation()}>
              <button onClick={() => handleAction(req, "approve")} className="btn-sm bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl px-3 py-1.5 text-xs font-medium flex items-center gap-1">
                <Check size={14} /> {t("common.approve")}
              </button>
              <button onClick={() => handleAction(req, "reject")} className="btn-sm bg-red-500 hover:bg-red-600 text-white rounded-xl px-3 py-1.5 text-xs font-medium flex items-center gap-1">
                <X size={14} /> {t("common.reject")}
              </button>
              <button onClick={() => handleAction(req, "return")} className="btn-sm bg-orange-500 hover:bg-orange-600 text-white rounded-xl px-3 py-1.5 text-xs font-medium flex items-center gap-1">
                <RotateCcw size={14} /> {t("approval.return")}
              </button>
            </div>
          )}
        </div>

        {expanded && (
          <div className="border-t border-slate-100 px-5 py-4 space-y-4">
            {/* Action comment input */}
            {showActions && activeStep && (
              <div className="flex gap-2">
                <input
                  value={actionComment}
                  onChange={e => setActionComment(e.target.value)}
                  placeholder={t("approval.actionComment")}
                  className="flex-1 rounded-xl border border-slate-200 px-3 py-2 text-sm"
                />
              </div>
            )}

            {/* Approval timeline */}
            <div>
              <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-3">{t("approval.timeline")}</h4>
              <div className="space-y-2">
                {req.steps.map(step => (
                  <div key={step.id} className="flex items-center gap-3">
                    <div className={clsx(
                      "h-8 w-8 rounded-full flex items-center justify-center text-xs font-bold shrink-0",
                      step.decision === "APPROVED" ? "bg-emerald-100 text-emerald-700" :
                      step.decision === "REJECTED" ? "bg-red-100 text-red-700" :
                      step.decision === "RETURNED" ? "bg-orange-100 text-orange-700" :
                      step.stepStatus === "ACTIVE" ? "bg-blue-100 text-blue-700 ring-2 ring-blue-300" :
                      "bg-slate-100 text-slate-400"
                    )}>
                      {step.stepOrder}
                    </div>
                    <div className="flex-1">
                      <div className="text-sm font-medium text-slate-700">{step.approverRole}</div>
                      {step.decidedBy && (
                        <div className="text-xs text-slate-400">
                          {step.decidedBy} - {step.decidedAt ? new Date(step.decidedAt).toLocaleString() : ""}
                        </div>
                      )}
                      {step.comment && <div className="text-xs text-slate-500 mt-0.5 italic">"{step.comment}"</div>}
                    </div>
                    <span className={clsx("text-xs font-medium",
                      step.decision === "APPROVED" ? "text-emerald-600" :
                      step.decision === "REJECTED" ? "text-red-600" :
                      step.decision === "RETURNED" ? "text-orange-600" :
                      step.stepStatus === "ACTIVE" ? "text-blue-600" : "text-slate-400"
                    )}>
                      {step.decision ? t(`status.${step.decision}`) : step.stepStatus === "ACTIVE" ? t("approval.inReview") : t("approval.waiting")}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* Comments */}
            {req.comments.length > 0 && (
              <div>
                <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">{t("approval.comments")}</h4>
                <div className="space-y-2">
                  {req.comments.map(c => (
                    <div key={c.id} className="flex gap-2 text-sm">
                      <span className="font-medium text-slate-700">{c.commentBy}:</span>
                      <span className="text-slate-600">{c.comment}</span>
                      <span className="text-xs text-slate-400 ml-auto">{new Date(c.commentAt).toLocaleString()}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Add comment */}
            <div className="flex gap-2">
              <input
                value={commentText}
                onChange={e => setCommentText(e.target.value)}
                placeholder={t("approval.addComment")}
                className="flex-1 rounded-xl border border-slate-200 px-3 py-2 text-sm"
              />
              <button
                onClick={() => handleAddComment(req.id)}
                className="rounded-xl bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-700 flex items-center gap-1"
              >
                <MessageSquare size={14} /> {t("common.submit")}
              </button>
            </div>
          </div>
        )}
      </div>
    );
  };

  return (
    <div>
      <PageHeader title={t("approval.title")} description={t("approval.description")} />

      {/* Tabs */}
      <div className="flex gap-1 mb-6 rounded-2xl bg-slate-100/80 p-1 w-fit">
        {tabs.map(tb => (
          <button
            key={tb.key}
            onClick={() => setTab(tb.key)}
            className={clsx(
              "rounded-xl px-4 py-2 text-sm font-medium transition-colors",
              tab === tb.key ? "bg-white text-brand-700 shadow-sm" : "text-slate-500 hover:text-slate-700"
            )}
          >
            {tb.label}
            {tb.count !== undefined && tb.count > 0 && (
              <span className="ml-1.5 rounded-full bg-red-500 px-1.5 py-0.5 text-[10px] font-bold text-white">{tb.count}</span>
            )}
          </button>
        ))}
      </div>

      {loading && <div className="text-center py-8 text-slate-400">{t("common.loading")}</div>}

      {/* Pending Tab */}
      {tab === "pending" && !loading && (
        <div className="space-y-3">
          {pending.length === 0 ? (
            <div className="text-center py-12 text-slate-400">
              <Clock size={40} className="mx-auto mb-3 text-slate-300" />
              <p>{t("approval.noPending")}</p>
            </div>
          ) : (
            pending.map(req => renderRequestRow(req, true))
          )}
        </div>
      )}

      {/* Submitted Tab */}
      {tab === "submitted" && !loading && (
        <div className="space-y-3">
          {submitted.length === 0 ? (
            <div className="text-center py-12 text-slate-400">{t("common.noData")}</div>
          ) : (
            submitted.map(req => renderRequestRow(req, false))
          )}
        </div>
      )}

      {/* Delegations Tab */}
      {tab === "delegations" && !loading && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button
              onClick={() => setShowDelegationForm(!showDelegationForm)}
              className="flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700"
            >
              <Plus size={16} /> {t("approval.newDelegation")}
            </button>
          </div>

          {showDelegationForm && (
            <div className="rounded-2xl border border-slate-200/80 bg-white/90 p-5 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1">{t("approval.delegateTo")}</label>
                  <input value={delegationForm.toUserId} onChange={e => setDelegationForm(f => ({...f, toUserId: e.target.value}))}
                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" placeholder="user-id" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1">{t("approval.reason")}</label>
                  <input value={delegationForm.reason} onChange={e => setDelegationForm(f => ({...f, reason: e.target.value}))}
                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1">{t("approval.startDate")}</label>
                  <input type="date" value={delegationForm.startDate} onChange={e => setDelegationForm(f => ({...f, startDate: e.target.value}))}
                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1">{t("approval.endDate")}</label>
                  <input type="date" value={delegationForm.endDate} onChange={e => setDelegationForm(f => ({...f, endDate: e.target.value}))}
                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" />
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setShowDelegationForm(false)} className="rounded-xl border border-slate-200 px-4 py-2 text-sm">{t("common.cancel")}</button>
                <button onClick={handleCreateDelegation} className="rounded-xl bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700">{t("common.save")}</button>
              </div>
            </div>
          )}

          <div className="space-y-2">
            {delegations.length === 0 ? (
              <div className="text-center py-12 text-slate-400">{t("common.noData")}</div>
            ) : (
              delegations.map(d => (
                <div key={d.id} className="flex items-center gap-4 rounded-2xl border border-slate-200/80 bg-white/90 px-5 py-4">
                  <div className="flex-1">
                    <div className="text-sm font-medium text-slate-900">
                      {d.fromUserId} → {d.toUserId}
                    </div>
                    <div className="text-xs text-slate-400">
                      {d.startDate} ~ {d.endDate}
                      {d.reason && <span className="ml-2">({d.reason})</span>}
                    </div>
                  </div>
                  <span className={clsx("rounded-full px-3 py-1 text-xs font-semibold",
                    d.isActive ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500"
                  )}>
                    {d.isActive ? t("common.active") : t("common.inactive")}
                  </span>
                  {d.isActive && (
                    <button onClick={() => handleDeleteDelegation(d.id)} className="text-slate-400 hover:text-red-500">
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
