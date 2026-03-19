import { useEffect, useCallback } from "react";

interface ShortcutHandlers {
  onSearch?: () => void;
  onNew?: () => void;
  onSave?: () => void;
  onRefresh?: () => void;
}

export function useKeyboardShortcuts(handlers: ShortcutHandlers = {}) {
  const handler = useCallback(
    (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      const isInput = target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.isContentEditable;

      // Ctrl/Cmd + K: Global search / command palette
      if ((e.ctrlKey || e.metaKey) && e.key === "k") {
        e.preventDefault();
        handlers.onSearch?.();
        return;
      }

      // Ctrl/Cmd + N: New record (skip if in input)
      if ((e.ctrlKey || e.metaKey) && e.key === "n" && !isInput) {
        e.preventDefault();
        handlers.onNew?.();
        return;
      }

      // Ctrl/Cmd + S: Save
      if ((e.ctrlKey || e.metaKey) && e.key === "s") {
        e.preventDefault();
        handlers.onSave?.();
        return;
      }

      // F5 or Ctrl+R: Refresh data (not page)
      if (e.key === "F5" || ((e.ctrlKey || e.metaKey) && e.key === "r")) {
        if (handlers.onRefresh) {
          e.preventDefault();
          handlers.onRefresh();
        }
      }
    },
    [handlers]
  );

  useEffect(() => {
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [handler]);
}
