import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { systemCodeApi, type SystemCode } from '../../../shared/api/adminApi';
import PageHeader from '../../../shared/components/PageHeader';
import DataGrid from '../../../shared/components/DataGrid';
import type { ColDef } from 'ag-grid-community';
import { Plus, Edit2, Trash2 } from 'lucide-react';

export default function SystemCodePage() {
  const { t } = useTranslation();
  const [codes, setCodes] = useState<SystemCode[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<SystemCode | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formGroupCode, setFormGroupCode] = useState('');
  const [formGroupName, setFormGroupName] = useState('');
  const [formDesc, setFormDesc] = useState('');

  const loadCodes = async () => {
    setLoading(true);
    try {
      setCodes(await systemCodeApi.getAll());
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadCodes(); }, []);

  const columnDefs: ColDef<SystemCode>[] = [
    { field: 'groupCode', headerName: t('admin.groupCode', '그룹 코드'), width: 150 },
    { field: 'groupName', headerName: t('admin.groupName', '그룹명'), flex: 1 },
    { field: 'description', headerName: t('common.description', '설명'), flex: 2 },
    {
      field: 'isSystem', headerName: t('admin.system', '시스템'), width: 100,
      cellRenderer: (p: any) => p.value ? '🔒' : '',
    },
    {
      headerName: t('admin.itemCount', '항목 수'), width: 100,
      valueGetter: (p: any) => p.data?.items?.length ?? 0,
    },
    {
      headerName: '', width: 100, sortable: false, filter: false,
      cellRenderer: (p: any) => {
        if (p.data?.isSystem) return null;
        return (
          <div className="flex gap-1">
            <button onClick={() => openEdit(p.data)} className="p-1 text-slate-400 hover:text-brand-600">
              <Edit2 size={14} />
            </button>
            <button onClick={() => handleDelete(p.data.groupCode)} className="p-1 text-slate-400 hover:text-red-500">
              <Trash2 size={14} />
            </button>
          </div>
        );
      },
    },
  ];

  const itemColDefs: ColDef[] = [
    { field: 'code', headerName: t('common.code', '코드'), width: 120 },
    { field: 'name', headerName: t('common.name', '명칭'), flex: 1 },
    { field: 'sortOrder', headerName: t('common.sortOrder', '순서'), width: 80 },
    { field: 'extra', headerName: t('admin.extra', '추가 속성'), flex: 1 },
  ];

  const openNew = () => {
    setSelected(null);
    setFormGroupCode('');
    setFormGroupName('');
    setFormDesc('');
    setShowForm(true);
  };

  const openEdit = (code: SystemCode) => {
    setSelected(code);
    setFormGroupCode(code.groupCode);
    setFormGroupName(code.groupName);
    setFormDesc(code.description ?? '');
    setShowForm(true);
  };

  const handleSave = async () => {
    if (selected) {
      await systemCodeApi.update(selected.groupCode, { groupName: formGroupName, description: formDesc });
    } else {
      await systemCodeApi.create({ groupCode: formGroupCode, groupName: formGroupName, description: formDesc });
    }
    setShowForm(false);
    loadCodes();
  };

  const handleDelete = async (groupCode: string) => {
    if (!confirm(t('common.confirmDelete', '삭제하시겠습니까?'))) return;
    await systemCodeApi.delete(groupCode);
    loadCodes();
  };

  return (
    <div>
      <PageHeader title={t('admin.systemCodes', '시스템 코드 관리')} actions={
        <button onClick={openNew} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> {t('common.create', '생성')}
        </button>
      } />

      <div className="grid grid-cols-2 gap-4">
        <div>
          <h4 className="text-sm font-medium text-slate-600 mb-2">{t('admin.codeGroups', '코드 그룹')}</h4>
          <DataGrid
            rowData={codes}
            columnDefs={columnDefs}
            loading={loading}
            height="calc(100vh - 380px)"
            onRowClicked={(row) => setSelected(row)}
          />
        </div>
        <div>
          <h4 className="text-sm font-medium text-slate-600 mb-2">
            {selected ? `${selected.groupCode} - ${selected.groupName}` : t('admin.selectGroup', '그룹을 선택하세요')}
          </h4>
          <DataGrid
            rowData={selected?.items ?? []}
            columnDefs={itemColDefs}
            height="calc(100vh - 380px)"
          />
        </div>
      </div>

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold mb-4">
              {selected ? t('admin.editCode', '코드 수정') : t('admin.createCode', '코드 생성')}
            </h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t('admin.groupCode', '그룹 코드')}</label>
                <input value={formGroupCode} onChange={(e) => setFormGroupCode(e.target.value)} disabled={!!selected}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t('admin.groupName', '그룹명')}</label>
                <input value={formGroupName} onChange={(e) => setFormGroupName(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t('common.description', '설명')}</label>
                <textarea value={formDesc} onChange={(e) => setFormDesc(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" rows={3} />
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button onClick={() => setShowForm(false)} className="btn-ghost">{t('common.cancel', '취소')}</button>
              <button onClick={handleSave} className="btn-primary">{t('common.save', '저장')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
