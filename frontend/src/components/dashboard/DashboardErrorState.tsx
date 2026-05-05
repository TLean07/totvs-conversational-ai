"use client";

import Navbar from "@/components/Navbar";

interface DashboardErrorStateProps {
  message: string;
  onRetry: () => void;
}

export default function DashboardErrorState({ message, onRetry }: DashboardErrorStateProps) {
  return (
    <>
      <Navbar />
      <main className="mx-auto max-w-3xl px-4 py-16">
        <section className="rounded-3xl border border-rose-200 bg-white p-8 text-center shadow-sm shadow-rose-100">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-rose-50 text-rose-500 ring-1 ring-rose-100">
            <svg aria-hidden="true" className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
            </svg>
          </div>
          <h1 className="text-xl font-semibold text-slate-900">Erro ao carregar dashboard</h1>
          <p className="mx-auto mt-2 max-w-xl text-sm text-slate-500">{message || "Os dados nao estao disponiveis no momento."}</p>
          <div className="mt-6 flex flex-col justify-center gap-3 sm:flex-row">
            <button
              type="button"
              onClick={onRetry}
              className="inline-flex items-center justify-center rounded-xl bg-[#1f5d9d] px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-[#184b80] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
            >
              Tentar novamente
            </button>
            <a
              href="/login"
              className="inline-flex items-center justify-center rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
            >
              Voltar para login
            </a>
          </div>
        </section>
      </main>
    </>
  );
}
