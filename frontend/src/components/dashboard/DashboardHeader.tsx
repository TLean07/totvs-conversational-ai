"use client";

import Link from "next/link";

interface DashboardHeaderProps {
  lastUpdated: Date | null;
  refreshing: boolean;
  onRefresh: () => void;
}

export default function DashboardHeader({ lastUpdated, refreshing, onRefresh }: DashboardHeaderProps) {
  return (
    <header className="mb-8 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm shadow-slate-200/70">
      <div className="border-b border-slate-100 bg-gradient-to-r from-[#16355f] via-[#1f5d9d] to-[#2a78bf] px-6 py-6 text-white">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-100">TOTVS Conversational Intelligence</p>
            <h1 className="mt-2 text-3xl font-bold tracking-tight sm:text-4xl">Dashboard executivo</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-blue-50">
              Visão consolidada de sentimentos, riscos, oportunidades e alertas gerados pela LeanIA.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <Link
              href="/transcricoes"
              className="inline-flex items-center justify-center rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-[#1f5d9d] shadow-sm transition hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-[#1f5d9d]"
            >
              Nova transcrição
            </Link>
            <button
              type="button"
              onClick={onRefresh}
              disabled={refreshing}
              className="inline-flex items-center justify-center rounded-xl border border-white/30 bg-white/10 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-[#1f5d9d]"
            >
              {refreshing ? "Atualizando..." : "Atualizar"}
            </button>
          </div>
        </div>
      </div>
      <div className="flex flex-col gap-3 px-6 py-4 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
        <span>Use os indicadores para priorizar contas em risco e acompanhar a saúde das reuniões.</span>
        <span className="font-medium text-slate-600">
          Atualizado: {lastUpdated ? lastUpdated.toLocaleString("pt-BR") : "agora"}
        </span>
      </div>
    </header>
  );
}
