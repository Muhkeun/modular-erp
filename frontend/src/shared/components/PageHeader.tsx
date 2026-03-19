import type { ReactNode } from "react";

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: ReactNode;
  breadcrumbs?: { label: string; path?: string }[];
}

export default function PageHeader({ title, description, actions, breadcrumbs }: PageHeaderProps) {
  return (
    <div data-testid="page-header" className="mb-6">
      {breadcrumbs && (
        <nav className="mb-3 flex flex-wrap items-center gap-2 text-sm text-slate-400">
          {breadcrumbs.map((bc, i) => (
            <span key={i} className="flex items-center gap-2">
              {i > 0 && <span className="text-slate-300">/</span>}
              {bc.path ? (
                <a
                  href={bc.path}
                  className="rounded-full border border-transparent px-2.5 py-1 transition-colors hover:border-slate-200 hover:bg-white hover:text-slate-700"
                >
                  {bc.label}
                </a>
              ) : (
                <span className="rounded-full bg-white px-2.5 py-1 font-medium text-slate-700 shadow-sm">
                  {bc.label}
                </span>
              )}
            </span>
          ))}
        </nav>
      )}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-[2rem] font-bold tracking-tight text-slate-950">{title}</h1>
          {description && <p className="mt-1.5 max-w-3xl text-sm text-slate-500">{description}</p>}
        </div>
        {actions && <div className="flex flex-wrap items-center gap-3">{actions}</div>}
      </div>
    </div>
  );
}
