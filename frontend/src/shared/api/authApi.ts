import api from "./client";

export interface SsoProvider {
  id: string;
  name: string;
  enabled: boolean;
  authorizeUrl: string | null;
}

export const authApi = {
  getSsoProviders: () =>
    api.get<{ success: boolean; data: SsoProvider[] }>("/api/v1/auth/sso/providers"),

  ssoAuthorize: (provider: string) =>
    api.get<{ success: boolean; data: { redirectUrl: string } }>(`/api/v1/auth/sso/authorize/${provider}`),

  ssoCallback: (provider: string, code: string, state?: string) =>
    api.post<{ success: boolean; data: { token: string } }>(`/api/v1/auth/sso/callback/${provider}`, { code, state }),
};
