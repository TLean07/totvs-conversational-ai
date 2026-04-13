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

  const handleMarcarVisto = async (id: number) => {
    await api.alertas.marcarVisto(id);
    loadData();
  };

  const handleMarcarResolvido = async (id: number) => {
    await api.alertas.marcarResolvido(id);
    loadData();
  };

  return (
    <>
      <Navbar />
      <main className="max-w-5xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Alertas</h1>
          <div className="flex gap-2">
            <button
              onClick={() => setFilter("novos")}
              className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                filter === "novos"
                  ? "bg-[#1d4a7a] text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              Novos
            </button>
            <button
              onClick={() => setFilter("todos")}
              className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                filter === "todos"
                  ? "bg-[#1d4a7a] text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              Todos
            </button>
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-700" />
          </div>
        ) : (
          <AlertList
            alertas={alertas}
            onMarcarVisto={handleMarcarVisto}
            onMarcarResolvido={handleMarcarResolvido}
          />
        )}
      </main>
    </>
  );
}
