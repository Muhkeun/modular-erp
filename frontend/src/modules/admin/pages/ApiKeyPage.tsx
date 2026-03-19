import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { apiKeyApi, type ApiKeyInfo, type ApiKeyCreateResult } from '../../../shared/api/adminPhase4Api';
import PageHeader from '../../../shared/components/PageHeader';
import DataGrid from '../../../shared/components/DataGrid';
import type { ColDef } from 'ag-grid-community';
import { Plus, Trash2, Copy } from 'lucide-react';

export default function ApiKeyPage() {
  const { t } = useTranslation();
  const [keys, setKeys] = useState<ApiKeyInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newKey, setNewKey] = useState<ApiKeyCreateResult | null>(null);
  const [formName, setFormName] = useState('');
  const [formDesc, setFormDesc] = useState('');

  const load = () => {
    setLoading(true);
    apiKeyApi.getAll().then(setKeys).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const columnDefs: ColDef<ApiKeyInfo>[] = [
    { field: 'name', headerName: t('common.name', '명칭'), flex: 1 },
    {
      field: 'keyPrefix', headerName: 'Key', width: 140,
      valueFormatter: (p: any) => `${p.value}...`,
    },
    {
      field: 'status', headerName: t('common.status', '상태'), width: 100,
      cellRenderer: (p: any) => {
        const colors: Record<string, string> = {
          ACTIVE: 'bg-green-100 text-green-700',
          REVOKED: 'bg-red-100 text-red-700',
          EXPIRED: 'bg-slate-100 text-slate-500',
        };
        return <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${colors[p.value] ?? ''}`}>{p.value}</span>;
      },
    },
    { field: 'rateLimit', headerName: 'Rate Limit', width: 110, valueFormatter: (p: any) => p.value ? `${p.value}/min` : 'Unlimited' },
    {
      field: 'lastUsedAt', headerName: 'Last Used', width: 160,
      valueFormatter: (p: any) => p.value ? new Date(p.value).toLocaleString() : 'Never',
    },
    {
      field: 'createdAt', headerName: 'Created', width: 160,
      valueFormatter: (p: any) => p.value ? new Date(p.value).toLocaleString() : '',
    },
    {
      headerName: '', width: 80, sortable: false, filter: false,
      cellRenderer: (p: any) => {
        if (p.data?.status !== 'ACTIVE') return null;
        return (
          <button onClick={() => handleRevoke(p.data.id)} className="p-1 text-slate-400 hover:text-red-500" title="Revoke">
            <Trash2 size={14} />
          </button>
        );
      },
    },
  ];

  const handleCreate = async () => {
    const result = await apiKeyApi.create({ name: formName, description: formDesc });
    setNewKey(result);
    setShowCreate(false);
    setFormName('');
    setFormDesc('');
    load();
  };

  const handleRevoke = async (id: number) => {
    if (!confirm('이 API 키를 폐기하시겠습니까?')) return;
    await apiKeyApi.revoke(id);
    load();
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  return (
    <div>
      <PageHeader title={t('admin.apiKeys', 'API Key 관리')} actions={
        <button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> {t('common.create', '생성')}
        </button>
      } />

      {/* New Key Display */}
      {newKey && (
        <div className="mb-4 rounded-xl border-2 border-green-200 bg-green-50 p-4">
          <div className="text-sm font-medium text-green-800 mb-2">
            API 키가 생성되었습니다. 이 키는 다시 표시되지 않습니다.
          </div>
          <div className="flex items-center gap-2 bg-white rounded-lg px-3 py-2 font-mono text-sm">
            <span className="flex-1 break-all">{newKey.rawKey}</span>
            <button onClick={() => copyToClipboard(newKey.rawKey)} className="text-slate-400 hover:text-brand-600">
              <Copy size={16} />
            </button>
          </div>
          <button onClick={() => setNewKey(null)} className="mt-2 text-xs text-green-600 hover:text-green-800">
            닫기
          </button>
        </div>
      )}

      <DataGrid rowData={keys} columnDefs={columnDefs} loading={loading} height="calc(100vh - 380px)" />

      {/* Create Modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold mb-4">API Key 생성</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">이름</label>
                <input value={formName} onChange={(e) => setFormName(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" placeholder="예: 외부 ERP 연동" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">설명</label>
                <textarea value={formDesc} onChange={(e) => setFormDesc(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" rows={2} />
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button onClick={() => setShowCreate(false)} className="btn-ghost">취소</button>
              <button onClick={handleCreate} disabled={!formName} className="btn-primary">생성</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
