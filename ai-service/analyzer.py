"""
LeanIA - motor de analise de transcricoes de reunioes comerciais (TOTVS).

Pipeline (100% local, sem APIs pagas):
  1. Normalizacao e segmentacao: locutores ([LOCUTOR N]:), turnos e sentencas.
  2. Papel de cada locutor (vendedor x cliente) por marcadores linguisticos.
  3. Entidades: produtos TOTVS (atual x oportunidade), concorrentes, modulos/areas.
  4. Sentimento por sentenca com lexico pt-BR, negacao, intensificadores e aspecto
     (area da empresa citada), agregado em POSITIVO / NEGATIVO / NEUTRO / MISTO.
     Um modelo local do Hugging Face (opcional) refina o score numerico.
  5. Oportunidades (upsell / cross-sell / novo produto), riscos (churn, concorrencia,
     insatisfacao, perda de confianca), persona do interlocutor, budget, confianca no
     vendedor, proximos passos, duvidas da jornada, elogios, reclamacoes e observacoes.
  6. Indicadores de qualidade da conversa (quem falou mais, perguntas, escuta ativa).

Exemplo de referencia (briefing do Challenge FIAP x TOTVS, pagina 15):
  "o nosso Protheus esta atendendo o backoffice, mas o time de RH esta sofrendo muito
   com a folha manual. O pessoal viu uma demo da Senior e gostou, mas eu prefiro
   consolidar tudo na TOTVS se o RM for realmente integrado. (...) nao mencione esse
   valor de R$ 50 mil (...) para o meu CFO"
  -> Protheus (atual) e RM (oportunidade); alerta Senior/RH; upsell Folha de Pagamento;
     persona Gestor Operacional; budget R$ 50.000,00; sentimento misto.
"""

from __future__ import annotations

import logging
import re
import unicodedata
from dataclasses import dataclass, field

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Catalogos
# ---------------------------------------------------------------------------

# alias (sem acento, minusculo) -> nome canonico
PRODUTOS_TOTVS: dict[str, str] = {
    "protheus": "TOTVS Protheus",
    "proteus": "TOTVS Protheus",   # grafias comuns em transcricao automatica
    "protheos": "TOTVS Protheus",
    "proteos": "TOTVS Protheus",
    "microsiga": "TOTVS Protheus",
    "totvs rm": "TOTVS RM",
    "rm": "TOTVS RM",
    "rm labore": "TOTVS RM",
    "fluig": "TOTVS Fluig",
    "datasul": "TOTVS Datasul",
    "winthor": "TOTVS Winthor",
    "techfin": "TOTVS Techfin",
    "carol": "TOTVS Carol",
    "rd station": "RD Station",
    "rdstation": "RD Station",
    "totvs moda": "TOTVS Moda",
    "totvs saude": "TOTVS Saude",
    "totvs logistica": "TOTVS Logistica",
    "totvs educacional": "TOTVS Educacional",
    "totvs hotelaria": "TOTVS Hotelaria",
    "totvs construcao": "TOTVS Construcao",
    "totvs juridico": "TOTVS Juridico",
    "totvs agro": "TOTVS Agro",
    "totvs varejo": "TOTVS Varejo",
    "totvs cloud": "TOTVS Cloud",
    "totvs assinatura eletronica": "TOTVS Assinatura Eletronica",
    "smart link": "TOTVS Smart Link",
    "gestao de pessoas": "TOTVS RM",
}

CONCORRENTES: dict[str, str] = {
    "senior sistemas": "Senior",
    "senior": "Senior",
    "sap business one": "SAP Business One",
    "sap b1": "SAP Business One",
    "sap": "SAP",
    "oracle netsuite": "Oracle NetSuite",
    "netsuite": "Oracle NetSuite",
    "oracle erp": "Oracle",
    "oracle": "Oracle",
    "sankhya": "Sankhya",
    "linx": "Linx",
    "omie": "Omie",
    "bling": "Bling",
    "conta azul": "Conta Azul",
    "tiny erp": "Tiny ERP",
    "sage": "Sage",
    "alterdata": "Alterdata",
    "mega sistemas": "Mega Sistemas",
    "nasajon": "Nasajon",
    "dynamics": "Microsoft Dynamics",
    "workday": "Workday",
    "gupy": "Gupy",
    "sistema senior": "Senior",
}

# Concorrentes com nome ambiguo: exigem contexto de sistema/proposta na mesma sentenca
CONCORRENTES_AMBIGUOS = {"senior", "oracle", "sage", "mega sistemas", "dynamics"}
CONTEXTO_SISTEMA = [
    "demo", "demonstracao", "proposta", "cotacao", "orcamento", "sistema", "erp", "solucao",
    "software", "plataforma", "ferramenta", "modulo", "licenca", "migrar", "trocar", "avaliar",
    "avaliando", "gostou", "gostaram", "preferem", "comparar", "concorrente", "fornecedor",
    "contrato", "implantar", "folha", "rh", "financeiro", "fiscal", "estoque",
]

# area -> (rotulo, produto TOTVS que atende, modulo sugerido)
AREAS: list[tuple[list[str], str, str, str]] = [
    (["folha de pagamento", "folha", "holerite", "esocial", "e-social", "ponto eletronico",
      "beneficios", "recrutamento", "departamento pessoal", "recursos humanos", "rh",
      "gestao de pessoas", "admissao", "ferias"],
     "RH", "TOTVS RM", "Folha de Pagamento"),
    (["estoque", "inventario", "controle de estoque", "compras", "suprimentos", "almoxarifado"],
     "Estoque", "TOTVS Protheus", "Estoque e Compras"),
    (["backoffice", "back office", "financeiro", "contabil", "contabilidade", "fiscal",
      "faturamento", "tesouraria", "contas a pagar", "contas a receber", "sped",
      "nota fiscal", "nfe", "patrimonio", "ativo fixo", "controladoria"],
     "Backoffice", "TOTVS Protheus", "Backoffice (Financeiro/Fiscal)"),
    (["workflow", "bpm", "ecm", "gestao de documentos", "aprovacoes", "processos internos",
      "assinatura eletronica", "portal"],
     "Processos", "TOTVS Fluig", "Workflow e Documentos"),
    (["producao", "pcp", "chao de fabrica", "manufatura", "mrp", "industria", "fabrica"],
     "Manufatura", "TOTVS Datasul", "Manufatura (PCP)"),
    (["distribuicao", "atacado", "wms", "logistica", "tms", "armazem", "frota", "entregas"],
     "Logistica", "TOTVS Logistica", "Logistica e Distribuicao"),
    (["crm", "marketing", "leads", "automacao de marketing", "funil", "prospeccao"],
     "CRM", "RD Station", "CRM e Marketing"),
    (["credito", "antecipacao", "antecipacao de recebiveis", "pagamentos", "cobranca", "boleto"],
     "Servicos financeiros", "TOTVS Techfin", "Credito e Pagamentos"),
    (["bi", "dashboards", "analytics", "relatorios gerenciais", "indicadores", "inteligencia de dados"],
     "Analytics", "TOTVS Carol", "Analytics"),
    (["saude", "prontuario", "hospital", "clinica", "operadora", "plano de saude"],
     "Saude", "TOTVS Saude", "Gestao de Saude"),
    (["educacional", "escola", "matricula", "alunos", "faculdade", "secretaria academica"],
     "Educacional", "TOTVS Educacional", "Gestao Educacional"),
    (["varejo", "pdv", "loja", "lojas", "frente de caixa", "e-commerce"],
     "Varejo", "TOTVS Varejo", "Varejo e PDV"),
    (["integracao", "integrar", "api", "integrado", "conectar", "sincronizar"],
     "Integracao", "TOTVS Fluig", "Integracao"),
]

# Lexico de sentimento (sem acento). Peso maior = sinal mais forte.
POSITIVOS: dict[str, float] = {
    "esta atendendo": 1.0, "atende bem": 1.2, "atendeu": 0.8, "atendendo": 0.6, "satisfeit": 1.3,
    "content": 0.8, "excelente": 1.3, "otimo": 1.1, "otima": 1.1, "muito bom": 1.1, "muito boa": 1.1,
    "funciona bem": 1.2, "gostamos": 1.0, "gostei": 1.0, "adoramos": 1.3, "perfeito": 1.1,
    "eficiente": 0.9, "produtiv": 0.8, "recomendo": 1.2, "aprovamos": 1.0, "feliz": 1.0,
    "superou": 1.2, "parabens": 1.2, "elogi": 1.0, "resolveu": 0.9, "melhorou": 0.9,
    "confiamos": 1.0, "bem atendid": 1.2, "tranquilo": 0.5, "sem problema": 0.6, "deu certo": 0.9,
    "funcionou": 0.7, "vale a pena": 0.9, "estamos gostando": 1.2, "esta funcionando": 0.8,
}
NEGATIVOS: dict[str, float] = {
    "sofrendo": 1.3, "frustrad": 1.3, "insatisf": 1.3, "decepcion": 1.3, "problema": 0.9,
    "erro": 0.8, "bug": 0.8, "nao funciona": 1.3, "ninguem resolve": 1.4, "reclamando": 1.0,
    "reclamacao": 1.0, "demora": 0.8, "lento": 0.8, "lentidao": 0.9, "trava": 0.9, "travando": 1.0,
    "pior": 0.9, "horrivel": 1.4, "pessimo": 1.4, "nao atende": 1.1, "falhando": 1.0, "caindo": 0.8,
    "instavel": 1.0, "dor": 0.7, "dificuldade": 0.8, "gargalo": 0.9, "retrabalho": 1.0,
    "sem retorno": 1.1, "nao respondem": 1.1, "prejuizo": 1.2, "atraso": 0.8, "manual": 0.5,
    "chamado": 0.4, "cancelar": 0.6, "trocar": 0.4, "perdendo": 0.9, "nao aguento": 1.3,
    "insuportavel": 1.4, "vergonha": 1.2, "absurdo": 1.1, "caro": 0.6, "complicado": 0.7,
    "perde muito tempo": 1.2, "perdemos muito tempo": 1.2, "perde tempo": 1.0, "perdemos tempo": 1.0,
    "perde venda": 1.2, "perdemos venda": 1.2, "perde cliente": 1.2, "nao da conta": 1.1, "sobrecarregad": 1.0,
    "inferno": 1.3, "um saco": 1.0, "terrivel": 1.3, "desastre": 1.3, "nao funciona direito": 1.2,
}
NEGACOES = {"nao", "sem", "nenhum", "nenhuma", "nunca", "jamais", "nem"}
INTENSIFICADORES = {"muito", "muita", "extremamente", "bastante", "demais", "super", "totalmente"}
ATENUADORES = {"pouco", "um pouco", "meio", "levemente"}

# Churn: verbo de saida + objeto que confirma que e sobre o contrato/sistema (evita "cancelar a reuniao")
CHURN_VERBOS = ["cancelar", "cancelamento", "encerrar", "rescindir", "descontinuar", "desistir",
                "nao renovar", "nao vamos renovar", "sair da", "migrar", "trocar", "substituir",
                "avaliar outras", "outras opcoes", "outro fornecedor", "outra solucao"]
CHURN_OBJETOS = ["contrato", "sistema", "licenca", "licencas", "assinatura", "totvs", "servico",
                 "plano", "fornecedor", "erp", "solucao", "ferramenta", "opcoes", "proposta", "modulo"]
CHURN_EXPLICITO = ["churn", "nao renovar", "nao vamos renovar", "encerrar contrato", "cancelar o contrato",
                   "cancelar contrato", "sair da totvs", "trocar de sistema", "trocar de fornecedor",
                   "migrar para", "rescindir"]

PERDA_CONFIANCA = ["perdi confianca", "perdemos confianca", "sem confianca", "nao confio", "desconfi",
                   "prometeram e nao", "nao cumpriu", "nao cumpriram", "quebrou a confianca", "enganad",
                   "mentiu", "mentira", "promessa vazia", "outro vendedor"]
CONFIANCA_POSITIVA = ["confio", "confiamos", "confianca em voces", "parceiro", "parceria", "recomendo",
                      "gosto de trabalhar", "sempre atende", "sempre ajudou", "conto com voce",
                      "entre nos", "nao mencione", "confidencial", "pode confiar", "prefiro consolidar"]

# Desejo de compra em primeira pessoa (frases soltas como "comprar" ou "interesse" geram falso positivo)
OPORTUNIDADE_DESEJO = ["prefiro", "preferimos", "queremos", "quero", "gostaria de", "gostariamos de",
                       "precisamos de", "precisamos", "preciso de", "temos necessidade", "consolidar",
                       "expandir", "ampliar", "implantar", "contratar", "adquirir", "vamos investir",
                       "pretendemos", "planejamos", "unificar", "centralizar", "automatizar",
                       "estamos avaliando", "temos interesse", "interesse em", "queremos contratar",
                       "quero contratar", "vamos fechar", "queremos fechar"]
OPORTUNIDADE_FORTE = ["aprovado", "aprovamos", "orcamento aprovado", "verba aprovada", "diretoria aprovou",
                      "proximo trimestre", "o quanto antes", "urgente", "fechar", "assinar", "quando podemos comecar"]
CONDICIONAIS = ["se o", "se a", "desde que", "caso", "contanto que", "depende"]

PROXIMOS_PASSOS = ["vou enviar", "vamos enviar", "envio a proposta", "proposta formal", "vamos agendar",
                   "vou agendar", "agendar", "amanha", "semana que vem", "proxima semana", "proximo passo",
                   "retorno", "conversar com meu socio", "conversar com a diretoria", "aguardar aprovacao",
                   "te ligo", "te mando", "vou conectar", "reuniao com", "follow up", "follow-up",
                   "depois do", "ate o final do mes", "ate sexta", "segunda-feira"]
DUVIDAS_JORNADA = ["quanto custa", "qual o preco", "qual e o preco", "quanto tempo", "prazo", "como funciona",
                   "integra com", "e se nao der certo", "garantia", "suporte", "implantacao", "treinamento",
                   "quem vai", "como fica", "e o que acontece", "posso", "consigo", "da para", "tem como"]

CARGOS_EXECUTIVO = ["ceo", "presidente", "dono", "dona", "proprietario", "proprietaria", "socio", "socia",
                    "fundador", "diretor geral", "diretora geral", "minha empresa", "meu negocio", "meu socio"]
CARGOS_FINANCEIRO = ["cfo", "diretor financeiro", "diretora financeira", "controller", "financeiro da empresa",
                     "gerente financeiro", "contador", "contadora", "tesouraria"]
CARGOS_TI = ["cto", "cio", "diretor de ti", "gerente de ti", "analista de ti", "arquiteto", "desenvolvedor",
             "infra", "equipe de tecnologia", "time de ti", "nosso ti"]
CARGOS_OPERACIONAL = ["gerente", "gestor", "gestora", "coordenador", "coordenadora", "supervisor", "supervisora",
                      "lider", "head", "manager", "operacao", "nosso time", "time de", "minha equipe",
                      "meu time", "backoffice", "processo", "rotina", "o pessoal", "a equipe"]
TERCEIROS_DECISORES = ["meu cfo", "o cfo", "para o cfo", "meu diretor", "a diretoria", "meu chefe",
                       "meu socio", "o presidente", "o dono", "conselho", "comite"]

VENDEDOR_MARCAS = ["nossa solucao", "nosso produto", "nossa plataforma", "a gente oferece", "oferecemos",
                   "nossa proposta", "vou enviar", "posso te mostrar", "demonstracao", "trial", "posso ajudar",
                   "temos confianca no que fazemos", "nosso time tecnico", "trabalhamos com", "voces querem fazer",
                   "me conta sobre", "qual e o principal desafio", "totvs oferece", "nosso modulo", "conectar voce",
                   "voces querem", "voces precisam", "o que voces", "processo de voces", "me conta", "como funciona hoje",
                   "voces usam", "voces tem", "posso apresentar", "vou apresentar", "nossa equipe vai"]
CLIENTE_MARCAS = ["a gente usa", "usamos", "nosso time", "nossa empresa", "meu cfo", "meu socio", "a gente atua",
                  "queremos", "precisamos", "estamos avaliando", "meu negocio", "nossas lojas", "nosso protheus",
                  "nosso rm", "nosso sistema", "a gente perde", "a gente tem", "nosso contador", "esta atendendo",
                  "sofrendo", "quanto custa", "qual o preco", "quanto tempo leva", "e se nao der certo"]

REGEX_LOCUTOR = re.compile(r"\[LOCUTOR\s*(\d+)\]\s*:", re.IGNORECASE)
REGEX_SENTENCA = re.compile(r"(?<=[.!?])\s+(?=[A-ZÁÉÍÓÚÂÊÔÃÕÇ\[\"'(])|\n+")
# Oracoes adversativas carregam sinais opostos ("X esta bom, mas Y esta ruim"): viram unidades separadas
REGEX_ORACAO = re.compile(r"\s*[,;]?\s+(?=(?:mas|por[eé]m|entretanto|no entanto|contudo|s[oó] que|todavia)\s)", re.IGNORECASE)
REGEX_VALOR = re.compile(
    r"(?P<moeda>r\$\s*)?(?P<num>\d{1,3}(?:[.\s]\d{3})*(?:,\d{1,2})?|\d+(?:[.,]\d+)?)\s*"
    r"(?P<esc>mil(?:hoes|hao|hões|hão)?|k|milh[oõ]es|milh[aã]o|bi(?:lh[oõ]es|lh[aã]o)?|reais)?"
    r"(?P<periodo>\s*(?:/|por)\s*(?:mes|mês|ano))?",
    re.IGNORECASE,
)
REGEX_ANONIMO = re.compile(r"\[(PESSOA|EMPRESA|LOCAL|EMAIL|TELEFONE|CPF|CNPJ)\]", re.IGNORECASE)


# ---------------------------------------------------------------------------
# Utilitarios de texto
# ---------------------------------------------------------------------------

def normalizar(texto: str) -> str:
    """minusculo, sem acento, espacos colapsados; mantem pontuacao para as regex."""
    texto = unicodedata.normalize("NFD", texto or "")
    texto = "".join(c for c in texto if unicodedata.category(c) != "Mn")
    return re.sub(r"[ \t]+", " ", texto.lower()).strip()


def _contem(norm: str, termos) -> bool:
    return any(t in norm for t in termos)


def _contem_palavra(norm: str, termo: str) -> bool:
    """Match por palavra inteira (evita 'rm' dentro de 'forma')."""
    return re.search(r"(?<![a-z0-9])" + re.escape(termo) + r"(?![a-z0-9])", norm) is not None


def _recortar(texto: str, limite: int = 240) -> str:
    texto = re.sub(r"\s+", " ", texto or "").strip()
    return texto if len(texto) <= limite else texto[: limite - 3].rstrip() + "..."


def _formatar_brl(valor: float) -> str:
    inteiro, centavos = divmod(round(valor * 100), 100)
    return f"R$ {inteiro:,}".replace(",", ".") + f",{centavos:02d}"


# ---------------------------------------------------------------------------
# Estruturas
# ---------------------------------------------------------------------------

@dataclass
class Sentenca:
    texto: str
    norm: str
    locutor: str | None
    papel: str  # "vendedor" | "cliente" | "desconhecido"
    indice: int
    pos: float = 0.0
    neg: float = 0.0
    areas: list[str] = field(default_factory=list)
    produtos: list[str] = field(default_factory=list)
    concorrentes: list[str] = field(default_factory=list)

    @property
    def polaridade(self) -> float:
        return self.pos - self.neg


@dataclass
class Turno:
    locutor: str | None
    texto: str


# ---------------------------------------------------------------------------
# Motor
# ---------------------------------------------------------------------------

class TranscricaoAnalyzer:
    def __init__(
        self,
        model_name: str = "nlptown/bert-base-multilingual-uncased-sentiment",
        enable_transformers: bool = True,
        max_sentiment_chunks: int = 8,
    ):
        self.sentiment_pipeline = None
        self.max_sentiment_chunks = max(1, max_sentiment_chunks)

        if enable_transformers:
            try:
                logger.info("Carregando modelo local gratuito de sentimento: %s", model_name)
                from transformers import pipeline

                self.sentiment_pipeline = pipeline(
                    "sentiment-analysis", model=model_name, truncation=True, max_length=512,
                )
                logger.info("Modelo local carregado com sucesso")
            except Exception as exc:  # pragma: no cover - depende do ambiente
                logger.warning("Modelo local indisponivel; analise seguira por regras: %s", exc.__class__.__name__)

    def is_ready(self) -> bool:
        return self.sentiment_pipeline is not None

    # ------------------------------------------------------------------ API

    def analisar(self, texto: str, contexto: dict | None = None) -> dict:
        contexto = contexto or {}
        texto = texto or ""

        # Transcricoes consolidadas geradas pelo backend (cabecalho com metricas) mantem o caminho legado
        metricas_legado = self._extrair_metricas_consolidadas(texto)
        consolidado = metricas_legado["partesAnalisadas"] > 0

        turnos = self._segmentar_turnos(texto)
        papeis = self._inferir_papeis(turnos)
        sentencas = self._segmentar_sentencas(turnos, papeis)
        for s in sentencas:
            self._anotar_sentenca(s, contexto)

        produtos = self._extrair_produtos(sentencas, contexto)
        concorrentes = sorted({c for s in sentencas for c in s.concorrentes})
        sentimento, detalhe_sentimento, score_regras = self._agregar_sentimento(sentencas)
        score_modelo = self._score_modelo(sentencas)
        score_sentimento = round(score_regras if score_modelo is None else (0.6 * score_regras + 0.4 * score_modelo), 2)

        budget, budget_valor, budget_frase = self._extrair_budget(sentencas)
        riscos = self._extrair_riscos(sentencas, concorrentes)
        oportunidades = self._extrair_oportunidades(sentencas, produtos, budget, contexto)
        persona, persona_obs = self._identificar_persona(sentencas)
        confianca = self._calcular_confianca(sentencas, riscos)
        insights = self._insights_adicionais(sentencas, budget, budget_frase, persona_obs)
        indicadores = self._indicadores_qualidade(texto, turnos, papeis, sentencas)

        metricas = metricas_legado if consolidado else self._metricas_por_turnos(turnos, sentencas, riscos, oportunidades)
        riscos_consolidados = self._consolidar_riscos(riscos, metricas)
        oportunidades_consolidadas = self._consolidar_oportunidades(oportunidades, metricas)
        alertas_consolidados = self._gerar_alertas_consolidados(metricas, riscos, concorrentes, consolidado)
        metricas["alertasConsolidados"] = len(alertas_consolidados)
        itens_criticos = self._extrair_itens_criticos(sentencas, riscos)
        proxima_acao = self._sugerir_proxima_acao(sentimento, oportunidades, riscos, concorrentes, insights)
        resumo = self._gerar_resumo(sentimento, detalhe_sentimento, produtos, oportunidades, riscos,
                                    persona, budget, contexto, proxima_acao)

        return {
            "sentimentoGeral": sentimento,
            "resumo": resumo,
            "personaIdentificada": persona,
            "budgetIdentificado": budget,
            "scoreConfiancaVendedor": confianca,
            "scoreSentimento": score_sentimento,
            "metricasAgregadas": metricas,
            "produtos": produtos,
            "oportunidades": oportunidades,
            "riscos": riscos,
            "riscosConsolidados": riscos_consolidados,
            "oportunidadesConsolidadas": oportunidades_consolidadas,
            "alertasConsolidados": alertas_consolidados,
            "itensCriticos": itens_criticos,
            "proximaAcao": proxima_acao,
            "sentimentoDetalhe": detalhe_sentimento,
            "insightsAdicionais": insights,
            "indicadoresQualidade": indicadores,
        }

    # ------------------------------------------------------- segmentacao

    def _segmentar_turnos(self, texto: str) -> list[Turno]:
        partes = REGEX_LOCUTOR.split(texto)
        if len(partes) <= 1:
            return [Turno(None, texto.strip())] if texto.strip() else []
        turnos = []
        # partes = [prefixo, id1, fala1, id2, fala2, ...]
        if partes[0].strip():
            turnos.append(Turno(None, partes[0].strip()))
        for i in range(1, len(partes) - 1, 2):
            fala = partes[i + 1].strip()
            if fala:
                turnos.append(Turno(partes[i], fala))
        return turnos

    def _inferir_papeis(self, turnos: list[Turno]) -> dict[str | None, str]:
        pontos: dict[str | None, float] = {}
        chars: dict[str | None, int] = {}
        for t in turnos:
            n = normalizar(t.texto)
            v = sum(1 for m in VENDEDOR_MARCAS if m in n)
            c = sum(1 for m in CLIENTE_MARCAS if m in n)
            pontos[t.locutor] = pontos.get(t.locutor, 0.0) + v - c
            chars[t.locutor] = chars.get(t.locutor, 0) + len(t.texto)
        papeis: dict[str | None, str] = {}
        if not turnos:
            return papeis
        if len(chars) == 1:
            locutor = next(iter(chars))
            # Monologo: normalmente e o relato do cliente (caso do briefing)
            papeis[locutor] = "vendedor" if pontos.get(locutor, 0) > 0 else "cliente"
            return papeis
        ordenados = sorted(pontos.items(), key=lambda kv: kv[1], reverse=True)
        vendedor = ordenados[0][0] if ordenados[0][1] > 0 else None
        # Diarizacao automatica fragmenta a mesma pessoa em varios ids: cada id recebe o papel
        # pela sua propria pontuacao; o mais "vendedor" e o mais "cliente" ancoram os extremos.
        for locutor in chars:
            p = pontos.get(locutor, 0)
            if locutor == vendedor or p > 0:
                papeis[locutor] = "vendedor"
            elif p < 0:
                papeis[locutor] = "cliente"
            else:
                papeis[locutor] = "cliente" if vendedor is not None and len(chars) == 2 else "desconhecido"
        if vendedor is not None and not any(v == "cliente" for v in papeis.values()):
            papeis[ordenados[-1][0]] = "cliente"
        if vendedor is None:
            # Sem marcador claro: quem mais pergunta costuma ser o vendedor qualificando
            perguntas = {l: sum(t.texto.count("?") for t in turnos if t.locutor == l) for l in chars}
            candidato = max(perguntas, key=perguntas.get)
            if perguntas[candidato] > 0:
                papeis[candidato] = "vendedor"
        return papeis

    def _segmentar_sentencas(self, turnos: list[Turno], papeis: dict) -> list[Sentenca]:
        sentencas: list[Sentenca] = []
        for t in turnos:
            for frase in REGEX_SENTENCA.split(t.texto):
                for pedaco in REGEX_ORACAO.split(frase or ""):
                    pedaco = (pedaco or "").strip()
                    if len(pedaco) < 3:
                        continue
                    sentencas.append(Sentenca(
                        texto=pedaco, norm=normalizar(pedaco), locutor=t.locutor,
                        papel=papeis.get(t.locutor, "desconhecido"), indice=len(sentencas),
                    ))
        return sentencas

    # ------------------------------------------------------- anotacao

    def _anotar_sentenca(self, s: Sentenca, contexto: dict) -> None:
        n = s.norm
        for aliases, rotulo, _produto, _modulo in AREAS:
            if any(_contem_palavra(n, a) for a in aliases) and rotulo not in s.areas:
                s.areas.append(rotulo)
        for alias, nome in PRODUTOS_TOTVS.items():
            if _contem_palavra(n, alias) and nome not in s.produtos:
                s.produtos.append(nome)
        for nome in self._context_list(contexto, "produtosFrequentes"):
            if _contem_palavra(n, normalizar(nome)) and nome not in s.produtos:
                s.produtos.append(nome)
        for alias, nome in CONCORRENTES.items():
            if not _contem_palavra(n, alias) or nome in s.concorrentes:
                continue
            if alias in CONCORRENTES_AMBIGUOS and not _contem(n, CONTEXTO_SISTEMA):
                continue
            s.concorrentes.append(nome)
        for nome in self._context_list(contexto, "concorrentesFrequentes"):
            if _contem_palavra(n, normalizar(nome)) and nome not in s.concorrentes:
                s.concorrentes.append(nome)
        s.pos, s.neg = self._pontuar_sentimento(n, bool(s.concorrentes))

    def _pontuar_sentimento(self, n: str, sobre_concorrente: bool) -> tuple[float, float]:
        pos = neg = 0.0
        palavras = n.split()

        def negado(idx_char: int) -> bool:
            prefixo = n[:idx_char].split()[-3:]
            return any(p.strip(",;") in NEGACOES for p in prefixo)

        def intensidade(idx_char: int) -> float:
            prefixo = n[:idx_char].split()[-2:]
            if any(p in INTENSIFICADORES for p in prefixo):
                return 1.5
            if any(p in ATENUADORES for p in prefixo):
                return 0.6
            return 1.0

        for termo, peso in POSITIVOS.items():
            for m in re.finditer(re.escape(termo), n):
                if negado(m.start()):
                    neg += peso * 0.8
                else:
                    pos += peso * intensidade(m.start())
        for termo, peso in NEGATIVOS.items():
            for m in re.finditer(re.escape(termo), n):
                if negado(m.start()):
                    pos += peso * 0.3
                else:
                    neg += peso * intensidade(m.start())
        # "manual" sozinho nao e dor; "sofrendo com ... manual" e
        if "manual" in n and not _contem(n, ["sofrendo", "dificuldade", "retrabalho", "perde", "perdemos", "demora"]):
            neg = max(0.0, neg - NEGATIVOS["manual"])
        # Elogio ao concorrente nao e satisfacao com a TOTVS: vira risco, nao sentimento positivo
        if sobre_concorrente and pos > 0:
            pos = 0.0
        if len(palavras) <= 2:
            pos, neg = pos * 0.5, neg * 0.5
        return round(pos, 2), round(neg, 2)

    # ------------------------------------------------------- sentimento

    def _agregar_sentimento(self, sentencas: list[Sentenca]) -> tuple[str, str, float]:
        relevantes = [s for s in sentencas if s.papel != "vendedor"] or sentencas
        pos = sum(s.pos for s in relevantes)
        neg = sum(s.neg for s in relevantes)
        pos_areas = self._areas_por_sinal(relevantes, positivo=True)
        neg_areas = self._areas_por_sinal(relevantes, positivo=False)
        total = pos + neg
        score = 0.0 if total == 0 else round((pos - neg) / total, 2)

        if pos >= 0.8 and neg >= 0.8 and min(pos, neg) / max(pos, neg) >= 0.3:
            rotulo = "MISTO"
        elif neg > pos and neg >= 0.8:
            rotulo = "NEGATIVO"
        elif pos > neg and pos >= 0.8:
            rotulo = "POSITIVO"
        else:
            rotulo = "NEUTRO"

        if rotulo == "MISTO":
            detalhe = "Misto"
            pos_area = next((a for a in pos_areas if a != "a solucao atual"), None)
            neg_area = next((a for a in neg_areas if a != "a solucao atual"), None)
            if pos_area and neg_area and pos_area != neg_area:
                detalhe += f" (Satisfeito com {pos_area} / Frustrado com {neg_area})"
            elif neg_area:
                detalhe += f" (elogios pontuais, mas frustracao com {neg_area})"
            elif pos_area:
                detalhe += f" (satisfeito com {pos_area}, com ressalvas)"
            else:
                detalhe += " (sinais positivos e negativos ao longo da conversa)"
        elif rotulo == "NEGATIVO":
            area = next((a for a in neg_areas if a != "a solucao atual"), None)
            detalhe = "Negativo" + (f" (insatisfacao com {area})" if area else " (insatisfacao recorrente)")
        elif rotulo == "POSITIVO":
            area = next((a for a in pos_areas if a != "a solucao atual"), None)
            detalhe = "Positivo" + (f" (satisfeito com {area})" if area else " (satisfacao geral)")
        else:
            detalhe = "Neutro (conversa informativa, sem sinais fortes)"
        return rotulo, detalhe, score

    def _areas_por_sinal(self, sentencas: list[Sentenca], positivo: bool) -> list[str]:
        pesos: dict[str, float] = {}
        for s in sentencas:
            valor = s.pos if positivo else s.neg
            if valor <= 0:
                continue
            for area in s.areas or ["a solucao atual"]:
                if area == "Integracao":
                    continue
                pesos[area] = pesos.get(area, 0.0) + valor
        return [a for a, _ in sorted(pesos.items(), key=lambda kv: kv[1], reverse=True)]

    def _score_modelo(self, sentencas: list[Sentenca]) -> float | None:
        if self.sentiment_pipeline is None or not sentencas:
            return None
        texto = " ".join(s.texto for s in sentencas if s.papel != "vendedor") or " ".join(s.texto for s in sentencas)
        if not texto.strip():
            return None
        chunks = [texto[i:i + 500] for i in range(0, len(texto), 500)]
        if len(chunks) > self.max_sentiment_chunks:
            passo = len(chunks) / self.max_sentiment_chunks
            chunks = [chunks[int(i * passo)] for i in range(self.max_sentiment_chunks)]
        estrelas = []
        for chunk in chunks:
            try:
                estrelas.append(int(self.sentiment_pipeline(chunk)[0]["label"][0]))
            except Exception as exc:  # pragma: no cover
                logger.warning("Falha ao classificar chunk de sentimento: %s", exc.__class__.__name__)
        if not estrelas:
            return None
        return round((sum(estrelas) / len(estrelas) - 3) / 2, 2)  # 1..5 -> -1..1

    # ------------------------------------------------------- produtos

    def _extrair_produtos(self, sentencas: list[Sentenca], contexto: dict) -> list[dict]:
        resultado: dict[str, dict] = {}
        for s in sentencas:
            for nome in s.produtos:
                tipo = self._classificar_produto(s.norm, nome)
                atual = resultado.get(nome)
                if atual is None:
                    resultado[nome] = {"nomeProduto": nome, "tipo": tipo, "contexto": _recortar(s.texto)}
                elif atual["tipo"] == "OPORTUNIDADE" and tipo == "ATUAL":
                    # "usamos X" pesa mais do que "queremos X" sobre o mesmo produto
                    atual["tipo"], atual["contexto"] = "ATUAL", _recortar(s.texto)
            for nome in s.concorrentes:
                if nome not in resultado:
                    resultado[nome] = {"nomeProduto": nome, "tipo": "CONCORRENTE", "contexto": _recortar(s.texto)}
        ordem = {"ATUAL": 0, "OPORTUNIDADE": 1, "CONCORRENTE": 2}
        return sorted(resultado.values(), key=lambda p: ordem[p["tipo"]])

    def _classificar_produto(self, n: str, nome: str) -> str:
        alias = normalizar(nome.replace("TOTVS ", "")).strip()
        pos_alias = n.find(alias)
        janela_antes = n[max(0, pos_alias - 40):pos_alias] if pos_alias >= 0 else ""
        janela_depois = n[pos_alias:pos_alias + 60] if pos_alias >= 0 else n
        condicional = any(c in janela_antes for c in CONDICIONAIS)
        posse = _contem(janela_antes, ["nosso ", "nossa ", "o nosso", "a nossa", "usamos", "a gente usa",
                                       "temos o", "temos a", "ja temos", "hoje o", "hoje a", "atualmente",
                                       "a gente tem", "utilizamos", "rodamos", "com o", "com a"])
        atual_depois = _contem(janela_depois, ["esta atendendo", "atende", "que usamos", "que a gente usa",
                                               "ja esta", "esta rodando", "implantado", "em producao"])
        desejo = _contem(n, OPORTUNIDADE_DESEJO) or _contem(janela_depois, ["for integrado", "for realmente",
                                                                            "fosse", "seria", "poderia"])
        if condicional or (desejo and not posse):
            return "OPORTUNIDADE"
        if posse or atual_depois:
            return "ATUAL"
        if desejo:
            return "OPORTUNIDADE"
        return "ATUAL"

    # ------------------------------------------------------- budget

    def _extrair_budget(self, sentencas: list[Sentenca]) -> tuple[str, float | None, str]:
        candidatos: list[tuple[float, str, str, bool]] = []
        for s in sentencas:
            # Faixas: "15 a 20 mil por mes", "entre 50 e 80 mil"
            faixa = re.search(r"(?:r\$\s*)?(\d+(?:[.,]\d+)?)\s*(?:a|e|ate)\s*(?:r\$\s*)?(\d+(?:[.,]\d+)?)\s*"
                              r"(mil(?:hoes|hao)?|k|milh[oa]es|milhao)(\s*(?:/|por)\s*(?:mes|ano))?", s.norm)
            if faixa:
                v1 = self._parse_valor(faixa.group(1), faixa.group(3))
                v2 = self._parse_valor(faixa.group(2), faixa.group(3))
                if v1 and v2 and v1 < v2:
                    periodo = (faixa.group(4) or "").strip()
                    sufixo = " por mes" if "mes" in periodo else " por ano" if "ano" in periodo else ""
                    candidatos.append((v2, f"{_formatar_brl(v1)} a {_formatar_brl(v2)}{sufixo}", s.texto, True))
                    continue
            for m in REGEX_VALOR.finditer(s.norm):
                if not m.group("moeda") and not m.group("esc"):
                    continue
                valor = self._parse_valor(m.group("num"), m.group("esc"))
                if valor is None or valor < 100:
                    continue
                periodo = (m.group("periodo") or "").strip()
                contexto_budget = _contem(s.norm, ["orcamento", "budget", "verba", "valor", "investir",
                                                   "investimento", "aprovado", "conversamos", "pagar", "custa",
                                                   "por mes", "mensal", "anual", "contrato", "proposta"])
                rotulo = _formatar_brl(valor) + (" por mes" if "mes" in periodo else " por ano" if "ano" in periodo else "")
                candidatos.append((valor, rotulo, s.texto, contexto_budget))
        if not candidatos:
            return "Nao identificado", None, ""
        candidatos.sort(key=lambda c: (not c[3], -c[0]))
        valor, rotulo, frase, _ = candidatos[0]
        return rotulo, valor, frase

    @staticmethod
    def _parse_valor(num: str, escala: str | None) -> float | None:
        bruto = num.replace(" ", "")
        try:
            if "," in bruto and "." in bruto:
                valor = float(bruto.replace(".", "").replace(",", "."))
            elif "," in bruto:
                valor = float(bruto.replace(",", "."))
            elif bruto.count(".") == 1 and len(bruto.split(".")[1]) != 3:
                valor = float(bruto)
            else:
                valor = float(bruto.replace(".", ""))
        except ValueError:
            return None
        esc = normalizar(escala or "")
        if esc.startswith("mil") and "milh" not in esc or esc == "k":
            valor *= 1_000
        elif "milh" in esc:
            valor *= 1_000_000
        elif esc.startswith("bi"):
            valor *= 1_000_000_000
        return valor

    # ------------------------------------------------------- riscos

    def _extrair_riscos(self, sentencas: list[Sentenca], concorrentes: list[str]) -> list[dict]:
        riscos: list[dict] = []
        vistos: set[tuple[str, str]] = set()

        def add(tipo: str, s: Sentenca, severidade: str, descricao: str, conc: str | None = None):
            chave = (tipo, descricao[:80])
            if chave in vistos:
                return
            vistos.add(chave)
            riscos.append({"tipo": tipo, "descricao": descricao, "severidade": severidade,
                           "concorrenteMencionado": conc})

        for s in sentencas:
            n = s.norm
            area = self._area_contexto(sentencas, s)
            if s.papel == "vendedor" and not s.concorrentes:
                continue

            for conc in s.concorrentes:
                avaliando = _contem(n, ["demo", "demonstracao", "proposta", "cotacao", "avaliando", "avaliar",
                                        "gostou", "gostaram", "preferem", "olhando", "conversando", "reuniao",
                                        "migrar", "trocar", "comparar", "apresentou", "orcamento"])
                sev = "ALTO" if avaliando else "MEDIO"
                desc = f"Cliente avaliando solucao da {conc}" + (f" para {area}" if area else "")
                if avaliando and _contem(n, ["gostou", "gostaram", "preferem"]):
                    desc += " e demonstrou interesse"
                add("CONCORRENCIA", s, sev, desc + f". Trecho: \"{_recortar(s.texto, 160)}\"", conc)

            churn = _contem(n, CHURN_EXPLICITO) or (
                _contem(n, CHURN_VERBOS) and _contem(n, CHURN_OBJETOS))
            if churn:
                conc = s.concorrentes[0] if s.concorrentes else None
                add("CHURN", s, "ALTO", "Sinal de cancelamento ou troca de fornecedor"
                    + (f" em {area}" if area else "") + f". Trecho: \"{_recortar(s.texto, 160)}\"", conc)

            if _contem(n, PERDA_CONFIANCA):
                add("PERDA_CONFIANCA", s, "ALTO",
                    f"Cliente sinaliza perda de confianca na relacao. Trecho: \"{_recortar(s.texto, 160)}\"")

            # Fala coloquial usa "problema" a toda hora: so vira risco com sinal forte e frase substantiva
            if s.neg >= 1.2 and len(n.split()) >= 6 and not s.concorrentes and not churn:
                sev = "ALTO" if s.neg >= 2.0 else "MEDIO"
                add("INSATISFACAO", s, sev, "Insatisfacao" + (f" com {area}" if area else "")
                    + f": \"{_recortar(s.texto, 160)}\"")
                riscos[-1]["_forca"] = s.neg
        ordem = {"ALTO": 0, "MEDIO": 1, "BAIXO": 2}
        riscos.sort(key=lambda r: (ordem[r["severidade"]], -r.get("_forca", 9.0)))
        for r in riscos:
            r.pop("_forca", None)
        return riscos[:12]

    def _area_contexto(self, sentencas: list[Sentenca], s: Sentenca) -> str | None:
        areas = [a for a in s.areas if a != "Integracao"]
        if areas:
            return areas[0]
        if s.indice > 0:
            anterior = sentencas[s.indice - 1]
            areas = [a for a in anterior.areas if a != "Integracao"]
            if areas:
                return areas[0]
        return None

    # ------------------------------------------------------- oportunidades

    def _extrair_oportunidades(self, sentencas: list[Sentenca], produtos: list[dict],
                               budget: str, contexto: dict) -> list[dict]:
        atuais = [p["nomeProduto"] for p in produtos if p["tipo"] == "ATUAL"]
        oportunidade_produtos = [p["nomeProduto"] for p in produtos if p["tipo"] == "OPORTUNIDADE"]
        cliente_totvs = bool(atuais)
        buckets: dict[str, dict] = {}

        def bucket(area_key: str, rotulo: str, produto: str, modulo: str) -> dict:
            return buckets.setdefault(area_key, {
                "area": rotulo, "produto": produto, "modulo": modulo, "score": 0.0,
                "dor": None, "desejo": None, "forte": False, "evidencias": [],
            })

        for s in sentencas:
            if s.papel == "vendedor":
                continue
            n = s.norm
            areas = [a for a in s.areas if a != "Integracao"]
            if not areas and s.indice > 0:
                areas = [a for a in sentencas[s.indice - 1].areas if a != "Integracao"]
            # "preciso conversar com meu socio" e um proximo passo, nao um desejo de compra;
            # "pode nao comprar" e negacao, nao intencao
            passo = _contem(n, PROXIMOS_PASSOS) or _contem(n, ["conversar com", "falar com", "pensar", "avisar"])
            termos = "|".join(re.escape(t) for t in OPORTUNIDADE_DESEJO)
            negado = re.search(r"\b(nao|nem|nunca)\s+(\w+\s+){0,2}(" + termos + ")", n)
            # "o que voces querem automatizar" e o vendedor perguntando, nao o cliente afirmando
            segunda_pessoa = re.search(r"\bvoces?\s+(\w+\s+){0,2}(" + termos + ")", n)
            desejo = _contem(n, OPORTUNIDADE_DESEJO) and not passo and not negado and not segunda_pessoa
            forte = _contem(n, OPORTUNIDADE_FORTE) and not passo and not negado
            dor = s.neg >= 1.2 and not s.concorrentes and len(n.split()) >= 6
            produto_oportunidade = next((p for p in s.produtos if p in oportunidade_produtos), None)
            if not (desejo or forte or dor or produto_oportunidade):
                continue

            alvo = None
            for aliases, rotulo, produto, modulo in AREAS:
                if rotulo in areas and rotulo != "Integracao":
                    alvo = (rotulo, produto, modulo)
                    break
            if produto_oportunidade:
                # a area do produto desejado, se souber
                info = next(((r, p, m) for _a, r, p, m in AREAS if p == produto_oportunidade), None)
                alvo = alvo or info or (produto_oportunidade, produto_oportunidade, produto_oportunidade)
                if alvo and alvo[1] != produto_oportunidade:
                    alvo = (alvo[0], produto_oportunidade, alvo[2])
            if alvo is None:
                alvo = ("Geral", "A definir", "solucao TOTVS a definir")

            b = bucket(alvo[0], alvo[0], alvo[1], alvo[2])
            if dor and not b["dor"]:
                b["dor"] = _recortar(s.texto, 160)
                b["score"] = max(b["score"], 0.55)
            if desejo or produto_oportunidade:
                b["desejo"] = b["desejo"] or _recortar(s.texto, 160)
                b["score"] = max(b["score"], 0.7)
            if forte:
                b["forte"] = True
                b["score"] = max(b["score"], 0.85)
            if len(b["evidencias"]) < 4:
                b["evidencias"].append(_recortar(s.texto, 200))

        oportunidades = []
        for b in buckets.values():
            generico = b["produto"] == "A definir"
            if generico and not b["desejo"]:
                continue  # dor sem area nem intencao nao vira oportunidade
            score = b["score"]
            if b["dor"] and b["desejo"] and not generico:
                score = max(score, 0.85)
            if generico:
                score = min(score, 0.5)
            if budget != "Nao identificado":
                score = min(1.0, score + 0.05)
            if any(o.lower() in b["area"].lower() for o in self._context_list(contexto, "oportunidadesFrequentes")):
                score = min(1.0, score + 0.05)
            score = round(min(score, 0.98), 2)
            tipo = self._tipo_oportunidade(b["produto"], atuais, cliente_totvs)
            grau = "Alta probabilidade" if score >= 0.8 else "Boa probabilidade" if score >= 0.6 else "Possibilidade"
            partes = [f"{grau} de venda ({b['modulo']})" if b["produto"] == "A definir"
                      else f"{grau} de venda do modulo de {b['modulo']}"]
            if b["produto"] != "A definir" and b["produto"] not in b["modulo"]:
                partes[0] += f" ({b['produto']})"
            if b["desejo"]:
                partes.append(f"cliente sinaliza intencao: \"{b['desejo']}\"")
            if b["dor"]:
                partes.append(f"dor relatada: \"{b['dor']}\"")
            oportunidades.append({
                "tipo": tipo,
                "descricao": "; ".join(partes) + ".",
                "produtoRelacionado": b["produto"] if b["produto"] != "A definir" else "A definir",
                "score": score,
                "valorEstimado": budget if budget != "Nao identificado" else "Nao estimado",
            })
        return sorted(oportunidades, key=lambda o: o["score"], reverse=True)[:6]

    @staticmethod
    def _tipo_oportunidade(produto: str, atuais: list[str], cliente_totvs: bool) -> str:
        if not cliente_totvs:
            return "NOVO_PRODUTO"
        if produto in ("RD Station", "TOTVS Techfin", "TOTVS Carol"):
            return "CROSS_SELL"
        return "UPSELL"

    # ------------------------------------------------------- persona

    def _identificar_persona(self, sentencas: list[Sentenca]) -> tuple[str, list[str]]:
        cliente = [s for s in sentencas if s.papel != "vendedor"] or sentencas
        texto = " ".join(s.norm for s in cliente)
        obs: list[str] = []
        pontos = {"executivo": 0, "financeiro": 0, "ti": 0, "operacional": 0}

        terceiro = next((t for t in TERCEIROS_DECISORES if t in texto), None)
        for cargo in CARGOS_EXECUTIVO:
            if _contem_palavra(texto, cargo):
                pontos["executivo"] += 2
        for cargo in CARGOS_FINANCEIRO:
            if _contem_palavra(texto, cargo):
                # "meu CFO" e um terceiro, nao o interlocutor
                if terceiro and cargo in terceiro:
                    continue
                pontos["financeiro"] += 2
        for cargo in CARGOS_TI:
            if cargo in texto:
                pontos["ti"] += 1
        for cargo in CARGOS_OPERACIONAL:
            if cargo in texto:
                pontos["operacional"] += 1
        if _contem(texto, ["roi", "payback", "custo", "investimento", "orcamento aprovado"]) and pontos["financeiro"] == 0 and not terceiro:
            pontos["financeiro"] += 1

        if terceiro:
            quem = "CFO" if "cfo" in terceiro else "diretoria" if "diretoria" in terceiro else terceiro.replace("meu ", "").replace("o ", "")
            obs.append(f"Menciona {quem} como decisor financeiro/final: o interlocutor influencia, mas nao fecha sozinho.")

        melhor = max(pontos, key=pontos.get)
        if pontos[melhor] == 0:
            return ("Interlocutor nao identificado (sem cargo ou funcao explicita)", obs)
        rotulos = {
            "executivo": "Socio/Executivo (decisor final)",
            "financeiro": "Decisor financeiro (CFO/Controller)",
            "ti": "Gestor de TI (influenciador tecnico)",
            "operacional": "Gestor Operacional (decisor tecnico/influenciador)",
        }
        return rotulos[melhor], obs

    # ------------------------------------------------------- confianca

    def _calcular_confianca(self, sentencas: list[Sentenca], riscos: list[dict]) -> float:
        cliente = [s for s in sentencas if s.papel != "vendedor"] or sentencas
        texto = " ".join(s.norm for s in cliente)
        score = 0.5
        score += 0.12 * sum(1 for t in CONFIANCA_POSITIVA if t in texto)
        score -= 0.2 * sum(1 for t in PERDA_CONFIANCA if t in texto)
        if any(r["tipo"] == "PERDA_CONFIANCA" for r in riscos):
            score -= 0.15
        if any(r["tipo"] == "CHURN" for r in riscos):
            score -= 0.1
        return max(0.0, min(1.0, round(score, 2)))

    # ------------------------------------------------------- insights extras

    def _insights_adicionais(self, sentencas: list[Sentenca], budget: str, budget_frase: str,
                             persona_obs: list[str]) -> dict:
        elogios, reclamacoes, duvidas, passos, observacoes = [], [], [], [], list(persona_obs)
        for s in sentencas:
            n = s.norm
            if s.papel != "vendedor":
                if s.pos >= 0.8 and not s.concorrentes and len(elogios) < 5:
                    elogios.append(_recortar(s.texto, 200))
                if s.neg >= 0.9 and len(reclamacoes) < 5:
                    reclamacoes.append(_recortar(s.texto, 200))
                if ("?" in s.texto or _contem(n, DUVIDAS_JORNADA)) and _contem(n, DUVIDAS_JORNADA) and len(duvidas) < 5:
                    duvidas.append(_recortar(s.texto, 200))
            if _contem(n, PROXIMOS_PASSOS) and len(passos) < 5:
                passos.append(_recortar(s.texto, 200))
            if _contem(n, ["nao mencione", "nao comente", "entre nos", "confidencial", "sigilo", "nao fale"]):
                alvo = "ao CFO" if "cfo" in n else "a terceiros"
                observacoes.append(f"Cliente pediu discricao{' sobre o valor ' + budget if budget != 'Nao identificado' and budget_frase == s.texto else ''} junto {alvo}: tratar a informacao como confidencial.")
            if _contem(n, ["roi do semestre", "roi do trimestre", "fechamento do trimestre", "fim do ano fiscal"]):
                observacoes.append("Decisor financeiro focado no ciclo de ROI: alinhar proposta ao calendario orcamentario.")
        return {
            "elogios": elogios,
            "reclamacoes": reclamacoes,
            "duvidasJornada": duvidas,
            "proximosPassos": passos,
            "observacoes": observacoes[:6],
        }

    # ------------------------------------------------------- indicadores

    def _indicadores_qualidade(self, texto: str, turnos: list[Turno], papeis: dict,
                               sentencas: list[Sentenca]) -> dict:
        chars_por_papel = {"vendedor": 0, "cliente": 0, "desconhecido": 0}
        perguntas = {"vendedor": 0, "cliente": 0}
        for t in turnos:
            papel = papeis.get(t.locutor, "desconhecido")
            chars_por_papel[papel] = chars_por_papel.get(papel, 0) + len(t.texto)
            if papel in perguntas:
                perguntas[papel] += t.texto.count("?")
        total = sum(chars_por_papel.values()) or 1
        vendedor_pct = round(100 * chars_por_papel["vendedor"] / total)
        cliente_pct = round(100 * chars_por_papel["cliente"] / total)
        locutores = {t.locutor for t in turnos if t.locutor is not None}
        palavras = len(re.findall(r"\w+", texto))
        anonimos = len(REGEX_ANONIMO.findall(texto))
        if vendedor_pct == 0 and cliente_pct == 0:
            escuta = "Nao mensuravel (sem identificacao de locutores)"
        elif vendedor_pct <= 45:
            escuta = "Vendedor ouviu mais do que falou"
        elif vendedor_pct <= 60:
            escuta = "Equilibrado"
        else:
            escuta = "Vendedor falou mais do que ouviu"
        qualidade = "alta" if palavras >= 150 and len(turnos) >= 2 else "media" if palavras >= 40 else "baixa"
        return {
            "locutores": len(locutores),
            "turnos": len(turnos),
            "sentencas": len(sentencas),
            "palavras": palavras,
            "participacaoVendedorPct": vendedor_pct,
            "participacaoClientePct": cliente_pct,
            "perguntasVendedor": perguntas["vendedor"],
            "perguntasCliente": perguntas["cliente"],
            "escutaAtiva": escuta,
            "tokensAnonimizados": anonimos,
            "qualidadeTranscricao": qualidade,
        }

    # ------------------------------------------------------- consolidacao

    def _extrair_metricas_consolidadas(self, texto: str) -> dict:
        def number(label: str) -> int:
            match = re.search(rf"{label}:\s*(\d+)", texto, re.IGNORECASE)
            return int(match.group(1)) if match else 0

        partes_match = re.search(r"SENTIMENTO_PARTES:\s*positivas=(\d+),\s*neutras=(\d+),\s*negativas=(\d+)", texto, re.IGNORECASE)
        positivas = int(partes_match.group(1)) if partes_match else 0
        neutras = int(partes_match.group(2)) if partes_match else 0
        negativas = int(partes_match.group(3)) if partes_match else 0
        total = number("TOTAL_PARTES_VALIDAS") or positivas + neutras + negativas
        return {
            "totalPartes": total, "partesAnalisadas": total,
            "partesNegativas": negativas, "partesNeutras": neutras, "partesPositivas": positivas,
            "riscosDetectados": number("RISCOS_DETECTADOS"),
            "oportunidadesDetectadas": number("OPORTUNIDADES_DETECTADAS"),
            "churnsPotenciais": number("CHURNS_POTENCIAIS"),
            "mencoesCriticas": number("MENCOES_CRITICAS"),
            "alertasConsolidados": 0,
            "chunksProcessados": max(1, (total + 49) // 50) if total else 1,
        }

    def _metricas_por_turnos(self, turnos: list[Turno], sentencas: list[Sentenca],
                             riscos: list[dict], oportunidades: list[dict]) -> dict:
        pos = neg = neu = 0
        for s in sentencas:
            if s.polaridade >= 0.8:
                pos += 1
            elif s.polaridade <= -0.8:
                neg += 1
            else:
                neu += 1
        total = len(sentencas)
        return {
            "totalPartes": total, "partesAnalisadas": total,
            "partesNegativas": neg, "partesNeutras": neu, "partesPositivas": pos,
            "riscosDetectados": len(riscos),
            "oportunidadesDetectadas": len(oportunidades),
            "churnsPotenciais": sum(1 for r in riscos if r["tipo"] == "CHURN"),
            "mencoesCriticas": sum(1 for r in riscos if r["severidade"] == "ALTO"),
            "alertasConsolidados": 0,
            "chunksProcessados": max(1, (len(turnos) + 49) // 50),
        }

    def _consolidar_riscos(self, riscos: list[dict], metricas: dict) -> list[dict]:
        rotulos = {"CHURN": "Churn", "CONCORRENCIA": "Concorrencia", "INSATISFACAO": "Insatisfacao",
                   "PERDA_CONFIANCA": "Perda de confianca"}
        recomendacoes = {
            "CHURN": "Acionar plano de retencao com causa raiz, responsavel e prazo; envolver lideranca comercial.",
            "CONCORRENCIA": "Preparar comparativo objetivo contra o concorrente e reforcar diferenciais de integracao TOTVS.",
            "INSATISFACAO": "Registrar as dores por area, abrir plano de acao com suporte/produto e dar retorno com data.",
            "PERDA_CONFIANCA": "Escalar para gestao da conta, reconhecer o problema e combinar entregas verificaveis.",
        }
        buckets: dict[str, dict] = {}
        for r in riscos:
            b = buckets.setdefault(r["tipo"], {
                "categoria": rotulos.get(r["tipo"], r["tipo"].title()),
                "severidade": r["severidade"], "ocorrencias": 0, "evidencias": [],
                "recomendacao": recomendacoes.get(r["tipo"], "Agrupar evidencias e definir plano de acao."),
            })
            b["ocorrencias"] += 1
            if r["severidade"] == "ALTO":
                b["severidade"] = "ALTO"
            if len(b["evidencias"]) < 5:
                b["evidencias"].append(r["descricao"][:240])
        if metricas.get("churnsPotenciais", 0) and "CHURN" not in buckets:
            buckets["CHURN"] = {"categoria": "Churn", "severidade": "ALTO", "ocorrencias": metricas["churnsPotenciais"],
                                "evidencias": [], "recomendacao": recomendacoes["CHURN"]}
        return list(buckets.values())

    def _consolidar_oportunidades(self, oportunidades: list[dict], metricas: dict) -> list[dict]:
        buckets: dict[str, dict] = {}
        for o in oportunidades:
            chave = o["produtoRelacionado"] if o["produtoRelacionado"] != "A definir" else o["tipo"]
            b = buckets.setdefault(chave, {
                "categoria": chave.replace("_", " ").title() if chave == o["tipo"] else chave,
                "score": 0.0, "ocorrencias": 0, "evidencias": [],
                "recomendacao": "Validar aderencia, estimar valor e encaminhar proposta com o modulo relacionado.",
            })
            b["ocorrencias"] += 1
            b["score"] = max(b["score"], o["score"])
            if len(b["evidencias"]) < 5:
                b["evidencias"].append(o["descricao"][:240])
        if metricas.get("oportunidadesDetectadas", 0) and not buckets:
            buckets["geral"] = {"categoria": "Melhoria no atendimento", "score": 0.72,
                                "ocorrencias": metricas["oportunidadesDetectadas"], "evidencias": [],
                                "recomendacao": "Transformar recorrencias em backlog comercial e de produto."}
        return list(buckets.values())

    def _gerar_alertas_consolidados(self, metricas: dict, riscos: list[dict], concorrentes: list[str],
                                    consolidado: bool) -> list[dict]:
        alertas = []
        if consolidado:
            total = metricas.get("partesAnalisadas", 0)
            negativas = metricas.get("partesNegativas", 0)
            taxa = negativas / total if total else 0
            if negativas >= 10 or taxa >= 0.15:
                alertas.append({"titulo": "Alta recorrencia de sentimento negativo", "categoria": "Sentimento negativo",
                                "severidade": "ALTO" if taxa >= 0.3 else "MEDIO", "ocorrencias": negativas,
                                "motivo": "Volume relevante de partes negativas na transcricao consolidada.",
                                "recomendacao": "Priorizar revisao das evidencias e contato de recuperacao."})
            if metricas.get("churnsPotenciais", 0):
                alertas.append({"titulo": "Indicadores de churn na transcricao", "categoria": "Churn", "severidade": "ALTO",
                                "ocorrencias": metricas["churnsPotenciais"],
                                "motivo": "Foram encontrados sinais de cancelamento, troca ou nao renovacao.",
                                "recomendacao": "Abrir plano de retencao com causa raiz e responsaveis."})
            if metricas.get("mencoesCriticas", 0) >= 5:
                alertas.append({"titulo": "Muitas mencoes criticas operacionais", "categoria": "Criticidade operacional",
                                "severidade": "ALTO" if metricas["mencoesCriticas"] >= 15 else "MEDIO",
                                "ocorrencias": metricas["mencoesCriticas"],
                                "motivo": "Recorrencia de mencoes criticas no material analisado.",
                                "recomendacao": "Consolidar causas tecnicas e criar plano de acao com suporte e produto."})
            return alertas

        por_conc: dict[str, int] = {}
        for r in riscos:
            if r["tipo"] == "CONCORRENCIA" and r.get("concorrenteMencionado"):
                por_conc[r["concorrenteMencionado"]] = por_conc.get(r["concorrenteMencionado"], 0) + 1
        for conc, n in por_conc.items():
            alertas.append({"titulo": f"Cliente avaliando concorrente: {conc}", "categoria": "Concorrencia",
                            "severidade": "ALTO", "ocorrencias": n,
                            "motivo": f"A {conc} foi citada em contexto de avaliacao/demonstracao.",
                            "recomendacao": "Antecipar proposta e destacar integracao nativa entre os modulos TOTVS."})
        churns = [r for r in riscos if r["tipo"] == "CHURN"]
        if churns:
            alertas.append({"titulo": "Risco de churn", "categoria": "Churn", "severidade": "ALTO", "ocorrencias": len(churns),
                            "motivo": "Cliente verbalizou cancelamento, troca ou nao renovacao.",
                            "recomendacao": "Acionar plano de retencao imediato com lideranca comercial."})
        insat = [r for r in riscos if r["tipo"] == "INSATISFACAO" and r["severidade"] == "ALTO"]
        if len(insat) >= 2:
            alertas.append({"titulo": "Insatisfacao recorrente", "categoria": "Sentimento negativo", "severidade": "MEDIO",
                            "ocorrencias": len(insat), "motivo": "Mais de uma dor forte relatada na mesma conversa.",
                            "recomendacao": "Consolidar as dores e retornar com plano de acao datado."})
        return alertas

    def _extrair_itens_criticos(self, sentencas: list[Sentenca], riscos: list[dict]) -> list[dict]:
        itens = []
        for s in sentencas:
            n = s.norm
            if s.concorrentes:
                itens.append({"tipo": "risco", "categoria": "Concorrencia", "trecho": _recortar(s.texto, 300), "severidade": "ALTO"})
            elif _contem(n, CHURN_EXPLICITO) or (_contem(n, CHURN_VERBOS) and _contem(n, CHURN_OBJETOS)):
                itens.append({"tipo": "risco", "categoria": "Churn", "trecho": _recortar(s.texto, 300), "severidade": "ALTO"})
            elif s.neg >= 1.2 and s.papel != "vendedor":
                itens.append({"tipo": "risco", "categoria": "Criticidade operacional", "trecho": _recortar(s.texto, 300), "severidade": "MEDIO"})
            elif _contem(n, ["urgente", "critico", "parado", "parada", "nao consigo emitir", "producao parada"]):
                itens.append({"tipo": "risco", "categoria": "Criticidade operacional", "trecho": _recortar(s.texto, 300), "severidade": "ALTO"})
            if len(itens) >= 20:
                break
        return itens

    # ------------------------------------------------------- resumo

    def _sugerir_proxima_acao(self, sentimento: str, oportunidades: list[dict], riscos: list[dict],
                              concorrentes: list[str], insights: dict) -> str:
        if any(r["tipo"] == "CHURN" for r in riscos):
            return "Acionar plano de retencao com a lideranca comercial antes do proximo contato."
        if concorrentes and oportunidades:
            o = oportunidades[0]
            return (f"Antecipar proposta do modulo de {o['produtoRelacionado']} com comparativo contra "
                    f"{', '.join(concorrentes)}, destacando integracao nativa.")
        if concorrentes:
            return f"Preparar comparativo objetivo contra {', '.join(concorrentes)} e agendar demonstracao."
        if oportunidades and oportunidades[0]["score"] >= 0.75:
            return "Enviar proposta ou agendar demonstracao com os decisores nos proximos dias."
        if oportunidades:
            return "Qualificar necessidade, budget e prazo antes de enviar proposta."
        if any(r["severidade"] == "ALTO" for r in riscos):
            return "Priorizar contato de recuperacao com plano de acao objetivo e data de retorno."
        if insights.get("duvidasJornada"):
            return "Responder as duvidas levantadas sobre preco, prazo e integracao com material objetivo."
        if sentimento == "POSITIVO":
            return "Explorar expansao de uso e pedir indicacoes internas."
        return "Coletar mais contexto sobre dor, budget e decisores."

    def _gerar_resumo(self, sentimento: str, detalhe: str, produtos: list[dict], oportunidades: list[dict],
                      riscos: list[dict], persona: str, budget: str, contexto: dict, proxima_acao: str) -> str:
        atuais = [p["nomeProduto"] for p in produtos if p["tipo"] == "ATUAL"]
        desejados = [p["nomeProduto"] for p in produtos if p["tipo"] == "OPORTUNIDADE"]
        concorrentes = [p["nomeProduto"] for p in produtos if p["tipo"] == "CONCORRENTE"]
        partes = []
        if atuais:
            partes.append(f"Cliente utiliza {', '.join(atuais)}" + (f" e avalia {', '.join(desejados)}" if desejados else ""))
        elif desejados:
            partes.append(f"Cliente avalia {', '.join(desejados)}")
        partes.append(f"Sentimento {detalhe.lower()}")
        altos = [r for r in riscos if r["severidade"] == "ALTO"]
        if concorrentes:
            partes.append(f"ATENCAO: concorrente em avaliacao ({', '.join(concorrentes)})")
        elif altos:
            partes.append(f"ATENCAO: {len(altos)} risco(s) de alta severidade")
        if oportunidades:
            o = oportunidades[0]
            partes.append(f"{o['descricao'].split(';')[0]}")
        if persona and not persona.startswith("Interlocutor nao"):
            partes.append(f"Interlocutor: {persona}")
        if budget != "Nao identificado":
            partes.append(f"Budget mencionado: {budget}")
        ajustes = self._context_list(contexto, "ajustesRecentes")[:2]
        if ajustes:
            partes.append(f"Calibrado com feedback recente: {'; '.join(ajustes)}")
        partes.append(f"Proxima acao sugerida: {proxima_acao.rstrip('.')}")
        return ". ".join(p.rstrip(".") for p in partes if p) + "."

    @staticmethod
    def _context_list(contexto: dict | None, key: str) -> list[str]:
        if not contexto:
            return []
        values = contexto.get(key) or []
        if not isinstance(values, list):
            return []
        return [str(v).strip() for v in values if str(v).strip()][:8]
