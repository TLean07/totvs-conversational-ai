const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

let accessToken: string | null = null;
let currentUser: import("./types").AuthUser | null = null;
let refreshing: Promise<import("./types").AuthResponse | null> | null = null;

function setSession(session: import("./types").AuthResponse | null) {
  accessToken = session?.accessToken ?? null;
  currentUser = session?.user ?? null;
}

function redirectToLogin() {
  if (typeof window !== "undefined" && window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}

async function refreshSession() {
  if (!refreshing) {
    refreshing = fetch(`${API_URL}/auth/refresh`, {
      method: "POST",
      credentials: "include",
    })
      .then(async (res) => {
        if (!res.ok) return null;
        return (await res.json()) as import("./types").AuthResponse;
      })
      .then((session) => {
        setSession(session);
        return session;
      })
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

async function request<T>(path: string, options?: RequestInit, retry = true): Promise<T> {
  if (!path.startsWith("/auth/") && !accessToken) {
    const refreshed = await refreshSession();
    if (!refreshed) {
      setSession(null);
      redirectToLogin();
      throw new Error("Sessao expirada. Faca login novamente.");
    }
  }

  const headers = new Headers(options?.headers);
  headers.set("Content-Type", "application/json");
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    credentials: "include",
    headers,
  });

  if (res.status === 401 && retry && !path.startsWith("/auth/login")) {
    const refreshed = await refreshSession();
    if (refreshed) {
      return request<T>(path, options, false);
    }
    setSession(null);
    redirectToLogin();
  }

  if (!res.ok) {
    const error = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(error.error || "Erro na requisicao");
  }

  if (res.status === 204) {
    return undefined as unknown as T;
  }

  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as unknown as T);
}

export const api = {
  auth: {
    login: async (email: string, password: string) => {
      const session = await request<import("./types").AuthResponse>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setSession(session);
      return session;
    },
    refresh: refreshSession,
    logout: async () => {
      await request<void>("/auth/logout", { method: "POST" }, false).catch(() => {});
      setSession(null);
      redirectToLogin();
    },
    user: () => currentUser,
  },

  users: {
    listar: () => request<import("./types").UserResponse[]>("/users"),
    criar: (data: {
      email: string;
      name: string;
      password: string;
      role: import("./types").UserRole;
    }) =>
      request<import("./types").UserResponse>("/users", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    desativar: (id: string) =>
      request<import("./types").UserResponse>(`/users/${id}/deactivate`, {
        method: "PATCH",
      }),
  },

  dashboard: () => request<import("./types").DashboardResponse>("/dashboard"),

  transcricoes: {
    listar: () => request<import("./types").TranscricaoResponse[]>("/transcricoes"),
    buscar: (id: string) =>
      request<import("./types").TranscricaoResponse>(`/transcricoes/${id}`),
    criar: (data: {
      textoOriginal: string;
      participantes?: string;
      dataReuniao?: string;
      duracaoMinutos?: number;
      cliente?: string;
      vendedor?: string;
    }) =>
      request<import("./types").TranscricaoResponse>("/transcricoes", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    deletar: (id: string) =>
      request<void>(`/transcricoes/${id}`, { method: "DELETE" }),
  },

  alertas: {
    listar: () => request<import("./types").AlertaResponse[]>("/alertas"),
    novos: () => request<import("./types").AlertaResponse[]>("/alertas/novos"),
    marcarVisto: (id: string) =>
      request<import("./types").AlertaResponse>(`/alertas/${id}/visto`, {
        method: "PATCH",
      }),
    marcarResolvido: (id: string) =>
      request<import("./types").AlertaResponse>(`/alertas/${id}/resolvido`, {
        method: "PATCH",
      }),
  },
};
