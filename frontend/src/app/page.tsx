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
} from "recharts";

const SENTIMENT_COLORS: Record<string, string> = {
  POSITIVO: "#10b981",
  NEGATIVO: "#f43f5e",
  NEUTRO: "#94a3b8",
  MISTO: "#f59e0b",
};

const SENTIMENT_LABELS: Record<string, string> = {
  POSITIVO: "Positivo",
  NEGATIVO: "Negativo",
  NEUTRO: "Neutro",
  MISTO: "Misto",
};

const RISK_COLORS: Record<string, string> = {
  CHURN: "#f43f5e",
  CONCORRENCIA: "#f97316",
  INSATISFACAO: "#eab308",
  PERDA_CONFIANCA: "#8b5cf6",
};

const RISK_LABELS: Record<string, string> = {
  CHURN: "Churn",
  CONCORRENCIA: "Concorrencia",
  INSATISFACAO: "Insatisfacao",
  PERDA_CONFIANCA: "Confianca",
};

function LoadingScreen() {
  return (
    <>
      <Navbar />
      <div className="flex flex-col items-center justify-center h-96 gap-4">
        <div className="relative">
          <div className="w-16 h-16 rounded-full border-4 border-slate-200" />
          <div className="w-16 h-16 rounded-full border-4 border-blue-500 border-t-transparent animate-spin absolute top-0" />
        </div>
        <p className="text-slate-400 text-sm font-medium">Carregando dashboard...</p>
      </div>
    </>
  );
}

export default function DashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filtroCliente, setFiltroCliente] = useState("");

  useEffect(() => {
    api
      .dashboard()
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingScreen />;

  if (error || !data) {
    return (
      <>
        <Navbar />
        <div className="max-w-2xl mx-auto px-4 py-16">
          <div className="bg-white rounded-2xl shadow-sm border border-rose-200 p-8 text-center">
            <div className="w-16 h-16 bg-rose-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-rose-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
              </svg>
            </div>
            <h2 className="text-lg font-semibold text-slate-800 mb-2">Erro ao carregar dashboard</h2>
            <p className="text-sm text-slate-500 mb-4">{error || "Dados indisponiveis"}</p>
            <p className="text-xs text-slate-400">
              Verifique se o backend esta rodando em http://localhost:8080
            </p>
          </div>
        </div>
      </>
    );
  }

  const sentimentoData = Object.entries(data.sentimentoDistribuicao).map(
    ([name, value]) => ({ name: SENTIMENT_LABELS[name] || name, value, key: name })
  );

  const riscoData = Object.entries(data.riscoPorTipo).map(([name, value]) => ({
    name: RISK_LABELS[name] || name,
    value,
    key: name,
  }));

  return (
    <>
      <Navbar />
      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Dashboard</h1>
          <p className="text-sm text-slate-500 mt-1">Visao geral da inteligencia conversacional</p>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
          <StatsCard
            title="Transcricoes"
            value={data.totalTranscricoes}
            color="blue"
            icon={<svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>}
          />
          <StatsCard
            title="Processadas"
            value={data.transcricoesProcessadas}
            color="green"
            icon={<svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
          />
          <StatsCard
            title="Oportunidades"
            value={data.totalOportunidades}
            color="purple"
            icon={<svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M2.25 18.75a60.07 60.07 0 0115.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 013 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 00-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 01-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 003 15h-.75M15 10.5a3 3 0 11-6 0 3 3 0 016 0zm3 0h.008v.008H18V10.5zm-12 0h.008v.008H6V10.5z" /></svg>}
          />
          <StatsCard
            title="Riscos"
            value={data.totalRiscos}
            color="red"
            icon={<svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" /></svg>}
          />
          <StatsCard
            title="Alertas Novos"
            value={data.alertasNovos}
            color="yellow"
            icon={<svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}><path strokeLinecap="round" strokeLinejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" /></svg>}
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
            <h2 className="text-base font-semibold text-slate-700 mb-1">Distribuicao de Sentimento</h2>
            <p className="text-xs text-slate-400 mb-4">Analise das transcricoes processadas</p>
            {sentimentoData.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={sentimentoData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={95}
                    paddingAngle={4}
                    dataKey="value"
                    label={({ name, value }) => `${name}: ${value}`}
                    labelLine={{ stroke: "#94a3b8", strokeWidth: 1 }}
                  >
                    {sentimentoData.map((entry) => (
                      <Cell
                        key={entry.key}
                        fill={SENTIMENT_COLORS[entry.key] || "#ccc"}
                        stroke="none"
                      />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#fff",
                      border: "1px solid #e2e8f0",
                      borderRadius: "12px",
                      boxShadow: "0 4px 6px -1px rgb(0 0 0 / 0.1)",
                      fontSize: "13px",
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex flex-col items-center justify-center py-16 text-slate-300">
                <svg className="w-10 h-10 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 6a7.5 7.5 0 107.5 7.5h-7.5V6z" />
                  <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 10.5H21A7.5 7.5 0 0013.5 3v7.5z" />
                </svg>
                <p className="text-sm">Sem dados para exibir</p>
              </div>
            )}
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
            <h2 className="text-base font-semibold text-slate-700 mb-1">Riscos por Tipo</h2>
            <p className="text-xs text-slate-400 mb-4">Classificacao dos riscos identificados</p>
            {riscoData.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={riscoData} barCategoryGap="25%">
                  <XAxis
                    dataKey="name"
                    tick={{ fontSize: 12, fill: "#64748b" }}
                    axisLine={{ stroke: "#e2e8f0" }}
                    tickLine={false}
                  />
                  <YAxis
                    allowDecimals={false}
                    tick={{ fontSize: 12, fill: "#64748b" }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#fff",
                      border: "1px solid #e2e8f0",
                      borderRadius: "12px",
                      boxShadow: "0 4px 6px -1px rgb(0 0 0 / 0.1)",
                      fontSize: "13px",
                    }}
                  />
                  <Bar dataKey="value" name="Quantidade" radius={[6, 6, 0, 0]}>
                    {riscoData.map((entry) => (
                      <Cell key={entry.key} fill={RISK_COLORS[entry.key] || "#6b7280"} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex flex-col items-center justify-center py-16 text-slate-300">
                <svg className="w-10 h-10 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
                </svg>
                <p className="text-sm">Sem dados para exibir</p>
              </div>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-base font-semibold text-slate-700">Alertas Recentes</h2>
                <p className="text-xs text-slate-400 mt-0.5">Ultimos alertas gerados pela IA</p>
              </div>
              <a href="/alertas" className="text-xs text-blue-600 hover:text-blue-700 font-medium hover:underline">
                Ver todos
              </a>
            </div>
            <AlertList alertas={data.alertasRecentes} />
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 flex flex-col">
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4 mb-4">
              <div>
                <h2 className="text-base font-semibold text-slate-700">Transcricoes</h2>
                <p className="text-xs text-slate-400 mt-0.5">Filtre as reunioes recentes analisadas</p>
              </div>
              <div className="flex items-center gap-3 w-full sm:w-auto">
                <div className="relative w-full">
                  <svg className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
                  </svg>
                  <input
                    type="text"
                    placeholder="Nome do cliente..."
                    value={filtroCliente}
                    onChange={(e) => setFiltroCliente(e.target.value)}
                    className="w-full sm:w-48 border border-slate-200 bg-slate-50 rounded-xl pl-8 pr-3 py-1.5 text-xs focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 focus:bg-white outline-none transition-all shadow-sm"
                  />
                </div>
                <a href="/transcricoes" className="text-xs text-blue-600 hover:text-blue-700 font-medium hover:underline shrink-0 hidden sm:block">
                  Ver todas
                </a>
              </div>
            </div>
            {data.ultimasTranscricoes.length > 0 ? (
              <div className="space-y-3 overflow-y-auto max-h-[420px] pr-1">
                {data.ultimasTranscricoes
                  .filter(t => !filtroCliente || t.cliente?.toLowerCase().includes(filtroCliente.toLowerCase()))
                  .map((t) => (
                  <a
                    key={t.id}
                    href={`/transcricoes/${t.id}`}
                    className="block rounded-xl border border-slate-200 p-4 hover:border-blue-300 hover:shadow-sm transition-all duration-200 group"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2 mb-2">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-sm text-slate-700 group-hover:text-blue-700 transition-colors">
                          {t.cliente || `Transcricao #${t.id}`}
                        </span>
                        {t.analise && (
                          <SentimentBadge sentimento={t.analise.sentimentoGeral as Sentimento} />
                        )}
                      </div>
                      <span className="text-xs text-slate-400">
                        {new Date(t.dataCriacao).toLocaleString("pt-BR")}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 line-clamp-2 leading-relaxed">{t.textoOriginal}</p>
                  </a>
                ))}
                {data.ultimasTranscricoes.filter(t => !filtroCliente || t.cliente?.toLowerCase().includes(filtroCliente.toLowerCase())).length === 0 && (
                  <div className="text-center py-6 text-slate-400 text-sm">
                    Nenhuma reuniao encontrada para este filtro.
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center py-12">
                <svg className="w-12 h-12 text-slate-300 mx-auto mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                </svg>
                <p className="text-slate-400 text-sm">Nenhuma transcricao registrada</p>
              </div>
            )}
          </div>
        </div>
      </main>
    </>
  );
}
