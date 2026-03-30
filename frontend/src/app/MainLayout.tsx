import { useState, useCallback, useMemo } from "react";
import { Outlet, Link, useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "../shared/hooks/useAuth";
import { useKeyboardShortcuts } from "../shared/hooks/useKeyboardShortcuts";
import {
  LayoutGrid, ChevronDown, ChevronRight, LogOut,
  Search, Bell, Menu, X, Settings,
  Sparkles,
  Command,
} from "lucide-react";
import AiChatWidget from "../shared/components/AiChatWidget";
import CommandPalette from "../shared/components/CommandPalette";
import ErrorBoundary from "../shared/components/ErrorBoundary";
import { clsx } from "clsx";
import { buildMenuSortMap, buildVisibleMenuCodeSet, navigationItems, type NavigationItem } from "../shared/navigation";
import { useMenuProfile } from "../shared/hooks/useMenuProfile";

function NavGroup({ item }: { item: NavigationItem }) {
  const location = useLocation();
  const { t } = useTranslation();
  const isActive = item.path
    ? location.pathname === item.path
    : item.children?.some((c) => location.pathname.startsWith(c.path));
  const [open, setOpen] = useState(isActive);

  if (item.path) {
    return (
      <Link to={item.path}
        className={clsx(
          "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
          isActive
            ? "bg-brand-50 text-brand-700"
            : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
        )}>
        <span className={clsx(isActive ? "text-brand-600" : "text-slate-400")}>{item.icon}</span>
        {t(item.labelKey)}
      </Link>
    );
  }

  return (
    <div>
      <button onClick={() => setOpen(!open)}
        className={clsx(
          "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
          isActive
            ? "text-brand-700"
            : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
        )}>
        <span className={clsx(isActive ? "text-brand-600" : "text-slate-400")}>{item.icon}</span>
        <span className="flex-1 text-left">{t(item.labelKey)}</span>
        {open ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
      </button>
      {open && (
        <div className="ml-8 mt-1 space-y-0.5 border-l-2 border-slate-100 pl-3">
          {item.children!.map((child) => {
            const childActive = location.pathname.startsWith(child.path);
            return (
              <Link key={child.path} to={child.path}
                className={clsx(
                  "block rounded-md px-3 py-2 text-sm transition-colors",
                  childActive
                    ? "bg-brand-50 text-brand-700 font-medium"
                    : "text-slate-500 hover:bg-slate-50 hover:text-slate-900"
                )}>
                {t(child.labelKey)}
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function MainLayout() {
  const { name, tenantId, logout } = useAuth();
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const menuProfile = useMenuProfile();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);

  const openCommandPalette = useCallback(() => {
    if (!menuProfile.hasResolvedProfile) return;
    setCommandPaletteOpen(true);
  }, [menuProfile.hasResolvedProfile]);

  useKeyboardShortcuts({
    onSearch: openCommandPalette,
  });

  const filteredNavigation = useMemo(() => {
    if (!menuProfile.isResolved || !menuProfile.hasResolvedProfile) return [];

    const visibleCodes = buildVisibleMenuCodeSet(menuProfile.visibleMenuCodes);
    const sortMap = buildMenuSortMap(menuProfile.data?.menuItems ?? []);

    const sortedItems = [...navigationItems]
      .map((item) => {
        if (item.path) {
          return visibleCodes.has(item.code) ? item : null;
        }

        const visibleChildren = (item.children ?? [])
          .filter((child) => visibleCodes.has(child.code))
          .sort((left, right) => (sortMap.get(left.code) ?? Number.MAX_SAFE_INTEGER) - (sortMap.get(right.code) ?? Number.MAX_SAFE_INTEGER));

        if (!visibleChildren.length && !visibleCodes.has(item.code)) {
          return null;
        }

        return { ...item, children: visibleChildren };
      })
      .filter((item): item is NavigationItem => item !== null)
      .sort((left, right) => {
        const leftKey = left.path ? left.code : left.children?.[0]?.code ?? left.code;
        const rightKey = right.path ? right.code : right.children?.[0]?.code ?? right.code;
        return (sortMap.get(leftKey) ?? Number.MAX_SAFE_INTEGER) - (sortMap.get(rightKey) ?? Number.MAX_SAFE_INTEGER);
      });

    return sortedItems;
  }, [menuProfile.data?.menuItems, menuProfile.hasResolvedProfile, menuProfile.isResolved, menuProfile.visibleMenuCodes]);

  const canAccessMenu = useCallback(
    (menuCode: string) => menuProfile.isResolved && menuProfile.visibleMenuCodes.includes(menuCode),
    [menuProfile.isResolved, menuProfile.visibleMenuCodes]
  );

  return (
    <div className="flex h-screen overflow-hidden bg-transparent">
      {/* Sidebar */}
      <aside data-testid="sidebar" className={clsx(
        "m-3 flex flex-col overflow-hidden rounded-[32px] border border-white/70 bg-white/88 shadow-[0_18px_60px_rgba(15,23,42,0.08)] backdrop-blur transition-all duration-200",
        sidebarOpen ? "w-64" : "w-0 overflow-hidden"
      )}>
        {/* Logo */}
        <div className="flex h-20 items-center gap-3 border-b border-slate-100/80 px-5">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-600 to-sky-500 shadow-[0_16px_30px_rgba(37,99,235,0.25)]">
            <LayoutGrid size={18} className="text-white" />
          </div>
          <div>
            <div className="text-sm font-bold tracking-tight text-slate-950">ModularERP</div>
            <div className="text-[11px] font-medium uppercase tracking-[0.22em] text-slate-400">{tenantId}</div>
          </div>
        </div>

        {/* Navigation */}
        <nav data-testid="sidebar-nav" className="flex-1 overflow-y-auto p-3 space-y-1">
          {menuProfile.isLoading && (
            <div className="px-3 py-4 text-sm text-slate-400">{t("common.loading")}</div>
          )}
          {!menuProfile.isLoading && filteredNavigation.map((item) => (
            <NavGroup key={item.labelKey} item={item} />
          ))}
          {!menuProfile.isLoading && menuProfile.isResolved && filteredNavigation.length === 0 && (
            <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50/80 px-4 py-4 text-sm text-slate-500">
              <p className="font-medium text-slate-700">No Menu Profile</p>
              <p className="mt-1 text-xs leading-5">
                Assigned menu profile not found. Navigation is blocked until a visible profile is applied.
              </p>
            </div>
          )}
        </nav>

        {/* User */}
        <div className="border-t border-slate-100/80 p-3">
          <div className="flex items-center gap-3 rounded-2xl bg-slate-50/90 px-3 py-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-2xl bg-brand-100 text-sm font-bold text-brand-700">
              {name?.charAt(0)?.toUpperCase() || "U"}
            </div>
            <div className="flex-1 min-w-0">
              <div className="truncate text-sm font-medium text-slate-900">{name}</div>
              <div className="truncate text-xs text-slate-400">Operations Workspace</div>
            </div>
            <button data-testid="logout-button" onClick={logout} className="text-slate-400 hover:text-red-500 transition-colors" title="Logout">
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top bar */}
        <header className="mx-3 mt-3 flex h-16 items-center justify-between rounded-[28px] border border-white/70 bg-white/78 px-6 shadow-[0_14px_40px_rgba(15,23,42,0.06)] backdrop-blur">
          <div className="flex items-center gap-4">
            <button data-testid="sidebar-toggle" onClick={() => setSidebarOpen(!sidebarOpen)}
              className="rounded-2xl p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700">
              {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
            <button
              onClick={openCommandPalette}
              disabled={!menuProfile.hasResolvedProfile}
              className="hidden items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-2.5 md:flex cursor-pointer hover:border-slate-300 transition disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Search size={16} className="text-slate-400" />
              <span className="w-80 text-left text-sm text-slate-400">
                {t("common.search")}...
              </span>
              <span className="flex items-center gap-1 rounded-full bg-white px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400 shadow-sm">
                <Command size={10} /> K
              </span>
            </button>
          </div>
          <div className="flex items-center gap-3">
            <button data-testid="lang-toggle" onClick={() => { const next = i18n.language === "ko" ? "en" : "ko"; i18n.changeLanguage(next); localStorage.setItem("locale", next); }} className="btn-ghost px-2 text-xs font-bold">
              {i18n.language === "ko" ? "EN" : "KO"}
            </button>
            {canAccessMenu("notifications") && (
            <button onClick={() => navigate('/notifications')} className="relative rounded-2xl p-2 text-slate-400 transition hover:bg-slate-50 hover:text-slate-600">
              <Bell size={20} />
              <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-red-500" />
            </button>
            )}
            {canAccessMenu("ai-chat") && (
            <button onClick={() => navigate('/ai-chat')} className="rounded-2xl p-2 text-slate-400 transition hover:bg-slate-50 hover:text-slate-600" title="AI Assistant">
              <Sparkles size={20} />
            </button>
            )}
            <button onClick={() => navigate('/settings')} className="rounded-2xl p-2 text-slate-400 transition hover:bg-slate-50 hover:text-slate-600">
              <Settings size={20} />
            </button>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto px-3 pb-3 pt-4">
          <div className="h-full rounded-[32px] border border-white/60 bg-white/22 p-6 backdrop-blur-sm">
          <ErrorBoundary>
            <Outlet />
          </ErrorBoundary>
          </div>
        </main>
      </div>
      <AiChatWidget />
      <CommandPalette
        open={commandPaletteOpen}
        onClose={() => setCommandPaletteOpen(false)}
        visibleMenuCodes={menuProfile.visibleMenuCodes}
      />
    </div>
  );
}
