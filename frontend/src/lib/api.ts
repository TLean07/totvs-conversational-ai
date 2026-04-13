const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(error.error || "Erro na requisição");
  }
  return res.json();
}

export const api = {
  dashboard: () => request<import("./types").DashboardResponse>("/dashboard"),

  transcricoes: {
    listar: () => request<import("./types").TranscricaoResponse[]>("/transcricoes"),
    buscar: (id: number) =>
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
    deletar: (id: number) =>
      request<void>(`/transcricoes/${id}`, { method: "DELETE" }),
  },

  alertas: {
    listar: () => request<import("./types").AlertaResponse[]>("/alertas"),
    novos: () => request<import("./types").AlertaResponse[]>("/alertas/novos"),
    marcarVisto: (id: number) =>
      request<import("./types").AlertaResponse>(`/alertas/${id}/visto`, {
        method: "PATCH",
      }),
    marcarResolvido: (id: number) =>
      request<import("./types").AlertaResponse>(`/alertas/${id}/resolvido`, {
        method: "PATCH",
      }),
  },
};
