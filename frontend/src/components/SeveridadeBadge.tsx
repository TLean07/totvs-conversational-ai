"use client";

import type { Severidade } from "@/lib/types";

const config: Record<Severidade, { label: string; className: string; icon: string }> = {
  ALTO: { label: "Alto", className: "bg-rose-50 text-rose-700 ring-1 ring-rose-200", icon: "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" },
  MEDIO: { label: "Medio", className: "bg-amber-50 text-amber-700 ring-1 ring-amber-200", icon: "M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" },
  BAIXO: { label: "Baixo", className: "bg-sky-50 text-sky-700 ring-1 ring-sky-200", icon: "M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" },
};

export default function SeveridadeBadge({ severidade }: { severidade: Severidade }) {
  const { label, className, icon } = config[severidade];
  return (
    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold ${className}`}>
      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d={icon} />
      </svg>
      {label}
    </span>
  );
}
