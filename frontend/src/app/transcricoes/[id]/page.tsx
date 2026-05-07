"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { api } from "@/lib/api";
import {
  contarOcorrencias,
  extrairOportunidadesDetalhadas,
  extrairRiscosDetalhados,
} from "@/lib/insightDetails";
import type { AnaliseFeedbackResponse, TranscricaoResponse } from "@/lib/types";
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

function escapeCsv(value: unknown) {
  const text = String(value ?? "");
  return `"${text.replace(/"/g, '""')}"`;
}

function escapeHtml(value: unknown) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function downloadFile(filename: string, content: string, type: string) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function analysisRows(data: TranscricaoResponse) {
  const rows: Array<[string, string, string]> = [
    ["Transcricao", "Cliente", data.cliente || "Nao informado"],
    ["Transcricao", "Vendedor", data.vendedor || "Nao informado"],
    ["Transcricao", "Status", data.status],
    ["Transcricao", "Data", new Date(data.dataCriacao).toLocaleString("pt-BR")],
    ["Transcricao", "Texto", data.textoOriginal],
  ];

  if (!data.analise) return rows;

  rows.push(["Analise", "Sentimento", data.analise.sentimentoGeral]);
  rows.push(["Analise", "Resumo", data.analise.resumo]);
  rows.push(["Analise", "Persona", data.analise.personaIdentificada]);
  rows.push(["Analise", "Budget", data.analise.budgetIdentificado]);
  rows.push(["Analise", "Confianca", `${Math.round(data.analise.scoreConfiancaVendedor * 100)}%`]);

  data.analise.produtos.forEach((produto) => {
    rows.push(["Produto", produto.tipo, `${produto.nomeProduto} - ${produto.contexto}`]);
  });
  data.analise.oportunidades.forEach((oportunidade) => {
    rows.push([
      "Oportunidade",
      oportunidade.tipo,
      `${oportunidade.descricao} | Produto: ${oportunidade.produtoRelacionado} | Score: ${Math.round(oportunidade.score * 100)}% | Valor: ${oportunidade.valorEstimado}`,
    ]);
  });
  data.analise.riscos.forEach((risco) => {
    rows.push([
      "Risco",
      risco.tipo,
      `${risco.descricao} | Severidade: ${risco.severidade} | Concorrente: ${risco.concorrenteMencionado || "Nao informado"}`,
    ]);
  });
  data.analise.alertas.forEach((alerta) => {
    rows.push(["Alerta", alerta.prioridade, `${alerta.tipo} - ${alerta.mensagem}`]);
  });

  return rows;
}

export default function TranscricaoDetailPage() {
  const params = useParams();
  const [data, setData] = useState<TranscricaoResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [feedbackText, setFeedbackText] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState("");
  const [feedbackLoading, setFeedbackLoading] = useState(false);
  const [feedbacks, setFeedbacks] = useState<AnaliseFeedbackResponse[]>([]);

  useEffect(() => {
    if (params.id) {
      Promise.all([
        api.transcricoes.buscar(params.id as string),
        api.transcricoes.listarFeedbacks(params.id as string).catch(() => []),
      ])
        .then(([transcricao, historico]) => {
          setData(transcricao);
          setFeedbacks(historico);
        })
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [params.id]);

  const handleReprocessar = async () => {
    if (!data) return;
    const updated = await api.transcricoes.reprocessar(data.id);
    setData(updated);
  };

  const handleFeedback = async (util: boolean) => {
    if (!data) return;
    setFeedbackLoading(true);
    setFeedbackMessage("");
    try {
      await api.transcricoes.feedback(data.id, { util, ajuste: feedbackText });
      const historico = await api.transcricoes.listarFeedbacks(data.id);
      setFeedbacks(historico);
      setFeedbackText("");
      setFeedbackMessage("Feedback registrado. A LeanIA usara esse contexto nas proximas analises.");
    } catch (error) {
      setFeedbackMessage(error instanceof Error ? error.message : "Nao foi possivel registrar o feedback.");
    } finally {
      setFeedbackLoading(false);
    }
  };

  const exportCsv = () => {
    if (!data) return;
    const csv = [
      ["Categoria", "Campo", "Valor"].map(escapeCsv).join(";"),
      ...analysisRows(data).map((row) => row.map(escapeCsv).join(";")),
    ].join("\n");
    downloadFile(`analise-${data.id}.csv`, `\uFEFF${csv}`, "text/csv;charset=utf-8");
  };

  const exportPdf = () => {
    if (!data) return;
    const rows = analysisRows(data)
      .map(([categoria, campo, valor]) => `
        <tr>
          <td>${escapeHtml(categoria)}</td>
          <td>${escapeHtml(campo)}</td>
          <td>${escapeHtml(valor)}</td>
        </tr>
      `)
      .join("");
    const html = `
      <!doctype html>
      <html lang="pt-BR">
        <head>
          <meta charset="utf-8" />
          <title>Analise ${escapeHtml(data.cliente || data.id)}</title>
          <style>
            body { font-family: Arial, sans-serif; color: #0f172a; margin: 32px; }
            h1 { color: #1a365d; margin-bottom: 4px; }
            p { color: #475569; }
            table { width: 100%; border-collapse: collapse; margin-top: 24px; font-size: 12px; }
            th, td { border: 1px solid #cbd5e1; padding: 8px; vertical-align: top; }
            th { background: #e2e8f0; text-align: left; }
            .meta { font-size: 12px; color: #64748b; }
          </style>
        </head>
        <body>
          <h1>TOTVS Conversational Intelligence</h1>
          <p>Analise gerada pela LeanIA para ${escapeHtml(data.cliente || `Transcricao ${data.id}`)}</p>
          <p class="meta">Exportado em ${new Date().toLocaleString("pt-BR")}</p>
          <table>
            <thead><tr><th>Categoria</th><th>Campo</th><th>Valor</th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
          <script>window.print();</script>
        </body>
      </html>
    `;
    const printWindow = window.open("", "_blank", "noopener,noreferrer");
    if (!printWindow) {
      downloadFile(`analise-${data.id}.html`, html, "text/html;charset=utf-8");
      return;
    }
    printWindow.document.write(html);
    printWindow.document.close();
  };

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
          <Link href="/transcricoes" className="text-sm text-blue-600 hover:underline mt-2 inline-block">
            Voltar para Transcricoes
          </Link>
        </div>
      </>
    );
  }

  const analise = data.analise;
  const riscosDetalhados = extrairRiscosDetalhados([data]);
  const oportunidadesDetalhadas = extrairOportunidadesDetalhadas([data]);
  const totalRiscos = contarOcorrencias(riscosDetalhados);
  const totalOportunidades = contarOcorrencias(oportunidadesDetalhadas);

  return (
    <>
      <Navbar />
      <main className="max-w-5xl mx-auto px-6 py-8">
        <Link href="/transcricoes" className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-blue-600 mb-6 group transition-colors">
          <svg className="w-4 h-4 group-hover:-translate-x-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
          Voltar para Transcricoes
        </Link>

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
            <div className="flex flex-wrap items-center justify-end gap-2">
              <button
                type="button"
                onClick={exportCsv}
                className="rounded-lg bg-white px-3 py-2 text-xs font-semibold text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50"
              >
                CSV
              </button>
              <button
                type="button"
                onClick={exportPdf}
                className="rounded-lg bg-white px-3 py-2 text-xs font-semibold text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50"
              >
                PDF
              </button>
              {data.status === "ERRO" && (
                <button
                  type="button"
                  onClick={handleReprocessar}
                  className="rounded-lg bg-amber-50 px-3 py-2 text-xs font-semibold text-amber-700 ring-1 ring-amber-200 hover:bg-amber-100"
                >
                  Reprocessar
                </button>
              )}
              {analise && <SentimentBadge sentimento={analise.sentimentoGeral} />}
            </div>
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
              <div className="mt-5 rounded-xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <h3 className="text-sm font-semibold text-slate-700">Feedback para a LeanIA</h3>
                    <p className="mt-1 text-xs text-slate-500">
                      Esse retorno entra no contexto das proximas analises gratuitas.
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      disabled={feedbackLoading}
                      onClick={() => handleFeedback(true)}
                      className="rounded-lg bg-emerald-600 px-3 py-2 text-xs font-semibold text-white disabled:opacity-60"
                    >
                      Util
                    </button>
                    <button
                      type="button"
                      disabled={feedbackLoading}
                      onClick={() => handleFeedback(false)}
                      className="rounded-lg bg-slate-700 px-3 py-2 text-xs font-semibold text-white disabled:opacity-60"
                    >
                      Precisa ajuste
                    </button>
                  </div>
                </div>
                <textarea
                  value={feedbackText}
                  onChange={(e) => setFeedbackText(e.target.value)}
                  maxLength={500}
                  rows={3}
                  className="mt-3 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                  placeholder="Ex.: o risco era menor, o produto correto era RM, o concorrente citado foi SAP..."
                />
                {feedbackMessage && (
                  <p className="mt-2 text-xs font-medium text-slate-500">{feedbackMessage}</p>
                )}
                <div className="mt-4 border-t border-slate-200 pt-4">
                  <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Historico de feedback
                  </h4>
                  {feedbacks.length > 0 ? (
                    <div className="mt-3 space-y-2">
                      {feedbacks.map((feedback) => (
                        <div key={feedback.id} className="rounded-lg border border-slate-200 bg-white px-3 py-2">
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <span className={`text-xs font-semibold ${feedback.util ? "text-emerald-700" : "text-amber-700"}`}>
                              {feedback.util ? "Marcado como util" : "Marcado para ajuste"}
                            </span>
                            <span className="text-xs text-slate-400">
                              {new Date(feedback.createdAt).toLocaleString("pt-BR")}
                            </span>
                          </div>
                          {feedback.ajuste && (
                            <p className="mt-1 text-sm text-slate-600">{feedback.ajuste}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="mt-2 text-sm text-slate-400">
                      Nenhum feedback registrado para esta analise.
                    </p>
                  )}
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
            {oportunidadesDetalhadas.length > 0 && (
              <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
                <div className="mb-4 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
                  <h2 className="text-base font-semibold text-slate-700">Oportunidades</h2>
                  <p className="text-xs font-semibold text-emerald-700">
                    {totalOportunidades} ocorrencia{totalOportunidades === 1 ? "" : "s"} em {oportunidadesDetalhadas.length} categoria{oportunidadesDetalhadas.length === 1 ? "" : "s"}
                  </p>
                </div>
                <div className="space-y-3">
                  {oportunidadesDetalhadas.map((o) => (
                    <div key={o.id} className="border-l-4 border-emerald-400 bg-gradient-to-r from-emerald-50 to-white rounded-r-xl p-4">
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-xs font-semibold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-lg ring-1 ring-emerald-200">
                          {o.categoria}
                        </span>
                        {o.score != null && (
                          <span className="text-xs font-medium text-slate-500 bg-slate-100 px-2 py-1 rounded-lg">
                            Score: {(o.score * 100).toFixed(0)}%
                          </span>
                        )}
                      </div>
                      <p className="mb-2 text-sm font-semibold text-slate-800">
                        {o.ocorrencias} ocorrencia{o.ocorrencias === 1 ? "" : "s"} consolidada{o.ocorrencias === 1 ? "" : "s"}
                      </p>
                      {o.evidencias.length > 0 && (
                        <ul className="space-y-2">
                          {o.evidencias.slice(0, 3).map((evidencia, index) => (
                            <li key={index} className="rounded-lg bg-white/70 px-3 py-2 text-xs leading-relaxed text-slate-600 ring-1 ring-emerald-100">
                              {evidencia}
                            </li>
                          ))}
                        </ul>
                      )}
                      <div className="mt-3 text-xs text-slate-500">
                        {o.recomendacao}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Riscos */}
            {riscosDetalhados.length > 0 && (
              <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
                <div className="mb-4 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
                  <h2 className="text-base font-semibold text-slate-700">Riscos</h2>
                  <p className="text-xs font-semibold text-rose-700">
                    {totalRiscos} ocorrencia{totalRiscos === 1 ? "" : "s"} em {riscosDetalhados.length} categoria{riscosDetalhados.length === 1 ? "" : "s"}
                  </p>
                </div>
                <div className="space-y-3">
                  {riscosDetalhados.map((r) => (
                    <div key={r.id} className="border-l-4 border-rose-400 bg-gradient-to-r from-rose-50 to-white rounded-r-xl p-4">
                      <div className="flex items-center gap-2 mb-2">
                        <SeveridadeBadge severidade={r.severidade} />
                        <span className="text-xs font-medium text-slate-500 bg-slate-100 px-2 py-0.5 rounded">{r.categoria}</span>
                      </div>
                      <p className="mb-2 text-sm font-semibold text-slate-800">
                        {r.ocorrencias} ocorrencia{r.ocorrencias === 1 ? "" : "s"} consolidada{r.ocorrencias === 1 ? "" : "s"}
                      </p>
                      {r.evidencias.length > 0 && (
                        <ul className="space-y-2">
                          {r.evidencias.slice(0, 3).map((evidencia, index) => (
                            <li key={index} className="rounded-lg bg-white/70 px-3 py-2 text-xs leading-relaxed text-slate-600 ring-1 ring-rose-100">
                              {evidencia}
                            </li>
                          ))}
                        </ul>
                      )}
                      <p className="mt-3 text-xs text-slate-500">{r.recomendacao}</p>
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
