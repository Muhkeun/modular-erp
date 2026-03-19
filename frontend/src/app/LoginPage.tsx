import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "../shared/hooks/useAuth";
import api from "../shared/api/client";
import { authApi } from "../shared/api/authApi";
import type { SsoProvider } from "../shared/api/authApi";
import { LayoutGrid } from "lucide-react";

const providerIcons: Record<string, { label: string; bg: string; text: string }> = {
  google: { label: "Google", bg: "bg-white border border-slate-200 hover:bg-slate-50", text: "text-slate-700" },
  "azure-ad": { label: "Microsoft", bg: "bg-[#2F2F2F] hover:bg-[#1a1a1a]", text: "text-white" },
  okta: { label: "Okta", bg: "bg-[#007DC1] hover:bg-[#006ba1]", text: "text-white" },
  saml: { label: "SAML SSO", bg: "bg-slate-700 hover:bg-slate-800", text: "text-white" },
};

export default function LoginPage() {
  const { login } = useAuth();
  const { t } = useTranslation();
  const [form, setForm] = useState({ tenantId: "DEFAULT", loginId: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const { data: ssoProviders } = useQuery({
    queryKey: ["sso-providers"],
    queryFn: () => authApi.getSsoProviders(),
    select: (res) => (res.data?.data || []).filter((p: SsoProvider) => p.enabled),
    retry: false,
  });

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

  const handleSsoLogin = async (provider: SsoProvider) => {
    try {
      const res = await authApi.ssoAuthorize(provider.id);
      if (res.data.success && res.data.data?.redirectUrl) {
        window.location.href = res.data.data.redirectUrl;
      }
    } catch {
      setError(t("auth.ssoFailed"));
    }
  };

  const enabledProviders = ssoProviders || [];

  return (
    <div className="flex min-h-screen">
      {/* Left panel -- branding */}
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

      {/* Right panel -- login form */}
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

          {/* SSO Buttons */}
          {enabledProviders.length > 0 && (
            <div className="space-y-3 mb-6">
              {enabledProviders.map((provider: SsoProvider) => {
                const style = providerIcons[provider.id] || providerIcons.saml;
                return (
                  <button
                    key={provider.id}
                    type="button"
                    onClick={() => handleSsoLogin(provider)}
                    className={`w-full flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${style.bg} ${style.text}`}
                  >
                    {t("auth.signInWith", { provider: style.label })}
                  </button>
                );
              })}
              <div className="relative my-4">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-slate-200" />
                </div>
                <div className="relative flex justify-center text-xs">
                  <span className="bg-white px-4 text-slate-400">{t("auth.orContinueWith")}</span>
                </div>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("auth.tenant")}</label>
              <input className="input" data-testid="tenant" value={form.tenantId}
                onChange={(e) => setForm({ ...form, tenantId: e.target.value })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("auth.loginId")}</label>
              <input className="input" data-testid="login-id" value={form.loginId} autoFocus
                onChange={(e) => setForm({ ...form, loginId: e.target.value })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("auth.password")}</label>
              <input className="input" data-testid="password" type="password" value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>

            {error && (
              <div data-testid="login-error" className="rounded-lg bg-red-50 text-red-700 text-sm px-4 py-3">{error}</div>
            )}

            <button type="submit" data-testid="login-button" disabled={loading}
              className="btn-primary w-full py-2.5">
              {loading ? t("common.signingIn") : t("auth.signIn")}
            </button>

            <div className="text-center">
              <button type="button" className="text-sm text-brand-600 hover:text-brand-700 font-medium">
                {t("auth.forgotPassword")}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
