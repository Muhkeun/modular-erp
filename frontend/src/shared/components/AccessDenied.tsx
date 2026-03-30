import { ShieldAlert } from "lucide-react";

interface AccessDeniedProps {
  title?: string;
  description?: string;
}

export default function AccessDenied({
  title = "Access Denied",
  description = "You do not have access to this workspace.",
}: AccessDeniedProps) {
  return (
    <div className="flex min-h-[320px] items-center justify-center">
      <div className="w-full max-w-xl rounded-[28px] border border-slate-200 bg-white/90 p-8 text-center shadow-[0_18px_60px_rgba(15,23,42,0.08)]">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-red-500">
          <ShieldAlert size={24} />
        </div>
        <h2 className="mt-5 text-2xl font-semibold tracking-tight text-slate-950">{title}</h2>
        <p className="mt-3 text-sm leading-6 text-slate-500">{description}</p>
      </div>
    </div>
  );
}
