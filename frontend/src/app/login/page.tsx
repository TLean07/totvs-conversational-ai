"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { api } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.auth.refresh().then((session) => {
      if (session) router.replace("/");
    });
  }, [router]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      await api.auth.login(email, password);
      router.replace("/");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Nao foi possivel entrar");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#eef3f8] text-slate-900">
      <header className="bg-gradient-to-r from-[#173a64] to-[#2868a8] text-white shadow-lg">
        <div className="mx-auto flex h-16 max-w-6xl items-center px-6">
          <div className="flex items-center gap-3">
            <Image
              src="/totvs-logo.png"
              alt="TOTVS"
              width={130}
              height={32}
              className="h-8 w-auto object-contain drop-shadow-sm"
            />
            <div className="hidden border-l border-white/30 py-1 pl-3 sm:block">
              <span className="text-[13px] font-light uppercase tracking-wide text-white/90">
                Conversational Intelligence
              </span>
            </div>
          </div>
        </div>
      </header>

      <section className="mx-auto grid min-h-[calc(100vh-4rem)] max-w-6xl grid-cols-1 items-center gap-10 px-6 py-10 lg:grid-cols-[1fr_420px]">
        <div className="hidden lg:block">
          <div className="max-w-xl">
            <div className="mb-6 inline-flex items-center gap-2 rounded bg-white px-3 py-2 text-sm font-semibold text-[#1a365d] shadow-sm ring-1 ring-slate-200">
              <span className="h-2 w-2 rounded-full bg-emerald-500" />
              Ambiente seguro
            </div>
            <h1 className="text-4xl font-bold leading-tight text-[#10294a]">
              Inteligencia conversacional para times que precisam agir rapido.
            </h1>
            <p className="mt-4 max-w-lg text-base leading-7 text-slate-600">
              Acesse transcricoes, alertas e analises comerciais com seguranca, rastreabilidade e controles por usuario.
            </p>
            <div className="mt-8 grid max-w-lg grid-cols-3 gap-3">
              <div className="rounded-lg bg-white p-4 shadow-sm ring-1 ring-slate-200">
                <p className="text-xs font-semibold uppercase text-slate-400">Dados</p>
                <p className="mt-1 text-sm font-semibold text-slate-700">Protegidos</p>
              </div>
              <div className="rounded-lg bg-white p-4 shadow-sm ring-1 ring-slate-200">
                <p className="text-xs font-semibold uppercase text-slate-400">Acesso</p>
                <p className="mt-1 text-sm font-semibold text-slate-700">Autenticado</p>
              </div>
              <div className="rounded-lg bg-white p-4 shadow-sm ring-1 ring-slate-200">
                <p className="text-xs font-semibold uppercase text-slate-400">IA</p>
                <p className="mt-1 text-sm font-semibold text-slate-700">Integrada</p>
              </div>
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="w-full rounded-lg border border-slate-200 bg-white p-8 shadow-xl shadow-slate-200/70">
          <div className="mb-7">
            <Image src="/totvs-logo.png" alt="TOTVS" width={146} height={36} className="h-9 w-auto object-contain" />
            <h2 className="mt-6 text-2xl font-bold text-[#10294a]">Entrar na plataforma</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              Use suas credenciais para acessar o painel de inteligencia conversacional.
            </p>
          </div>

          <label className="block text-sm font-semibold text-slate-700 mb-1.5">E-mail</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
            placeholder="seu.email@empresa.com"
            className="w-full rounded-lg border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#2b6cb0] focus:bg-white focus:ring-4 focus:ring-blue-500/10"
          />

          <label className="block text-sm font-semibold text-slate-700 mt-4 mb-1.5">Senha</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            autoComplete="current-password"
            placeholder="Digite sua senha"
            className="w-full rounded-lg border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#2b6cb0] focus:bg-white focus:ring-4 focus:ring-blue-500/10"
          />

          {error && (
            <p className="mt-4 rounded-lg border border-rose-100 bg-rose-50 px-3 py-2 text-sm text-rose-700">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="mt-6 flex w-full items-center justify-center rounded-lg bg-[#1a365d] px-4 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-[#244d7c] focus:outline-none focus:ring-4 focus:ring-blue-500/20 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? "Entrando..." : "Entrar"}
          </button>
        </form>
      </section>
    </main>
  );
}
