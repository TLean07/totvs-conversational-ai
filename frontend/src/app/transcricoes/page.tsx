"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { TranscricaoResponse, Sentimento } from "@/lib/types";
import Navbar from "@/components/Navbar";
import SentimentBadge from "@/components/SentimentBadge";

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
      <main className="max-w-7xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-900">Transcricoes</h1>
          <button
            onClick={() => setShowForm(!showForm)}
            className="px-4 py-2 bg-[#1d4a7a] text-white rounded-lg hover:bg-[#163d66] transition-colors"
          >
            {showForm ? "Cancelar" : "+ Nova Transcricao"}
          </button>
        </div>

        {showForm && (
          <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border p-6 mb-6">
            <h2 className="text-lg font-semibold mb-4">Enviar Nova Transcricao</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Cliente</label>
                <input type="text" value={form.cliente}
                  onChange={(e) => setForm({ ...form, cliente: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="Nome do cliente" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Vendedor</label>
                <input type="text" value={form.vendedor}
                  onChange={(e) => setForm({ ...form, vendedor: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="Nome do vendedor" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Participantes</label>
                <input type="text" value={form.participantes}
                  onChange={(e) => setForm({ ...form, participantes: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="Joao, Maria, Pedro" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Duracao (min)</label>
                <input type="number" value={form.duracaoMinutos}
                  onChange={(e) => setForm({ ...form, duracaoMinutos: e.target.value })}
                  className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="30" />
              </div>
            </div>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Transcricao da Reuniao *
              </label>
              <textarea value={form.textoOriginal} required rows={6}
                onChange={(e) => setForm({ ...form, textoOriginal: e.target.value })}
                className="w-full border rounded-lg px-3 py-2 text-sm"
                placeholder="Cole aqui o texto da transcricao da reuniao..." />
            </div>
            <button type="submit" disabled={submitting}
              className="px-6 py-2 bg-[#4CAF50] text-white rounded-lg hover:bg-[#45a049] disabled:opacity-50 transition-colors">
              {submitting ? "Analisando com IA..." : "Enviar e Analisar"}
            </button>
            {submitting && (
              <p className="text-sm text-gray-500 mt-2">
                Aguarde... A IA esta processando a transcricao.
              </p>
            )}
          </form>
        )}

        {loading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-700" />
          </div>
        ) : transcricoes.length === 0 ? (
          <div className="bg-white rounded-xl shadow-sm border p-12 text-center">
            <p className="text-gray-500">Nenhuma transcricao encontrada.</p>
            <p className="text-sm text-gray-400 mt-1">
              Clique em &quot;+ Nova Transcricao&quot; para comecar.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {transcricoes.map((t) => (
              <a key={t.id} href={`/transcricoes/${t.id}`}
                className="block bg-white rounded-xl shadow-sm border p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <h3 className="font-semibold text-gray-800">
                      {t.cliente || `Transcricao #${t.id}`}
                    </h3>
                    {t.analise && (
                      <SentimentBadge sentimento={t.analise.sentimentoGeral as Sentimento} />
                    )}
                    <span className={`text-xs px-2 py-0.5 rounded-full ${
                      t.status === "PROCESSADA" ? "bg-green-100 text-green-700"
                        : t.status === "ERRO" ? "bg-red-100 text-red-700"
                        : "bg-yellow-100 text-yellow-700"
                    }`}>
                      {t.status}
                    </span>
                  </div>
                  <span className="text-xs text-gray-400">
                    {new Date(t.dataCriacao).toLocaleString("pt-BR")}
                  </span>
                </div>
                <p className="text-sm text-gray-600 line-clamp-2 mb-3">{t.textoOriginal}</p>
                {t.analise && (
                  <div className="flex gap-4 text-xs text-gray-500">
                    <span>Oportunidades: {t.analise.oportunidades.length}</span>
                    <span>Riscos: {t.analise.riscos.length}</span>
                    <span>Produtos: {t.analise.produtos.length}</span>
                    {t.vendedor && <span>Vendedor: {t.vendedor}</span>}
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
