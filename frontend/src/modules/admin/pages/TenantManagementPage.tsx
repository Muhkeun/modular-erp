import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { tenantApi, type Tenant } from '../../../shared/api/adminPhase4Api';
import PageHeader from '../../../shared/components/PageHeader';
import DataGrid from '../../../shared/components/DataGrid';
import type { ColDef } from 'ag-grid-community';
import { Plus, Pause, Play } from 'lucide-react';

const PLAN_COLORS: Record<string, string> = {
  FREE: 'bg-slate-100 text-slate-600',
  STARTER: 'bg-blue-100 text-blue-700',
  PROFESSIONAL: 'bg-purple-100 text-purple-700',
  ENTERPRISE: 'bg-amber-100 text-amber-700',
};

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-700',
  SUSPENDED: 'bg-red-100 text-red-700',
  DEACTIVATED: 'bg-slate-100 text-slate-500',
};

export default function TenantManagementPage() {
  const { t } = useTranslation();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    tenantApi.getAll().then(setTenants).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const columnDefs: ColDef<Tenant>[] = [
    { field: 'tenantId', headerName: 'Tenant ID', width: 130 },
    { field: 'name', headerName: t('common.name', '명칭'), flex: 1 },
    {
      field: 'plan', headerName: 'Plan', width: 120,
      cellRenderer: (p: any) => (
        <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${PLAN_COLORS[p.value] ?? ''}`}>
          {p.value}
        </span>
      ),
    },
    {
      field: 'status', headerName: t('common.status', '상태'), width: 110,
      cellRenderer: (p: any) => (
        <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${STATUS_COLORS[p.value] ?? ''}`}>
          {p.value}
        </span>
      ),
    },
    {
      headerName: t('admin.users', '사용자'), width: 120,
      valueGetter: (p: any) => `${p.data?.currentUsers ?? 0} / ${p.data?.maxUsers ?? 0}`,
    },
    {
      headerName: t('admin.storage', '저장공간'), width: 120,
      valueGetter: (p: any) => `${p.data?.maxStorageMb ?? 0} MB`,
    },
    {
      headerName: '', width: 100, sortable: false, filter: false,
      cellRenderer: (p: any) => {
        const tenant = p.data as Tenant;
        return (
          <div className="flex gap-1">
            {tenant.status === 'ACTIVE' ? (
              <button onClick={() => handleSuspend(tenant.tenantId)} className="p-1 text-slate-400 hover:text-red-500" title="정지">
                <Pause size={14} />
              </button>
            ) : (
              <button onClick={() => handleActivate(tenant.tenantId)} className="p-1 text-slate-400 hover:text-green-500" title="활성화">
                <Play size={14} />
              </button>
            )}
          </div>
        );
      },
    },
  ];

  const handleSuspend = async (tenantId: string) => {
    if (!confirm(`${tenantId} 테넌트를 정지하시겠습니까?`)) return;
    await tenantApi.suspend(tenantId);
    load();
  };

  const handleActivate = async (tenantId: string) => {
    await tenantApi.activate(tenantId);
    load();
  };

  return (
    <div>
      <PageHeader title={t('admin.tenants', '테넌트 관리')} actions={
        <button className="btn-primary flex items-center gap-2">
          <Plus size={16} /> {t('common.create', '생성')}
        </button>
      } />

      <DataGrid rowData={tenants} columnDefs={columnDefs} loading={loading} height="calc(100vh - 340px)" />
    </div>
  );
}
