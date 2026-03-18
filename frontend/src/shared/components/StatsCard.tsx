import { ReactNode } from "react";
import { clsx } from "clsx";

interface StatsCardProps {
  title: string;
  value: string | number;
  change?: string;
  changeType?: "positive" | "negative" | "neutral";
  icon?: ReactNode;
  iconColor?: string;
}

export default function StatsCard({
  title, value, change, changeType = "neutral", icon, iconColor = "bg-brand-50 text-brand-600",
}: StatsCardProps) {
  return (
    <div className="card p-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-slate-500">{title}</p>
          <p className="mt-2 text-3xl font-bold text-slate-900 tracking-tight">{value}</p>
          {change && (
            <p className={clsx("mt-2 text-sm font-medium", {
              "text-emerald-600": changeType === "positive",
              "text-red-600": changeType === "negative",
              "text-slate-500": changeType === "neutral",
            })}>
              {change}
            </p>
          )}
        </div>
        {icon && (
          <div className={clsx("flex h-12 w-12 items-center justify-center rounded-xl", iconColor)}>
            {icon}
          </div>
        )}
      </div>
    </div>
  );
}
