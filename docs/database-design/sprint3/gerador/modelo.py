# -*- coding: utf-8 -*-
"""
Modelo relacional do TOTVS Inteligencia Conversacional (fonte unica de verdade).

A partir deste modelo sao gerados: o script DDL, o design do Oracle SQL Developer Data Modeler,
os diagramas do documento e o dicionario de dados. Os tipos seguem exatamente o que o backend
(Spring Data JPA / Hibernate, ddl-auto=validate) espera encontrar no Oracle 19c da FIAP.
"""

EQUIPE = "Company 404"
SOLUCAO = "TOTVS Inteligencia Conversacional"
INTEGRANTES = [
    ("Bruno Gama Masseiras", "RM562526"),
    ("Kauai Rosa de Assis Rocha", "RM563256"),
    ("Lucas Gilmar Pedro da Silva", "RM566231"),
    ("Leandro Afonso Silva Santos Junior", "RM561344"),
    ("Murilo da Silva Basto", "RM565445"),
]

SEVERIDADES = "('ALTO','MEDIO','BAIXO')"

# Sequencias e identidade ------------------------------------------------------------------
SEQUENCIAS = [
    {
        "nome": "SEQ_TRANSCRICAO_PROTOCOLO",
        "start": 1,
        "increment": 1,
        "comentario": "Numero de protocolo sequencial e legivel das transcricoes (NUM_PROTOCOLO). "
                      "O ID continua sendo UUID gerado pela aplicacao.",
    },
]


def col(nome, tipo, comentario, nulo=True, default=None, check=None, identity=False):
    return {
        "nome": nome, "tipo": tipo, "comentario": comentario, "nulo": nulo,
        "default": default, "check": check, "identity": identity,
    }


METRICAS = [
    col("MET_TOTAL_PARTES", "NUMBER(10,0)", "Metricas agregadas: quantidade de partes (turnos/registros) encontradas na reuniao"),
    col("MET_PARTES_ANALISADAS", "NUMBER(10,0)", "Metricas agregadas: partes efetivamente analisadas pela LeanIA"),
    col("MET_PARTES_NEGATIVAS", "NUMBER(10,0)", "Metricas agregadas: partes com sentimento negativo"),
    col("MET_PARTES_NEUTRAS", "NUMBER(10,0)", "Metricas agregadas: partes com sentimento neutro"),
    col("MET_PARTES_POSITIVAS", "NUMBER(10,0)", "Metricas agregadas: partes com sentimento positivo"),
    col("MET_RISCOS_DETECTADOS", "NUMBER(10,0)", "Metricas agregadas: total de riscos detectados"),
    col("MET_OPORTUNIDADES_DETECTADAS", "NUMBER(10,0)", "Metricas agregadas: total de oportunidades detectadas"),
    col("MET_CHURNS_POTENCIAIS", "NUMBER(10,0)", "Metricas agregadas: sinais de churn (cancelamento/troca) encontrados"),
    col("MET_MENCOES_CRITICAS", "NUMBER(10,0)", "Metricas agregadas: mencoes criticas (operacionais ou comerciais)"),
    col("MET_ALERTAS_CONSOLIDADOS", "NUMBER(10,0)", "Metricas agregadas: alertas consolidados gerados por recorrencia"),
    col("MET_CHUNKS_PROCESSADOS", "NUMBER(10,0)", "Metricas agregadas: blocos (chunks) de texto enviados a LeanIA"),
]


def _lista_filha(nome, pai, fk_col, fk_nome, pk_nome, comentario, colunas, comentario_lista):
    """Tabela filha ordenada (POSICAO preserva a ordem da lista na aplicacao)."""
    return {
        "nome": nome,
        "comentario": comentario,
        "colunas": [
            col(fk_col, "VARCHAR2(36 CHAR)", "Referencia a %s.ID (dono da lista)" % pai, nulo=False),
            col("POSICAO", "NUMBER(10,0)", "Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA", nulo=False),
        ] + colunas,
        "pk": {"nome": pk_nome, "colunas": ["POSICAO", fk_col]},
        "uks": [],
        "fks": [{"nome": fk_nome, "colunas": [fk_col], "ref": pai, "ref_colunas": ["ID"], "on_delete": "CASCADE"}],
        "checks": [],
        "indices": [],
        "grupo": comentario_lista,
    }


TABELAS = []

# --- Seguranca e acesso -------------------------------------------------------------------
TABELAS.append({
    "nome": "APP_USERS",
    "comentario": "Usuarios que acessam a plataforma (login por e-mail e senha com hash BCrypt)",
    "colunas": [
        col("ID", "VARCHAR2(36 CHAR)", "Identificador UUID gerado pela aplicacao (chave primaria)", nulo=False),
        col("EMAIL", "VARCHAR2(320 CHAR)", "E-mail de login; unico no sistema (UK_APP_USERS_EMAIL)", nulo=False),
        col("USER_NAME", "VARCHAR2(200 CHAR)", "Nome de exibicao do usuario"),
        col("PASSWORD_HASH", "VARCHAR2(200 CHAR)", "Hash BCrypt da senha; a senha em claro nunca e armazenada"),
        col("USER_ROLE", "VARCHAR2(20 CHAR)", "Perfil de acesso: ADMIN (ve tudo) ou USER (ve apenas o que criou)",
            check="USER_ROLE IN ('ADMIN','USER')"),
        col("IS_ACTIVE", "NUMBER(1,0)", "1 = usuario ativo (pode logar); 0 = desativado", nulo=False, check="IS_ACTIVE IN (0,1)"),
        col("CREATED_AT", "TIMESTAMP(6)", "Data/hora de criacao do usuario"),
    ],
    "pk": {"nome": "PK_APP_USERS", "colunas": ["ID"]},
    "uks": [{"nome": "UK_APP_USERS_EMAIL", "colunas": ["EMAIL"]}],
    "fks": [],
    "checks": [],
    "indices": [],
    "grupo": "Seguranca e acesso",
})

TABELAS.append({
    "nome": "REFRESH_TOKENS",
    "comentario": "Sessoes de refresh do JWT (cookie HttpOnly). Apenas o hash SHA-256 do token e persistido; REVOKED_AT preenchido invalida o token",
    "colunas": [
        col("ID", "VARCHAR2(36 CHAR)", "Identificador UUID da sessao (chave primaria)", nulo=False),
        col("USER_ID", "VARCHAR2(36 CHAR)", "Usuario dono da sessao (APP_USERS.ID)", nulo=False),
        col("TOKEN_HASH", "VARCHAR2(64 CHAR)", "SHA-256 (hex) do refresh token; unico (UK_REFRESH_TOKENS_HASH)", nulo=False),
        col("CREATED_AT", "TIMESTAMP(6)", "Data/hora de emissao do token"),
        col("EXPIRES_AT", "TIMESTAMP(6)", "Data/hora de expiracao do token"),
        col("REVOKED_AT", "TIMESTAMP(6)", "Data/hora de revogacao (logout ou rotacao); NULL = token valido"),
    ],
    "pk": {"nome": "PK_REFRESH_TOKENS", "colunas": ["ID"]},
    "uks": [{"nome": "UK_REFRESH_TOKENS_HASH", "colunas": ["TOKEN_HASH"]}],
    "fks": [{"nome": "FK_REFRESH_TOKENS_USER", "colunas": ["USER_ID"], "ref": "APP_USERS", "ref_colunas": ["ID"], "on_delete": "CASCADE"}],
    "checks": [],
    "indices": [{"nome": "IDX_REFRESH_TOKENS_USER", "colunas": ["USER_ID"]}],
    "grupo": "Seguranca e acesso",
})

# --- Transcricoes -------------------------------------------------------------------------
TABELAS.append({
    "nome": "TRANSCRICOES",
    "comentario": "Transcricao de reuniao comercial (manual, JSON/CSV TOTVS ou audio). 1 reuniao = 1 linha; o texto integral fica em CLOB",
    "colunas": [
        col("ID", "VARCHAR2(36 CHAR)", "Identificador UUID gerado pela aplicacao (chave primaria)", nulo=False),
        col("NUM_PROTOCOLO", "NUMBER(10,0)", "Numero de protocolo sequencial e legivel (SEQ_TRANSCRICAO_PROTOCOLO); unico",
            nulo=False, default="SEQ_TRANSCRICAO_PROTOCOLO.NEXTVAL"),
        col("OWNER_ID", "VARCHAR2(36 CHAR)", "Usuario que cadastrou/importou a reuniao (APP_USERS.ID)"),
        col("TEXTO_ORIGINAL", "CLOB", "Texto integral da transcricao usado na analise"),
        col("PARTICIPANTES", "VARCHAR2(1000 CHAR)", "Participantes informados (nomes ou LOCUTOR N quando anonimizado)"),
        col("DATA_REUNIAO", "TIMESTAMP(6)", "Data/hora em que a reuniao aconteceu"),
        col("DURACAO_MINUTOS", "NUMBER(10,0)", "Duracao da reuniao em minutos"),
        col("CLIENTE", "VARCHAR2(200 CHAR)", "Nome (ou codigo CODT) do cliente"),
        col("VENDEDOR", "VARCHAR2(200 CHAR)", "Vendedor/consultor TOTVS responsavel pela reuniao"),
        col("ORIGEM_TRANSCRICAO", "VARCHAR2(30 CHAR)", "Origem do texto: manual, json, csv ou parakeet (transcricao de audio)"),
        col("MEETING_ID", "VARCHAR2(100 CHAR)", "ID_MEETING do JSON/CSV TOTVS; unico quando informado (NULL nao entra no indice unico)"),
        col("MEETING_DATE", "VARCHAR2(50 CHAR)", "DT_MEETING original do arquivo TOTVS (texto, como veio na fonte)"),
        col("FORMATO", "VARCHAR2(50 CHAR)", "FORMATO_MEETING do arquivo TOTVS (VIDEO, LINK...)"),
        col("MEETING_STATUS", "VARCHAR2(50 CHAR)", "STATUS_MEETING do arquivo TOTVS (ex.: COMPLETED)"),
        col("MEETING_DURATION", "VARCHAR2(30 CHAR)", "Duracao original no formato hh:mm:ss do arquivo TOTVS"),
        col("CODT", "VARCHAR2(50 CHAR)", "Codigo do cliente na TOTVS (CODT)"),
        col("FLG_EXTERNO", "NUMBER(1,0)", "1 = reuniao externa (com cliente); 0 = interna", check="FLG_EXTERNO IN (0,1)"),
        col("CREATED_AT_SOURCE", "VARCHAR2(50 CHAR)", "DT_CRIACAO do registro na fonte TOTVS"),
        col("TRANSCRIPTION_TEXT", "CLOB", "Texto bruto da fonte (ANON_TRANSCRICAO ou saida do modelo de audio) antes de normalizacao"),
        col("SOURCE_FILE_NAME", "VARCHAR2(200 CHAR)", "Nome do arquivo importado (JSON, CSV ou audio)"),
        col("IMPORT_BATCH_ID", "VARCHAR2(36 CHAR)", "UUID do lote de importacao; agrupa as reunioes de um mesmo arquivo"),
        col("IMPORTED_AT", "TIMESTAMP(6)", "Data/hora da importacao do arquivo"),
        col("MISSING_MEETING_ID", "NUMBER(1,0)", "1 = a fonte nao trouxe ID_MEETING (MEETING_ID gerado pela aplicacao)",
            nulo=False, check="MISSING_MEETING_ID IN (0,1)"),
        col("AUDIO_FILE_NAME", "VARCHAR2(200 CHAR)", "Nome do arquivo de audio quando a origem e transcricao de audio"),
        col("TRANSCRIPTION_MODEL", "VARCHAR2(100 CHAR)", "Modelo usado na transcricao do audio (ex.: parakeet)"),
        col("TRANSCRIPTION_LANGUAGE", "VARCHAR2(10 CHAR)", "Idioma detectado/informado na transcricao do audio (ex.: pt)"),
        col("AUDIO_DURATION_SECONDS", "FLOAT(53)", "Duracao do audio em segundos"),
        col("TRANSCRIPTION_PROCESSING_MS", "NUMBER(10,0)", "Tempo de processamento da transcricao do audio em milissegundos"),
        col("UF", "VARCHAR2(10 CHAR)", "UF do cliente (metadado TOTVS)"),
        col("CNAE", "VARCHAR2(20 CHAR)", "CNAE principal do cliente (metadado TOTVS)"),
        col("NOME_UNIDADE", "VARCHAR2(200 CHAR)", "Unidade TOTVS responsavel pela conta (ex.: TOTVS SAUDE)"),
        col("NOME_SEGMENTO", "VARCHAR2(200 CHAR)", "Segmento do cliente (SERVICOS, SAUDE, MANUFATURA, VAREJO...)"),
        col("FAIXA_FATURAMENTO", "VARCHAR2(200 CHAR)", "Faixa de faturamento do cliente (FAIXA_FATURAMENTO_CLIENTE)"),
        col("TP_RECURSO", "VARCHAR2(50 CHAR)", "Tipo do contato na fonte: lead, customer, VENDAS, SUPORTE, OPERACIONAL"),
        col("NOTA_NPS", "NUMBER(10,0)", "Nota NPS (0-10) informada pelo cliente na ultima pesquisa"),
        col("DT_ULTIMA_PESQUISA", "VARCHAR2(50 CHAR)", "Competencia (AAAA-MM) da ultima pesquisa NPS"),
        col("STATUS", "VARCHAR2(20 CHAR)", "Estado do processamento pela LeanIA: PENDENTE, PROCESSANDO, PROCESSADA, ERRO",
            check="STATUS IN ('PENDENTE','PROCESSANDO','PROCESSADA','ERRO')"),
        col("DATA_CRIACAO", "TIMESTAMP(6)", "Data/hora de criacao do registro"),
        col("DATA_PROCESSAMENTO", "TIMESTAMP(6)", "Data/hora em que a analise foi concluida"),
    ] + METRICAS + [
        col("RESUMO_EXECUTIVO", "CLOB", "Resumo executivo consolidado da reuniao (copiado da analise atual)"),
        col("ANALISE_ID", "VARCHAR2(36 CHAR)", "Analise atual da transcricao (ANALISES.ID); unica e fica NULL quando a analise e removida"),
    ],
    "pk": {"nome": "PK_TRANSCRICOES", "colunas": ["ID"]},
    "uks": [
        {"nome": "UK_TRANSCRICOES_MEETING_ID", "colunas": ["MEETING_ID"]},
        {"nome": "UK_TRANSCRICOES_PROTOCOLO", "colunas": ["NUM_PROTOCOLO"]},
        {"nome": "UK_TRANSCRICOES_ANALISE", "colunas": ["ANALISE_ID"]},
    ],
    "fks": [
        {"nome": "FK_TRANSCRICOES_OWNER", "colunas": ["OWNER_ID"], "ref": "APP_USERS", "ref_colunas": ["ID"], "on_delete": None},
        # FK para ANALISES e criada via ALTER TABLE depois de ANALISES existir (referencia circular 1:1)
        {"nome": "FK_TRANSCRICOES_ANALISE", "colunas": ["ANALISE_ID"], "ref": "ANALISES", "ref_colunas": ["ID"], "on_delete": "SET NULL", "posterior": True},
    ],
    "checks": [],
    "indices": [
        {"nome": "IDX_TRANSC_OWNER_CRIACAO", "colunas": ["OWNER_ID", "DATA_CRIACAO"]},
        {"nome": "IDX_TRANSC_OWNER_STATUS", "colunas": ["OWNER_ID", "STATUS"]},
        {"nome": "IDX_TRANSC_CLIENTE", "colunas": ["CLIENTE"]},
        {"nome": "IDX_TRANSC_STATUS", "colunas": ["STATUS"]},
        {"nome": "IDX_TRANSC_DATA_CRIACAO", "colunas": ["DATA_CRIACAO"]},
        {"nome": "IDX_TRANSC_IMPORT_BATCH", "colunas": ["IMPORT_BATCH_ID"]},
        {"nome": "IDX_TRANSC_SEGMENTO", "colunas": ["NOME_SEGMENTO"]},
        {"nome": "IDX_TRANSC_UF", "colunas": ["UF"]},
    ],
    "grupo": "Transcricoes",
})

TABELAS.append(_lista_filha(
    "TRANSCRICAO_WARNINGS", "TRANSCRICOES", "TRANSCRICAO_ID", "FK_TRANSCRICAO_WARNINGS", "PK_TRANSCRICAO_WARNINGS",
    "Avisos gerados na importacao/transcricao da reuniao (ex.: MISSING_ID_MEETING, INVALID_DT_MEETING)",
    [col("WARNING", "VARCHAR2(200 CHAR)", "Codigo ou texto do aviso")],
    "Transcricoes"))

TABELAS.append(_lista_filha(
    "TRANSCRICAO_RISCOS_CONS", "TRANSCRICOES", "TRANSCRICAO_ID", "FK_TRANSC_RISCOS_CONS", "PK_TRANSC_RISCOS_CONS",
    "Riscos consolidados da reuniao agrupados por categoria (copia da analise atual, usada pelo dashboard)",
    [
        col("CATEGORIA", "VARCHAR2(200 CHAR)", "Categoria do risco: Churn, Concorrencia, Insatisfacao, Perda de confianca..."),
        col("SEVERIDADE", "VARCHAR2(10 CHAR)", "Severidade consolidada: ALTO, MEDIO ou BAIXO", check="SEVERIDADE IN " + SEVERIDADES),
        col("OCORRENCIAS", "NUMBER(10,0)", "Quantidade de vezes que o risco apareceu na reuniao"),
        col("EVIDENCIAS", "CLOB", "Lista JSON de ate 5 trechos curtos que evidenciam o risco"),
        col("RECOMENDACAO", "CLOB", "Recomendacao de acao comercial para mitigar o risco"),
    ],
    "Transcricoes"))

TABELAS.append(_lista_filha(
    "TRANSCRICAO_OPORT_CONS", "TRANSCRICOES", "TRANSCRICAO_ID", "FK_TRANSC_OPORT_CONS", "PK_TRANSC_OPORT_CONS",
    "Oportunidades consolidadas da reuniao agrupadas por categoria (copia da analise atual, usada pelo dashboard)",
    [
        col("CATEGORIA", "VARCHAR2(200 CHAR)", "Categoria da oportunidade: Upsell, Cross-sell, Novo produto, Melhoria no atendimento..."),
        col("SCORE", "FLOAT(53)", "Probabilidade estimada da oportunidade (0 a 1)"),
        col("OCORRENCIAS", "NUMBER(10,0)", "Quantidade de sinais da oportunidade na reuniao"),
        col("EVIDENCIAS", "CLOB", "Lista JSON de ate 5 trechos curtos que evidenciam a oportunidade"),
        col("RECOMENDACAO", "CLOB", "Recomendacao de acao comercial para aproveitar a oportunidade"),
    ],
    "Transcricoes"))

TABELAS.append(_lista_filha(
    "TRANSCRICAO_ALERTAS_CONS", "TRANSCRICOES", "TRANSCRICAO_ID", "FK_TRANSC_ALERTAS_CONS", "PK_TRANSC_ALERTAS_CONS",
    "Alertas consolidados da reuniao (titulo, categoria, severidade, motivo e recomendacao)",
    [
        col("TITULO", "VARCHAR2(500 CHAR)", "Titulo curto do alerta (ex.: Risco de churn)"),
        col("CATEGORIA", "VARCHAR2(200 CHAR)", "Categoria do alerta (Churn, Concorrencia, Sentimento negativo...)"),
        col("SEVERIDADE", "VARCHAR2(10 CHAR)", "Severidade: ALTO, MEDIO ou BAIXO", check="SEVERIDADE IN " + SEVERIDADES),
        col("OCORRENCIAS", "NUMBER(10,0)", "Quantidade de ocorrencias que originaram o alerta"),
        col("MOTIVO", "CLOB", "Motivo do alerta em linguagem de negocio"),
        col("RECOMENDACAO", "CLOB", "Acao recomendada ao time comercial"),
    ],
    "Transcricoes"))

TABELAS.append(_lista_filha(
    "TRANSCRICAO_ITENS_CRITICOS", "TRANSCRICOES", "TRANSCRICAO_ID", "FK_TRANSC_ITENS_CRITICOS", "PK_TRANSC_ITENS_CRITICOS",
    "Trechos mais importantes da reuniao para revisao humana (evidencias de risco ou oportunidade)",
    [
        col("TIPO", "VARCHAR2(100 CHAR)", "Tipo do item: risco ou oportunidade"),
        col("CATEGORIA", "VARCHAR2(200 CHAR)", "Categoria do item (Churn, Concorrencia, Upsell...)"),
        col("TRECHO", "CLOB", "Trecho literal da transcricao (ate 300 caracteres)"),
        col("SEVERIDADE", "VARCHAR2(10 CHAR)", "Severidade: ALTO, MEDIO ou BAIXO", check="SEVERIDADE IN " + SEVERIDADES),
    ],
    "Transcricoes"))

TABELAS.append({
    "nome": "TRANSCRICAO_STATUS_HIST",
    "comentario": "Historico das mudancas de STATUS da transcricao (PENDENTE > PROCESSANDO > PROCESSADA/ERRO). "
                  "Alimentado automaticamente pelo trigger TRG_TRANSCRICOES_STATUS_HIST",
    "colunas": [
        col("ID", "NUMBER(19,0)", "Identificador numerico gerado por IDENTITY (chave primaria)", nulo=False, identity=True),
        col("TRANSCRICAO_ID", "VARCHAR2(36 CHAR)", "Transcricao que mudou de status (TRANSCRICOES.ID)", nulo=False),
        col("STATUS_ANTERIOR", "VARCHAR2(20 CHAR)", "Status antes da mudanca; NULL no registro de criacao"),
        col("STATUS_NOVO", "VARCHAR2(20 CHAR)", "Status depois da mudanca", nulo=False,
            check="STATUS_NOVO IN ('PENDENTE','PROCESSANDO','PROCESSADA','ERRO')"),
        col("ALTERADO_EM", "TIMESTAMP(6)", "Data/hora da mudanca", nulo=False, default="SYSTIMESTAMP"),
        col("ALTERADO_POR", "VARCHAR2(100 CHAR)", "Usuario Oracle da sessao que fez a mudanca", default="USER"),
        col("OBSERVACAO", "VARCHAR2(500 CHAR)", "Observacao livre (motivo do erro, reprocessamento, etc.)"),
    ],
    "pk": {"nome": "PK_TRANSC_STATUS_HIST", "colunas": ["ID"]},
    "uks": [],
    "fks": [{"nome": "FK_TRANSC_STATUS_HIST", "colunas": ["TRANSCRICAO_ID"], "ref": "TRANSCRICOES", "ref_colunas": ["ID"], "on_delete": "CASCADE"}],
    "checks": [],
    "indices": [{"nome": "IDX_TRANSC_STATUS_HIST", "colunas": ["TRANSCRICAO_ID", "ALTERADO_EM"]}],
    "grupo": "Transcricoes",
})

# --- Analises (LeanIA) --------------------------------------------------------------------
TABELAS.append({
    "nome": "ANALISES",
    "comentario": "Resultado da analise da LeanIA para uma transcricao: sentimento, persona, budget, scores, metricas, proxima acao e insights",
    "colunas": [
        col("ID", "VARCHAR2(36 CHAR)", "Identificador UUID gerado pela aplicacao (chave primaria)", nulo=False),
        col("OWNER_ID", "VARCHAR2(36 CHAR)", "Usuario dono da transcricao analisada (APP_USERS.ID)"),
        col("TRANSCRICAO_ID", "VARCHAR2(36 CHAR)", "Transcricao analisada (TRANSCRICOES.ID)"),
        col("SENTIMENTO_GERAL", "VARCHAR2(20 CHAR)", "Sentimento geral da conversa: POSITIVO, NEGATIVO, NEUTRO ou MISTO",
            check="SENTIMENTO_GERAL IN ('POSITIVO','NEGATIVO','NEUTRO','MISTO')"),
        col("RESUMO", "CLOB", "Resumo textual da analise gerado pela LeanIA"),
        col("PERSONA_IDENTIFICADA", "VARCHAR2(500 CHAR)", "Persona/cargo do interlocutor (ex.: Gestor de TI (influenciador tecnico))"),
        col("BUDGET_IDENTIFICADO", "VARCHAR2(500 CHAR)", "Valor de budget mencionado na reuniao (ex.: R$ 25.000,00 por mes)"),
        col("SCORE_CONFIANCA_VENDEDOR", "FLOAT(53)", "Confianca do vendedor percebida na conversa (0 a 1)"),
        col("SCORE_SENTIMENTO", "FLOAT(53)", "Score numerico do sentimento (-1 negativo a +1 positivo)"),
    ] + METRICAS + [
        col("RESUMO_EXECUTIVO", "CLOB", "Resumo executivo para o diretor comercial"),
        col("DATA_ANALISE", "TIMESTAMP(6)", "Data/hora em que a analise foi gerada"),
        col("PROXIMA_ACAO", "VARCHAR2(500 CHAR)", "Proxima acao comercial sugerida pela LeanIA"),
        col("SENTIMENTO_DETALHE", "VARCHAR2(300 CHAR)", "Sentimento por area, ex.: Misto (Satisfeito com Backoffice / Frustrado com RH)"),
        col("INSIGHTS_JSON", "CLOB", "JSON: elogios, reclamacoes, duvidasJornada, proximosPassos, observacoes"),
        col("INDICADORES_JSON", "CLOB", "JSON: locutores, participacao vendedor/cliente, perguntas, escuta ativa, qualidade da transcricao"),
    ],
    "pk": {"nome": "PK_ANALISES", "colunas": ["ID"]},
    "uks": [],
    "fks": [
        {"nome": "FK_ANALISES_OWNER", "colunas": ["OWNER_ID"], "ref": "APP_USERS", "ref_colunas": ["ID"], "on_delete": None},
        {"nome": "FK_ANALISES_TRANSCRICAO", "colunas": ["TRANSCRICAO_ID"], "ref": "TRANSCRICOES", "ref_colunas": ["ID"], "on_delete": "CASCADE"},
    ],
    "checks": [],
    "indices": [
        {"nome": "IDX_ANALISES_OWNER", "colunas": ["OWNER_ID"]},
        {"nome": "IDX_ANALISES_TRANSCRICAO", "colunas": ["TRANSCRICAO_ID"]},
        {"nome": "IDX_ANALISES_DATA", "colunas": ["DATA_ANALISE"]},
    ],
    "grupo": "Analises",
})

TABELAS.append(_lista_filha(
    "ANALISE_PRODUTOS", "ANALISES", "ANALISE_ID", "FK_ANALISE_PRODUTOS", "PK_ANALISE_PRODUTOS",
    "Produtos identificados na reuniao: em uso (ATUAL), oportunidade ou concorrente",
    [
        col("NOME_PRODUTO", "VARCHAR2(300 CHAR)", "Nome do produto (TOTVS Protheus, TOTVS RM, TOTVS Fluig, SAP, Senior, Omie...)"),
        col("TIPO", "VARCHAR2(20 CHAR)", "ATUAL (cliente usa), OPORTUNIDADE (pode comprar) ou CONCORRENTE",
            check="TIPO IN ('ATUAL','OPORTUNIDADE','CONCORRENTE')"),
        col("CONTEXTO", "CLOB", "Trecho da conversa em que o produto foi citado"),
    ],
    "Analises"))

TABELAS.append(_lista_filha(
    "ANALISE_OPORTUNIDADES", "ANALISES", "ANALISE_ID", "FK_ANALISE_OPORTUNIDADES", "PK_ANALISE_OPORTUNIDADES",
    "Oportunidades de venda detectadas na reuniao (upsell, cross-sell, novo produto)",
    [
        col("TIPO", "VARCHAR2(20 CHAR)", "UPSELL, CROSS_SELL ou NOVO_PRODUTO", check="TIPO IN ('UPSELL','CROSS_SELL','NOVO_PRODUTO')"),
        col("DESCRICAO", "CLOB", "Descricao da oportunidade em linguagem de negocio"),
        col("PRODUTO_RELACIONADO", "VARCHAR2(300 CHAR)", "Produto/modulo TOTVS relacionado (ou A definir)"),
        col("SCORE", "FLOAT(53)", "Probabilidade estimada de fechamento (0 a 1)"),
        col("VALOR_ESTIMADO", "VARCHAR2(200 CHAR)", "Valor estimado mencionado (ex.: R$ 50.000,00) ou Nao estimado"),
    ],
    "Analises"))

TABELAS.append(_lista_filha(
    "ANALISE_RISCOS", "ANALISES", "ANALISE_ID", "FK_ANALISE_RISCOS", "PK_ANALISE_RISCOS",
    "Riscos detectados na reuniao (churn, concorrencia, insatisfacao, perda de confianca)",
    [
        col("TIPO", "VARCHAR2(20 CHAR)", "CHURN, CONCORRENCIA, INSATISFACAO ou PERDA_CONFIANCA",
            check="TIPO IN ('CHURN','CONCORRENCIA','INSATISFACAO','PERDA_CONFIANCA')"),
        col("DESCRICAO", "CLOB", "Descricao do risco com o trecho que o evidencia"),
        col("SEVERIDADE", "VARCHAR2(10 CHAR)", "Severidade: ALTO, MEDIO ou BAIXO", check="SEVERIDADE IN " + SEVERIDADES),
        col("CONCORRENTE_MENCIONADO", "VARCHAR2(300 CHAR)", "Concorrente citado quando TIPO = CONCORRENCIA (SAP, Senior, Omie, Bling...)"),
    ],
    "Analises"))

TABELAS.append(_lista_filha(
    "ANALISE_RISCOS_CONS", "ANALISES", "ANALISE_ID", "FK_ANALISE_RISCOS_CONS", "PK_ANALISE_RISCOS_CONS",
    "Riscos consolidados por categoria gerados pela analise (origem da copia em TRANSCRICAO_RISCOS_CONS)",
    [
        col("CATEGORIA", "VARCHAR2(200 CHAR)", "Categoria do risco consolidado"),
        col("SEVERIDADE", "VARCHAR2(10 CHAR)", "Severidade consolidada: ALTO, MEDIO ou BAIXO", check="SEVERIDADE IN " + SEVERIDADES),
        col("OCORRENCIAS", "NUMBER(10,0)", "Quantidade de ocorrencias do risco"),
        col("EVIDENCIAS", "CLOB", "Lista JSON de trechos que evidenciam o risco"),
        col("RECOMENDACAO", "CLOB", "Recomendacao de acao para mitigar o risco"),
    ],
    "Analises"))

TABELAS.append(_lista_filha(
    "ANALISE_OPORT_CONS", "ANALISES", "ANALISE_ID", "FK_ANALISE_OPORT_CONS", "PK_ANALISE_OPORT_CONS",
    "Oportunidades consolidadas por categoria geradas pela analise (origem da copia em TRANSCRICAO_OPORT_CONS)",
    [
        col("CATEGORIA", "VARCHAR2(200 CHAR)", "Categoria da oportunidade consolidada"),
        col("SCORE", "FLOAT(53)", "Probabilidade estimada (0 a 1)"),
        col("OCORRENCIAS", "NUMBER(10,0)", "Quantidade de sinais da oportunidade"),
        col("EVIDENCIAS", "CLOB", "Lista JSON de trechos que evidenciam a oportunidade"),
        col("RECOMENDACAO", "CLOB", "Recomendacao de acao comercial"),
    ],
    "Analises"))

TABELAS.append(_lista_filha(
    "ANALISE_ALERTAS_CONS", "ANALISES", "ANALISE_ID", "FK_ANALISE_ALERTAS_CONS", "PK_ANALISE_ALERTAS_CONS",
    "Alertas consolidados gerados pela analise (origem da copia em TRANSCRICAO_ALERTAS_CONS e dos ALERTAS do tipo CONSOLIDADO_*)",
    [
        col("TITULO", "VARCHAR2(500 CHAR)", "Titulo curto do alerta"),
        col("CATEGORIA", "VARCHAR2(200 CHAR)", "Categoria do alerta"),
        col("SEVERIDADE", "VARCHAR2(10 CHAR)", "Severidade: ALTO, MEDIO ou BAIXO", check="SEVERIDADE IN " + SEVERIDADES),
        col("OCORRENCIAS", "NUMBER(10,0)", "Quantidade de ocorrencias"),
        col("MOTIVO", "CLOB", "Motivo do alerta"),
        col("RECOMENDACAO", "CLOB", "Acao recomendada"),
    ],
    "Analises"))

TABELAS.append(_lista_filha(
    "ANALISE_ITENS_CRITICOS", "ANALISES", "ANALISE_ID", "FK_ANALISE_ITENS_CRITICOS", "PK_ANALISE_ITENS_CRITICOS",
    "Trechos criticos apontados pela analise (origem da copia em TRANSCRICAO_ITENS_CRITICOS)",
    [
        col("TIPO", "VARCHAR2(100 CHAR)", "Tipo do item: risco ou oportunidade"),
        col("CATEGORIA", "VARCHAR2(200 CHAR)", "Categoria do item"),
        col("TRECHO", "CLOB", "Trecho literal da transcricao"),
        col("SEVERIDADE", "VARCHAR2(10 CHAR)", "Severidade: ALTO, MEDIO ou BAIXO", check="SEVERIDADE IN " + SEVERIDADES),
    ],
    "Analises"))

# --- Alertas, feedback e historico ---------------------------------------------------------
TABELAS.append({
    "nome": "ALERTAS",
    "comentario": "Alertas acionaveis para o time comercial (churn, concorrente, oportunidade alta, consolidados) com ciclo NOVO > VISTO > RESOLVIDO",
    "colunas": [
        col("ID", "VARCHAR2(36 CHAR)", "Identificador UUID gerado pela aplicacao (chave primaria)", nulo=False),
        col("OWNER_ID", "VARCHAR2(36 CHAR)", "Usuario dono da transcricao que originou o alerta (APP_USERS.ID)"),
        col("ANALISE_ID", "VARCHAR2(36 CHAR)", "Analise que gerou o alerta (ANALISES.ID)"),
        col("TIPO", "VARCHAR2(100 CHAR)", "Tipo: RISCO_CHURN, RISCO_INSATISFACAO, RISCO_CONCORRENCIA, RISCO_PERDA_CONFIANCA, CONCORRENTE_DETECTADO, OPORTUNIDADE_ALTA, CHURN_IMINENTE, CONSOLIDADO_*"),
        col("TITULO", "VARCHAR2(500 CHAR)", "Titulo exibido no painel de alertas"),
        col("CATEGORIA", "VARCHAR2(200 CHAR)", "Categoria de negocio (Churn, Concorrencia, Sentimento negativo, OPORTUNIDADE_ALTA...)"),
        col("MENSAGEM", "CLOB", "Mensagem completa do alerta com o trecho que o justifica"),
        col("MOTIVO", "CLOB", "Motivo resumido (alertas consolidados)"),
        col("RECOMENDACAO", "CLOB", "Acao recomendada (alertas consolidados)"),
        col("OCORRENCIAS", "NUMBER(10,0)", "Quantidade de ocorrencias que originaram o alerta"),
        col("PRIORIDADE", "VARCHAR2(10 CHAR)", "Prioridade: ALTO, MEDIO ou BAIXO", check="PRIORIDADE IN " + SEVERIDADES),
        col("STATUS", "VARCHAR2(20 CHAR)", "Ciclo de tratamento: NOVO, VISTO ou RESOLVIDO", check="STATUS IN ('NOVO','VISTO','RESOLVIDO')"),
        col("DATA_CRIACAO", "TIMESTAMP(6)", "Data/hora de geracao do alerta"),
    ],
    "pk": {"nome": "PK_ALERTAS", "colunas": ["ID"]},
    "uks": [],
    "fks": [
        {"nome": "FK_ALERTAS_OWNER", "colunas": ["OWNER_ID"], "ref": "APP_USERS", "ref_colunas": ["ID"], "on_delete": None},
        {"nome": "FK_ALERTAS_ANALISE", "colunas": ["ANALISE_ID"], "ref": "ANALISES", "ref_colunas": ["ID"], "on_delete": "CASCADE"},
    ],
    "checks": [],
    "indices": [
        {"nome": "IDX_ALERTAS_STATUS_PRIO_DATA", "colunas": ["STATUS", "PRIORIDADE", "DATA_CRIACAO"]},
        {"nome": "IDX_ALERTAS_OWNER_STATUS", "colunas": ["OWNER_ID", "STATUS", "PRIORIDADE", "DATA_CRIACAO"]},
        {"nome": "IDX_ALERTAS_ANALISE", "colunas": ["ANALISE_ID"]},
    ],
    "grupo": "Alertas e feedback",
})

TABELAS.append({
    "nome": "ALERTA_STATUS_HIST",
    "comentario": "Historico das mudancas de STATUS do alerta (NOVO > VISTO > RESOLVIDO). "
                  "Alimentado automaticamente pelo trigger TRG_ALERTAS_STATUS_HIST",
    "colunas": [
        col("ID", "NUMBER(19,0)", "Identificador numerico gerado por IDENTITY (chave primaria)", nulo=False, identity=True),
        col("ALERTA_ID", "VARCHAR2(36 CHAR)", "Alerta que mudou de status (ALERTAS.ID)", nulo=False),
        col("STATUS_ANTERIOR", "VARCHAR2(20 CHAR)", "Status antes da mudanca; NULL no registro de criacao"),
        col("STATUS_NOVO", "VARCHAR2(20 CHAR)", "Status depois da mudanca", nulo=False, check="STATUS_NOVO IN ('NOVO','VISTO','RESOLVIDO')"),
        col("ALTERADO_EM", "TIMESTAMP(6)", "Data/hora da mudanca", nulo=False, default="SYSTIMESTAMP"),
        col("ALTERADO_POR", "VARCHAR2(100 CHAR)", "Usuario Oracle da sessao que fez a mudanca", default="USER"),
        col("OBSERVACAO", "VARCHAR2(500 CHAR)", "Observacao livre (quem tratou, resultado do contato, etc.)"),
    ],
    "pk": {"nome": "PK_ALERTA_STATUS_HIST", "colunas": ["ID"]},
    "uks": [],
    "fks": [{"nome": "FK_ALERTA_STATUS_HIST", "colunas": ["ALERTA_ID"], "ref": "ALERTAS", "ref_colunas": ["ID"], "on_delete": "CASCADE"}],
    "checks": [],
    "indices": [{"nome": "IDX_ALERTA_STATUS_HIST", "colunas": ["ALERTA_ID", "ALTERADO_EM"]}],
    "grupo": "Alertas e feedback",
})

TABELAS.append({
    "nome": "ANALISE_FEEDBACKS",
    "comentario": "Avaliacao (util/ajuste) dada pelo usuario a uma analise. Alimenta o contexto adaptativo da LeanIA",
    "colunas": [
        col("ID", "VARCHAR2(36 CHAR)", "Identificador UUID gerado pela aplicacao (chave primaria)", nulo=False),
        col("OWNER_ID", "VARCHAR2(36 CHAR)", "Usuario que avaliou (APP_USERS.ID)"),
        col("TRANSCRICAO_ID", "VARCHAR2(36 CHAR)", "Transcricao avaliada (TRANSCRICOES.ID)"),
        col("ANALISE_ID", "VARCHAR2(36 CHAR)", "Analise avaliada (ANALISES.ID); fica NULL se a analise for refeita"),
        col("UTIL", "NUMBER(1,0)", "1 = analise considerada util; 0 = nao util", nulo=False, check="UTIL IN (0,1)"),
        col("AJUSTE", "CLOB", "Texto livre com a correcao/ajuste sugerido pelo usuario"),
        col("CREATED_AT", "TIMESTAMP(6)", "Data/hora do feedback"),
    ],
    "pk": {"nome": "PK_ANALISE_FEEDBACKS", "colunas": ["ID"]},
    "uks": [],
    "fks": [
        {"nome": "FK_FEEDBACKS_OWNER", "colunas": ["OWNER_ID"], "ref": "APP_USERS", "ref_colunas": ["ID"], "on_delete": None},
        {"nome": "FK_FEEDBACKS_TRANSCRICAO", "colunas": ["TRANSCRICAO_ID"], "ref": "TRANSCRICOES", "ref_colunas": ["ID"], "on_delete": "CASCADE"},
        {"nome": "FK_FEEDBACKS_ANALISE", "colunas": ["ANALISE_ID"], "ref": "ANALISES", "ref_colunas": ["ID"], "on_delete": "SET NULL"},
    ],
    "checks": [],
    "indices": [
        {"nome": "IDX_FEEDBACK_OWNER", "colunas": ["OWNER_ID"]},
        {"nome": "IDX_FEEDBACK_TRANSCRICAO", "colunas": ["TRANSCRICAO_ID"]},
        {"nome": "IDX_FEEDBACK_ANALISE", "colunas": ["ANALISE_ID"]},
        {"nome": "IDX_FEEDBACK_CREATED", "colunas": ["CREATED_AT"]},
    ],
    "grupo": "Alertas e feedback",
})

# Triggers (PL/SQL) ------------------------------------------------------------------------
TRIGGERS = [
    {
        "nome": "TRG_TRANSCRICOES_STATUS_HIST",
        "comentario": "Registra em TRANSCRICAO_STATUS_HIST toda mudanca de STATUS da transcricao",
        "corpo": """CREATE OR REPLACE TRIGGER TRG_TRANSCRICOES_STATUS_HIST
    AFTER UPDATE OF STATUS ON TRANSCRICOES
    FOR EACH ROW
    WHEN (NVL(OLD.STATUS, '-') <> NVL(NEW.STATUS, '-'))
BEGIN
    INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO)
    VALUES (:NEW.ID, :OLD.STATUS, :NEW.STATUS);
END;
/""",
    },
    {
        "nome": "TRG_ALERTAS_STATUS_HIST",
        "comentario": "Registra em ALERTA_STATUS_HIST toda mudanca de STATUS do alerta",
        "corpo": """CREATE OR REPLACE TRIGGER TRG_ALERTAS_STATUS_HIST
    AFTER UPDATE OF STATUS ON ALERTAS
    FOR EACH ROW
    WHEN (NVL(OLD.STATUS, '-') <> NVL(NEW.STATUS, '-'))
BEGIN
    INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO)
    VALUES (:NEW.ID, :OLD.STATUS, :NEW.STATUS);
END;
/""",
    },
]


def tabela(nome):
    for t in TABELAS:
        if t["nome"] == nome:
            return t
    raise KeyError(nome)


def coluna(t, nome):
    for c in t["colunas"]:
        if c["nome"] == nome:
            return c
    raise KeyError(nome)


def fks_de(t):
    return [fk for fk in t["fks"]]


def todas_fks():
    for t in TABELAS:
        for fk in t["fks"]:
            yield t, fk


def cardinalidade(t, fk):
    """1:1 quando a coluna FK e unica na tabela filha; senao 1:N."""
    for uk in t["uks"]:
        if uk["colunas"] == fk["colunas"]:
            return "1:1"
    return "1:N"


def fk_identificadora(t, fk):
    return all(c in t["pk"]["colunas"] for c in fk["colunas"])


def fk_obrigatoria(t, fk):
    return all(not coluna(t, c)["nulo"] for c in fk["colunas"])
