"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { AlertaResponse, Severidade } from "@/lib/types";
import Navbar from "@/components/Navbar";
import AlertList from "@/components/AlertList";

export default function AlertasPage() {
  const [alertas, setAlertas] = useState<AlertaResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<"todos" | "novos">("novos");
  const [filtroSeveridade, setFiltroSeveridade] = useState<Severidade | "">("");

  const loadData = useCallback((showLoading = true) => {
    if (showLoading) setLoading(true);
    const fetcher = filter === "novos" ? api.alertas.novos() : api.alertas.listar();
    fetcher
      .then(setAlertas)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [filter]);

  useEffect(() => {
    const fetcher = filter === "novos" ? api.alertas.novos() : api.alertas.listar();
    fetcher
      .then(setAlertas)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [filter]);

  const handleMarcarVisto = async (id: string) => {
    await api.alertas.marcarVisto(id);
    loadData();
  };

  const handleMarcarResolvido = async (id: string) => {
    await api.alertas.marcarResolvido(id);
    loadData();
  };

  return (
    <>
      <Navbar />
      <main className="max-w-5xl mx-auto px-6 py-8">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Alertas</h1>
            <p className="text-sm text-slate-500 mt-1">Notificações geradas pela LeanIA</p>
          </div>
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 shrink-0">
            <div className="flex items-center gap-1.5">
              <span className="text-xs font-medium text-slate-400">Severidade:</span>
              {(["", "ALTO", "MEDIO", "BAIXO"] as const).map((s) => {
                const labels: Record<string, string> = { "": "Todas", ALTO: "Alta", MEDIO: "Média", BAIXO: "Baixa" };
                const activeColors: Record<string, string> = { "": "bg-slate-700 text-white", ALTO: "bg-rose-500 text-white", MEDIO: "bg-amber-500 text-white", BAIXO: "bg-sky-500 text-white" };
                const active = filtroSeveridade === s;
                return (
                  <button key={s} onClick={() => setFiltroSeveridade(s)}
                    className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${active ? activeColors[s] : "bg-slate-100 text-slate-500 hover:bg-slate-200"}`}>
                    {labels[s]}
                  </button>
                );
              })}
            </div>
          <div className="flex flex-wrap bg-slate-100 rounded-xl p-1 shrink-0">
            <button
              onClick={() => {
                setLoading(true);
                setFilter("novos");
              }}
              className={`flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${
                filter === "novos"
                  ? "bg-white text-slate-800 shadow-sm"
                  : "text-slate-500 hover:text-slate-700"
              }`}
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
              </svg>
              Novos
            </button>
            <button
              onClick={() => {
                setLoading(true);
                setFilter("todos");
              }}
              className={`flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${
                filter === "todos"
                  ? "bg-white text-slate-800 shadow-sm"
                  : "text-slate-500 hover:text-slate-700"
              }`}
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 6.75h12M8.25 12h12m-12 5.25h12M3.75 6.75h.007v.008H3.75V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM3.75 12h.007v.008H3.75V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.007v.008H3.75v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
              </svg>
              Todos
            </button>
          </div>
          </div>
        </div>

        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 gap-4">
            <div className="relative">
              <div className="w-12 h-12 rounded-full border-4 border-slate-200" />
              <div className="w-12 h-12 rounded-full border-4 border-blue-500 border-t-transparent animate-spin absolute top-0" />
            </div>
            <p className="text-slate-400 text-sm">Carregando alertas...</p>
          </div>
        ) : (() => {
          const filtrados = filtroSeveridade
            ? alertas.filter((a) => a.prioridade === filtroSeveridade)
            : alertas;
          return (
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
              <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
                <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
                  <p className="text-xs font-semibold uppercase tracking-wide text-amber-700">Alertas exibidos</p>
                  <p className="mt-1 text-2xl font-bold text-amber-900">{filtrados.length}</p>
                  <p className="mt-1 text-xs text-amber-700">Grupos consolidados gerados pela LeanIA.</p>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Ocorrências cobertas</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">
                    {filtrados.reduce((total, alerta) => total + Math.max(0, alerta.ocorrencias || 0), 0)}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">Soma interna dos alertas consolidados.</p>
                </div>
                <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4">
                  <p className="text-xs font-semibold uppercase tracking-wide text-rose-700">Alta prioridade</p>
                  <p className="mt-1 text-2xl font-bold text-rose-900">
                    {filtrados.filter((alerta) => alerta.prioridade === "ALTO").length}
                  </p>
                  <p className="mt-1 text-xs text-rose-700">Alertas que pedem ação imediata.</p>
                </div>
              </div>
              {filtrados.length !== alertas.length && (
                <p className="text-xs text-slate-400 mb-3">
                  Mostrando <span className="font-semibold text-slate-600">{filtrados.length}</span> de{" "}
                  <span className="font-semibold text-slate-600">{alertas.length}</span> alertas
                </p>
              )}
              <AlertList
                alertas={filtrados}
                onMarcarVisto={handleMarcarVisto}
                onMarcarResolvido={handleMarcarResolvido}
              />
            </div>
          );
        })()}
      </main>
    </>
  );
}
