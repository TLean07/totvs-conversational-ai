"use client";

import type { AlertaResponse } from "@/lib/types";
import SeveridadeBadge from "./SeveridadeBadge";

interface AlertListProps {
  alertas: AlertaResponse[];
  onMarcarVisto?: (id: string) => void;
  onMarcarResolvido?: (id: string) => void;
  processingId?: string | null;
}

const TIPO_LABELS: Record<string, string> = {
  CHURN: "Risco de Churn",
  CONSOLIDADO_CHURN: "Churn consolidado",
  CONSOLIDADO_SENTIMENTO_NEGATIVO: "Sentimento negativo",
  CONSOLIDADO_CRITICIDADE_OPERACIONAL: "Criticidade operacional",
  CONCORRENCIA: "Concorrencia",
  INSATISFACAO: "Insatisfacao",
  PERDA_CONFIANCA: "Perda de Confianca",
  OPORTUNIDADE: "Oportunidade",
};

export default function AlertList({ alertas, onMarcarVisto, onMarcarResolvido, processingId }: AlertListProps) {
  if (alertas.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50/80 px-6 py-12 text-center">
        <svg aria-hidden="true" className="mx-auto mb-3 h-12 w-12 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        <p className="font-semibold text-slate-700">Nenhum alerta encontrado</p>
        <p className="mt-1 text-sm text-slate-500">Quando a LeanIA identificar riscos ou oportunidades, eles aparecem aqui.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {alertas.map((alerta) => {
        const isProcessing = processingId === alerta.id;

        return (
        <div
          key={alerta.id}
          className={`rounded-2xl border p-4 transition-all duration-200 hover:shadow-md ${
            alerta.status === "NOVO"
              ? "border-amber-200 bg-amber-50/40"
              : alerta.status === "VISTO"
              ? "bg-slate-50 border-slate-200"
              : "bg-emerald-50/50 border-emerald-200"
          }`}
        >
          <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
            <div className="flex-1 min-w-0 w-full">
              <div className="flex flex-wrap items-center gap-2 mb-2">
                <SeveridadeBadge severidade={alerta.prioridade} />
                <span className="text-xs font-medium text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                  {TIPO_LABELS[alerta.tipo] || alerta.tipo}
                </span>
                {alerta.status === "RESOLVIDO" && (
                  <span className="text-xs font-medium text-emerald-600 bg-emerald-100 px-2 py-0.5 rounded">
                    Resolvido
                  </span>
                )}
              </div>
              {alerta.titulo && <p className="mb-1 text-sm font-semibold text-slate-900">{alerta.titulo}</p>}
              <p className="text-sm text-slate-700 leading-relaxed break-words">{alerta.mensagem}</p>
              {alerta.ocorrencias > 0 && (
                <p className="mt-2 text-xs font-semibold text-slate-500">
                  {alerta.ocorrencias} ocorrencia{alerta.ocorrencias === 1 ? "" : "s"} consolidada{alerta.ocorrencias === 1 ? "" : "s"}
                </p>
              )}
              {(alerta.motivo || alerta.recomendacao) && (
                <div className="mt-3 grid gap-2 text-xs text-slate-600 md:grid-cols-2">
                  {alerta.motivo && (
                    <div className="rounded-xl bg-white/70 px-3 py-2 ring-1 ring-slate-100">
                      <span className="font-semibold text-slate-700">Motivo: </span>
                      {alerta.motivo}
                    </div>
                  )}
                  {alerta.recomendacao && (
                    <div className="rounded-xl bg-white/70 px-3 py-2 ring-1 ring-slate-100">
                      <span className="font-semibold text-slate-700">Recomendacao: </span>
                      {alerta.recomendacao}
                    </div>
                  )}
                </div>
              )}
              <p className="text-xs text-slate-400 mt-2">
                {new Date(alerta.dataCriacao).toLocaleString("pt-BR")}
              </p>
            </div>
            {(alerta.status === "NOVO" || alerta.status === "VISTO") && (
              <div className="flex gap-2 shrink-0 sm:mt-0 mt-2 w-full sm:w-auto justify-end">
                {alerta.status === "NOVO" && onMarcarVisto && (
                  <button
                    onClick={() => onMarcarVisto(alerta.id)}
                    disabled={isProcessing}
                    className="rounded-lg bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 ring-1 ring-slate-200 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {isProcessing ? "Salvando..." : "Visto"}
                  </button>
                )}
                {onMarcarResolvido && (
                  <button
                    onClick={() => onMarcarResolvido(alerta.id)}
                    disabled={isProcessing}
                    className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {isProcessing ? "Salvando..." : "Resolver"}
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      );
      })}
    </div>
  );
}
