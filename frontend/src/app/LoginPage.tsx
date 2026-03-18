import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAuth } from "../shared/hooks/useAuth";
import api from "../shared/api/client";
import { LayoutGrid } from "lucide-react";

export default function LoginPage() {
  const { login } = useAuth();
  const { t } = useTranslation();
  const [form, setForm] = useState({ tenantId: "DEFAULT", loginId: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await api.post("/api/v1/auth/login", form);
      if (res.data.success) {
        login(res.data.data);
        window.location.href = "/";
      }
    } catch {
      setError(t("auth.loginFailed"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen">
      {/* Left panel — branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-brand-600 via-brand-700 to-brand-900 items-center justify-center p-12">
        <div className="max-w-md text-white">
          <div className="flex items-center gap-3 mb-8">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/20 backdrop-blur">
              <LayoutGrid className="h-7 w-7 text-white" />
            </div>
            <span className="text-2xl font-bold tracking-tight">{t("auth.brandTitle")}</span>
          </div>
          <h1 className="text-4xl font-bold leading-tight mb-4">
            {t("auth.brandTitle")}
          </h1>
          <p className="text-brand-100 text-lg leading-relaxed whitespace-pre-line">
            {t("auth.brandDesc")}
          </p>
          <div className="mt-12 grid grid-cols-2 gap-4">
            {([
              ["nav.procurement", "Procurement"],
              ["nav.logistics", "Inventory"],
              ["nav.sales", "Sales"],
              ["nav.finance", "Finance"],
              ["nav.hr", "HR"],
              ["nav.quality", "Quality"],
            ] as const).map(([key]) => (
              <div key={key} className="rounded-lg bg-white/10 backdrop-blur px-4 py-3 text-sm font-medium">
                {t(key)}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right panel — login form */}
      <div className="flex flex-1 items-center justify-center p-8">
        <div className="w-full max-w-sm">
          <div className="lg:hidden flex items-center gap-3 mb-8">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600">
              <LayoutGrid className="h-6 w-6 text-white" />
            </div>
            <span className="text-xl font-bold text-slate-900">ModularERP</span>
          </div>

          <h2 className="text-2xl font-bold text-slate-900 mb-1">{t("auth.welcome")}</h2>
          <p className="text-slate-500 mb-8">{t("auth.signInDesc")}</p>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("auth.tenant")}</label>
              <input className="input" value={form.tenantId}
                onChange={(e) => setForm({ ...form, tenantId: e.target.value })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("auth.loginId")}</label>
              <input className="input" value={form.loginId} autoFocus
                onChange={(e) => setForm({ ...form, loginId: e.target.value })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("auth.password")}</label>
              <input className="input" type="password" value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>

            {error && (
              <div className="rounded-lg bg-red-50 text-red-700 text-sm px-4 py-3">{error}</div>
            )}

            <button type="submit" disabled={loading}
              className="btn-primary w-full py-2.5">
              {loading ? t("common.signingIn") : t("auth.signIn")}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
