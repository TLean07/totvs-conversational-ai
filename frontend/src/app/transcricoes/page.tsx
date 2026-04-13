"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { TranscricaoResponse, Sentimento } from "@/lib/types";
import Navbar from "@/components/Navbar";
import SentimentBadge from "@/components/SentimentBadge";
import VoiceRecorder from "@/components/VoiceRecorder";

export default function TranscricoesPage() {
  const [transcricoes, setTranscricoes] = useState<TranscricaoResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    textoOriginal: "",
    cliente: "",
    vendedor: "",
    participantes: "",
    duracaoMinutos: "",
  });

  const loadData = () => {
    setLoading(true);
    api.transcricoes
      .listar()
      .then(setTranscricoes)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await api.transcricoes.criar({
        textoOriginal: form.textoOriginal,
        cliente: form.cliente || undefined,
        vendedor: form.vendedor || undefined,
        participantes: form.participantes || undefined,
        duracaoMinutos: form.duracaoMinutos ? parseInt(form.duracaoMinutos) : undefined,
      });
      setForm({ textoOriginal: "", cliente: "", vendedor: "", participantes: "", duracaoMinutos: "" });
      setShowForm(false);
      loadData();
    } catch {
      alert("Erro ao enviar transcricao. Verifique se o backend esta rodando.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Navbar />
      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Transcricoes</h1>
            <p className="text-sm text-slate-500 mt-1">Gerencie e analise transcricoes de reunioes</p>
          </div>
          <button
            onClick={() => setShowForm(!showForm)}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 shadow-sm ${
              showForm
                ? "bg-slate-200 text-slate-700 hover:bg-slate-300"
                : "bg-gradient-to-r from-[#1a365d] to-[#2b6cb0] text-white hover:shadow-md hover:-translate-y-0.5"
            }`}
          >
            {showForm ? (
              <>
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
                Cancelar
              </>
            ) : (
              <>
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
                Nova Transcricao
              </>
            )}
          </button>
        </div>

        {showForm && (
          <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-8">
            <h2 className="text-lg font-semibold text-slate-800 mb-1">Enviar Nova Transcricao</h2>
            <p className="text-sm text-slate-400 mb-6">Preencha os dados da reuniao e cole a transcricao</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5 mb-5">
              <div>
                <label className="block text-sm font-medium text-slate-600 mb-1.5">Cliente</label>
                <input
                  type="text"
                  value={form.cliente}
                  onChange={(e) => setForm({ ...form, cliente: e.target.value })}
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all"
                  placeholder="Nome do cliente"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-600 mb-1.5">Vendedor</label>
                <input
                  type="text"
                  value={form.vendedor}
                  onChange={(e) => setForm({ ...form, vendedor: e.target.value })}
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all"
                  placeholder="Nome do vendedor"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-600 mb-1.5">Participantes</label>
                <input
                  type="text"
                  value={form.participantes}
                  onChange={(e) => setForm({ ...form, participantes: e.target.value })}
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all"
                  placeholder="Joao Silva, Maria Santos"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-600 mb-1.5">Duracao (min)</label>
                <input
                  type="number"
                  value={form.duracaoMinutos}
                  onChange={(e) => setForm({ ...form, duracaoMinutos: e.target.value })}
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all"
                  placeholder="30"
                />
              </div>
            </div>
            <div className="mb-6">
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-sm font-medium text-slate-600">
                  Transcricao da Reuniao <span className="text-rose-500">*</span>
                </label>
                <VoiceRecorder
                  currentText={form.textoOriginal}
                  onTranscript={(text) => setForm({ ...form, textoOriginal: text })}
                />
              </div>
              <textarea
                value={form.textoOriginal}
                required
                rows={8}
                onChange={(e) => setForm({ ...form, textoOriginal: e.target.value })}
                className="w-full border border-slate-300 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all resize-none"
                placeholder="Cole aqui o texto da transcricao ou clique em 'Gravar Reuniao' para transcrever em tempo real..."
              />
            </div>
            <div className="flex items-center gap-4">
              <button
                type="submit"
                disabled={submitting}
                className="flex items-center gap-2 px-6 py-2.5 bg-gradient-to-r from-emerald-500 to-emerald-600 text-white rounded-xl hover:shadow-md disabled:opacity-50 transition-all duration-200 font-semibold text-sm"
              >
                {submitting ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    Analisando com IA...
                  </>
                ) : (
                  <>
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z" />
                    </svg>
                    Enviar e Analisar
                  </>
                )}
              </button>
              {submitting && (
                <p className="text-sm text-slate-400">A IA esta processando a transcricao...</p>
              )}
            </div>
          </form>
        )}

        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 gap-4">
            <div className="relative">
              <div className="w-12 h-12 rounded-full border-4 border-slate-200" />
              <div className="w-12 h-12 rounded-full border-4 border-blue-500 border-t-transparent animate-spin absolute top-0" />
            </div>
            <p className="text-slate-400 text-sm">Carregando transcricoes...</p>
          </div>
        ) : transcricoes.length === 0 ? (
          <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-16 text-center">
            <svg className="w-16 h-16 text-slate-300 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
            </svg>
            <p className="text-slate-500 font-medium">Nenhuma transcricao encontrada</p>
            <p className="text-sm text-slate-400 mt-1">
              Clique em &quot;Nova Transcricao&quot; para comecar
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {transcricoes.map((t) => (
              <a
                key={t.id}
                href={`/transcricoes/${t.id}`}
                className="block bg-white rounded-2xl shadow-sm border border-slate-200 p-6 hover:shadow-md hover:border-blue-300 transition-all duration-200 group"
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center text-white font-bold text-sm shrink-0">
                      {(t.cliente || "T")[0].toUpperCase()}
                    </div>
                    <div>
                      <h3 className="font-semibold text-slate-800 group-hover:text-blue-700 transition-colors">
                        {t.cliente || `Transcricao #${t.id}`}
                      </h3>
                      {t.vendedor && (
                        <p className="text-xs text-slate-400">Vendedor: {t.vendedor}</p>
                      )}
                    </div>
                    {t.analise && (
                      <SentimentBadge sentimento={t.analise.sentimentoGeral as Sentimento} />
                    )}
                    <span className={`text-xs px-2.5 py-1 rounded-full font-semibold ${
                      t.status === "PROCESSADA" ? "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200"
                        : t.status === "ERRO" ? "bg-rose-50 text-rose-700 ring-1 ring-rose-200"
                        : "bg-amber-50 text-amber-700 ring-1 ring-amber-200"
                    }`}>
                      {t.status}
                    </span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-slate-400">
                      {new Date(t.dataCriacao).toLocaleString("pt-BR")}
                    </span>
                    <svg className="w-4 h-4 text-slate-300 group-hover:text-blue-500 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
                    </svg>
                  </div>
                </div>
                <p className="text-sm text-slate-500 line-clamp-2 mb-3 leading-relaxed">{t.textoOriginal}</p>
                {t.analise && (
                  <div className="flex gap-4 text-xs">
                    <span className="flex items-center gap-1 text-violet-600 bg-violet-50 px-2 py-1 rounded-lg">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                      {t.analise.oportunidades.length} Oportunidades
                    </span>
                    <span className="flex items-center gap-1 text-rose-600 bg-rose-50 px-2 py-1 rounded-lg">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" /></svg>
                      {t.analise.riscos.length} Riscos
                    </span>
                    <span className="flex items-center gap-1 text-blue-600 bg-blue-50 px-2 py-1 rounded-lg">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}><path strokeLinecap="round" strokeLinejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" /></svg>
                      {t.analise.produtos.length} Produtos
                    </span>
                  </div>
                )}
              </a>
            ))}
          </div>
        )}
      </main>
    </>
  );
}
