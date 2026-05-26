"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import {
  extrairRiscosDetalhados,
  type RiscoDetalhado,
} from "@/lib/insightDetails";
import type { TranscricaoResponse, TipoRisco, Severidade } from "@/lib/types";
import Navbar from "@/components/Navbar";
import SeveridadeBadge from "@/components/SeveridadeBadge";
import Link from "next/link";

const TIPO_LABELS: Record<TipoRisco, string> = {
  CHURN: "Churn",
  CONCORRENCIA: "Concorrencia",
  INSATISFACAO: "Insatisfacao",
  PERDA_CONFIANCA: "Perda de Confianca",
};

const TIPO_COLORS: Record<TipoRisco, string> = {
  CHURN: "bg-rose-100 text-rose-700",
  CONCORRENCIA: "bg-orange-100 text-orange-700",
  INSATISFACAO: "bg-yellow-100 text-yellow-700",
  PERDA_CONFIANCA: "bg-purple-100 text-purple-700",
};

const SEV_ORDER: Record<Severidade, number> = { ALTO: 0, MEDIO: 1, BAIXO: 2 };

export default function RiscosPage() {
  const [riscos, setRiscos] = useState<RiscoDetalhado[]>([]);
  const [loading, setLoading] = useState(true);
  const [filtroTipo, setFiltroTipo] = useState<TipoRisco | "">("");
  const [filtroSev, setFiltroSev] = useState<Severidade | "">("");

  useEffect(() => {
    api.transcricoes
      .listar()
      .then((transcricoes: TranscricaoResponse[]) => {
        const todos = extrairRiscosDetalhados(transcricoes);
        todos.sort((a, b) => SEV_ORDER[a.severidade] - SEV_ORDER[b.severidade]);
        setRiscos(todos);
      })
      .finally(() => setLoading(false));
  }, []);

  const filtrados = riscos.filter((r) => {
    if (filtroTipo && r.tipo !== filtroTipo) return false;
    if (filtroSev && r.severidade !== filtroSev) return false;
    return true;
  });
  const basesConsolidadas = new Set(filtrados.map((risco) => risco.transcricaoId)).size;

  return (
    <>
      <Navbar />
      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="mb-6 flex items-center gap-3">
          <Link href="/" className="text-slate-400 hover:text-slate-600 transition-colors">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
            </svg>
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Riscos</h1>
            <p className="text-sm text-slate-500 mt-0.5">
              {loading
                ? "Carregando..."
                : `${filtrados.length} risco${filtrados.length !== 1 ? "s" : ""} consolidado${filtrados.length !== 1 ? "s" : ""}`}
            </p>
          </div>
        </div>

        {!loading && (
          <section className="mb-6 grid grid-cols-1 gap-3 md:grid-cols-2">
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Riscos consolidados</p>
              <p className="mt-1 text-2xl font-bold text-slate-900">{filtrados.length}</p>
              <p className="mt-1 text-xs text-slate-500">Cada card agrupa evidencias semelhantes.</p>
            </div>
            <div className="rounded-2xl border border-blue-200 bg-blue-50 p-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-blue-600">Bases consolidadas</p>
              <p className="mt-1 text-2xl font-bold text-blue-900">{basesConsolidadas}</p>
              <p className="mt-1 text-xs text-blue-700">JSON/documento unico usado nesta visao.</p>
            </div>
          </section>
        )}

        <div className="flex gap-2 mb-3 flex-wrap">
          {([["", "Todos os tipos"], ["CHURN", "Churn"], ["CONCORRENCIA", "Concorrencia"], ["INSATISFACAO", "Insatisfacao"], ["PERDA_CONFIANCA", "Perda de Confianca"]] as [TipoRisco | "", string][]).map(([val, label]) => (
            <button
              key={val}
              onClick={() => setFiltroTipo(val)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all ${
                filtroTipo === val
                  ? "bg-rose-600 text-white shadow-sm"
                  : "bg-white text-slate-600 border border-slate-200 hover:border-rose-300"
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        <div className="flex gap-2 mb-6 flex-wrap">
          {([["", "Todas severidades"], ["ALTO", "Alto"], ["MEDIO", "Medio"], ["BAIXO", "Baixo"]] as [Severidade | "", string][]).map(([val, label]) => (
            <button
              key={val}
              onClick={() => setFiltroSev(val)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all ${
                filtroSev === val
                  ? "bg-slate-700 text-white shadow-sm"
                  : "bg-white text-slate-600 border border-slate-200 hover:border-slate-400"
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="relative">
              <div className="w-12 h-12 rounded-full border-4 border-slate-200" />
              <div className="w-12 h-12 rounded-full border-4 border-rose-500 border-t-transparent animate-spin absolute top-0" />
            </div>
          </div>
        ) : filtrados.length === 0 ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center">
            <div className="w-14 h-14 bg-rose-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-7 h-7 text-rose-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
              </svg>
            </div>
            <p className="text-slate-500 font-medium">Nenhum risco encontrado</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {filtrados.map((r) => (
              <Link
                key={r.id}
                href={`/transcricoes/${r.transcricaoId}`}
                className="bg-white rounded-2xl border border-slate-200 p-5 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 block"
              >
                <div className="flex items-start justify-between mb-3">
                  <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${TIPO_COLORS[r.tipo]}`}>
                    {r.categoria || TIPO_LABELS[r.tipo]}
                  </span>
                  <SeveridadeBadge severidade={r.severidade} />
                </div>
                <p className="mb-3 text-sm font-semibold text-slate-800">Risco consolidado</p>
                {r.evidencias.length > 0 && (
                  <ul className="mb-3 space-y-2">
                    {r.evidencias.slice(0, 3).map((evidencia, index) => (
                      <li key={index} className="rounded-xl bg-slate-50 px-3 py-2 text-xs leading-relaxed text-slate-600">
                        {evidencia}
                      </li>
                    ))}
                  </ul>
                )}
                {r.ocorrencias > 1 && (
                  <p className="mb-3 text-xs text-slate-400">
                    Cobertura interna: {r.ocorrencias} ocorrencia{r.ocorrencias === 1 ? "" : "s"} agrupada{r.ocorrencias === 1 ? "" : "s"}.
                  </p>
                )}
                <p className="text-sm text-slate-700 leading-relaxed mb-3">{r.recomendacao}</p>
                <div className="flex items-center justify-between text-xs pt-3 border-t border-slate-100">
                  <span className="font-medium text-slate-500 truncate">{r.cliente}</span>
                  <span className="text-slate-400">
                    {r.legado ? "Registro legado" : "Consolidado"}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
    </>
  );
}
