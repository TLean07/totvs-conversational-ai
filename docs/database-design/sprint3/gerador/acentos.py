# -*- coding: utf-8 -*-
"""
Acentuação dos textos do modelo para o documento.

Os comentários gravados no banco (COMMENT ON) e os scripts SQL ficam sem acentos para
funcionar em qualquer codificação de cliente (SQL*Plus, SQLcl, SQL Developer). No PDF, o
mesmo texto é exibido com a grafia correta a partir deste mapa de palavras.
"""
import re

PALAVRAS = {
    "acao": "ação", "acionaveis": "acionáveis", "analise": "análise", "analises": "análises",
    "aplicacao": "aplicação", "area": "área", "ate": "até", "audio": "áudio", "avaliacao": "avaliação",
    "codigo": "código", "competencia": "competência", "concluida": "concluída", "concorrencia": "concorrência",
    "confianca": "confiança", "copia": "cópia", "correcao": "correção", "criacao": "criação",
    "criticas": "críticas", "criticos": "críticos", "descricao": "descrição", "duracao": "duração",
    "emissao": "emissão", "evidencia": "evidência", "evidencias": "evidências", "exibicao": "exibição",
    "expiracao": "expiração", "geracao": "geração", "historico": "histórico", "importacao": "importação",
    "indice": "índice", "insatisfacao": "insatisfação", "invalida": "inválida", "legivel": "legível",
    "mencoes": "menções", "mes": "mês", "metricas": "métricas", "modulo": "módulo", "mudanca": "mudança",
    "mudancas": "mudanças", "nao": "não", "negocio": "negócio", "normalizacao": "normalização",
    "numerico": "numérico", "numero": "número", "observacao": "observação", "observacoes": "observações",
    "ocorrencias": "ocorrências", "participacao": "participação", "posicao": "posição", "primaria": "primária",
    "proxima": "próxima", "recomendacao": "recomendação", "recorrencia": "recorrência",
    "referencia": "referência", "responsavel": "responsável", "reuniao": "reunião", "reunioes": "reuniões",
    "revisao": "revisão", "revogacao": "revogação", "rotacao": "rotação", "saida": "saída",
    "seguranca": "segurança", "sessao": "sessão", "sessoes": "sessões", "tecnico": "técnico",
    "titulo": "título", "transcricao": "transcrição", "transcricoes": "transcrições", "ultima": "última",
    "unica": "única", "unico": "único", "usuario": "usuário", "usuarios": "usuários", "util": "útil",
    "valido": "válido", "ve": "vê", "so": "só", "ja": "já", "tambem": "também", "dominio": "domínio",
    "atraves": "através", "automatico": "automático", "consolidacao": "consolidação", "conclusao": "conclusão",
}

# frases em que o "e" isolado é o verbo "é"
FRASES = [
    ("nunca e armazenada", "nunca é armazenada"),
    ("token e persistido", "token é persistido"),
    ("a origem e transcricao", "a origem é transcrição"),
    ("a analise e removida", "a análise é removida"),
]

_RE = re.compile(r"[A-Za-z]+")


def _troca(m):
    w = m.group(0)
    if w.isupper():          # identificadores e siglas (ID, CLOB, NPS...) ficam como estão
        return w
    novo = PALAVRAS.get(w.lower())
    if not novo:
        return w
    if w[0].isupper():
        return novo[0].upper() + novo[1:]
    return novo


def acentuar(texto):
    if not texto:
        return texto
    for a, b in FRASES:
        texto = texto.replace(a, b)
    return _RE.sub(_troca, texto)
