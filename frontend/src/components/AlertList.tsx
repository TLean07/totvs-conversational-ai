"use client";

import type { AlertaResponse } from "@/lib/types";
import SeveridadeBadge from "./SeveridadeBadge";

interface AlertListProps {
  alertas: AlertaResponse[];
  onMarcarVisto?: (id: number) => void;
  onMarcarResolvido?: (id: number) => void;
}

export default function AlertList({ alertas, onMarcarVisto, onMarcarResolvido }: AlertListProps) {
  if (alertas.length === 0) {
    return <p className="text-gray-500 text-center py-8">Nenhum alerta encontrado.</p>;
  }

  return (
    <div className="space-y-3">
      {alertas.map((alerta) => (
        <div
          key={alerta.id}
          className="border rounded-lg p-4 bg-white shadow-sm hover:shadow-md transition-shadow"
        >
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <SeveridadeBadge severidade={alerta.prioridade} />
                <span className="text-xs text-gray-500 font-mono">{alerta.tipo}</span>
              </div>
              <p className="text-sm text-gray-800">{alerta.mensagem}</p>
              <p className="text-xs text-gray-400 mt-1">
                {new Date(alerta.dataCriacao).toLocaleString("pt-BR")}
              </p>
            </div>
            {alerta.status === "NOVO" && (
              <div className="flex gap-1 ml-3">
                {onMarcarVisto && (
                  <button
                    onClick={() => onMarcarVisto(alerta.id)}
                    className="text-xs px-2 py-1 bg-blue-100 text-blue-700 rounded hover:bg-blue-200"
                  >
                    Visto
                  </button>
                )}
                {onMarcarResolvido && (
                  <button
                    onClick={() => onMarcarResolvido(alerta.id)}
                    className="text-xs px-2 py-1 bg-green-100 text-green-700 rounded hover:bg-green-200"
                  >
                    Resolver
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
