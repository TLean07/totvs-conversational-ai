"""
Motor de analise de transcricoes usando HuggingFace (sentimento) + regras (entidades).
"""

import logging
import re

logger = logging.getLogger(__name__)


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
                    "sentiment-analysis",
                    model=model_name,
                    truncation=True,
                    max_length=512,
                )
                logger.info("Modelo local carregado com sucesso")
            except Exception as exc:
                logger.warning(
                    "Modelo local indisponivel; analise seguira por fallback de regras: %s",
                    exc.__class__.__name__,
                )

    def is_ready(self) -> bool:
        return self.sentiment_pipeline is not None

    def analisar(self, texto: str, contexto: dict | None = None) -> dict:
        texto_lower = texto.lower()
        contexto = contexto or {}

        sentimento = self._analisar_sentimento(texto)
        produtos = self._extrair_produtos(texto, texto_lower, contexto)
        concorrentes = self._extrair_concorrentes(texto_lower, contexto)
        oportunidades = self._extrair_oportunidades(texto, texto_lower, produtos)
        riscos = self._extrair_riscos(texto, texto_lower, concorrentes)
        metricas = self._extrair_metricas_consolidadas(texto)
        riscos_consolidados = self._consolidar_riscos(riscos, metricas)
        oportunidades_consolidadas = self._consolidar_oportunidades(oportunidades, metricas)
        alertas_consolidados = self._gerar_alertas_consolidados(metricas, riscos_consolidados)
        persona = self._identificar_persona(texto_lower)
        budget = self._extrair_budget(texto)
        score_confianca = self._calcular_confianca(texto_lower)
        resumo = self._gerar_resumo(texto, sentimento, produtos, oportunidades, riscos, contexto)

        return {
            "sentimentoGeral": sentimento,
            "resumo": resumo,
            "personaIdentificada": persona,
            "budgetIdentificado": budget,
            "scoreConfiancaVendedor": score_confianca,
            "scoreSentimento": self._score_sentimento(sentimento, metricas),
            "metricasAgregadas": metricas,
            "produtos": produtos,
            "oportunidades": oportunidades,
            "riscos": riscos,
            "riscosConsolidados": riscos_consolidados,
            "oportunidadesConsolidadas": oportunidades_consolidadas,
            "alertasConsolidados": alertas_consolidados,
            "itensCriticos": self._extrair_itens_criticos(texto, riscos_consolidados),
        }

    def _analisar_sentimento(self, texto: str) -> str:
        if self.sentiment_pipeline is None:
            return self._analisar_sentimento_por_regras(texto.lower())

        # Divide texto em chunks se for muito grande
        chunks = [texto[i:i+500] for i in range(0, len(texto), 500)]
        chunks = chunks[:self.max_sentiment_chunks]
        scores = []

        for chunk in chunks:
            try:
                result = self.sentiment_pipeline(chunk)[0]
                # Modelo retorna 1-5 estrelas
                star = int(result["label"][0])
                scores.append(star)
            except Exception as exc:
                logger.warning("Falha ao classificar chunk de sentimento: %s", exc.__class__.__name__)

        if not scores:
            return self._analisar_sentimento_por_regras(texto.lower())

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

    def _analisar_sentimento_por_regras(self, texto_lower: str) -> str:
        tem_positivo = any(s in texto_lower for s in SINAIS_SATISFACAO)
        tem_negativo = any(s in texto_lower for s in SINAIS_INSATISFACAO)
        tem_churn = any(s in texto_lower for s in SINAIS_CHURN)

        if tem_positivo and (tem_negativo or tem_churn):
            return "MISTO"
        if tem_churn or tem_negativo:
            return "NEGATIVO"
        if tem_positivo:
            return "POSITIVO"
        return "NEUTRO"

    def _extrair_produtos(self, texto: str, texto_lower: str, contexto: dict | None = None) -> list:
        produtos = []
        encontrados = set()

        for chave, nome in PRODUTOS_TOTVS.items():
            if chave in texto_lower and nome not in encontrados:
                encontrados.add(nome)
                contexto_texto = self._extrair_contexto(texto, chave)

                # Determinar se e produto atual ou oportunidade
                tipo = "ATUAL"
                ctx_lower = contexto_texto.lower()
                if any(s in ctx_lower for s in ["precisamos", "queremos", "implantar", "interesse", "novo"]):
                    tipo = "OPORTUNIDADE"

                produtos.append({
                    "nomeProduto": nome,
                    "tipo": tipo,
                    "contexto": contexto_texto,
                })

        for nome in self._context_list(contexto, "produtosFrequentes"):
            chave = nome.lower()
            if chave in texto_lower and nome not in encontrados:
                encontrados.add(nome)
                produtos.append({
                    "nomeProduto": nome,
                    "tipo": "ATUAL",
                    "contexto": self._extrair_contexto(texto, chave),
                })

        # Adicionar concorrentes como produtos tipo CONCORRENTE
        for chave, nome in CONCORRENTES.items():
            if chave in texto_lower and nome not in encontrados:
                encontrados.add(nome)
                contexto_texto = self._extrair_contexto(texto, chave)
                produtos.append({
                    "nomeProduto": nome,
                    "tipo": "CONCORRENTE",
                    "contexto": contexto_texto,
                })

        return produtos

    def _extrair_concorrentes(self, texto_lower: str, contexto: dict | None = None) -> list:
        encontrados = []
        for chave, nome in CONCORRENTES.items():
            if chave in texto_lower and nome not in encontrados:
                encontrados.append(nome)
        for nome in self._context_list(contexto, "concorrentesFrequentes"):
            if nome.lower() in texto_lower and nome not in encontrados:
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

    def _extrair_metricas_consolidadas(self, texto: str) -> dict:
        def number(label: str) -> int:
            match = re.search(rf"{label}:\s*(\d+)", texto, re.IGNORECASE)
            return int(match.group(1)) if match else 0

        partes_match = re.search(
            r"SENTIMENTO_PARTES:\s*positivas=(\d+),\s*neutras=(\d+),\s*negativas=(\d+)",
            texto,
            re.IGNORECASE,
        )
        positivas = int(partes_match.group(1)) if partes_match else 0
        neutras = int(partes_match.group(2)) if partes_match else 0
        negativas = int(partes_match.group(3)) if partes_match else 0
        total = number("TOTAL_PARTES_VALIDAS") or positivas + neutras + negativas
        riscos = number("RISCOS_DETECTADOS")
        oportunidades = number("OPORTUNIDADES_DETECTADAS")
        churns = number("CHURNS_POTENCIAIS")
        criticas = number("MENCOES_CRITICAS")
        alertas = 0
        if negativas >= 10 or (total and negativas / total >= 0.15):
            alertas += 1
        if churns:
            alertas += 1
        if criticas >= 5:
            alertas += 1

        return {
            "totalPartes": total,
            "partesAnalisadas": total,
            "partesNegativas": negativas,
            "partesNeutras": neutras,
            "partesPositivas": positivas,
            "riscosDetectados": riscos,
            "oportunidadesDetectadas": oportunidades,
            "churnsPotenciais": churns,
            "mencoesCriticas": criticas,
            "alertasConsolidados": alertas,
            "chunksProcessados": max(1, (total + 49) // 50) if total else 1,
        }

    def _consolidar_riscos(self, riscos: list, metricas: dict) -> list:
        buckets: dict[str, dict] = {}
        for risco in riscos:
            categoria = risco.get("tipo") or "INSATISFACAO"
            bucket = buckets.setdefault(categoria, {
                "categoria": categoria.title().replace("_", " "),
                "severidade": "ALTO" if categoria == "CHURN" else "MEDIO",
                "ocorrencias": 0,
                "evidencias": [],
                "recomendacao": "Agrupar evidencias, priorizar causa raiz e definir responsavel pelo plano de acao.",
            })
            bucket["ocorrencias"] += 1
            if len(bucket["evidencias"]) < 5:
                bucket["evidencias"].append(risco.get("descricao", "")[:240])

        if metricas.get("churnsPotenciais", 0) and "CHURN" not in buckets:
            buckets["CHURN"] = {
                "categoria": "Churn",
                "severidade": "ALTO",
                "ocorrencias": metricas["churnsPotenciais"],
                "evidencias": [],
                "recomendacao": "Acionar plano de retencao e revisar causas recorrentes.",
            }
        if metricas.get("riscosDetectados", 0) and "INSATISFACAO" not in buckets:
            buckets["INSATISFACAO"] = {
                "categoria": "Insatisfacao",
                "severidade": "ALTO" if metricas["riscosDetectados"] >= 15 else "MEDIO",
                "ocorrencias": metricas["riscosDetectados"],
                "evidencias": [],
                "recomendacao": "Priorizar dores recorrentes e criar plano de comunicacao com o cliente.",
            }
        return list(buckets.values())

    def _consolidar_oportunidades(self, oportunidades: list, metricas: dict) -> list:
        buckets: dict[str, dict] = {}
        for oportunidade in oportunidades:
            categoria = oportunidade.get("tipo") or "NOVO_PRODUTO"
            bucket = buckets.setdefault(categoria, {
                "categoria": categoria.title().replace("_", " "),
                "score": 0.65,
                "ocorrencias": 0,
                "evidencias": [],
                "recomendacao": "Validar necessidade, impacto e proximo passo comercial.",
            })
            bucket["ocorrencias"] += 1
            bucket["score"] = max(bucket["score"], oportunidade.get("score", 0.65))
            if len(bucket["evidencias"]) < 5:
                bucket["evidencias"].append(oportunidade.get("descricao", "")[:240])
        if metricas.get("oportunidadesDetectadas", 0) and not buckets:
            buckets["NOVO_PRODUTO"] = {
                "categoria": "Melhoria no atendimento",
                "score": 0.72,
                "ocorrencias": metricas["oportunidadesDetectadas"],
                "evidencias": [],
                "recomendacao": "Transformar recorrencias em backlog comercial e de produto.",
            }
        return list(buckets.values())

    def _gerar_alertas_consolidados(self, metricas: dict, riscos_consolidados: list) -> list:
        alertas = []
        total = metricas.get("partesAnalisadas", 0)
        negativas = metricas.get("partesNegativas", 0)
        taxa_negativa = negativas / total if total else 0
        if negativas >= 10 or taxa_negativa >= 0.15:
            alertas.append({
                "titulo": "Alta recorrencia de sentimento negativo",
                "categoria": "Sentimento negativo",
                "severidade": "ALTO" if taxa_negativa >= 0.3 else "MEDIO",
                "ocorrencias": negativas,
                "motivo": "Volume relevante de partes negativas na transcricao consolidada.",
                "recomendacao": "Priorizar revisao das evidencias e contato de recuperacao.",
            })
        if metricas.get("churnsPotenciais", 0):
            alertas.append({
                "titulo": "Indicadores de churn no CSV",
                "categoria": "Churn",
                "severidade": "ALTO",
                "ocorrencias": metricas["churnsPotenciais"],
                "motivo": "Foram encontrados sinais de cancelamento, troca ou nao renovacao.",
                "recomendacao": "Abrir plano de retencao com causa raiz e responsaveis.",
            })
        if metricas.get("mencoesCriticas", 0) >= 5:
            alertas.append({
                "titulo": "Muitas mencoes criticas operacionais",
                "categoria": "Criticidade operacional",
                "severidade": "ALTO" if metricas["mencoesCriticas"] >= 15 else "MEDIO",
                "ocorrencias": metricas["mencoesCriticas"],
                "motivo": "Recorrencia de mencoes criticas no material analisado.",
                "recomendacao": "Consolidar causas tecnicas e criar plano de acao com suporte e produto.",
            })
        return alertas

    def _extrair_itens_criticos(self, texto: str, riscos_consolidados: list) -> list:
        itens = []
        for linha in texto.splitlines():
            linha_lower = linha.lower()
            if any(s in linha_lower for s in ["cancelar", "nao renovar", "churn", "urgente", "critico", "parado", "erro"]):
                itens.append({
                    "tipo": "risco",
                    "categoria": "Churn" if any(s in linha_lower for s in ["cancelar", "nao renovar", "churn"]) else "Criticidade operacional",
                    "trecho": linha.strip()[:300],
                    "severidade": "ALTO",
                })
            if len(itens) >= 20:
                break
        return itens

    def _score_sentimento(self, sentimento: str, metricas: dict) -> float:
        total = metricas.get("partesAnalisadas", 0)
        if total:
            return round((metricas.get("partesPositivas", 0) - metricas.get("partesNegativas", 0)) / total, 2)
        return {"POSITIVO": 0.7, "MISTO": 0.0, "NEUTRO": 0.0, "NEGATIVO": -0.7}.get(sentimento, 0.0)

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
                      oportunidades: list, riscos: list, contexto: dict | None = None) -> str:
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

        contexto_partes = self._resumir_contexto_adaptativo(contexto)
        if contexto_partes:
            partes.append(contexto_partes)

        proxima_acao = self._sugerir_proxima_acao(sentimento, oportunidades, riscos, prod_conc)
        if proxima_acao:
            partes.append(f"Proxima acao sugerida: {proxima_acao}")

        return ". ".join(p for p in partes if p) + "."

    def _context_list(self, contexto: dict | None, key: str) -> list:
        if not contexto:
            return []
        values = contexto.get(key) or []
        if not isinstance(values, list):
            return []
        return [str(value).strip() for value in values if str(value).strip()][:8]

    def _resumir_contexto_adaptativo(self, contexto: dict | None) -> str:
        produtos = self._context_list(contexto, "produtosFrequentes")[:3]
        riscos = self._context_list(contexto, "riscosFrequentes")[:3]
        ajustes = self._context_list(contexto, "ajustesRecentes")[:2]
        if not produtos and not riscos and not ajustes:
            return ""

        partes = []
        if produtos:
            partes.append(f"LeanIA considerou historico recorrente de produtos: {', '.join(produtos)}")
        if riscos:
            partes.append(f"padroes de risco frequentes: {', '.join(riscos)}")
        if ajustes:
            partes.append(f"feedbacks recentes para calibracao: {', '.join(ajustes)}")
        return "; ".join(partes)

    def _sugerir_proxima_acao(self, sentimento: str, oportunidades: list,
                              riscos: list, concorrentes: list) -> str:
        if any(r.get("severidade") == "ALTO" for r in riscos):
            if concorrentes:
                return "acionar lideranca comercial e preparar comparativo contra concorrente"
            return "priorizar contato de recuperacao com plano de acao objetivo"

        if oportunidades:
            alta = any(o.get("score", 0) >= 0.75 for o in oportunidades)
            if alta:
                return "enviar proposta ou agendar demonstracao com decisores"
            return "qualificar necessidade, budget e prazo antes da proposta"

        if sentimento == "NEGATIVO":
            return "registrar dores principais e confirmar responsavel pelo retorno"

        if sentimento == "POSITIVO":
            return "explorar expansao de uso e pedir indicacoes internas"

        return "coletar mais contexto sobre dor, budget e decisores"
