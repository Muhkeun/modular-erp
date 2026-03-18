import { create } from "zustand";

interface AuthState {
  token: string | null;
  userId: string | null;
  name: string | null;
  tenantId: string | null;
  roles: string[];
  locale: string;
  isAuthenticated: boolean;
  login: (data: LoginData) => void;
  logout: () => void;
}

interface LoginData {
  token: string;
  userId: string;
  name: string;
  tenantId: string;
  roles: string[];
  locale: string;
}

export const useAuth = create<AuthState>((set) => ({
  token: localStorage.getItem("token"),
  userId: localStorage.getItem("userId"),
  name: localStorage.getItem("userName"),
  tenantId: localStorage.getItem("tenantId"),
  roles: JSON.parse(localStorage.getItem("roles") || "[]"),
  locale: localStorage.getItem("locale") || "ko",
  isAuthenticated: !!localStorage.getItem("token"),

  login: (data) => {
    localStorage.setItem("token", data.token);
    localStorage.setItem("userId", data.userId);
    localStorage.setItem("userName", data.name);
    localStorage.setItem("tenantId", data.tenantId);
    localStorage.setItem("roles", JSON.stringify(data.roles));
    localStorage.setItem("locale", data.locale);
    set({ ...data, isAuthenticated: true });
  },

  logout: () => {
    localStorage.clear();
    set({
      token: null, userId: null, name: null, tenantId: null,
      roles: [], locale: "ko", isAuthenticated: false,
    });
  },
}));
