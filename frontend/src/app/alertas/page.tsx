"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { AlertaResponse } from "@/lib/types";
import Navbar from "@/components/Navbar";
import AlertList from "@/components/AlertList";

export default function AlertasPage() {
  const [alertas, setAlertas] = useState<AlertaResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<"todos" | "novos">("novos");

  const loadData = () => {
    setLoading(true);
    const fetcher = filter === "novos" ? api.alertas.novos() : api.alertas.listar();
    fetcher
      .then(setAlertas)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
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
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Alertas</h1>
            <p className="text-sm text-slate-500 mt-1">Notificacoes geradas pela analise de IA</p>
          </div>
          <div className="flex bg-slate-100 rounded-xl p-1">
            <button
              onClick={() => setFilter("novos")}
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
              onClick={() => setFilter("todos")}
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

        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 gap-4">
            <div className="relative">
              <div className="w-12 h-12 rounded-full border-4 border-slate-200" />
              <div className="w-12 h-12 rounded-full border-4 border-blue-500 border-t-transparent animate-spin absolute top-0" />
            </div>
            <p className="text-slate-400 text-sm">Carregando alertas...</p>
          </div>
        ) : (
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
            <AlertList
              alertas={alertas}
              onMarcarVisto={handleMarcarVisto}
              onMarcarResolvido={handleMarcarResolvido}
            />
          </div>
        )}
      </main>
    </>
  );
}
