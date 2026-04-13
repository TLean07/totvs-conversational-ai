"use client";

import type { AlertaResponse } from "@/lib/types";
import SeveridadeBadge from "./SeveridadeBadge";

interface AlertListProps {
  alertas: AlertaResponse[];
  onMarcarVisto?: (id: string) => void;
  onMarcarResolvido?: (id: string) => void;
}

const TIPO_LABELS: Record<string, string> = {
  CHURN: "Risco de Churn",
  CONCORRENCIA: "Concorrencia",
  INSATISFACAO: "Insatisfacao",
  PERDA_CONFIANCA: "Perda de Confianca",
  OPORTUNIDADE: "Oportunidade",
};

export default function AlertList({ alertas, onMarcarVisto, onMarcarResolvido }: AlertListProps) {
  if (alertas.length === 0) {
    return (
      <div className="text-center py-12">
        <svg className="w-12 h-12 text-slate-300 mx-auto mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
        <p className="text-slate-400 font-medium">Nenhum alerta encontrado</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {alertas.map((alerta) => (
        <div
          key={alerta.id}
          className={`rounded-xl p-4 border transition-all duration-200 hover:shadow-md ${
            alerta.status === "NOVO"
              ? "bg-white border-l-4 border-l-amber-400 border-t-slate-200 border-r-slate-200 border-b-slate-200"
              : alerta.status === "VISTO"
              ? "bg-slate-50 border-slate-200"
              : "bg-emerald-50/50 border-emerald-200"
          }`}
        >
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-2">
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
              <p className="text-sm text-slate-700 leading-relaxed">{alerta.mensagem}</p>
              <p className="text-xs text-slate-400 mt-2">
                {new Date(alerta.dataCriacao).toLocaleString("pt-BR")}
              </p>
            </div>
            {alerta.status === "NOVO" && (
              <div className="flex gap-2 shrink-0">
                {onMarcarVisto && (
                  <button
                    onClick={() => onMarcarVisto(alerta.id)}
                    className="text-xs px-3 py-1.5 bg-slate-100 text-slate-600 rounded-lg hover:bg-slate-200 font-medium transition-colors"
                  >
                    Visto
                  </button>
                )}
                {onMarcarResolvido && (
                  <button
                    onClick={() => onMarcarResolvido(alerta.id)}
                    className="text-xs px-3 py-1.5 bg-emerald-100 text-emerald-700 rounded-lg hover:bg-emerald-200 font-medium transition-colors"
                  >
                    Resolver
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
