"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { OportunidadeResponse, TranscricaoResponse, TipoOportunidade } from "@/lib/types";
import Navbar from "@/components/Navbar";
import Link from "next/link";

interface OportunidadeComContexto extends OportunidadeResponse {
  transcricaoId: string;
  cliente: string;
}

const TIPO_LABELS: Record<TipoOportunidade, string> = {
  UPSELL: "Upsell",
  CROSS_SELL: "Cross-sell",
  NOVO_PRODUTO: "Novo Produto",
};

const TIPO_COLORS: Record<TipoOportunidade, string> = {
  UPSELL: "bg-violet-100 text-violet-700",
  CROSS_SELL: "bg-blue-100 text-blue-700",
  NOVO_PRODUTO: "bg-emerald-100 text-emerald-700",
};

export default function OportunidadesPage() {
  const [oportunidades, setOportunidades] = useState<OportunidadeComContexto[]>([]);
  const [loading, setLoading] = useState(true);
  const [filtroTipo, setFiltroTipo] = useState<TipoOportunidade | "">("");

  useEffect(() => {
    api.transcricoes
      .listar()
      .then((transcricoes: TranscricaoResponse[]) => {
        const todas: OportunidadeComContexto[] = [];
        for (const t of transcricoes) {
          if (!t.analise) continue;
          for (const op of t.analise.oportunidades) {
            todas.push({ ...op, transcricaoId: t.id, cliente: t.cliente || "Cliente nao informado" });
          }
        }
        setOportunidades(todas);
      })
      .finally(() => setLoading(false));
  }, []);

  const filtradas = filtroTipo ? oportunidades.filter((o) => o.tipo === filtroTipo) : oportunidades;

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
            <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Oportunidades</h1>
            <p className="text-sm text-slate-500 mt-0.5">
              {loading ? "Carregando..." : `${filtradas.length} oportunidade${filtradas.length !== 1 ? "s" : ""} encontrada${filtradas.length !== 1 ? "s" : ""}`}
            </p>
          </div>
        </div>

        <div className="flex gap-2 mb-6 flex-wrap">
          {([["", "Todas"], ["UPSELL", "Upsell"], ["CROSS_SELL", "Cross-sell"], ["NOVO_PRODUTO", "Novo Produto"]] as [TipoOportunidade | "", string][]).map(([val, label]) => (
            <button
              key={val}
              onClick={() => setFiltroTipo(val)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all ${
                filtroTipo === val
                  ? "bg-violet-600 text-white shadow-sm"
                  : "bg-white text-slate-600 border border-slate-200 hover:border-violet-300"
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
              <div className="w-12 h-12 rounded-full border-4 border-violet-500 border-t-transparent animate-spin absolute top-0" />
            </div>
          </div>
        ) : filtradas.length === 0 ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center">
            <div className="w-14 h-14 bg-violet-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-7 h-7 text-violet-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 18.75a60.07 60.07 0 0115.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 013 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 00-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 01-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 003 15h-.75M15 10.5a3 3 0 11-6 0 3 3 0 016 0zm3 0h.008v.008H18V10.5zm-12 0h.008v.008H6V10.5z" />
              </svg>
            </div>
            <p className="text-slate-500 font-medium">Nenhuma oportunidade encontrada</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
            {filtradas.map((op, i) => (
              <Link
                key={i}
                href={`/transcricoes/${op.transcricaoId}`}
                className="bg-white rounded-2xl border border-slate-200 p-5 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 block"
              >
                <div className="flex items-start justify-between mb-3">
                  <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${TIPO_COLORS[op.tipo]}`}>
                    {TIPO_LABELS[op.tipo]}
                  </span>
                  {op.score != null && (
                    <span className="text-xs font-medium text-slate-400">
                      {Math.round(op.score * 100)}% confianca
                    </span>
                  )}
                </div>
                <p className="text-sm text-slate-700 leading-relaxed mb-3">{op.descricao}</p>
                <div className="flex items-center justify-between text-xs text-slate-400 pt-3 border-t border-slate-100">
                  <span className="font-medium text-slate-500 truncate">{op.cliente}</span>
                  <div className="flex items-center gap-3 shrink-0">
                    {op.produtoRelacionado && (
                      <span className="truncate max-w-[100px]">{op.produtoRelacionado}</span>
                    )}
                    {op.valorEstimado && (
                      <span className="text-emerald-600 font-semibold">{op.valorEstimado}</span>
                    )}
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </main>
    </>
  );
}
