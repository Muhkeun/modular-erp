import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { roleApi, type Role } from '../../../shared/api/adminApi';
import PageHeader from '../../../shared/components/PageHeader';
import DataGrid from '../../../shared/components/DataGrid';
import type { ColDef } from 'ag-grid-community';
import { Plus, Edit2, Trash2 } from 'lucide-react';

export default function RoleManagementPage() {
  const { t } = useTranslation();
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [showForm, setShowForm] = useState(false);

  // Form state
  const [formCode, setFormCode] = useState('');
  const [formName, setFormName] = useState('');
  const [formDesc, setFormDesc] = useState('');

  const loadRoles = async () => {
    setLoading(true);
    try {
      const data = await roleApi.getAll();
      setRoles(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadRoles(); }, []);

  const columnDefs: ColDef<Role>[] = [
    { field: 'code', headerName: t('admin.roleCode', '역할 코드'), width: 150 },
    { field: 'name', headerName: t('admin.roleName', '역할명'), flex: 1 },
    { field: 'description', headerName: t('common.description', '설명'), flex: 2 },
    {
      field: 'isSystem', headerName: t('admin.system', '시스템'), width: 100,
      cellRenderer: (p: any) => p.value ? '🔒' : '',
    },
    {
      headerName: t('admin.permissionCount', '권한 수'), width: 120,
      valueGetter: (p: any) => p.data?.permissions?.length ?? 0,
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
            <button onClick={() => handleDelete(p.data.code)} className="p-1 text-slate-400 hover:text-red-500">
              <Trash2 size={14} />
            </button>
          </div>
        );
      },
    },
  ];

  const openNew = () => {
    setEditingRole(null);
    setFormCode('');
    setFormName('');
    setFormDesc('');
    setShowForm(true);
  };

  const openEdit = (role: Role) => {
    setEditingRole(role);
    setFormCode(role.code);
    setFormName(role.name);
    setFormDesc(role.description ?? '');
    setShowForm(true);
  };

  const handleSave = async () => {
    if (editingRole) {
      await roleApi.update(editingRole.code, { name: formName, description: formDesc });
    } else {
      await roleApi.create({ code: formCode, name: formName, description: formDesc });
    }
    setShowForm(false);
    loadRoles();
  };

  const handleDelete = async (code: string) => {
    if (!confirm(t('common.confirmDelete', '삭제하시겠습니까?'))) return;
    await roleApi.delete(code);
    loadRoles();
  };

  return (
    <div>
      <PageHeader title={t('admin.roles', '역할 관리')} actions={
        <button onClick={openNew} className="btn-primary flex items-center gap-2">
          <Plus size={16} /> {t('common.create', '생성')}
        </button>
      } />

      <DataGrid rowData={roles} columnDefs={columnDefs} loading={loading} height="calc(100vh - 340px)" />

      {/* Simple Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold mb-4">
              {editingRole ? t('admin.editRole', '역할 수정') : t('admin.createRole', '역할 생성')}
            </h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t('admin.roleCode', '역할 코드')}</label>
                <input
                  value={formCode}
                  onChange={(e) => setFormCode(e.target.value)}
                  disabled={!!editingRole}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm disabled:bg-slate-100"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t('admin.roleName', '역할명')}</label>
                <input
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">{t('common.description', '설명')}</label>
                <textarea
                  value={formDesc}
                  onChange={(e) => setFormDesc(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
                  rows={3}
                />
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
