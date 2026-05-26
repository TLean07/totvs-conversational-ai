"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import type { DashboardResponse, Sentimento, StatusTranscricao } from "@/lib/types";
import Navbar from "@/components/Navbar";
import StatsCard from "@/components/StatsCard";
import AlertList from "@/components/AlertList";
import SentimentBadge from "@/components/SentimentBadge";
import ChartCard from "@/components/dashboard/ChartCard";
import DashboardErrorState from "@/components/dashboard/DashboardErrorState";
import DashboardHeader from "@/components/dashboard/DashboardHeader";
import DashboardSkeleton from "@/components/dashboard/DashboardSkeleton";
import EmptyState from "@/components/dashboard/EmptyState";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
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
  CHURN: "#e11d48",
  CONCORRENCIA: "#f97316",
  INSATISFACAO: "#eab308",
  PERDA_CONFIANCA: "#7c3aed",
};

const RISK_LABELS: Record<string, string> = {
  CHURN: "Churn",
  CONCORRENCIA: "Concorrencia",
  INSATISFACAO: "Insatisfacao",
  PERDA_CONFIANCA: "Confianca",
};

const OPORTUNIDADE_COLORS: Record<string, string> = {
  UPSELL: "#7c3aed",
  CROSS_SELL: "#2563eb",
  NOVO_PRODUTO: "#059669",
};

const OPORTUNIDADE_LABELS: Record<string, string> = {
  UPSELL: "Upsell",
  CROSS_SELL: "Cross-sell",
  NOVO_PRODUTO: "Novo produto",
};

const SENTIMENTO_FILTER: Array<{ value: Sentimento | ""; label: string; active: string }> = [
  { value: "", label: "Todos", active: "bg-slate-900 text-white" },
  { value: "POSITIVO", label: "Positivo", active: "bg-emerald-600 text-white" },
  { value: "NEGATIVO", label: "Negativo", active: "bg-rose-600 text-white" },
  { value: "NEUTRO", label: "Neutro", active: "bg-slate-600 text-white" },
  { value: "MISTO", label: "Misto", active: "bg-amber-500 text-white" },
];

const STATUS_FILTER: Array<{ value: StatusTranscricao | ""; label: string; active: string }> = [
  { value: "", label: "Todos", active: "bg-slate-900 text-white" },
  { value: "PROCESSADA", label: "Processada", active: "bg-emerald-600 text-white" },
  { value: "PROCESSANDO", label: "Processando", active: "bg-blue-600 text-white" },
  { value: "PENDENTE", label: "Pendente", active: "bg-amber-500 text-white" },
  { value: "ERRO", label: "Erro", active: "bg-rose-600 text-white" },
];

type ChartItem = {
  name: string;
  value: number;
  key: string;
};

function mapChartData(source: Record<string, number>, labels: Record<string, string>) {
  return Object.entries(source)
    .filter(([, value]) => value > 0)
    .map(([name, value]) => ({
      name: labels[name] || name,
      value,
      key: name,
    }));
}

function ChartLegend({ data, colors }: { data: ChartItem[]; colors: Record<string, string> }) {
  if (data.length === 0) return null;

  return (
    <div className="grid gap-2 sm:grid-cols-2">
      {data.map((entry) => (
        <div key={entry.key} className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 px-3 py-2">
          <div className="flex min-w-0 items-center gap-2">
            <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: colors[entry.key] || "#64748b" }} />
            <span className="truncate text-sm font-medium text-slate-600">{entry.name}</span>
          </div>
          <span className="text-sm font-bold text-slate-900">{entry.value}</span>
        </div>
      ))}
    </div>
  );
}

function metricIcon(path: string) {
  return (
    <svg aria-hidden="true" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.6}>
      <path strokeLinecap="round" strokeLinejoin="round" d={path} />
    </svg>
  );
}

export default function DashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [processingAlertId, setProcessingAlertId] = useState<string | null>(null);
  const [filtroCliente, setFiltroCliente] = useState("");
  const [filtroSentimento, setFiltroSentimento] = useState<Sentimento | "">("");
  const [filtroStatus, setFiltroStatus] = useState<StatusTranscricao | "">("");

  const loadData = useCallback(async (mode: "initial" | "refresh" = "initial") => {
    if (mode === "initial") {
      setLoading(true);
    } else {
      setRefreshing(true);
    }
    setError(null);
    setActionError(null);

    try {
      const response = await api.dashboard();
      setData(response);
      setLastUpdated(new Date());
    } catch (err) {
      const message = err instanceof Error ? err.message : "Erro ao carregar dashboard";
      if (mode === "initial") {
        setError(message);
      } else {
        setActionError(message);
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadData("initial");
  }, [loadData]);

  const handleMarcarVisto = async (id: string) => {
    setProcessingAlertId(id);
    setActionError(null);
    try {
      await api.alertas.marcarVisto(id);
      await loadData("refresh");
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Nao foi possivel atualizar o alerta.");
    } finally {
      setProcessingAlertId(null);
    }
  };

  const handleMarcarResolvido = async (id: string) => {
    setProcessingAlertId(id);
    setActionError(null);
    try {
      await api.alertas.marcarResolvido(id);
      await loadData("refresh");
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Nao foi possivel resolver o alerta.");
    } finally {
      setProcessingAlertId(null);
    }
  };

  const sentimentoData = useMemo(
    () => (data ? mapChartData(data.sentimentoDistribuicao, SENTIMENT_LABELS) : []),
    [data]
  );

  const riscoData = useMemo(
    () => (data ? mapChartData(data.riscoPorTipo, RISK_LABELS) : []),
    [data]
  );

  const oportunidadeData = useMemo(
    () => (data ? mapChartData(data.oportunidadePorTipo, OPORTUNIDADE_LABELS) : []),
    [data]
  );

  const transcricoesFiltradas = useMemo(() => {
    if (!data) return [];

    return data.ultimasTranscricoes.filter((transcricao) => {
      if (filtroCliente && !transcricao.cliente?.toLowerCase().includes(filtroCliente.toLowerCase())) return false;
      if (filtroSentimento && transcricao.analise?.sentimentoGeral !== filtroSentimento) return false;
      if (filtroStatus && transcricao.status !== filtroStatus) return false;
      return true;
    });
  }, [data, filtroCliente, filtroSentimento, filtroStatus]);

  const activeFilterCount = [filtroCliente, filtroSentimento, filtroStatus].filter(Boolean).length;
  const alertasConsolidados = data?.alertasConsolidados || data?.alertasNovos || 0;
  const basesConsolidadas = data?.transcricoesProcessadas || data?.totalTranscricoes || 0;
  const processingRate = data?.totalTranscricoes
    ? Math.round((data.transcricoesProcessadas / data.totalTranscricoes) * 100)
    : 0;

  const clearFilters = () => {
    setFiltroCliente("");
    setFiltroSentimento("");
    setFiltroStatus("");
  };

  if (loading && !data) return <DashboardSkeleton />;

  if (error || !data) {
    return <DashboardErrorState message={error || "Dados indisponiveis"} onRetry={() => loadData("initial")} />;
  }

  return (
    <>
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <DashboardHeader lastUpdated={lastUpdated} refreshing={refreshing} onRefresh={() => loadData("refresh")} />

        {actionError && (
          <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800">
            {actionError}
          </div>
        )}

        <section aria-label="Indicadores principais" className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          <StatsCard
            title="Transcricoes"
            value={data.totalTranscricoes}
            subtitle="Total registrado"
            color="blue"
            href="/transcricoes"
            icon={metricIcon("M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z")}
          />
          <StatsCard
            title="Processadas"
            value={`${processingRate}%`}
            subtitle={`${data.transcricoesProcessadas} concluidas`}
            color="green"
            href="/transcricoes?status=PROCESSADA"
            icon={metricIcon("M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z")}
          />
          <StatsCard
            title="Bases consolidadas"
            value={basesConsolidadas}
            subtitle="JSONs/documentos unicos"
            color="blue"
            icon={metricIcon("M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5")}
          />
          <StatsCard
            title="Oportunidades"
            value={data.totalOportunidades}
            subtitle="Consolidadas"
            color="purple"
            href="/oportunidades"
            icon={metricIcon("M2.25 18.75a60.07 60.07 0 0115.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 013 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 00-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 01-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 003 15h-.75M15 10.5a3 3 0 11-6 0 3 3 0 016 0z")}
          />
          <StatsCard
            title="Riscos"
            value={data.totalRiscos}
            subtitle="Consolidados"
            color="red"
            href="/riscos"
            icon={metricIcon("M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z")}
          />
          <StatsCard
            title="Alertas novos"
            value={alertasConsolidados}
            subtitle="Consolidados"
            color="yellow"
            href="/alertas"
            icon={metricIcon("M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0")}
          />
        </section>

        <section className="mb-8 grid grid-cols-1 gap-4 lg:grid-cols-3">
          <div className="rounded-2xl border border-rose-200 bg-rose-50 p-5">
            <p className="text-sm font-semibold text-rose-700">Riscos consolidados</p>
            <p className="mt-2 text-3xl font-bold text-rose-800">{data.totalRiscos}</p>
            <p className="mt-1 text-sm text-rose-700/80">Categorias de risco agrupadas para priorizacao.</p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/60">
            <p className="text-sm font-semibold text-slate-600">Oportunidades consolidadas</p>
            <p className="mt-2 text-3xl font-bold text-slate-900">{data.totalOportunidades}</p>
            <p className="mt-1 text-sm text-slate-500">Categorias comerciais agrupadas pela LeanIA.</p>
          </div>
          <div className="rounded-2xl border border-blue-200 bg-blue-50 p-5">
            <p className="text-sm font-semibold text-blue-700">Alertas consolidados</p>
            <p className="mt-2 text-3xl font-bold text-blue-900">{alertasConsolidados}</p>
            <p className="mt-1 text-sm text-blue-700/80">Alertas acionaveis sem duplicar ocorrencias internas.</p>
          </div>
        </section>

        <section className="mb-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
          <ChartCard title="Distribuicao de sentimento" description="Sentimento consolidado por transcricao analisada.">
            {sentimentoData.length > 0 ? (
              <>
                <div className="h-72" aria-hidden="true">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={sentimentoData} cx="50%" cy="50%" innerRadius={70} outerRadius={105} paddingAngle={4} dataKey="value">
                        {sentimentoData.map((entry) => (
                          <Cell key={entry.key} fill={SENTIMENT_COLORS[entry.key] || "#64748b"} stroke="none" />
                        ))}
                      </Pie>
                      <Tooltip contentStyle={{ borderRadius: "14px", border: "1px solid #e2e8f0", fontSize: "13px" }} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <p className="sr-only">
                  Distribuicao de sentimento consolidada: {sentimentoData.map((item) => `${item.name} ${item.value}`).join(", ")}.
                </p>
                <ChartLegend data={sentimentoData} colors={SENTIMENT_COLORS} />
              </>
            ) : (
              <EmptyState compact title="Sem sentimentos ainda" description="Envie transcricoes para a LeanIA gerar a distribuicao." />
            )}
          </ChartCard>

          <ChartCard title="Riscos por tipo" description="Categorias consolidadas de risco, sem duplicar ocorrencias internas.">
            {riscoData.length > 0 ? (
              <>
                <div className="h-72" aria-hidden="true">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={riscoData} barCategoryGap="25%">
                      <CartesianGrid stroke="#e2e8f0" vertical={false} />
                      <XAxis dataKey="name" tick={{ fontSize: 12, fill: "#64748b" }} axisLine={false} tickLine={false} />
                      <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: "#64748b" }} axisLine={false} tickLine={false} />
                      <Tooltip contentStyle={{ borderRadius: "14px", border: "1px solid #e2e8f0", fontSize: "13px" }} />
                      <Bar dataKey="value" name="Consolidadas" radius={[8, 8, 0, 0]}>
                        {riscoData.map((entry) => (
                          <Cell key={entry.key} fill={RISK_COLORS[entry.key] || "#64748b"} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
                <p className="sr-only">
                  Riscos consolidados por tipo: {riscoData.map((item) => `${item.name} ${item.value}`).join(", ")}.
                </p>
              </>
            ) : (
              <EmptyState compact title="Sem riscos identificados" description="Os riscos aparecem aqui quando uma analise detectar sinais criticos." />
            )}
          </ChartCard>
        </section>

        <ChartCard
          title="Oportunidades por tipo"
          description="Categorias comerciais consolidadas, sem duplicar ocorrencias internas."
          className="mb-8"
        >
          {oportunidadeData.length > 0 ? (
            <>
              <div className="h-56" aria-hidden="true">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={oportunidadeData} layout="vertical" barCategoryGap="30%">
                    <CartesianGrid stroke="#e2e8f0" horizontal={false} />
                    <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12, fill: "#64748b" }} axisLine={false} tickLine={false} />
                    <YAxis type="category" dataKey="name" tick={{ fontSize: 12, fill: "#64748b" }} axisLine={false} tickLine={false} width={96} />
                    <Tooltip contentStyle={{ borderRadius: "14px", border: "1px solid #e2e8f0", fontSize: "13px" }} />
                    <Bar dataKey="value" name="Consolidadas" radius={[0, 8, 8, 0]}>
                      {oportunidadeData.map((entry) => (
                        <Cell key={entry.key} fill={OPORTUNIDADE_COLORS[entry.key] || "#64748b"} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <p className="sr-only">
                Oportunidades consolidadas por tipo: {oportunidadeData.map((item) => `${item.name} ${item.value}`).join(", ")}.
              </p>
            </>
          ) : (
            <EmptyState
              compact
              title="Sem oportunidades registradas"
              description="Quando uma reuniao indicar upsell, cross-sell ou novo produto, a oportunidade aparece neste grafico."
            />
          )}
        </ChartCard>

        <section className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <ChartCard title="Alertas recentes" description="Ultimos alertas gerados pela LeanIA para acompanhamento imediato.">
            <div className="mb-4 flex justify-end">
              <Link href="/alertas" className="text-sm font-semibold text-blue-700 hover:text-blue-800 hover:underline">
                Ver todos
              </Link>
            </div>
            <AlertList
              alertas={data.alertasRecentes}
              onMarcarVisto={handleMarcarVisto}
              onMarcarResolvido={handleMarcarResolvido}
              processingId={processingAlertId}
            />
          </ChartCard>

          <ChartCard title="Transcricoes recentes" description="Filtre reunioes analisadas por cliente, sentimento e status.">
            <div className="mb-4 grid gap-3">
              <label className="text-sm font-semibold text-slate-700" htmlFor="dashboard-client-filter">
                Buscar cliente
              </label>
              <div className="relative">
                <svg aria-hidden="true" className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
                </svg>
                <input
                  id="dashboard-client-filter"
                  type="search"
                  placeholder="Nome do cliente"
                  value={filtroCliente}
                  onChange={(event) => setFiltroCliente(event.target.value)}
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10"
                />
              </div>
              <div className="flex flex-wrap gap-2" aria-label="Filtrar por sentimento">
                {SENTIMENTO_FILTER.map((option) => {
                  const active = filtroSentimento === option.value;
                  return (
                    <button
                      key={option.value}
                      type="button"
                      aria-pressed={active}
                      onClick={() => setFiltroSentimento(option.value)}
                      className={`rounded-full px-3 py-1.5 text-xs font-semibold transition ${
                        active ? option.active : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                      }`}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
              <div className="flex flex-wrap gap-2" aria-label="Filtrar por status">
                {STATUS_FILTER.map((option) => {
                  const active = filtroStatus === option.value;
                  return (
                    <button
                      key={option.value}
                      type="button"
                      aria-pressed={active}
                      onClick={() => setFiltroStatus(option.value)}
                      className={`rounded-full px-3 py-1.5 text-xs font-semibold transition ${
                        active ? option.active : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                      }`}
                    >
                      {option.label}
                    </button>
                  );
                })}
              </div>
              <div className="flex items-center justify-between gap-3 text-sm text-slate-500">
                <span>
                  {transcricoesFiltradas.length} de {data.ultimasTranscricoes.length} reunioes
                </span>
                {activeFilterCount > 0 && (
                  <button type="button" onClick={clearFilters} className="font-semibold text-blue-700 hover:text-blue-800 hover:underline">
                    Limpar filtros ({activeFilterCount})
                  </button>
                )}
              </div>
            </div>

            {data.ultimasTranscricoes.length > 0 ? (
              <div className="max-h-[520px] space-y-3 overflow-y-auto pr-1">
                {transcricoesFiltradas.map((transcricao) => (
                  <Link
                    key={transcricao.id}
                    href={`/transcricoes/${transcricao.id}`}
                    className="group block rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-blue-200 hover:bg-blue-50/40 hover:shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex min-w-0 items-center gap-2">
                        <span className="truncate text-sm font-semibold text-slate-900 group-hover:text-blue-800">
                          {transcricao.cliente || `Transcricao #${transcricao.id}`}
                        </span>
                        {transcricao.analise && <SentimentBadge sentimento={transcricao.analise.sentimentoGeral as Sentimento} />}
                      </div>
                      <span className="text-xs font-medium text-slate-400">{new Date(transcricao.dataCriacao).toLocaleString("pt-BR")}</span>
                    </div>
                    <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">{transcricao.textoOriginal}</p>
                    <div className="mt-3 flex flex-wrap items-center gap-2 text-xs font-medium text-slate-500">
                      <span className="rounded-full bg-slate-100 px-2 py-1">{transcricao.status}</span>
                      <span className="rounded-full bg-blue-50 px-2 py-1 text-blue-700">
                        {transcricao.origemTranscricao === "parakeet" ? "Audio Parakeet" : transcricao.origemTranscricao === "json" ? "JSON TOTVS" : "Manual"}
                      </span>
                      {transcricao.transcriptionModel && <span>Modelo: {transcricao.transcriptionModel}</span>}
                      {transcricao.vendedor && <span>Vendedor: {transcricao.vendedor}</span>}
                    </div>
                  </Link>
                ))}
                {transcricoesFiltradas.length === 0 && (
                  <EmptyState compact title="Nenhuma reuniao encontrada" description="Ajuste os filtros ou limpe a busca para ver mais transcricoes." />
                )}
              </div>
            ) : (
              <EmptyState
                title="Nenhuma transcricao registrada"
                description="Envie uma reuniao para alimentar a LeanIA e iniciar o acompanhamento do dashboard."
                action={
                  <Link href="/transcricoes" className="rounded-xl bg-[#1f5d9d] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#184b80]">
                    Enviar transcricao
                  </Link>
                }
              />
            )}
          </ChartCard>
        </section>
      </main>
    </>
  );
}
