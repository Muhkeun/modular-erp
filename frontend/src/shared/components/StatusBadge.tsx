import { useTranslation } from "react-i18next";
import { clsx } from "clsx";

const statusStyles: Record<string, string> = {
  DRAFT: "bg-slate-100 text-slate-600",
  SUBMITTED: "bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-600/20",
  APPROVED: "bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20",
  REJECTED: "bg-red-50 text-red-700 ring-1 ring-inset ring-red-600/20",
  IN_PROGRESS: "bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-600/20",
  COMPLETED: "bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20",
  CANCELLED: "bg-slate-100 text-slate-500",
  PLANNED: "bg-slate-100 text-slate-600",
  RELEASED: "bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-600/20",
  SENT: "bg-purple-50 text-purple-700 ring-1 ring-inset ring-purple-600/20",
  POSTED: "bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20",
  REVERSED: "bg-red-50 text-red-700 ring-1 ring-inset ring-red-600/20",
  CLOSED: "bg-slate-100 text-slate-500",
  CONFIRMED: "bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20",
  PENDING: "bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-600/20",
  ACTIVE: "bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20",
  OPEN: "bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-600/20",
};

interface StatusBadgeProps {
  status: string;
  className?: string;
}

export default function StatusBadge({ status, className }: StatusBadgeProps) {
  const { t } = useTranslation();
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        statusStyles[status] || "bg-slate-100 text-slate-600",
        className
      )}
    >
      {t(`status.${status}`, status)}
    </span>
  );
}
