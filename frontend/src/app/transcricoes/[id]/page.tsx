"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import type { TranscricaoResponse } from "@/lib/types";
import Navbar from "@/components/Navbar";
import SentimentBadge from "@/components/SentimentBadge";
import SeveridadeBadge from "@/components/SeveridadeBadge";
import AlertList from "@/components/AlertList";

const TIPO_PRODUTO_LABEL = { ATUAL: "Em uso", OPORTUNIDADE: "Oportunidade", CONCORRENTE: "Concorrente" };
const TIPO_PRODUTO_CONFIG = {
  ATUAL: { bg: "bg-blue-50", text: "text-blue-700", ring: "ring-blue-200", icon: "M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" },
  OPORTUNIDADE: { bg: "bg-emerald-50", text: "text-emerald-700", ring: "ring-emerald-200", icon: "M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z" },
  CONCORRENTE: { bg: "bg-rose-50", text: "text-rose-700", ring: "ring-rose-200", icon: "M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" },
};

export default function TranscricaoDetailPage() {
  const params = useParams();
  const [data, setData] = useState<TranscricaoResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (params.id) {
      api.transcricoes
        .buscar(params.id as string)
        .then(setData)
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [params.id]);

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="flex flex-col items-center justify-center h-96 gap-4">
          <div className="relative">
            <div className="w-16 h-16 rounded-full border-4 border-slate-200" />
            <div className="w-16 h-16 rounded-full border-4 border-blue-500 border-t-transparent animate-spin absolute top-0" />
          </div>
          <p className="text-slate-400 text-sm">Carregando analise...</p>
        </div>
      </>
    );
  }

  if (!data) {
    return (
      <>
        <Navbar />
        <div className="max-w-5xl mx-auto px-6 py-16 text-center">
          <svg className="w-16 h-16 text-slate-300 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9.75 9.75l4.5 4.5m0-4.5l-4.5 4.5M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p className="text-slate-500 font-medium">Transcricao nao encontrada</p>
          <a href="/transcricoes" className="text-sm text-blue-600 hover:underline mt-2 inline-block">
            Voltar para Transcricoes
          </a>
        </div>
      </>
    );
  }

  const analise = data.analise;

  return (
    <>
      <Navbar />
      <main className="max-w-5xl mx-auto px-6 py-8">
        <a href="/transcricoes" className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-blue-600 mb-6 group transition-colors">
          <svg className="w-4 h-4 group-hover:-translate-x-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
          Voltar para Transcricoes
        </a>

        {/* Header Card */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
          <div className="flex items-start justify-between mb-5">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center text-white font-bold text-lg">
                {(data.cliente || "T")[0].toUpperCase()}
              </div>
              <div>
                <h1 className="text-xl font-bold text-slate-800">
                  {data.cliente || `Transcricao #${data.id}`}
                </h1>
                <div className="flex items-center gap-3 mt-1 text-sm text-slate-400">
                  {data.vendedor && <span>Vendedor: {data.vendedor}</span>}
                  {data.duracaoMinutos && <span>{data.duracaoMinutos} min</span>}
                  <span>{data.status}</span>
                </div>
              </div>
            </div>
            {analise && <SentimentBadge sentimento={analise.sentimentoGeral} />}
          </div>

          {data.participantes && (
            <div className="flex items-center gap-2 mb-4 text-sm">
              <svg className="w-4 h-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" />
              </svg>
              <span className="text-slate-500">{data.participantes}</span>
            </div>
          )}

          <div className="bg-slate-50 rounded-xl p-5 border border-slate-100">
            <h3 className="text-sm font-semibold text-slate-600 mb-2 flex items-center gap-1.5">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.087.16 2.185.283 3.293.369V21l4.076-4.076a1.526 1.526 0 011.037-.443 48.282 48.282 0 005.68-.494c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
              </svg>
              Transcricao Original
            </h3>
            <p className="text-sm text-slate-600 whitespace-pre-wrap leading-relaxed">{data.textoOriginal}</p>
          </div>
        </div>

        {analise && (
          <>
            {/* Resumo */}
            <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
              <h2 className="text-base font-semibold text-slate-700 mb-1">Resumo da Analise</h2>
              <p className="text-sm text-slate-500 mb-5 leading-relaxed">{analise.resumo}</p>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-4 border border-blue-100">
                  <div className="flex items-center gap-2 mb-2">
                    <svg className="w-4 h-4 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                    </svg>
                    <span className="text-xs font-semibold text-blue-600 uppercase tracking-wide">Persona</span>
                  </div>
                  <p className="text-sm font-medium text-slate-700">{analise.personaIdentificada}</p>
                </div>
                <div className="bg-gradient-to-br from-emerald-50 to-green-50 rounded-xl p-4 border border-emerald-100">
                  <div className="flex items-center gap-2 mb-2">
                    <svg className="w-4 h-4 text-emerald-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 18.75a60.07 60.07 0 0115.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 013 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 00-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 01-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 003 15h-.75M15 10.5a3 3 0 11-6 0 3 3 0 016 0zm3 0h.008v.008H18V10.5zm-12 0h.008v.008H6V10.5z" /></svg>
                    <span className="text-xs font-semibold text-emerald-600 uppercase tracking-wide">Budget</span>
                  </div>
                  <p className="text-sm font-medium text-slate-700">{analise.budgetIdentificado}</p>
                </div>
                <div className="bg-gradient-to-br from-violet-50 to-purple-50 rounded-xl p-4 border border-violet-100">
                  <div className="flex items-center gap-2 mb-2">
                    <svg className="w-4 h-4 text-violet-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" /></svg>
                    <span className="text-xs font-semibold text-violet-600 uppercase tracking-wide">Confianca no Vendedor</span>
                  </div>
                  <p className="text-sm font-medium text-slate-700">
                    {analise.scoreConfiancaVendedor != null
                      ? `${(analise.scoreConfiancaVendedor * 100).toFixed(0)}%`
                      : "N/A"}
                  </p>
                </div>
              </div>
            </div>

            {/* Produtos */}
            {analise.produtos.length > 0 && (
              <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
                <h2 className="text-base font-semibold text-slate-700 mb-4">Produtos Identificados</h2>
                <div className="space-y-3">
                  {analise.produtos.map((p, i) => {
                    const config = TIPO_PRODUTO_CONFIG[p.tipo];
                    return (
                      <div key={i} className={`${config.bg} rounded-xl p-4 border ${config.ring} ring-1 flex items-start gap-3`}>
                        <div className={`w-8 h-8 rounded-lg ${config.bg} flex items-center justify-center shrink-0`}>
                          <svg className={`w-4 h-4 ${config.text}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                            <path strokeLinecap="round" strokeLinejoin="round" d={config.icon} />
                          </svg>
                        </div>
                        <div>
                          <div className="flex items-center gap-2 mb-1">
                            <span className="font-semibold text-sm text-slate-700">{p.nomeProduto}</span>
                            <span className={`text-xs px-2 py-0.5 rounded-full font-semibold ${config.text} ${config.bg} ring-1 ${config.ring}`}>
                              {TIPO_PRODUTO_LABEL[p.tipo]}
                            </span>
                          </div>
                          <p className="text-sm text-slate-500">{p.contexto}</p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Oportunidades */}
            {analise.oportunidades.length > 0 && (
              <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
                <h2 className="text-base font-semibold text-slate-700 mb-4">Oportunidades</h2>
                <div className="space-y-3">
                  {analise.oportunidades.map((o, i) => (
                    <div key={i} className="border-l-4 border-emerald-400 bg-gradient-to-r from-emerald-50 to-white rounded-r-xl p-4">
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-xs font-semibold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-lg ring-1 ring-emerald-200">
                          {o.tipo}
                        </span>
                        {o.score != null && (
                          <span className="text-xs font-medium text-slate-500 bg-slate-100 px-2 py-1 rounded-lg">
                            Score: {(o.score * 100).toFixed(0)}%
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-slate-700 leading-relaxed">{o.descricao}</p>
                      <div className="flex gap-4 mt-3 text-xs text-slate-400">
                        {o.produtoRelacionado && (
                          <span className="flex items-center gap-1">
                            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" /></svg>
                            {o.produtoRelacionado}
                          </span>
                        )}
                        {o.valorEstimado && (
                          <span className="flex items-center gap-1">
                            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                            {o.valorEstimado}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Riscos */}
            {analise.riscos.length > 0 && (
              <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
                <h2 className="text-base font-semibold text-slate-700 mb-4">Riscos</h2>
                <div className="space-y-3">
                  {analise.riscos.map((r, i) => (
                    <div key={i} className="border-l-4 border-rose-400 bg-gradient-to-r from-rose-50 to-white rounded-r-xl p-4">
                      <div className="flex items-center gap-2 mb-2">
                        <SeveridadeBadge severidade={r.severidade} />
                        <span className="text-xs font-medium text-slate-500 bg-slate-100 px-2 py-0.5 rounded">{r.tipo}</span>
                      </div>
                      <p className="text-sm text-slate-700 leading-relaxed">{r.descricao}</p>
                      {r.concorrenteMencionado && (
                        <p className="text-xs text-rose-600 mt-2 flex items-center gap-1">
                          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" /></svg>
                          Concorrente: {r.concorrenteMencionado}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Alertas */}
            {analise.alertas.length > 0 && (
              <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
                <h2 className="text-base font-semibold text-slate-700 mb-4">Alertas Gerados</h2>
                <AlertList alertas={analise.alertas} />
              </div>
            )}
          </>
        )}
      </main>
    </>
  );
}
