export type Sentimento = "POSITIVO" | "NEGATIVO" | "NEUTRO" | "MISTO";
export type StatusTranscricao = "PENDENTE" | "PROCESSANDO" | "PROCESSADA" | "ERRO";
export type TipoRisco = "CHURN" | "CONCORRENCIA" | "INSATISFACAO" | "PERDA_CONFIANCA";
export type TipoOportunidade = "UPSELL" | "CROSS_SELL" | "NOVO_PRODUTO";
export type Severidade = "ALTO" | "MEDIO" | "BAIXO";
export type StatusAlerta = "NOVO" | "VISTO" | "RESOLVIDO";
export type TipoProduto = "ATUAL" | "OPORTUNIDADE" | "CONCORRENTE";

export interface ProdutoResponse {
  id: number;
  nomeProduto: string;
  tipo: TipoProduto;
  contexto: string;
}

export interface OportunidadeResponse {
  id: number;
  tipo: TipoOportunidade;
  descricao: string;
  produtoRelacionado: string;
  score: number;
  valorEstimado: string;
}

export interface RiscoResponse {
  id: number;
  tipo: TipoRisco;
  descricao: string;
  severidade: Severidade;
  concorrenteMencionado: string | null;
}

export interface AlertaResponse {
  id: number;
  tipo: string;
  mensagem: string;
  prioridade: Severidade;
  status: StatusAlerta;
  dataCriacao: string;
}

export interface AnaliseResponse {
  id: number;
  sentimentoGeral: Sentimento;
  resumo: string;
  personaIdentificada: string;
  budgetIdentificado: string;
  scoreConfiancaVendedor: number;
  dataAnalise: string;
  produtos: ProdutoResponse[];
  oportunidades: OportunidadeResponse[];
  riscos: RiscoResponse[];
  alertas: AlertaResponse[];
}

export interface TranscricaoResponse {
  id: number;
  textoOriginal: string;
  participantes: string;
  dataReuniao: string;
  duracaoMinutos: number;
  cliente: string;
  vendedor: string;
  status: StatusTranscricao;
  dataCriacao: string;
  dataProcessamento: string;
  analise: AnaliseResponse | null;
}

export interface DashboardResponse {
  totalTranscricoes: number;
  transcricoesProcessadas: number;
  totalOportunidades: number;
  totalRiscos: number;
  alertasNovos: number;
  sentimentoDistribuicao: Record<string, number>;
  riscoPorTipo: Record<string, number>;
  oportunidadePorTipo: Record<string, number>;
  alertasRecentes: AlertaResponse[];
  ultimasTranscricoes: TranscricaoResponse[];
}
