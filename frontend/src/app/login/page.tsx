"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
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
    <main className="min-h-screen flex items-center justify-center bg-slate-100 px-6">
      <form onSubmit={handleSubmit} className="w-full max-w-md bg-white border border-slate-200 rounded-2xl shadow-sm p-8">
        <img src="/totvs-logo.png" alt="TOTVS" className="h-9 w-auto mb-6" />
        <h1 className="text-xl font-bold text-slate-800">Acessar plataforma</h1>
        <p className="text-sm text-slate-500 mt-1 mb-6">Entre com o usuario configurado no backend.</p>

        <label className="block text-sm font-medium text-slate-600 mb-1.5">E-mail</label>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          autoComplete="email"
          className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"
        />

        <label className="block text-sm font-medium text-slate-600 mt-4 mb-1.5">Senha</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          minLength={8}
          autoComplete="current-password"
          className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"
        />

        {error && (
          <p className="mt-4 text-sm text-rose-600 bg-rose-50 border border-rose-100 rounded-xl px-3 py-2">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={loading}
          className="mt-6 w-full px-4 py-2.5 rounded-xl bg-[#1a365d] text-white font-semibold text-sm hover:bg-[#244d7c] disabled:opacity-60 transition-colors"
        >
          {loading ? "Entrando..." : "Entrar"}
        </button>
      </form>
    </main>
  );
}
