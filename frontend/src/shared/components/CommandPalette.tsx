import { useState, useEffect, useMemo, useRef, useCallback } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Search, LayoutGrid, ShoppingCart, Truck, Factory, DollarSign,
  Users, Package, CalendarClock, Building2, ClipboardCheck, FileText,
  Plus, ArrowRight,
} from "lucide-react";
import { clsx } from "clsx";

interface PaletteItem {
  id: string;
  title: string;
  category: "page" | "action" | "recent";
  icon: React.ReactNode;
  shortcut?: string;
  action: () => void;
  keywords?: string[];
}

interface CommandPaletteProps {
  open: boolean;
  onClose: () => void;
}

function fuzzyMatch(text: string, query: string): boolean {
  const lower = text.toLowerCase();
  const q = query.toLowerCase();
  let qi = 0;
  for (let i = 0; i < lower.length && qi < q.length; i++) {
    if (lower[i] === q[qi]) qi++;
  }
  return qi === q.length;
}

export default function CommandPalette({ open, onClose }: CommandPaletteProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const go = useCallback(
    (path: string) => {
      navigate(path);
      onClose();
    },
    [navigate, onClose]
  );

  const items = useMemo<PaletteItem[]>(
    () => [
      // Pages
      { id: "dashboard", title: t("nav.dashboard"), category: "page", icon: <LayoutGrid size={16} />, action: () => go("/dashboard"), keywords: ["home"] },
      { id: "pr", title: t("nav.purchaseRequests"), category: "page", icon: <ShoppingCart size={16} />, action: () => go("/purchase/requests"), keywords: ["pr", "procurement"] },
      { id: "po", title: t("nav.purchaseOrders"), category: "page", icon: <ShoppingCart size={16} />, action: () => go("/purchase/orders"), keywords: ["po"] },
      { id: "so", title: t("nav.salesOrders"), category: "page", icon: <DollarSign size={16} />, action: () => go("/sales/orders") },
      { id: "wo", title: t("nav.workOrders"), category: "page", icon: <Factory size={16} />, action: () => go("/production/work-orders") },
      { id: "gr", title: t("nav.goodsReceipt"), category: "page", icon: <Truck size={16} />, action: () => go("/logistics/gr") },
      { id: "gi", title: t("nav.goodsIssue"), category: "page", icon: <Truck size={16} />, action: () => go("/logistics/gi") },
      { id: "stock", title: t("nav.stockOverview"), category: "page", icon: <Package size={16} />, action: () => go("/logistics/stock") },
      { id: "mrp", title: t("nav.mrp"), category: "page", icon: <CalendarClock size={16} />, action: () => go("/planning/mrp") },
      { id: "je", title: t("nav.journalEntries"), category: "page", icon: <Building2 size={16} />, action: () => go("/account/journal") },
      { id: "hr", title: t("nav.hr"), category: "page", icon: <Users size={16} />, action: () => go("/hr") },
      { id: "items", title: t("nav.items"), category: "page", icon: <Package size={16} />, action: () => go("/master-data/items") },
      { id: "quality", title: t("nav.quality"), category: "page", icon: <ClipboardCheck size={16} />, action: () => go("/quality") },
      { id: "approvals", title: t("nav.approvals"), category: "page", icon: <FileText size={16} />, action: () => go("/approvals") },
      // Actions
      { id: "new-pr", title: t("pr.newPr"), category: "action", icon: <Plus size={16} />, shortcut: "Ctrl+N", action: () => go("/purchase/requests"), keywords: ["create", "new"] },
      { id: "new-po", title: t("po.newPo"), category: "action", icon: <Plus size={16} />, action: () => go("/purchase/orders") },
      { id: "new-so", title: t("so.newSo"), category: "action", icon: <Plus size={16} />, action: () => go("/sales/orders") },
      { id: "new-item", title: t("item.newItem"), category: "action", icon: <Plus size={16} />, action: () => go("/master-data/items/new") },
    ],
    [t, go]
  );

  const filtered = useMemo(() => {
    if (!query.trim()) return items.slice(0, 10);
    return items.filter(
      (item) =>
        fuzzyMatch(item.title, query) ||
        item.keywords?.some((kw) => fuzzyMatch(kw, query))
    );
  }, [items, query]);

  useEffect(() => {
    if (open) {
      setQuery("");
      setActiveIndex(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [open]);

  useEffect(() => {
    setActiveIndex(0);
  }, [query]);

  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
        return;
      }
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveIndex((i) => Math.min(i + 1, filtered.length - 1));
      }
      if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveIndex((i) => Math.max(i - 1, 0));
      }
      if (e.key === "Enter" && filtered[activeIndex]) {
        e.preventDefault();
        filtered[activeIndex].action();
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [open, onClose, filtered, activeIndex]);

  if (!open) return null;

  const categoryLabels: Record<string, string> = {
    page: t("commandPalette.pages", "Pages"),
    action: t("commandPalette.actions", "Actions"),
    recent: t("commandPalette.recentPages", "Recent"),
  };

  // Group by category
  const grouped = filtered.reduce<Record<string, PaletteItem[]>>((acc, item) => {
    (acc[item.category] ??= []).push(item);
    return acc;
  }, {});

  let globalIdx = -1;

  return createPortal(
    <div className="fixed inset-0 z-[9997] flex items-start justify-center pt-[15vh] bg-slate-950/50 backdrop-blur-sm" onClick={onClose}>
      <div
        className="w-full max-w-lg rounded-[28px] border border-white/70 bg-white/95 shadow-[0_32px_80px_rgba(15,23,42,0.28)] backdrop-blur overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Search input */}
        <div className="flex items-center gap-3 border-b border-slate-100 px-5 py-4">
          <Search size={18} className="text-slate-400" />
          <input
            ref={inputRef}
            className="flex-1 border-0 bg-transparent text-sm text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-0"
            placeholder={t("commandPalette.placeholder", "Search pages, actions...")}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <kbd className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-0.5 text-[11px] font-medium text-slate-400">ESC</kbd>
        </div>

        {/* Results */}
        <div className="max-h-80 overflow-y-auto p-2">
          {filtered.length === 0 ? (
            <div className="py-8 text-center text-sm text-slate-400">
              {t("commandPalette.noResults", "No results found")}
            </div>
          ) : (
            Object.entries(grouped).map(([category, categoryItems]) => (
              <div key={category}>
                <div className="px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
                  {categoryLabels[category] || category}
                </div>
                {categoryItems.map((item) => {
                  globalIdx++;
                  const idx = globalIdx;
                  return (
                    <button
                      key={item.id}
                      className={clsx(
                        "flex w-full items-center gap-3 rounded-2xl px-3 py-2.5 text-left text-sm transition",
                        idx === activeIndex
                          ? "bg-brand-50 text-brand-700"
                          : "text-slate-600 hover:bg-slate-50"
                      )}
                      onClick={item.action}
                      onMouseEnter={() => setActiveIndex(idx)}
                    >
                      <span className={clsx(idx === activeIndex ? "text-brand-500" : "text-slate-400")}>
                        {item.icon}
                      </span>
                      <span className="flex-1 font-medium">{item.title}</span>
                      {item.shortcut && (
                        <kbd className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-0.5 text-[10px] font-medium text-slate-400">
                          {item.shortcut}
                        </kbd>
                      )}
                      <ArrowRight size={14} className="text-slate-300" />
                    </button>
                  );
                })}
              </div>
            ))
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center gap-4 border-t border-slate-100 px-5 py-3 text-[11px] text-slate-400">
          <span><kbd className="font-medium">↑↓</kbd> navigate</span>
          <span><kbd className="font-medium">↵</kbd> select</span>
          <span><kbd className="font-medium">esc</kbd> close</span>
        </div>
      </div>
    </div>,
    document.body
  );
}
