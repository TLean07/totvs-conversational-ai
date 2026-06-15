import type {
  OportunidadeConsolidada,
  OportunidadeResponse,
  RiscoConsolidado,
  RiscoResponse,
  Severidade,
  TipoOportunidade,
  TipoRisco,
  TranscricaoResponse,
} from "./types";

export interface RiscoDetalhado {
  id: string;
  transcricaoId: string;
  cliente: string;
  categoria: string;
  tipo: TipoRisco;
  severidade: Severidade;
  ocorrencias: number;
  evidencias: string[];
  recomendacao: string;
  legado: boolean;
}

export interface OportunidadeDetalhada {
  id: string;
  transcricaoId: string;
  cliente: string;
  categoria: string;
  tipo: TipoOportunidade;
  score: number;
  ocorrencias: number;
  evidencias: string[];
  recomendacao: string;
  legado: boolean;
}

export function extrairRiscosDetalhados(transcricoes: TranscricaoResponse[]) {
  return transcricoes.flatMap((transcricao) => {
    const consolidados = preferirConsolidados(
      transcricao.analise?.riscosConsolidados,
      transcricao.riscosConsolidados
    );

    if (consolidados.length > 0) {
      return consolidados.map((risco, index) =>
        riscoConsolidadoParaDetalhe(risco, transcricao, index)
      );
    }

    return (transcricao.analise?.riscos ?? []).map((risco, index) =>
      riscoLegadoParaDetalhe(risco, transcricao, index)
    );
  });
}

export function extrairOportunidadesDetalhadas(transcricoes: TranscricaoResponse[]) {
  return transcricoes.flatMap((transcricao) => {
    const consolidadas = preferirConsolidados(
      transcricao.analise?.oportunidadesConsolidadas,
      transcricao.oportunidadesConsolidadas
    );

    if (consolidadas.length > 0) {
      return consolidadas.map((oportunidade, index) =>
        oportunidadeConsolidadaParaDetalhe(oportunidade, transcricao, index)
      );
    }

    return (transcricao.analise?.oportunidades ?? []).map((oportunidade, index) =>
      oportunidadeLegadaParaDetalhe(oportunidade, transcricao, index)
    );
  });
}

export function contarOcorrencias<T extends { ocorrencias: number }>(items: T[]) {
  return items.reduce((total, item) => total + Math.max(0, item.ocorrencias || 0), 0);
}

export function contarPartesAnalisadas(transcricoes: TranscricaoResponse[]) {
  return transcricoes.reduce((total, transcricao) => {
    const metricas = transcricao.analise?.metricasAgregadas ?? transcricao.metricasAgregadas;
    return total + Math.max(0, metricas?.partesAnalisadas ?? 0);
  }, 0);
}

export function totalRiscosDaTranscricao(transcricao: TranscricaoResponse) {
  const consolidados = preferirConsolidados(
    transcricao.analise?.riscosConsolidados,
    transcricao.riscosConsolidados
  );
  if (consolidados.length > 0) {
    return consolidados.length;
  }

  return transcricao.analise?.riscos.length ?? 0;
}

export function totalOportunidadesDaTranscricao(transcricao: TranscricaoResponse) {
  const consolidadas = preferirConsolidados(
    transcricao.analise?.oportunidadesConsolidadas,
    transcricao.oportunidadesConsolidadas
  );
  if (consolidadas.length > 0) {
    return consolidadas.length;
  }

  return transcricao.analise?.oportunidades.length ?? 0;
}

function riscoConsolidadoParaDetalhe(
  risco: RiscoConsolidado,
  transcricao: TranscricaoResponse,
  index: number
): RiscoDetalhado {
  return {
    id: `${transcricao.id}-risco-consolidado-${index}`,
    transcricaoId: transcricao.id,
    cliente: transcricao.cliente || "Cliente não informado",
    categoria: risco.categoria || "Risco",
    tipo: normalizarTipoRisco(risco.categoria),
    severidade: risco.severidade || "MEDIO",
    ocorrencias: Math.max(1, risco.ocorrencias || 0),
    evidencias: limitarEvidencias(risco.evidencias),
    recomendacao: risco.recomendacao || "Priorizar revisão comercial e acompanhar a evolução do cliente.",
    legado: false,
  };
}

function riscoLegadoParaDetalhe(
  risco: RiscoResponse,
  transcricao: TranscricaoResponse,
  index: number
): RiscoDetalhado {
  return {
    id: `${transcricao.id}-risco-legado-${index}`,
    transcricaoId: transcricao.id,
    cliente: transcricao.cliente || "Cliente não informado",
    categoria: risco.tipo,
    tipo: risco.tipo,
    severidade: risco.severidade,
    ocorrencias: 1,
    evidencias: [risco.descricao].filter(Boolean),
    recomendacao: risco.concorrenteMencionado
      ? `Comparar proposta do concorrente ${risco.concorrenteMencionado} e reforcar diferenciais TOTVS.`
      : "Validar causa raiz e criar plano de acompanhamento.",
    legado: true,
  };
}

function oportunidadeConsolidadaParaDetalhe(
  oportunidade: OportunidadeConsolidada,
  transcricao: TranscricaoResponse,
  index: number
): OportunidadeDetalhada {
  return {
    id: `${transcricao.id}-oportunidade-consolidada-${index}`,
    transcricaoId: transcricao.id,
    cliente: transcricao.cliente || "Cliente não informado",
    categoria: oportunidade.categoria || "Oportunidade",
    tipo: normalizarTipoOportunidade(oportunidade.categoria),
    score: oportunidade.score ?? 0,
    ocorrencias: Math.max(1, oportunidade.ocorrencias || 0),
    evidencias: limitarEvidencias(oportunidade.evidencias),
    recomendacao: oportunidade.recomendacao || "Validar aderência da oportunidade e encaminhar próxima ação comercial.",
    legado: false,
  };
}

function oportunidadeLegadaParaDetalhe(
  oportunidade: OportunidadeResponse,
  transcricao: TranscricaoResponse,
  index: number
): OportunidadeDetalhada {
  return {
    id: `${transcricao.id}-oportunidade-legada-${index}`,
    transcricaoId: transcricao.id,
    cliente: transcricao.cliente || "Cliente não informado",
    categoria: oportunidade.tipo,
    tipo: oportunidade.tipo,
    score: oportunidade.score ?? 0,
    ocorrencias: 1,
    evidencias: [oportunidade.descricao].filter(Boolean),
    recomendacao: oportunidade.produtoRelacionado
      ? `Validar interesse em ${oportunidade.produtoRelacionado} e estimar proposta.`
      : "Transformar o sinal comercial em próxima ação com responsável definido.",
    legado: true,
  };
}

function preferirConsolidados<T>(primary?: T[] | null, secondary?: T[] | null) {
  if (primary && primary.length > 0) return primary;
  if (secondary && secondary.length > 0) return secondary;
  return [];
}

function limitarEvidencias(evidencias?: string[] | null) {
  return (evidencias ?? [])
    .filter((evidencia) => Boolean(evidencia?.trim()))
    .slice(0, 5);
}

function normalizarTipoRisco(categoria?: string | null): TipoRisco {
  const normalized = normalizar(categoria);
  if (normalized.includes("CHURN") || normalized.includes("RETENCAO")) return "CHURN";
  if (normalized.includes("CONCORR")) return "CONCORRENCIA";
  if (normalized.includes("CONFIAN")) return "PERDA_CONFIANCA";
  return "INSATISFACAO";
}

function normalizarTipoOportunidade(categoria?: string | null): TipoOportunidade {
  const normalized = normalizar(categoria);
  if (normalized.includes("UPSELL") || normalized.includes("MODULO") || normalized.includes("LICENCA")) return "UPSELL";
  if (normalized.includes("CROSS")) return "CROSS_SELL";
  return "NOVO_PRODUTO";
}

function normalizar(value?: string | null) {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
}
