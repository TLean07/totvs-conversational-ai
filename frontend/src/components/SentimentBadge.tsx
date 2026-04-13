"use client";

import type { Sentimento } from "@/lib/types";

const config: Record<Sentimento, { label: string; className: string }> = {
  POSITIVO: { label: "Positivo", className: "bg-green-100 text-green-800" },
  NEGATIVO: { label: "Negativo", className: "bg-red-100 text-red-800" },
  NEUTRO: { label: "Neutro", className: "bg-gray-100 text-gray-800" },
  MISTO: { label: "Misto", className: "bg-yellow-100 text-yellow-800" },
};

export default function SentimentBadge({ sentimento }: { sentimento: Sentimento }) {
  const { label, className } = config[sentimento];
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${className}`}>
      {label}
    </span>
  );
}
