import { useEffect, useId, useMemo, useRef, useState } from "react";
import { Search, ChevronsUpDown, X } from "lucide-react";

export interface SearchOption {
  value: string;
  label: string;
  description?: string;
  meta?: string;
  keywords?: string[];
}

interface SearchModalProps {
  open: boolean;
  title: string;
  description?: string;
  options: SearchOption[];
  initialQuery?: string;
  emptyText?: string;
  onClose: () => void;
  onSelect: (option: SearchOption) => void;
}

interface SearchSelectProps {
  label: string;
  value: string;
  options: SearchOption[];
  placeholder?: string;
  searchTitle?: string;
  searchDescription?: string;
  helper?: string;
  emptyText?: string;
  disabled?: boolean;
  onSelect: (option: SearchOption) => void;
  onClear?: () => void;
}

function filterOptions(options: SearchOption[], query: string) {
  const term = query.trim().toLowerCase();
  if (!term) return options;

  return options.filter((option) => {
    const bag = [
      option.value,
      option.label,
      option.description ?? "",
      option.meta ?? "",
      ...(option.keywords ?? []),
    ]
      .join(" ")
      .toLowerCase();

    return bag.includes(term);
  });
}

function SearchModal({
  open,
  title,
  description,
  options,
  initialQuery = "",
  emptyText = "검색 결과가 없습니다.",
  onClose,
  onSelect,
}: SearchModalProps) {
  const [query, setQuery] = useState(initialQuery);
  const [activeIndex, setActiveIndex] = useState(0);

  useEffect(() => {
    if (!open) return;

    setQuery(initialQuery);
    setActiveIndex(0);

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [initialQuery, onClose, open]);

  const filtered = useMemo(() => filterOptions(options, query).slice(0, 24), [options, query]);

  useEffect(() => {
    setActiveIndex(0);
  }, [query]);

  if (!open) return null;

  const selectActive = () => {
    const target = filtered[activeIndex];
    if (!target) return;
    onSelect(target);
  };

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/55 px-4 backdrop-blur-sm">
      <div
        className="w-full max-w-3xl rounded-[28px] border border-white/60 bg-white/95 p-5 shadow-[0_32px_80px_rgba(15,23,42,0.28)]"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-brand-600">
              Search Workspace
            </p>
            <h3 className="mt-1 text-2xl font-semibold tracking-tight text-slate-950">{title}</h3>
            {description && <p className="mt-2 text-sm text-slate-500">{description}</p>}
          </div>
          <button
            type="button"
            className="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200 bg-white text-slate-500 transition hover:border-slate-300 hover:text-slate-800"
            onClick={onClose}
          >
            <X size={18} />
          </button>
        </div>

        <div className="lookup-shell">
          <Search size={18} className="text-slate-400" />
          <input
            autoFocus
            className="lookup-input"
            placeholder="코드, 이름, 설명으로 검색"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "ArrowDown") {
                event.preventDefault();
                setActiveIndex((current) => Math.min(current + 1, Math.max(filtered.length - 1, 0)));
              }
              if (event.key === "ArrowUp") {
                event.preventDefault();
                setActiveIndex((current) => Math.max(current - 1, 0));
              }
              if (event.key === "Enter") {
                event.preventDefault();
                selectActive();
              }
            }}
          />
        </div>

        <div className="mt-4 overflow-hidden rounded-3xl border border-slate-200/80 bg-slate-50/70">
          <div className="flex items-center justify-between border-b border-slate-200/80 px-4 py-3 text-xs font-medium uppercase tracking-[0.2em] text-slate-400">
            <span>Results</span>
            <span>{filtered.length.toLocaleString("ko-KR")} items</span>
          </div>
          <div className="max-h-[420px] overflow-y-auto p-2">
            {filtered.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-slate-200 bg-white px-4 py-10 text-center text-sm text-slate-400">
                {emptyText}
              </div>
            ) : (
              filtered.map((option, index) => (
                <button
                  key={option.value}
                  type="button"
                  className={`lookup-option ${index === activeIndex ? "lookup-option-active" : ""}`}
                  onClick={() => onSelect(option)}
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="truncate font-semibold text-slate-900">{option.label}</span>
                      <span className="lookup-code">{option.value}</span>
                    </div>
                    {option.description && (
                      <p className="mt-1 truncate text-sm text-slate-500">{option.description}</p>
                    )}
                  </div>
                  {option.meta && <span className="lookup-meta">{option.meta}</span>}
                </button>
              ))
            )}
          </div>
        </div>
      </div>
      <button type="button" className="absolute inset-0 -z-10" onClick={onClose} aria-label="close search modal" />
    </div>
  );
}

export default function SearchSelect({
  label,
  value,
  options,
  placeholder = "검색 후 선택",
  searchTitle,
  searchDescription,
  helper,
  emptyText,
  disabled,
  onSelect,
  onClear,
}: SearchSelectProps) {
  const inputId = useId();
  const containerRef = useRef<HTMLDivElement | null>(null);
  const selectedOption = options.find((option) => option.value === value);
  const [query, setQuery] = useState(selectedOption?.label ?? value ?? "");
  const [activeIndex, setActiveIndex] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);

  useEffect(() => {
    setQuery(selectedOption?.label ?? value ?? "");
  }, [selectedOption, value]);

  useEffect(() => {
    const handlePointerDown = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  const filtered = useMemo(() => filterOptions(options, query).slice(0, 8), [options, query]);

  useEffect(() => {
    setActiveIndex(0);
  }, [query]);

  const commitSelection = (option: SearchOption) => {
    onSelect(option);
    setQuery(option.label);
    setMenuOpen(false);
    setModalOpen(false);
  };

  return (
    <>
      <div ref={containerRef} className="space-y-2">
        <label htmlFor={inputId} className="field-label">
          {label}
        </label>
        <div className="lookup-shell">
          <Search size={18} className="text-slate-400" />
          <input
            id={inputId}
            className="lookup-input"
            value={query}
            placeholder={placeholder}
            disabled={disabled}
            onFocus={() => setMenuOpen(true)}
            onChange={(event) => {
              setQuery(event.target.value);
              setMenuOpen(true);
            }}
            onKeyDown={(event) => {
              if (event.key === "ArrowDown") {
                event.preventDefault();
                setMenuOpen(true);
                setActiveIndex((current) => Math.min(current + 1, Math.max(filtered.length - 1, 0)));
              }

              if (event.key === "ArrowUp") {
                event.preventDefault();
                setActiveIndex((current) => Math.max(current - 1, 0));
              }

              if (event.key === "Enter" && filtered[activeIndex]) {
                event.preventDefault();
                commitSelection(filtered[activeIndex]);
              }

              if (event.key === "Escape") {
                setMenuOpen(false);
                setQuery(selectedOption?.label ?? value ?? "");
              }
            }}
          />
          {value && onClear ? (
            <button
              type="button"
              className="lookup-icon-button"
              onClick={() => {
                onClear();
                setQuery("");
                setMenuOpen(false);
              }}
            >
              <X size={16} />
            </button>
          ) : null}
          <button
            type="button"
            className="lookup-icon-button"
            onClick={() => setModalOpen(true)}
            disabled={disabled}
            aria-label={`${label} search modal`}
          >
            <ChevronsUpDown size={16} />
          </button>
        </div>
        {helper && <p className="field-helper">{helper}</p>}

        {menuOpen && !disabled && (
          <div className="lookup-menu">
            {filtered.length === 0 ? (
              <div className="px-4 py-6 text-sm text-slate-400">{emptyText ?? "검색 결과가 없습니다."}</div>
            ) : (
              filtered.map((option, index) => (
                <button
                  key={option.value}
                  type="button"
                  className={`lookup-option ${index === activeIndex ? "lookup-option-active" : ""}`}
                  onMouseDown={(event) => {
                    event.preventDefault();
                    commitSelection(option);
                  }}
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="truncate font-semibold text-slate-900">{option.label}</span>
                      <span className="lookup-code">{option.value}</span>
                    </div>
                    {option.description && (
                      <p className="mt-1 truncate text-sm text-slate-500">{option.description}</p>
                    )}
                  </div>
                  {option.meta && <span className="lookup-meta">{option.meta}</span>}
                </button>
              ))
            )}
          </div>
        )}
      </div>

      <SearchModal
        open={modalOpen}
        title={searchTitle ?? label}
        description={searchDescription}
        options={options}
        initialQuery={query}
        emptyText={emptyText}
        onClose={() => setModalOpen(false)}
        onSelect={commitSelection}
      />
    </>
  );
}
