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
const TIPO_PRODUTO_COLOR = {
  ATUAL: "bg-blue-100 text-blue-800",
  OPORTUNIDADE: "bg-green-100 text-green-800",
  CONCORRENTE: "bg-red-100 text-red-800",
};

export default function TranscricaoDetailPage() {
  const params = useParams();
  const [data, setData] = useState<TranscricaoResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (params.id) {
      api.transcricoes
        .buscar(Number(params.id))
        .then(setData)
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [params.id]);

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

  if (!data) {
    return (
      <>
        <Navbar />
        <div className="max-w-5xl mx-auto px-4 py-8">
          <p className="text-red-600">Transcricao nao encontrada.</p>
        </div>
      </>
    );
  }

  const analise = data.analise;

  return (
    <>
      <Navbar />
      <main className="max-w-5xl mx-auto px-4 py-8">
        <a href="/transcricoes" className="text-sm text-blue-600 hover:underline mb-4 inline-block">
          &larr; Voltar para Transcricoes
        </a>

        <div className="bg-white rounded-xl shadow-sm border p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-xl font-bold text-gray-900">
              {data.cliente || `Transcricao #${data.id}`}
            </h1>
            {analise && <SentimentBadge sentimento={analise.sentimentoGeral} />}
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4 text-sm">
            {data.vendedor && (
              <div><span className="text-gray-500">Vendedor:</span> <span className="font-medium">{data.vendedor}</span></div>
            )}
            {data.participantes && (
              <div><span className="text-gray-500">Participantes:</span> <span className="font-medium">{data.participantes}</span></div>
            )}
            {data.duracaoMinutos && (
              <div><span className="text-gray-500">Duracao:</span> <span className="font-medium">{data.duracaoMinutos} min</span></div>
            )}
            <div><span className="text-gray-500">Status:</span> <span className="font-medium">{data.status}</span></div>
          </div>

          <div className="bg-gray-50 rounded-lg p-4">
            <h3 className="text-sm font-medium text-gray-700 mb-2">Transcricao Original</h3>
            <p className="text-sm text-gray-600 whitespace-pre-wrap">{data.textoOriginal}</p>
          </div>
        </div>

        {analise && (
          <>
            {/* Resumo da Analise */}
            <div className="bg-white rounded-xl shadow-sm border p-6 mb-6">
              <h2 className="text-lg font-semibold text-gray-800 mb-3">Resumo da Analise</h2>
              <p className="text-sm text-gray-700 mb-4">{analise.resumo}</p>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                <div className="bg-blue-50 rounded-lg p-3">
                  <span className="text-blue-600 font-medium">Persona</span>
                  <p className="text-gray-800 mt-1">{analise.personaIdentificada}</p>
                </div>
                <div className="bg-green-50 rounded-lg p-3">
                  <span className="text-green-600 font-medium">Budget</span>
                  <p className="text-gray-800 mt-1">{analise.budgetIdentificado}</p>
                </div>
                <div className="bg-purple-50 rounded-lg p-3">
                  <span className="text-purple-600 font-medium">Confianca no Vendedor</span>
                  <p className="text-gray-800 mt-1">
                    {analise.scoreConfiancaVendedor != null
                      ? `${(analise.scoreConfiancaVendedor * 100).toFixed(0)}%`
                      : "N/A"}
                  </p>
                </div>
              </div>
            </div>

            {/* Produtos Identificados */}
            {analise.produtos.length > 0 && (
              <div className="bg-white rounded-xl shadow-sm border p-6 mb-6">
                <h2 className="text-lg font-semibold text-gray-800 mb-3">Produtos Identificados</h2>
                <div className="space-y-3">
                  {analise.produtos.map((p) => (
                    <div key={p.id} className="border rounded-lg p-3 flex items-start gap-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                        TIPO_PRODUTO_COLOR[p.tipo]
                      }`}>
                        {TIPO_PRODUTO_LABEL[p.tipo]}
                      </span>
                      <div>
                        <p className="font-medium text-gray-800">{p.nomeProduto}</p>
                        <p className="text-sm text-gray-500 mt-1">{p.contexto}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Oportunidades */}
            {analise.oportunidades.length > 0 && (
              <div className="bg-white rounded-xl shadow-sm border p-6 mb-6">
                <h2 className="text-lg font-semibold text-gray-800 mb-3">Oportunidades</h2>
                <div className="space-y-3">
                  {analise.oportunidades.map((o) => (
                    <div key={o.id} className="border-l-4 border-green-400 bg-green-50 rounded-r-lg p-4">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-xs font-medium text-green-700 bg-green-200 px-2 py-0.5 rounded">
                          {o.tipo}
                        </span>
                        {o.score != null && (
                          <span className="text-xs text-gray-500">
                            Score: {(o.score * 100).toFixed(0)}%
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-800 mt-1">{o.descricao}</p>
                      <div className="flex gap-4 mt-2 text-xs text-gray-500">
                        {o.produtoRelacionado && <span>Produto: {o.produtoRelacionado}</span>}
                        {o.valorEstimado && <span>Valor: {o.valorEstimado}</span>}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Riscos */}
            {analise.riscos.length > 0 && (
              <div className="bg-white rounded-xl shadow-sm border p-6 mb-6">
                <h2 className="text-lg font-semibold text-gray-800 mb-3">Riscos</h2>
                <div className="space-y-3">
                  {analise.riscos.map((r) => (
                    <div key={r.id} className="border-l-4 border-red-400 bg-red-50 rounded-r-lg p-4">
                      <div className="flex items-center gap-2 mb-1">
                        <SeveridadeBadge severidade={r.severidade} />
                        <span className="text-xs font-mono text-gray-500">{r.tipo}</span>
                      </div>
                      <p className="text-sm text-gray-800 mt-1">{r.descricao}</p>
                      {r.concorrenteMencionado && (
                        <p className="text-xs text-red-600 mt-1">
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
              <div className="bg-white rounded-xl shadow-sm border p-6">
                <h2 className="text-lg font-semibold text-gray-800 mb-3">Alertas Gerados</h2>
                <AlertList alertas={analise.alertas} />
              </div>
            )}
          </>
        )}
      </main>
    </>
  );
}
