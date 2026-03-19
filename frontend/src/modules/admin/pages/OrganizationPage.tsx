import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { organizationApi, type Organization, type OrgType } from '../../../shared/api/adminApi';
import PageHeader from '../../../shared/components/PageHeader';
import { Plus, ChevronRight, ChevronDown, Building2, Factory, Users, Briefcase } from 'lucide-react';

const ORG_TYPES: { value: OrgType; label: string; icon: React.ReactNode }[] = [
  { value: 'COMPANY', label: '회사', icon: <Building2 size={16} /> },
  { value: 'OPERATING_UNIT', label: '사업장', icon: <Briefcase size={16} /> },
  { value: 'PLANT', label: '공장', icon: <Factory size={16} /> },
  { value: 'DEPARTMENT', label: '부서', icon: <Users size={16} /> },
];

function OrgNode({ org, level = 0 }: { org: Organization; level?: number }) {
  const [open, setOpen] = useState(true);
  const hasChildren = org.children && org.children.length > 0;
  const typeInfo = ORG_TYPES.find(t => t.value === org.orgType);

  return (
    <div>
      <div
        className="flex items-center gap-2 px-3 py-2 hover:bg-slate-50 rounded-lg cursor-pointer"
        style={{ paddingLeft: `${level * 24 + 12}px` }}
        onClick={() => setOpen(!open)}
      >
        {hasChildren ? (
          open ? <ChevronDown size={14} className="text-slate-400" /> : <ChevronRight size={14} className="text-slate-400" />
        ) : (
          <span className="w-3.5" />
        )}
        <span className="text-slate-500">{typeInfo?.icon}</span>
        <span className="text-sm font-medium text-slate-700">{org.name}</span>
        <span className="text-xs text-slate-400 ml-1">({org.code})</span>
        <span className="ml-auto text-xs text-slate-400">{typeInfo?.label}</span>
      </div>
      {open && hasChildren && (
        <div>
          {org.children.map((child) => (
            <OrgNode key={child.id} org={child} level={level + 1} />
          ))}
        </div>
      )}
    </div>
  );
}

export default function OrganizationPage() {
  const { t } = useTranslation();
  const [tree, setTree] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    organizationApi.getTree().then(setTree).finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <PageHeader title={t('admin.organizations', '조직 관리')} actions={
        <button className="btn-primary flex items-center gap-2">
          <Plus size={16} /> {t('common.create', '생성')}
        </button>
      } />

      <div className="rounded-xl border border-slate-200 bg-white p-4">
        {loading ? (
          <div className="text-sm text-slate-400 text-center py-8">Loading...</div>
        ) : tree.length === 0 ? (
          <div className="text-sm text-slate-400 text-center py-8">{t('admin.noOrganizations', '등록된 조직이 없습니다')}</div>
        ) : (
          tree.map((org) => <OrgNode key={org.id} org={org} />)
        )}
      </div>
    </div>
  );
}
