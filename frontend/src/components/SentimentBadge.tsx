"use client";

import type { Sentimento } from "@/lib/types";

const config: Record<Sentimento, { label: string; className: string; dot: string }> = {
  POSITIVO: { label: "Positivo", className: "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200", dot: "bg-emerald-500" },
  NEGATIVO: { label: "Negativo", className: "bg-rose-50 text-rose-700 ring-1 ring-rose-200", dot: "bg-rose-500" },
  NEUTRO: { label: "Neutro", className: "bg-slate-50 text-slate-600 ring-1 ring-slate-200", dot: "bg-slate-400" },
  MISTO: { label: "Misto", className: "bg-amber-50 text-amber-700 ring-1 ring-amber-200", dot: "bg-amber-500" },
};

export default function SentimentBadge({ sentimento }: { sentimento: Sentimento }) {
  const { label, className, dot } = config[sentimento];
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${className}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${dot}`} />
      {label}
    </span>
  );
}
