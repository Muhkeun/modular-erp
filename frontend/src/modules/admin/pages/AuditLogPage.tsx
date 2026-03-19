import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { auditLogApi, type AuditLog, type AuditAction } from '../../../shared/api/adminApi';
import PageHeader from '../../../shared/components/PageHeader';
import DataGrid from '../../../shared/components/DataGrid';
import type { ColDef } from 'ag-grid-community';

const AUDIT_ACTIONS: AuditAction[] = ['CREATE', 'READ', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'EXPORT', 'IMPORT', 'APPROVE', 'REJECT'];

export default function AuditLogPage() {
  const { t } = useTranslation();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterAction, setFilterAction] = useState<AuditAction | ''>('');
  const [filterEntityType, setFilterEntityType] = useState('');

  const loadLogs = async () => {
    setLoading(true);
    try {
      const result = await auditLogApi.search({
        action: filterAction || undefined,
        entityType: filterEntityType || undefined,
        size: 100,
      });
      setLogs(result.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadLogs(); }, [filterAction, filterEntityType]);

  const columnDefs: ColDef<AuditLog>[] = [
    { field: 'createdAt', headerName: t('common.datetime', '일시'), width: 180,
      valueFormatter: (p: any) => p.value ? new Date(p.value).toLocaleString() : '' },
    { field: 'userId', headerName: t('common.user', '사용자'), width: 120 },
    { field: 'action', headerName: t('admin.action', '액션'), width: 100,
      cellStyle: (p: any) => {
        const colors: Record<string, string> = { CREATE: '#16a34a', UPDATE: '#2563eb', DELETE: '#dc2626' };
        return { color: colors[p.value] ?? '#64748b', fontWeight: 600 };
      },
    },
    { field: 'entityType', headerName: t('admin.entityType', '대상 유형'), width: 150 },
    { field: 'entityId', headerName: t('admin.entityId', '대상 ID'), width: 100 },
    { field: 'ipAddress', headerName: 'IP', width: 130 },
  ];

  return (
    <div>
      <PageHeader title={t('admin.auditLogs', '감사 로그')} />

      {/* Filters */}
      <div className="flex gap-3 mb-4">
        <select
          value={filterAction}
          onChange={(e) => setFilterAction(e.target.value as AuditAction | '')}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="">{t('admin.allActions', '전체 액션')}</option>
          {AUDIT_ACTIONS.map((a) => <option key={a} value={a}>{a}</option>)}
        </select>
        <input
          placeholder={t('admin.entityType', '대상 유형')}
          value={filterEntityType}
          onChange={(e) => setFilterEntityType(e.target.value)}
          className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
        />
      </div>

      <DataGrid rowData={logs} columnDefs={columnDefs} loading={loading} height="calc(100vh - 380px)" />
    </div>
  );
}
