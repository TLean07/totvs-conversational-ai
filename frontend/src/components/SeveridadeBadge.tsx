"use client";

import type { Severidade } from "@/lib/types";

const config: Record<Severidade, { label: string; className: string }> = {
  ALTO: { label: "Alto", className: "bg-red-100 text-red-800" },
  MEDIO: { label: "Medio", className: "bg-yellow-100 text-yellow-800" },
  BAIXO: { label: "Baixo", className: "bg-blue-100 text-blue-800" },
};

export default function SeveridadeBadge({ severidade }: { severidade: Severidade }) {
  const { label, className } = config[severidade];
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${className}`}>
      {label}
    </span>
  );
}
