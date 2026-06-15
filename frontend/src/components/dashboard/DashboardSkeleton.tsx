"use client";

import Navbar from "@/components/Navbar";

export default function DashboardSkeleton() {
  return (
    <>
      <Navbar />
      <main className="flex min-h-[calc(100vh-4rem)] items-center justify-center px-4 py-12">
        <section className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-xl shadow-slate-200/70">
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-50 text-[#1f5d9d] ring-1 ring-blue-100">
            <div className="h-9 w-9 animate-spin rounded-full border-4 border-blue-100 border-t-[#1f5d9d]" />
          </div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-blue-700">
            LeanIA
          </p>
          <h1 className="mt-2 text-xl font-bold text-slate-900">
            Carregando dashboard
          </h1>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            Estamos buscando indicadores, alertas e análises mais recentes da sua operação.
          </p>
          <div className="mt-6 overflow-hidden rounded-full bg-slate-100">
            <div className="h-2 w-1/2 animate-pulse rounded-full bg-gradient-to-r from-[#1f5d9d] to-[#2b6cb0]" />
          </div>
        </section>
      </main>
    </>
  );
}
