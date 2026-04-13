"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { DashboardResponse, Sentimento } from "@/lib/types";
import Navbar from "@/components/Navbar";
import StatsCard from "@/components/StatsCard";
import AlertList from "@/components/AlertList";
import SentimentBadge from "@/components/SentimentBadge";
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";

const SENTIMENT_COLORS: Record<string, string> = {
  POSITIVO: "#22c55e",
  NEGATIVO: "#ef4444",
  NEUTRO: "#9ca3af",
  MISTO: "#eab308",
};

const RISK_COLORS: Record<string, string> = {
  CHURN: "#ef4444",
  CONCORRENCIA: "#f97316",
  INSATISFACAO: "#eab308",
  PERDA_CONFIANCA: "#8b5cf6",
};

export default function DashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .dashboard()
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="flex items-center justify-center h-96">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700" />
        </div>
      </>
    );
  }

  if (error || !data) {
    return (
      <>
        <Navbar />
        <div className="max-w-7xl mx-auto px-4 py-8">
          <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
            <p className="text-red-700 font-medium">Erro ao carregar dashboard</p>
            <p className="text-red-500 text-sm mt-1">{error || "Dados indisponiveis"}</p>
            <p className="text-gray-500 text-xs mt-3">
              Verifique se o backend esta rodando em http://localhost:8080
            </p>
          </div>
        </div>
      </>
    );
  }

  const sentimentoData = Object.entries(data.sentimentoDistribuicao).map(
    ([name, value]) => ({ name, value })
  );

  const riscoData = Object.entries(data.riscoPorTipo).map(([name, value]) => ({
    name,
    value,
  }));

  return (
    <>
      <Navbar />
      <main className="max-w-7xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">
          Dashboard - Inteligencia Conversacional
        </h1>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
          <StatsCard title="Transcricoes" value={data.totalTranscricoes} icon="📝" color="blue" />
          <StatsCard title="Processadas" value={data.transcricoesProcessadas} icon="✅" color="green" />
          <StatsCard title="Oportunidades" value={data.totalOportunidades} icon="💰" color="purple" />
          <StatsCard title="Riscos" value={data.totalRiscos} icon="⚠️" color="red" />
          <StatsCard title="Alertas Novos" value={data.alertasNovos} icon="🔔" color="yellow" />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-sm border p-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">
              Distribuicao de Sentimento
            </h2>
            {sentimentoData.length > 0 ? (
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie data={sentimentoData} cx="50%" cy="50%" outerRadius={90} dataKey="value"
                    label={({ name, value }) => `${name}: ${value}`}>
                    {sentimentoData.map((entry) => (
                      <Cell key={entry.name} fill={SENTIMENT_COLORS[entry.name] || "#ccc"} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-gray-400 text-center py-12">Sem dados</p>
            )}
          </div>

          <div className="bg-white rounded-xl shadow-sm border p-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">Riscos por Tipo</h2>
            {riscoData.length > 0 ? (
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={riscoData}>
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="value" name="Quantidade">
                    {riscoData.map((entry) => (
                      <Cell key={entry.name} fill={RISK_COLORS[entry.name] || "#6b7280"} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-gray-400 text-center py-12">Sem dados</p>
            )}
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border p-6 mb-8">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Alertas Recentes</h2>
          <AlertList alertas={data.alertasRecentes} />
        </div>

        <div className="bg-white rounded-xl shadow-sm border p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Ultimas Transcricoes</h2>
          {data.ultimasTranscricoes.length > 0 ? (
            <div className="space-y-3">
              {data.ultimasTranscricoes.map((t) => (
                <a key={t.id} href={`/transcricoes/${t.id}`}
                  className="block border rounded-lg p-4 hover:bg-gray-50 transition-colors">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-3">
                      <span className="font-medium text-gray-800">
                        {t.cliente || `Transcricao #${t.id}`}
                      </span>
                      {t.analise && (
                        <SentimentBadge sentimento={t.analise.sentimentoGeral as Sentimento} />
                      )}
                    </div>
                    <span className="text-xs text-gray-400">
                      {new Date(t.dataCriacao).toLocaleString("pt-BR")}
                    </span>
                  </div>
                  <p className="text-sm text-gray-600 line-clamp-2">{t.textoOriginal}</p>
                </a>
              ))}
            </div>
          ) : (
            <p className="text-gray-400 text-center py-8">Nenhuma transcricao registrada.</p>
          )}
        </div>
      </main>
    </>
  );
}
