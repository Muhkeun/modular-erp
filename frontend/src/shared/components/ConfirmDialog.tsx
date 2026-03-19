import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { AlertTriangle } from "lucide-react";
import { clsx } from "clsx";

interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: "default" | "danger";
}

type ConfirmFn = (options: ConfirmOptions) => Promise<boolean>;

const ConfirmContext = createContext<ConfirmFn | null>(null);

export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{
    open: boolean;
    options: ConfirmOptions;
    resolve: ((v: boolean) => void) | null;
  }>({
    open: false,
    options: { title: "", message: "" },
    resolve: null,
  });

  const confirm: ConfirmFn = useCallback((options) => {
    return new Promise<boolean>((resolve) => {
      setState({ open: true, options, resolve });
    });
  }, []);

  const handleConfirm = () => {
    state.resolve?.(true);
    setState((s) => ({ ...s, open: false }));
  };

  const handleCancel = () => {
    state.resolve?.(false);
    setState((s) => ({ ...s, open: false }));
  };

  useEffect(() => {
    if (!state.open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") handleCancel();
      if (e.key === "Enter") handleConfirm();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  });

  const isDanger = state.options.variant === "danger";

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {state.open &&
        createPortal(
          <div className="fixed inset-0 z-[9998] flex items-center justify-center bg-slate-950/50 px-4 backdrop-blur-sm">
            <div className="w-full max-w-md rounded-[28px] border border-white/70 bg-white p-6 shadow-[0_32px_80px_rgba(15,23,42,0.28)]">
              <div className="flex items-start gap-4">
                {isDanger && (
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-red-50">
                    <AlertTriangle size={20} className="text-red-500" />
                  </div>
                )}
                <div>
                  <h3 className="text-lg font-semibold text-slate-900">{state.options.title}</h3>
                  <p className="mt-2 text-sm text-slate-500">{state.options.message}</p>
                </div>
              </div>
              <div className="mt-6 flex justify-end gap-3">
                <button className="btn-secondary" onClick={handleCancel}>
                  {state.options.cancelLabel || "Cancel"}
                </button>
                <button
                  className={clsx(isDanger ? "btn-danger" : "btn-primary")}
                  onClick={handleConfirm}
                  autoFocus
                >
                  {state.options.confirmLabel || "Confirm"}
                </button>
              </div>
            </div>
          </div>,
          document.body
        )}
    </ConfirmContext.Provider>
  );
}

export function useConfirm(): ConfirmFn {
  const ctx = useContext(ConfirmContext);
  if (!ctx) throw new Error("useConfirm must be used within ConfirmProvider");
  return ctx;
}
