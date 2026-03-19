import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { workflowApi, type Workflow, type WorkflowStep } from '../../../shared/api/adminPhase4Api';
import PageHeader from '../../../shared/components/PageHeader';
import { Plus, Play, ChevronRight } from 'lucide-react';

const STEP_TYPE_LABELS: Record<string, string> = {
  APPROVAL: '결재', NOTIFICATION: '통보', CONDITION: '조건', PARALLEL: '병렬결재',
};

const APPROVER_TYPE_LABELS: Record<string, string> = {
  SPECIFIC_USER: '지정 사용자', ROLE: '역할', DEPARTMENT_HEAD: '부서장',
  MANAGER_LEVEL: '상위 관리자', REQUESTER_MANAGER: '요청자 직속상관',
};

const STEP_COLORS: Record<string, string> = {
  APPROVAL: 'bg-blue-50 border-blue-200 text-blue-700',
  NOTIFICATION: 'bg-amber-50 border-amber-200 text-amber-700',
  CONDITION: 'bg-purple-50 border-purple-200 text-purple-700',
  PARALLEL: 'bg-green-50 border-green-200 text-green-700',
};

function StepCard({ step }: { step: WorkflowStep }) {
  return (
    <div className={`rounded-xl border-2 p-4 ${STEP_COLORS[step.stepType] ?? 'bg-slate-50 border-slate-200'}`}>
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-bold uppercase tracking-wider opacity-60">
          Step {step.stepOrder}
        </span>
        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-white/60">
          {STEP_TYPE_LABELS[step.stepType]}
        </span>
      </div>
      <div className="font-semibold text-sm mb-1">{step.name}</div>
      <div className="text-xs opacity-70">
        {APPROVER_TYPE_LABELS[step.approverType]}: {step.approverValue}
      </div>
      {step.condition && (
        <div className="text-xs mt-1 opacity-50">조건: {step.condition}</div>
      )}
    </div>
  );
}

export default function WorkflowDesignerPage() {
  const { t } = useTranslation();
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [selected, setSelected] = useState<Workflow | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    workflowApi.getAll().then(setWorkflows).finally(() => setLoading(false));
  }, []);

  const handleActivate = async (id: number) => {
    await workflowApi.activate(id);
    const updated = await workflowApi.getAll();
    setWorkflows(updated);
    if (selected?.id === id) setSelected(updated.find(w => w.id === id) ?? null);
  };

  return (
    <div>
      <PageHeader title={t('admin.workflows', '워크플로우 디자이너')} actions={
        <button className="btn-primary flex items-center gap-2">
          <Plus size={16} /> {t('common.create', '생성')}
        </button>
      } />

      <div className="grid grid-cols-3 gap-6">
        {/* Workflow List */}
        <div className="col-span-1 space-y-2">
          <h4 className="text-sm font-medium text-slate-500 mb-3">{t('admin.workflowList', '워크플로우 목록')}</h4>
          {loading ? (
            <div className="text-sm text-slate-400 text-center py-4">Loading...</div>
          ) : workflows.length === 0 ? (
            <div className="text-sm text-slate-400 text-center py-8">등록된 워크플로우가 없습니다</div>
          ) : (
            workflows.map((wf) => (
              <button
                key={wf.id}
                onClick={() => setSelected(wf)}
                className={`w-full text-left rounded-xl border p-3 transition ${
                  selected?.id === wf.id
                    ? 'border-brand-300 bg-brand-50'
                    : 'border-slate-200 bg-white hover:border-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-slate-700">{wf.name}</span>
                  {wf.isCurrent && (
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-green-100 text-green-700">ACTIVE</span>
                  )}
                </div>
                <div className="text-xs text-slate-400 mt-1">
                  {wf.documentType} · v{wf.version} · {wf.steps.length} steps
                </div>
              </button>
            ))
          )}
        </div>

        {/* Flow Visualization */}
        <div className="col-span-2">
          {selected ? (
            <div>
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-lg font-semibold text-slate-800">{selected.name}</h3>
                  <p className="text-sm text-slate-400">{selected.documentType} · v{selected.version}</p>
                </div>
                <div className="flex gap-2">
                  {!selected.isCurrent && (
                    <button onClick={() => handleActivate(selected.id)} className="btn-primary flex items-center gap-1 text-sm">
                      <Play size={14} /> 활성화
                    </button>
                  )}
                </div>
              </div>

              {/* Step Flow */}
              <div className="space-y-3">
                {selected.steps.map((step, idx) => (
                  <div key={step.id}>
                    <StepCard step={step} />
                    {idx < selected.steps.length - 1 && (
                      <div className="flex justify-center py-1">
                        <ChevronRight size={20} className="text-slate-300 rotate-90" />
                      </div>
                    )}
                  </div>
                ))}
                {selected.steps.length === 0 && (
                  <div className="text-sm text-slate-400 text-center py-12 border-2 border-dashed border-slate-200 rounded-xl">
                    단계를 추가하세요
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="text-sm text-slate-400 text-center py-20">
              좌측에서 워크플로우를 선택하세요
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
