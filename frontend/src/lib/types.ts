export type Sentimento = "POSITIVO" | "NEGATIVO" | "NEUTRO" | "MISTO";
export type StatusTranscricao = "PENDENTE" | "PROCESSANDO" | "PROCESSADA" | "ERRO";
export type TipoRisco = "CHURN" | "CONCORRENCIA" | "INSATISFACAO" | "PERDA_CONFIANCA";
export type TipoOportunidade = "UPSELL" | "CROSS_SELL" | "NOVO_PRODUTO";
export type Severidade = "ALTO" | "MEDIO" | "BAIXO";
export type StatusAlerta = "NOVO" | "VISTO" | "RESOLVIDO";
export type TipoProduto = "ATUAL" | "OPORTUNIDADE" | "CONCORRENTE";
export type UserRole = "ADMIN" | "USER";

export interface AuthUser {
  id: string;
  email: string;
  name: string;
  role: UserRole;
}

export interface AuthResponse {
  accessToken: string;
  expiresInSeconds: number;
  user: AuthUser;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface UserResponse {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
}

export interface ProdutoResponse {
  nomeProduto: string;
  tipo: TipoProduto;
  contexto: string;
}

export interface OportunidadeResponse {
  tipo: TipoOportunidade;
  descricao: string;
  produtoRelacionado: string;
  score: number;
  valorEstimado: string;
}

export interface RiscoResponse {
  tipo: TipoRisco;
  descricao: string;
  severidade: Severidade;
  concorrenteMencionado: string | null;
}

export interface AlertaResponse {
  id: string;
  tipo: string;
  titulo: string;
  categoria: string;
  mensagem: string;
  motivo: string;
  recomendacao: string;
  ocorrencias: number;
  prioridade: Severidade;
  status: StatusAlerta;
  dataCriacao: string;
}

export interface MetricasAgregadas {
  totalPartes: number;
  partesAnalisadas: number;
  partesNegativas: number;
  partesNeutras: number;
  partesPositivas: number;
  riscosDetectados: number;
  oportunidadesDetectadas: number;
  churnsPotenciais: number;
  mencoesCriticas: number;
  alertasConsolidados: number;
  chunksProcessados: number;
}

export interface RiscoConsolidado {
  categoria: string;
  severidade: Severidade;
  ocorrencias: number;
  evidencias: string[];
  recomendacao: string;
}

export interface OportunidadeConsolidada {
  categoria: string;
  score: number;
  ocorrencias: number;
  evidencias: string[];
  recomendacao: string;
}

export interface AlertaConsolidado {
  titulo: string;
  categoria: string;
  severidade: Severidade;
  ocorrencias: number;
  motivo: string;
  recomendacao: string;
}

export interface ItemCritico {
  tipo: string;
  categoria: string;
  trecho: string;
  severidade: Severidade;
}

export interface AnaliseResponse {
  id: string;
  sentimentoGeral: Sentimento;
  resumo: string;
  personaIdentificada: string;
  budgetIdentificado: string;
  scoreConfiancaVendedor: number;
  scoreSentimento: number;
  metricasAgregadas: MetricasAgregadas | null;
  resumoExecutivo: string;
  dataAnalise: string;
  produtos: ProdutoResponse[];
  oportunidades: OportunidadeResponse[];
  riscos: RiscoResponse[];
  alertas: AlertaResponse[];
  riscosConsolidados: RiscoConsolidado[];
  oportunidadesConsolidadas: OportunidadeConsolidada[];
  alertasConsolidados: AlertaConsolidado[];
  itensCriticos: ItemCritico[];
}

export interface TranscricaoResponse {
  id: string;
  textoOriginal: string;
  participantes: string;
  dataReuniao: string;
  duracaoMinutos: number;
  cliente: string;
  vendedor: string;
  origemTranscricao: string | null;
  meetingId: string | null;
  meetingDate: string | null;
  format: string | null;
  meetingStatus: string | null;
  duration: string | null;
  codt: string | null;
  external: boolean | null;
  createdAtSource: string | null;
  transcriptionText: string | null;
  sourceFileName: string | null;
  importBatchId: string | null;
  importedAt: string | null;
  missingMeetingId: boolean;
  audioFileName: string | null;
  transcriptionModel: string | null;
  transcriptionLanguage: string | null;
  audioDurationSeconds: number | null;
  transcriptionProcessingTimeMs: number | null;
  transcriptionWarnings: string[];
  status: StatusTranscricao;
  dataCriacao: string;
  dataProcessamento: string;
  metricasAgregadas: MetricasAgregadas | null;
  resumoExecutivo: string;
  riscosConsolidados: RiscoConsolidado[];
  oportunidadesConsolidadas: OportunidadeConsolidada[];
  alertasConsolidados: AlertaConsolidado[];
  itensCriticos: ItemCritico[];
  analise: AnaliseResponse | null;
}

export interface AnaliseFeedbackResponse {
  id: string;
  transcricaoId: string;
  analiseId: string;
  util: boolean;
  ajuste: string;
  createdAt: string;
}

export interface DashboardResponse {
  totalTranscricoes: number;
  transcricoesProcessadas: number;
  totalOportunidades: number;
  totalRiscos: number;
  alertasNovos: number;
  totalPartes: number;
  partesAnalisadas: number;
  partesNegativas: number;
  partesPositivas: number;
  partesNeutras: number;
  churnsPotenciais: number;
  mencoesCriticas: number;
  alertasConsolidados: number;
  sentimentoDistribuicao: Record<string, number>;
  riscoPorTipo: Record<string, number>;
  oportunidadePorTipo: Record<string, number>;
  alertasRecentes: AlertaResponse[];
  ultimasTranscricoes: TranscricaoResponse[];
}

export interface TranscricaoImportResponse {
  success: boolean;
  sourceFile: string;
  batchId: string;
  found: number;
  imported: number;
  skipped: number;
  failed: number;
  items: TranscricaoImportItemResponse[];
  totalRegistros: number;
  importados: number;
  ignorados: number;
  avisos: string[];
}

export interface TranscricaoImportItemResponse {
  index: number;
  meetingId: string;
  transcriptionId: string | null;
  status: "IMPORTED" | "SKIPPED_DUPLICATE" | "FAILED";
  reason: string | null;
  warnings: string[];
}
