"""
Motor de analise de transcricoes usando HuggingFace (sentimento) + regras (entidades).
"""

import re
from transformers import pipeline


# Produtos TOTVS e seus modulos conhecidos
PRODUTOS_TOTVS = {
    "protheus": "TOTVS Protheus",
    "rm": "TOTVS RM",
    "fluig": "TOTVS Fluig",
    "datasul": "TOTVS Datasul",
    "techfin": "TOTVS Techfin",
    "carol": "TOTVS Carol",
    "rd station": "RD Station",
    "totvs moda": "TOTVS Moda",
    "totvs saude": "TOTVS Saude",
    "totvs logistica": "TOTVS Logistica",
    "totvs educacional": "TOTVS Educacional",
    "totvs hotelaria": "TOTVS Hotelaria",
    "totvs construcao": "TOTVS Construcao",
    "totvs juridico": "TOTVS Juridico",
    "winthor": "TOTVS Winthor",
}

# Concorrentes conhecidos
CONCORRENTES = {
    "sap": "SAP",
    "sap business one": "SAP Business One",
    "sap b1": "SAP Business One",
    "oracle": "Oracle",
    "senior": "Senior",
    "senior sistemas": "Senior",
    "sage": "Sage",
    "linx": "Linx",
    "sankhya": "Sankhya",
    "mega": "Mega Sistemas",
    "alterdata": "Alterdata",
    "conta azul": "Conta Azul",
    "omie": "Omie",
    "bling": "Bling",
    "tiny": "Tiny ERP",
    "microsiga": "Microsiga",
}

# Modulos / areas de produto
MODULOS = [
    "folha de pagamento", "folha", "rh", "recursos humanos",
    "financeiro", "contabil", "contabilidade", "fiscal",
    "compras", "estoque", "faturamento", "vendas",
    "crm", "bi", "analytics", "wms", "tms",
    "pcp", "producao", "manufatura", "qualidade",
    "ativo fixo", "patrimonio", "tesouraria",
    "ponto eletronico", "beneficios", "recrutamento",
    "e-social", "sped", "nfe", "nota fiscal",
]

# Palavras/frases que indicam insatisfacao
SINAIS_INSATISFACAO = [
    "frustrad", "insatisf", "decepcion", "problem", "erro", "bug",
    "nao funciona", "ninguem resolve", "chamado", "reclamando",
    "demora", "lento", "trava", "pior", "horrivel", "pessimo",
    "nao atende", "deficiente", "falhando", "caindo",
    "suporte ruim", "sem retorno", "nao respondem",
]

# Palavras/frases que indicam satisfacao
SINAIS_SATISFACAO = [
    "satisfeit", "content", "excelente", "otimo", "muito bom",
    "atendeu", "funciona bem", "gostamos", "adoramos", "perfeito",
    "eficiente", "produtiv", "recomendo", "aprovamos", "feliz",
    "superou", "expectativa", "qualidade",
]

# Sinais de oportunidade de venda
SINAIS_OPORTUNIDADE = [
    "precisamos", "queremos", "gostar[ií]amos", "necessidade",
    "expandir", "ampliar", "novo modulo", "implantar",
    "orcamento", "budget", "verba", "investir",
    "aprovado", "diretoria aprovou", "proximo trimestre",
    "interesse", "proposta", "cotacao", "demonstracao", "demo",
]

# Sinais de risco de churn
SINAIS_CHURN = [
    "trocar", "cancelar", "avaliar outras", "outras opcoes",
    "concorren", "migrando", "migrar", "sair da",
    "contrato vence", "nao renovar", "desistir",
    "vou ter que", "ultima chance", "deadline",
]

# Padroes de valor monetario
REGEX_VALOR = re.compile(
    r"R\$\s*[\d.,]+(?:\s*(?:mil|reais|milhao|milhoes|bi|bilhao))?|"
    r"[\d.,]+\s*(?:mil|reais|milhao|milhoes)\b",
    re.IGNORECASE,
)

# Padroes de persona/cargo
CARGOS = [
    "diretor", "gerente", "coordenador", "supervisor", "analista",
    "ceo", "cto", "cfo", "cio", "vp", "vice-presidente",
    "presidente", "socio", "dono", "proprietario",
    "gestor", "lider", "head", "manager", "controller",
    "contador", "contadora", "financeiro", "comprador",
    "ti", "tecnologia", "rh", "comercial", "operacional",
]


class TranscricaoAnalyzer:
    def __init__(self):
        print("Carregando modelo de sentimento...")
        self.sentiment_pipeline = pipeline(
            "sentiment-analysis",
            model="nlptown/bert-base-multilingual-uncased-sentiment",
            truncation=True,
            max_length=512,
        )
        self._ready = True
        print("Modelo carregado com sucesso!")

    def is_ready(self) -> bool:
        return self._ready

    def analisar(self, texto: str) -> dict:
        texto_lower = texto.lower()

        sentimento = self._analisar_sentimento(texto)
        produtos = self._extrair_produtos(texto, texto_lower)
        concorrentes = self._extrair_concorrentes(texto_lower)
        oportunidades = self._extrair_oportunidades(texto, texto_lower, produtos)
        riscos = self._extrair_riscos(texto, texto_lower, concorrentes)
        persona = self._identificar_persona(texto_lower)
        budget = self._extrair_budget(texto)
        score_confianca = self._calcular_confianca(texto_lower)
        resumo = self._gerar_resumo(texto, sentimento, produtos, oportunidades, riscos)

        return {
            "sentimentoGeral": sentimento,
            "resumo": resumo,
            "personaIdentificada": persona,
            "budgetIdentificado": budget,
            "scoreConfiancaVendedor": score_confianca,
            "produtos": produtos,
            "oportunidades": oportunidades,
            "riscos": riscos,
        }

    def _analisar_sentimento(self, texto: str) -> str:
        # Divide texto em chunks se for muito grande
        chunks = [texto[i:i+500] for i in range(0, len(texto), 500)]
        scores = []

        for chunk in chunks:
            result = self.sentiment_pipeline(chunk)[0]
            # Modelo retorna 1-5 estrelas
            star = int(result["label"][0])
            scores.append(star)

        avg = sum(scores) / len(scores)

        # Verifica sinais mistos
        texto_lower = texto.lower()
        tem_positivo = any(s in texto_lower for s in SINAIS_SATISFACAO)
        tem_negativo = any(s in texto_lower for s in SINAIS_INSATISFACAO)

        if tem_positivo and tem_negativo:
            return "MISTO"
        elif avg >= 4:
            return "POSITIVO"
        elif avg <= 2:
            return "NEGATIVO"
        elif tem_negativo:
            return "NEGATIVO"
        elif tem_positivo:
            return "POSITIVO"
        else:
            return "NEUTRO"

    def _extrair_produtos(self, texto: str, texto_lower: str) -> list:
        produtos = []
        encontrados = set()

        for chave, nome in PRODUTOS_TOTVS.items():
            if chave in texto_lower and nome not in encontrados:
                encontrados.add(nome)
                contexto = self._extrair_contexto(texto, chave)

                # Determinar se e produto atual ou oportunidade
                tipo = "ATUAL"
                ctx_lower = contexto.lower()
                if any(s in ctx_lower for s in ["precisamos", "queremos", "implantar", "interesse", "novo"]):
                    tipo = "OPORTUNIDADE"

                produtos.append({
                    "nomeProduto": nome,
                    "tipo": tipo,
                    "contexto": contexto,
                })

        # Adicionar concorrentes como produtos tipo CONCORRENTE
        for chave, nome in CONCORRENTES.items():
            if chave in texto_lower and nome not in encontrados:
                encontrados.add(nome)
                contexto = self._extrair_contexto(texto, chave)
                produtos.append({
                    "nomeProduto": nome,
                    "tipo": "CONCORRENTE",
                    "contexto": contexto,
                })

        return produtos

    def _extrair_concorrentes(self, texto_lower: str) -> list:
        encontrados = []
        for chave, nome in CONCORRENTES.items():
            if chave in texto_lower and nome not in encontrados:
                encontrados.append(nome)
        return encontrados

    def _extrair_oportunidades(self, texto: str, texto_lower: str, produtos: list) -> list:
        oportunidades = []
        frases = texto.split(".")

        for frase in frases:
            frase_lower = frase.lower().strip()
            if not frase_lower:
                continue

            for padrao in SINAIS_OPORTUNIDADE:
                if re.search(padrao, frase_lower):
                    # Identificar modulos mencionados
                    modulos_mencionados = [m for m in MODULOS if m in frase_lower]
                    produto_relacionado = ""

                    if modulos_mencionados:
                        produto_relacionado = self._inferir_produto_modulo(modulos_mencionados[0])

                    # Determinar tipo
                    produtos_atuais = [p["nomeProduto"] for p in produtos if p["tipo"] == "ATUAL"]
                    if produto_relacionado and any(
                        p for p in produtos_atuais
                        if produto_relacionado.lower() in p.lower()
                    ):
                        tipo = "UPSELL"
                    elif produtos_atuais:
                        tipo = "CROSS_SELL"
                    else:
                        tipo = "NOVO_PRODUTO"

                    # Score baseado em indicadores
                    score = 0.5
                    if any(w in frase_lower for w in ["aprovado", "aprovamos", "orcamento"]):
                        score = 0.9
                    elif any(w in frase_lower for w in ["queremos", "precisamos", "necessidade"]):
                        score = 0.7
                    elif any(w in frase_lower for w in ["interesse", "demo", "proposta"]):
                        score = 0.6

                    descricao = frase.strip()
                    if len(descricao) > 200:
                        descricao = descricao[:200] + "..."

                    oportunidades.append({
                        "tipo": tipo,
                        "descricao": descricao,
                        "produtoRelacionado": produto_relacionado or "A definir",
                        "score": score,
                        "valorEstimado": self._extrair_budget(frase) or "Nao estimado",
                    })
                    break  # uma oportunidade por frase

        return oportunidades

    def _extrair_riscos(self, texto: str, texto_lower: str, concorrentes: list) -> list:
        riscos = []
        frases = texto.split(".")

        for frase in frases:
            frase_lower = frase.lower().strip()
            if not frase_lower:
                continue

            # Risco de concorrencia
            for conc in concorrentes:
                if conc.lower() in frase_lower:
                    riscos.append({
                        "tipo": "CONCORRENCIA",
                        "descricao": frase.strip()[:200],
                        "severidade": "ALTO",
                        "concorrenteMencionado": conc,
                    })
                    break

            # Risco de insatisfacao
            for sinal in SINAIS_INSATISFACAO:
                if sinal in frase_lower:
                    riscos.append({
                        "tipo": "INSATISFACAO",
                        "descricao": frase.strip()[:200],
                        "severidade": "MEDIO" if not any(s in frase_lower for s in SINAIS_CHURN) else "ALTO",
                        "concorrenteMencionado": None,
                    })
                    break

            # Risco de churn
            for sinal in SINAIS_CHURN:
                if sinal in frase_lower:
                    # Evitar duplicata se ja pegou como concorrencia
                    ja_existe = any(
                        r["descricao"] == frase.strip()[:200]
                        for r in riscos
                    )
                    if not ja_existe:
                        conc_na_frase = next(
                            (c for c in concorrentes if c.lower() in frase_lower), None
                        )
                        riscos.append({
                            "tipo": "CHURN",
                            "descricao": frase.strip()[:200],
                            "severidade": "ALTO",
                            "concorrenteMencionado": conc_na_frase,
                        })
                    break

        return riscos

    def _identificar_persona(self, texto_lower: str) -> str:
        encontrados = []
        for cargo in CARGOS:
            if cargo in texto_lower:
                encontrados.append(cargo.title())

        if encontrados:
            return ", ".join(encontrados[:3])
        return "Nao identificado"

    def _extrair_budget(self, texto: str) -> str:
        matches = REGEX_VALOR.findall(texto)
        if matches:
            return matches[0]
        return "Nao identificado"

    def _calcular_confianca(self, texto_lower: str) -> float:
        score = 0.5

        positivos = ["confio", "confianca", "parceiro", "recomendo", "gosto de trabalhar",
                      "sempre atende", "relacao", "bom atendimento"]
        negativos = ["nao confio", "desconfiado", "enganado", "mentiu", "promessas",
                      "nao cumpriu", "frustrado com voce", "outro vendedor"]

        for p in positivos:
            if p in texto_lower:
                score += 0.1

        for n in negativos:
            if n in texto_lower:
                score -= 0.15

        return max(0.0, min(1.0, round(score, 2)))

    def _extrair_contexto(self, texto: str, termo: str) -> str:
        idx = texto.lower().find(termo)
        if idx == -1:
            return ""
        start = max(0, idx - 80)
        end = min(len(texto), idx + len(termo) + 80)
        contexto = texto[start:end].strip()
        if start > 0:
            contexto = "..." + contexto
        if end < len(texto):
            contexto = contexto + "..."
        return contexto

    def _inferir_produto_modulo(self, modulo: str) -> str:
        mapa = {
            "folha de pagamento": "TOTVS RM",
            "folha": "TOTVS RM",
            "rh": "TOTVS RM",
            "recursos humanos": "TOTVS RM",
            "ponto eletronico": "TOTVS RM",
            "beneficios": "TOTVS RM",
            "recrutamento": "TOTVS RM",
            "e-social": "TOTVS RM",
            "financeiro": "TOTVS Protheus",
            "contabil": "TOTVS Protheus",
            "fiscal": "TOTVS Protheus",
            "compras": "TOTVS Protheus",
            "estoque": "TOTVS Protheus",
            "faturamento": "TOTVS Protheus",
            "vendas": "TOTVS Protheus",
            "pcp": "TOTVS Protheus",
            "producao": "TOTVS Protheus",
            "crm": "RD Station",
            "bi": "TOTVS Carol",
            "analytics": "TOTVS Carol",
        }
        return mapa.get(modulo, "TOTVS Protheus")

    def _gerar_resumo(self, texto: str, sentimento: str, produtos: list,
                      oportunidades: list, riscos: list) -> str:
        partes = []

        prod_atuais = [p["nomeProduto"] for p in produtos if p["tipo"] == "ATUAL"]
        prod_conc = [p["nomeProduto"] for p in produtos if p["tipo"] == "CONCORRENTE"]

        if prod_atuais:
            partes.append(f"Cliente utiliza {', '.join(prod_atuais)}")

        sent_map = {
            "POSITIVO": "Sentimento geral positivo na conversa",
            "NEGATIVO": "Sentimento geral negativo na conversa",
            "NEUTRO": "Sentimento neutro na conversa",
            "MISTO": "Sentimento misto na conversa (aspectos positivos e negativos)",
        }
        partes.append(sent_map.get(sentimento, ""))

        if oportunidades:
            partes.append(f"{len(oportunidades)} oportunidade(s) de venda identificada(s)")

        if riscos:
            riscos_altos = [r for r in riscos if r["severidade"] == "ALTO"]
            if riscos_altos:
                partes.append(f"ATENCAO: {len(riscos_altos)} risco(s) de alta severidade")

        if prod_conc:
            partes.append(f"Concorrentes mencionados: {', '.join(prod_conc)}")

        return ". ".join(p for p in partes if p) + "."
