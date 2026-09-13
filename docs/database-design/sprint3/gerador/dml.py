# -*- coding: utf-8 -*-
"""Gera o script DML: carga inicial (INSERT), evolucao dos processos (UPDATE) e exclusoes (DELETE)."""
from modelo import EQUIPE, SOLUCAO, INTEGRANTES, TABELAS

BCRYPT_DEMO = "$2b$10$KaMclFmqsp2mIPTj8pv8su6Dp4uWI2q9/KlTqW963mOtkD4CUlK/K"  # Senha@123

# Identificadores fixos (UUID v4 validos) para o script ser legivel e reexecutavel apos a limpeza
U = {i: "aaaaaaaa-0000-4000-8000-%012d" % i for i in range(1, 6)}      # usuarios
RT = {i: "bbbbbbbb-0000-4000-8000-%012d" % i for i in range(1, 7)}     # refresh tokens
T = {i: "cccccccc-0000-4000-8000-%012d" % i for i in range(1, 10)}     # transcricoes
A = {i: "dddddddd-0000-4000-8000-%012d" % i for i in range(1, 8)}      # analises
AL = {i: "eeeeeeee-0000-4000-8000-%012d" % i for i in range(1, 9)}     # alertas
FB = {i: "ffffffff-0000-4000-8000-%012d" % i for i in range(1, 7)}     # feedbacks
LOTE = {1: "99999999-0000-4000-8000-000000000001", 2: "99999999-0000-4000-8000-000000000002"}


class Raw(str):
    """Expressao SQL inserida sem aspas (TO_TIMESTAMP, NULL, SYSTIMESTAMP...)."""


def ts(valor):
    return Raw("TO_TIMESTAMP('%s', 'YYYY-MM-DD HH24:MI:SS')" % valor)


def sql_val(v):
    if v is None:
        return "NULL"
    if isinstance(v, Raw):
        return str(v)
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, float)):
        return repr(v)
    return "'" + str(v).replace("'", "''") + "'"


def ins(tabela, **valores):
    cols = ", ".join(valores.keys())
    vals = ", ".join(sql_val(v) for v in valores.values())
    return "INSERT INTO %s (%s)\nVALUES (%s);\n" % (tabela, cols, vals)


def bloco(titulo):
    return "\n-- -------------------------------------------------------------------------------------\n-- %s\n-- -------------------------------------------------------------------------------------\n" % titulo


def metricas(total, analisadas, neg, neu, pos, riscos, oport, churn, mencoes, alertas, chunks):
    return dict(MET_TOTAL_PARTES=total, MET_PARTES_ANALISADAS=analisadas, MET_PARTES_NEGATIVAS=neg,
                MET_PARTES_NEUTRAS=neu, MET_PARTES_POSITIVAS=pos, MET_RISCOS_DETECTADOS=riscos,
                MET_OPORTUNIDADES_DETECTADAS=oport, MET_CHURNS_POTENCIAIS=churn, MET_MENCOES_CRITICAS=mencoes,
                MET_ALERTAS_CONSOLIDADOS=alertas, MET_CHUNKS_PROCESSADOS=chunks)


# ------------------------------------------------------------------------------------------
# Dados ficticios porem realistas (clientes, reunioes, analises e alertas)
# ------------------------------------------------------------------------------------------
USUARIOS = [
    dict(ID=U[1], EMAIL="diretoria.comercial@company404.com.br", USER_NAME="Helena Prado (Diretora Comercial)", USER_ROLE="ADMIN", IS_ACTIVE=1, CREATED_AT=ts("2026-07-01 08:00:00")),
    dict(ID=U[2], EMAIL="rafael.moura@company404.com.br", USER_NAME="Rafael Moura (Account Executive)", USER_ROLE="USER", IS_ACTIVE=1, CREATED_AT=ts("2026-07-01 08:05:00")),
    dict(ID=U[3], EMAIL="juliana.castro@company404.com.br", USER_NAME="Juliana Castro (Gerente de Vendas)", USER_ROLE="USER", IS_ACTIVE=1, CREATED_AT=ts("2026-07-01 08:10:00")),
    dict(ID=U[4], EMAIL="marcos.lima@company404.com.br", USER_NAME="Marcos Lima (CS Manager)", USER_ROLE="USER", IS_ACTIVE=1, CREATED_AT=ts("2026-07-02 09:00:00")),
    dict(ID=U[5], EMAIL="patricia.nunes@company404.com.br", USER_NAME="Patricia Nunes (Analista de BI)", USER_ROLE="USER", IS_ACTIVE=0, CREATED_AT=ts("2026-07-02 09:30:00")),
]

TOKENS = [
    dict(ID=RT[1], USER_ID=U[1], TOKEN_HASH="3f1c9b6a2e7d4c5b8a9f0e1d2c3b4a5968778695a4b3c2d1e0f9a8b7c6d5e4f3", CREATED_AT=ts("2026-09-01 08:00:00"), EXPIRES_AT=ts("2026-09-08 08:00:00"), REVOKED_AT=None),
    dict(ID=RT[2], USER_ID=U[2], TOKEN_HASH="8b2d4f6a1c3e5a7b9d0f2e4c6a8b0d1f3e5a7c9b1d3f5a7c9e1b3d5f7a9c1e3b", CREATED_AT=ts("2026-09-01 09:15:00"), EXPIRES_AT=ts("2026-09-08 09:15:00"), REVOKED_AT=None),
    dict(ID=RT[3], USER_ID=U[2], TOKEN_HASH="c4e6a8b0d2f4a6c8e0b2d4f6a8c0e2b4d6f8a0c2e4b6d8f0a2c4e6b8d0f2a4c6", CREATED_AT=ts("2026-08-20 10:00:00"), EXPIRES_AT=ts("2026-08-27 10:00:00"), REVOKED_AT=ts("2026-08-21 18:40:00")),
    dict(ID=RT[4], USER_ID=U[3], TOKEN_HASH="d5f7a9b1c3e5a7b9d1f3a5c7e9b1d3f5a7c9e1b3d5f7a9c1e3b5d7f9a1c3e5b7", CREATED_AT=ts("2026-09-02 07:50:00"), EXPIRES_AT=ts("2026-09-09 07:50:00"), REVOKED_AT=None),
    dict(ID=RT[5], USER_ID=U[4], TOKEN_HASH="e6a8b0c2d4f6a8b0c2e4d6f8a0b2c4e6d8f0a2b4c6e8d0f2a4b6c8e0d2f4a6b8", CREATED_AT=ts("2026-08-10 14:00:00"), EXPIRES_AT=ts("2026-08-17 14:00:00"), REVOKED_AT=None),
    dict(ID=RT[6], USER_ID=U[3], TOKEN_HASH="f7b9c1d3e5a7b9c1d3f5a7b9c1e3d5f7a9b1c3e5d7f9a1b3c5e7d9f1a3b5c7d9", CREATED_AT=ts("2026-09-05 11:20:00"), EXPIRES_AT=ts("2026-09-12 11:20:00"), REVOKED_AT=None),
]


def transcricao(i, **kw):
    base = dict(ID=T[i], MISSING_MEETING_ID=0, STATUS="PROCESSADA")
    base.update(kw)
    return base


TRANSCRICOES = [
    transcricao(1, OWNER_ID=U[2], CLIENTE="Metalurgica Andrade", VENDEDOR="Rafael Moura",
                PARTICIPANTES="LOCUTOR 1, LOCUTOR 2, LOCUTOR 3", DATA_REUNIAO=ts("2026-08-04 09:30:00"), DURACAO_MINUTOS=52,
                TEXTO_ORIGINAL="[LOCUTOR 1]: Bom dia, pessoal. Hoje queria revisar o uso do Protheus no financeiro. [LOCUTOR 2]: O modulo financeiro atende bem, mas a aprovacao de compras ainda e por e-mail. [LOCUTOR 1]: O Fluig resolve exatamente esse fluxo de aprovacao. [LOCUTOR 2]: Interessante, precisamos de uma proposta ate o fim do mes. [LOCUTOR 3]: O orcamento previsto para automacao e de uns R$ 60 mil.",
                ORIGEM_TRANSCRICAO="json", MEETING_ID="2026080401", MEETING_DATE="2026-08-04 09:30:00", FORMATO="VIDEO", MEETING_STATUS="COMPLETED",
                MEETING_DURATION="00:52:10", CODT="TFAND1", FLG_EXTERNO=1, CREATED_AT_SOURCE="2026-08-04 12:05:44",
                TRANSCRIPTION_TEXT="[LOCUTOR 1]: Bom dia, pessoal. Hoje queria revisar o uso do Protheus no financeiro...",
                SOURCE_FILE_NAME="ANON_transcricao_agosto.json", IMPORT_BATCH_ID=LOTE[1], IMPORTED_AT=ts("2026-08-05 08:00:00"),
                UF="RS", CNAE="2511000", NOME_UNIDADE="TOTVS RIO GRANDE DO SUL", NOME_SEGMENTO="MANUFATURA",
                FAIXA_FATURAMENTO="04.SMB I - De R$ 20.000.001 a R$ 60.000.000", TP_RECURSO="customer", NOTA_NPS=8, DT_ULTIMA_PESQUISA="2026-06",
                DATA_CRIACAO=ts("2026-08-05 08:00:00"), DATA_PROCESSAMENTO=ts("2026-08-05 08:03:12"),
                **metricas(3, 3, 1, 1, 1, 1, 1, 0, 1, 1, 1),
                RESUMO_EXECUTIVO="Cliente satisfeito com o Protheus e com forte interesse no Fluig para aprovacoes. Budget de R$ 60 mil. Enviar proposta ate o fim do mes.",
                ANALISE_ID=A[1]),
    transcricao(2, OWNER_ID=U[2], CLIENTE="Clinica Vida Plena", VENDEDOR="Rafael Moura",
                PARTICIPANTES="LOCUTOR 1, LOCUTOR 2", DATA_REUNIAO=ts("2026-08-06 15:00:00"), DURACAO_MINUTOS=38,
                TEXTO_ORIGINAL="[LOCUTOR 2]: Estamos ha tres semanas com o chamado do RM aberto e ninguem retorna. [LOCUTOR 1]: Entendo a frustracao, vou escalar hoje. [LOCUTOR 2]: A Senior ja nos apresentou uma proposta e a diretoria esta pensando em trocar. Se nao resolver ate o fim do mes, vamos cancelar o contrato.",
                ORIGEM_TRANSCRICAO="json", MEETING_ID="2026080602", MEETING_DATE="2026-08-06 15:00:00", FORMATO="VIDEO", MEETING_STATUS="COMPLETED",
                MEETING_DURATION="00:38:44", CODT="TFVID2", FLG_EXTERNO=1, CREATED_AT_SOURCE="2026-08-06 17:20:10",
                TRANSCRIPTION_TEXT="[LOCUTOR 2]: Estamos ha tres semanas com o chamado do RM aberto e ninguem retorna...",
                SOURCE_FILE_NAME="ANON_transcricao_agosto.json", IMPORT_BATCH_ID=LOTE[1], IMPORTED_AT=ts("2026-08-05 08:00:00"),
                UF="SP", CNAE="8630501", NOME_UNIDADE="TOTVS SAUDE", NOME_SEGMENTO="SAUDE",
                FAIXA_FATURAMENTO="02.MICRO II - De R$ 500.001 a R$ 10.000.000", TP_RECURSO="customer", NOTA_NPS=3, DT_ULTIMA_PESQUISA="2026-07",
                DATA_CRIACAO=ts("2026-08-05 08:00:01"), DATA_PROCESSAMENTO=ts("2026-08-05 08:04:40"),
                **metricas(3, 3, 3, 0, 0, 3, 0, 2, 2, 2, 1),
                RESUMO_EXECUTIVO="Cliente insatisfeito com o suporte do RM, com proposta da Senior na mesa e ameaca de cancelamento. Acionar plano de retencao imediato.",
                ANALISE_ID=A[2]),
    transcricao(3, OWNER_ID=U[3], CLIENTE="Distribuidora Horizonte", VENDEDOR="Juliana Castro",
                PARTICIPANTES="Juliana Castro, Carlos Menezes (Diretor)", DATA_REUNIAO=ts("2026-08-11 10:00:00"), DURACAO_MINUTOS=45,
                TEXTO_ORIGINAL="Carlos: Hoje usamos o Bling, mas ele nao acompanha nosso crescimento no estoque. Juliana: O Protheus tem o modulo de distribuicao e logistica integrado ao fiscal. Carlos: Gostei. Temos verba aprovada de R$ 120 mil para este ano e queremos implantar antes do quarto trimestre.",
                ORIGEM_TRANSCRICAO="csv", MEETING_ID="CSV-000731", MEETING_DATE="11/08/2026", FORMATO="LINK", MEETING_STATUS="COMPLETED",
                MEETING_DURATION="00:45:00", CODT="TFHOR3", FLG_EXTERNO=1, CREATED_AT_SOURCE="2026-08-11 12:00:00",
                TRANSCRIPTION_TEXT="Carlos: Hoje usamos o Bling, mas ele nao acompanha nosso crescimento no estoque...",
                SOURCE_FILE_NAME="transcricoes_mock_totvs.csv", IMPORT_BATCH_ID=LOTE[2], IMPORTED_AT=ts("2026-08-12 09:10:00"),
                UF="MG", CNAE="4639701", NOME_UNIDADE="TOTVS MINAS GERAIS", NOME_SEGMENTO="DISTRIBUICAO",
                FAIXA_FATURAMENTO="03.MICRO III - De R$ 10.000.001 a R$ 20.000.000", TP_RECURSO="lead", NOTA_NPS=None, DT_ULTIMA_PESQUISA=None,
                DATA_CRIACAO=ts("2026-08-12 09:10:00"), DATA_PROCESSAMENTO=ts("2026-08-12 09:12:05"),
                **metricas(3, 3, 0, 1, 2, 0, 2, 0, 0, 1, 1),
                RESUMO_EXECUTIVO="Lead com verba aprovada de R$ 120 mil e dor clara em estoque. Alta probabilidade de novo produto (Protheus).",
                ANALISE_ID=A[3]),
    transcricao(4, OWNER_ID=U[3], CLIENTE="Colegio Nova Geracao", VENDEDOR="Juliana Castro",
                PARTICIPANTES="Juliana Castro, Beatriz Rocha (Coordenadora Administrativa)", DATA_REUNIAO=ts("2026-08-14 14:00:00"), DURACAO_MINUTOS=30,
                TEXTO_ORIGINAL="Beatriz: O RM Educacional atende a secretaria, mas o portal do aluno e lento nas matriculas. Juliana: Podemos revisar a infraestrutura e apresentar o modulo de cobranca automatica. Beatriz: Nosso limite e de R$ 8 mil por mes. Vamos avaliar com calma.",
                ORIGEM_TRANSCRICAO="manual", MEETING_ID=None, MEETING_DATE=None, FORMATO=None, MEETING_STATUS=None,
                MEETING_DURATION=None, CODT=None, FLG_EXTERNO=1, CREATED_AT_SOURCE=None,
                TRANSCRIPTION_TEXT=None, SOURCE_FILE_NAME=None, IMPORT_BATCH_ID=None, IMPORTED_AT=None,
                UF="PR", CNAE="8513900", NOME_UNIDADE="TOTVS PARANA", NOME_SEGMENTO="EDUCACIONAL",
                FAIXA_FATURAMENTO="02.MICRO II - De R$ 500.001 a R$ 10.000.000", TP_RECURSO="customer", NOTA_NPS=7, DT_ULTIMA_PESQUISA="2026-05",
                DATA_CRIACAO=ts("2026-08-14 16:30:00"), DATA_PROCESSAMENTO=ts("2026-08-14 16:31:48"),
                **metricas(3, 3, 1, 2, 0, 1, 1, 0, 0, 0, 1),
                RESUMO_EXECUTIVO="Cliente neutro; reclamacao pontual de lentidao no portal. Oportunidade de upsell (cobranca automatica) com budget de R$ 8 mil/mes.",
                ANALISE_ID=A[4]),
    transcricao(5, OWNER_ID=U[4], CLIENTE="Supermercados Bom Preco", VENDEDOR="Marcos Lima",
                PARTICIPANTES="LOCUTOR 1, LOCUTOR 2, LOCUTOR 3, LOCUTOR 4", DATA_REUNIAO=ts("2026-08-19 11:00:00"), DURACAO_MINUTOS=64,
                TEXTO_ORIGINAL="[LOCUTOR 3]: O PDV travou duas vezes na Black Friday passada e ninguem nos avisou da correcao. [LOCUTOR 1]: A correcao foi publicada, mas falhamos na comunicacao. [LOCUTOR 3]: Sinceramente, perdemos a confianca. A Oracle NetSuite esta nos procurando com uma proposta agressiva. [LOCUTOR 4]: Precisamos de garantias antes de renovar.",
                ORIGEM_TRANSCRICAO="json", MEETING_ID="2026081905", MEETING_DATE="2026-08-19 11:00:00", FORMATO="VIDEO", MEETING_STATUS="COMPLETED",
                MEETING_DURATION="01:04:38", CODT="TFBOM5", FLG_EXTERNO=1, CREATED_AT_SOURCE="2026-08-19 13:30:00",
                TRANSCRIPTION_TEXT="[LOCUTOR 3]: O PDV travou duas vezes na Black Friday passada...",
                SOURCE_FILE_NAME="ANON_transcricao_agosto.json", IMPORT_BATCH_ID=LOTE[1], IMPORTED_AT=ts("2026-08-05 08:00:00"),
                UF="BA", CNAE="4711302", NOME_UNIDADE="TOTVS NORDESTE", NOME_SEGMENTO="VAREJO",
                FAIXA_FATURAMENTO="06.SMB III - De R$ 120.000.001 a R$ 240.000.000", TP_RECURSO="customer", NOTA_NPS=4, DT_ULTIMA_PESQUISA="2026-07",
                DATA_CRIACAO=ts("2026-08-20 08:00:00"), DATA_PROCESSAMENTO=ts("2026-08-20 08:05:22"),
                **metricas(4, 4, 3, 1, 0, 3, 1, 1, 2, 2, 2),
                RESUMO_EXECUTIVO="Cliente com perda de confianca apos falhas no PDV e concorrente (Oracle NetSuite) em negociacao. Renovacao em risco.",
                ANALISE_ID=A[5]),
    transcricao(6, OWNER_ID=U[2], CLIENTE="Transportadora Rota Sul", VENDEDOR="Rafael Moura",
                PARTICIPANTES="Rafael Moura, Fernanda Dias (CFO)", DATA_REUNIAO=ts("2026-08-25 16:00:00"), DURACAO_MINUTOS=41,
                TEXTO_ORIGINAL="Fernanda: O Datasul esta redondo na operacao, o time adora. Nosso gargalo hoje e capital de giro para antecipar recebiveis. Rafael: O TOTVS Techfin faz antecipacao integrada ao ERP. Fernanda: Otimo, vamos avancar. Pode mandar a proposta.",
                ORIGEM_TRANSCRICAO="parakeet", MEETING_ID="AUD-2026-0825", MEETING_DATE=None, FORMATO=None, MEETING_STATUS=None,
                MEETING_DURATION=None, CODT=None, FLG_EXTERNO=1, CREATED_AT_SOURCE=None,
                TRANSCRIPTION_TEXT="fernanda o datasul esta redondo na operacao o time adora nosso gargalo hoje e capital de giro...",
                SOURCE_FILE_NAME="reuniao_rota_sul.mp3", IMPORT_BATCH_ID=None, IMPORTED_AT=None,
                AUDIO_FILE_NAME="reuniao_rota_sul.mp3", TRANSCRIPTION_MODEL="parakeet", TRANSCRIPTION_LANGUAGE="pt",
                AUDIO_DURATION_SECONDS=2461.8, TRANSCRIPTION_PROCESSING_MS=48310,
                UF="SC", CNAE="4930202", NOME_UNIDADE="TOTVS SANTA CATARINA", NOME_SEGMENTO="LOGISTICA",
                FAIXA_FATURAMENTO="05.SMB II - De R$ 60.000.001 a R$ 120.000.000", TP_RECURSO="customer", NOTA_NPS=9, DT_ULTIMA_PESQUISA="2026-06",
                DATA_CRIACAO=ts("2026-08-25 17:10:00"), DATA_PROCESSAMENTO=ts("2026-08-25 17:12:30"),
                **metricas(3, 3, 0, 0, 3, 0, 1, 0, 0, 1, 1),
                RESUMO_EXECUTIVO="Cliente promotor; cross-sell de TOTVS Techfin (antecipacao de recebiveis) com decisao favoravel da CFO. Enviar proposta.",
                ANALISE_ID=A[6]),
    # reunioes que evoluem na secao de UPDATE
    transcricao(7, OWNER_ID=U[3], CLIENTE="Construtora Alvorada", VENDEDOR="Juliana Castro", STATUS="PENDENTE",
                PARTICIPANTES="Juliana Castro, Paulo Siqueira (Gerente de Obras)", DATA_REUNIAO=ts("2026-09-08 09:00:00"), DURACAO_MINUTOS=35,
                TEXTO_ORIGINAL="Paulo: Controlamos as obras em planilha e perdemos prazo de medicao. Juliana: O Protheus Construcao e Projetos integra medicao, compras e financeiro. Paulo: Queremos ver uma demonstracao ainda este mes.",
                ORIGEM_TRANSCRICAO="manual", MEETING_ID=None, MISSING_MEETING_ID=1, FLG_EXTERNO=1,
                UF="GO", CNAE="4120400", NOME_UNIDADE="TOTVS CENTRO-OESTE", NOME_SEGMENTO="CONSTRUCAO E PROJETOS",
                FAIXA_FATURAMENTO="04.SMB I - De R$ 20.000.001 a R$ 60.000.000", TP_RECURSO="lead",
                DATA_CRIACAO=ts("2026-09-08 11:00:00")),
    transcricao(8, OWNER_ID=U[4], CLIENTE="Agro Serra Verde", VENDEDOR="Marcos Lima", STATUS="PROCESSANDO",
                PARTICIPANTES="LOCUTOR 1, LOCUTOR 2", DATA_REUNIAO=ts("2026-09-09 08:30:00"), DURACAO_MINUTOS=27,
                TEXTO_ORIGINAL="[LOCUTOR 1]: A safra foi boa e queremos integrar a balanca ao ERP. [LOCUTOR 2]: O TOTVS Agro faz essa integracao com o financeiro.",
                ORIGEM_TRANSCRICAO="json", MEETING_ID="2026090908", MEETING_DATE="09/09/26 08:30", FORMATO="VIDEO", MEETING_STATUS="COMPLETED",
                MEETING_DURATION="00:27:05", CODT="TFAGR8", FLG_EXTERNO=1,
                SOURCE_FILE_NAME="ANON_transcricao_setembro.json", IMPORT_BATCH_ID=LOTE[2], IMPORTED_AT=ts("2026-09-10 07:45:00"),
                UF="MS", CNAE="0111301", NOME_UNIDADE="TOTVS CENTRO-OESTE", NOME_SEGMENTO="AGROINDUSTRIA",
                FAIXA_FATURAMENTO="07.UPM I - De R$ 240.000.001 a R$ 480.000.000", TP_RECURSO="customer", NOTA_NPS=8, DT_ULTIMA_PESQUISA="2026-08",
                DATA_CRIACAO=ts("2026-09-10 07:45:00")),
    transcricao(9, OWNER_ID=U[2], CLIENTE="Hospital Sao Lucas", VENDEDOR="Rafael Moura", STATUS="ERRO",
                PARTICIPANTES=None, DATA_REUNIAO=None, DURACAO_MINUTOS=None,
                TEXTO_ORIGINAL="(arquivo de transcricao vazio)",
                ORIGEM_TRANSCRICAO="json", MEETING_ID="2026091009", MEETING_DATE=None, FORMATO="VIDEO", MEETING_STATUS="COMPLETED",
                CODT="TFSLU9", FLG_EXTERNO=1, MISSING_MEETING_ID=0,
                SOURCE_FILE_NAME="ANON_transcricao_setembro.json", IMPORT_BATCH_ID=LOTE[2], IMPORTED_AT=ts("2026-09-10 07:45:00"),
                UF="RJ", CNAE="8610101", NOME_UNIDADE="TOTVS SAUDE", NOME_SEGMENTO="SAUDE",
                FAIXA_FATURAMENTO="06.SMB III - De R$ 120.000.001 a R$ 240.000.000", TP_RECURSO="customer",
                DATA_CRIACAO=ts("2026-09-10 07:45:01")),
]

WARNINGS = [
    (T[3], 0, "INVALID_NOTA_NPS"),
    (T[3], 1, "INVALID_DT_MEETING"),
    (T[6], 0, "Audio sem metadados de reuniao (data e participantes informados manualmente)"),
    (T[7], 0, "MISSING_ID_MEETING"),
    (T[8], 0, "INVALID_DT_MEETING"),
    (T[8], 1, "INVALID_DT_CRIACAO"),
    (T[9], 0, "INVALID_DT_MEETING"),
    (T[9], 1, "INVALID_DT_CRIACAO"),
    (T[9], 2, "Texto da transcricao vazio: analise nao executada"),
]

# (dono, posicao, categoria, severidade, ocorrencias, evidencias, recomendacao) - mesma lista na transcricao e na analise
RISCOS_CONS = [
    (1, 0, "Insatisfacao", "MEDIO", 1, '["a aprovacao de compras ainda e por e-mail"]', "Mostrar o ganho de tempo com o fluxo de aprovacao do Fluig."),
    (2, 0, "Churn", "ALTO", 2, '["vamos cancelar o contrato", "a diretoria esta pensando em trocar"]', "Acionar plano de retencao imediato com lideranca comercial."),
    (2, 1, "Concorrencia", "MEDIO", 1, '["A Senior ja nos apresentou uma proposta"]', "Levantar diferenciais frente a Senior e reforcar o roadmap do RM."),
    (4, 0, "Insatisfacao", "BAIXO", 1, '["o portal do aluno e lento nas matriculas"]', "Agendar diagnostico de infraestrutura antes do periodo de matriculas."),
    (5, 0, "Perda de confianca", "ALTO", 1, '["Sinceramente, perdemos a confianca"]', "Envolver a diretoria de CS e apresentar plano de comunicacao de incidentes."),
    (5, 1, "Concorrencia", "MEDIO", 2, '["A Oracle NetSuite esta nos procurando", "proposta agressiva"]', "Preparar contraproposta de renovacao com garantias de SLA."),
]
OPORT_CONS = [
    (1, 0, "Upsell", 0.78, 2, '["O Fluig resolve exatamente esse fluxo", "precisamos de uma proposta ate o fim do mes"]', "Enviar proposta do Fluig com foco em aprovacao de compras."),
    (2, 0, "Melhoria no atendimento", 0.60, 1, '["vou escalar hoje"]', "Resolver o chamado e registrar retorno formal ao cliente."),
    (3, 0, "Novo produto", 0.81, 2, '["Temos verba aprovada de R$ 120 mil", "implantar antes do quarto trimestre"]', "Apresentar Protheus Distribuicao e Logistica com cronograma de implantacao."),
    (4, 0, "Upsell", 0.55, 1, '["apresentar o modulo de cobranca automatica"]', "Enviar simulacao de economia com cobranca automatica."),
    (5, 0, "Upsell", 0.42, 1, '["Precisamos de garantias antes de renovar"]', "Incluir monitoramento proativo do PDV na renovacao."),
    (6, 0, "Cross-sell", 0.74, 2, '["capital de giro para antecipar recebiveis", "Pode mandar a proposta"]', "Enviar proposta do TOTVS Techfin de antecipacao de recebiveis."),
]
ALERTAS_CONS = [
    (1, 0, "Oportunidade alta", "Upsell", "MEDIO", 2, "Cliente pediu proposta do Fluig com prazo definido.", "Enviar proposta ate o fim do mes."),
    (2, 0, "Risco de churn", "Churn", "ALTO", 2, "Cliente verbalizou cancelamento e troca de fornecedor.", "Acionar plano de retencao imediato com lideranca comercial."),
    (2, 1, "Concorrente citado", "Concorrencia", "MEDIO", 1, "Senior apresentou proposta ao cliente.", "Reforcar diferenciais e roadmap do RM."),
    (5, 0, "Perda de confianca", "Perda de confianca", "ALTO", 1, "Cliente declarou ter perdido a confianca apos incidentes.", "Plano de comunicacao de incidentes e reuniao com diretoria de CS."),
    (5, 1, "Concorrente citado", "Concorrencia", "MEDIO", 2, "Oracle NetSuite em negociacao com o cliente.", "Contraproposta de renovacao com garantias de SLA."),
    (6, 0, "Oportunidade alta", "Cross-sell", "MEDIO", 2, "CFO aprovou avancar com antecipacao de recebiveis.", "Enviar proposta do TOTVS Techfin."),
]
ITENS_CRITICOS = [
    (1, 0, "oportunidade", "Upsell", "O Fluig resolve exatamente esse fluxo de aprovacao.", "MEDIO"),
    (2, 0, "risco", "Churn", "Se nao resolver ate o fim do mes, vamos cancelar o contrato.", "ALTO"),
    (2, 1, "risco", "Concorrencia", "A Senior ja nos apresentou uma proposta e a diretoria esta pensando em trocar.", "MEDIO"),
    (3, 0, "oportunidade", "Novo produto", "Temos verba aprovada de R$ 120 mil para este ano.", "MEDIO"),
    (5, 0, "risco", "Perda de confianca", "Sinceramente, perdemos a confianca.", "ALTO"),
    (5, 1, "risco", "Concorrencia", "A Oracle NetSuite esta nos procurando com uma proposta agressiva.", "MEDIO"),
    (6, 0, "oportunidade", "Cross-sell", "Nosso gargalo hoje e capital de giro para antecipar recebiveis.", "MEDIO"),
]


def analise(i, owner, sentimento, resumo, persona, budget, conf, sent, met, resumo_exec, data, prox, detalhe, insights, indicadores):
    return dict(ID=A[i], OWNER_ID=owner, TRANSCRICAO_ID=T[i], SENTIMENTO_GERAL=sentimento, RESUMO=resumo,
                PERSONA_IDENTIFICADA=persona, BUDGET_IDENTIFICADO=budget, SCORE_CONFIANCA_VENDEDOR=conf, SCORE_SENTIMENTO=sent,
                **met, RESUMO_EXECUTIVO=resumo_exec, DATA_ANALISE=data, PROXIMA_ACAO=prox, SENTIMENTO_DETALHE=detalhe,
                INSIGHTS_JSON=insights, INDICADORES_JSON=indicadores)


ANALISES = [
    analise(1, U[2], "MISTO", "Sentimento misto (satisfeito com Backoffice / frustrado com aprovacoes manuais). Boa probabilidade de venda do modulo de processos (TOTVS Fluig). Interlocutor: Gestor Financeiro (decisor tecnico). Budget mencionado: R$ 60.000,00.",
            "Gestor Financeiro (decisor tecnico)", "R$ 60.000,00", 0.72, 0.15, metricas(3, 3, 1, 1, 1, 1, 1, 0, 1, 1, 1),
            TRANSCRICOES[0]["RESUMO_EXECUTIVO"], ts("2026-08-05 08:03:12"), "Enviar proposta ou agendar demonstracao com os decisores nos proximos dias.",
            "Misto (Satisfeito com Backoffice / Frustrado com aprovacoes)",
            '{"elogios":["O modulo financeiro atende bem"],"reclamacoes":["a aprovacao de compras ainda e por e-mail"],"duvidasJornada":[],"proximosPassos":["precisamos de uma proposta ate o fim do mes"],"observacoes":[]}',
            '{"locutores":3,"turnos":5,"sentencas":7,"palavras":78,"participacaoVendedorPct":45,"participacaoClientePct":55,"perguntasVendedor":1,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}'),
    analise(2, U[2], "NEGATIVO", "Sentimento negativo (insatisfacao recorrente com suporte). Risco alto de churn com concorrente (Senior) na mesa. Interlocutor: Gestor Administrativo (decisor). Budget nao mencionado.",
            "Gestor Administrativo (decisor)", "Nao identificado", 0.48, -0.85, metricas(3, 3, 3, 0, 0, 3, 0, 2, 2, 2, 1),
            TRANSCRICOES[1]["RESUMO_EXECUTIVO"], ts("2026-08-05 08:04:40"), "Acionar plano de retencao imediato com lideranca comercial.",
            "Negativo (insatisfacao com suporte)",
            '{"elogios":[],"reclamacoes":["ha tres semanas com o chamado do RM aberto e ninguem retorna"],"duvidasJornada":[],"proximosPassos":["vou escalar hoje"],"observacoes":["ameaca de cancelamento"]}',
            '{"locutores":2,"turnos":4,"sentencas":6,"palavras":64,"participacaoVendedorPct":30,"participacaoClientePct":70,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Vendedor ouviu mais do que falou","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}'),
    analise(3, U[3], "POSITIVO", "Sentimento positivo. Alta probabilidade de venda de novo produto (TOTVS Protheus - Distribuicao e Logistica). Interlocutor: Socio/Executivo (decisor final). Budget mencionado: R$ 120.000,00.",
            "Socio/Executivo (decisor final)", "R$ 120.000,00", 0.81, 0.72, metricas(3, 3, 0, 1, 2, 0, 2, 0, 0, 1, 1),
            TRANSCRICOES[2]["RESUMO_EXECUTIVO"], ts("2026-08-12 09:12:05"), "Enviar proposta ou agendar demonstracao com os decisores nos proximos dias.",
            "Positivo (entusiasmo com a solucao)",
            '{"elogios":["Gostei"],"reclamacoes":["ele nao acompanha nosso crescimento no estoque"],"duvidasJornada":[],"proximosPassos":["implantar antes do quarto trimestre"],"observacoes":[]}',
            '{"locutores":2,"turnos":4,"sentencas":6,"palavras":66,"participacaoVendedorPct":40,"participacaoClientePct":60,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}'),
    analise(4, U[3], "NEUTRO", "Sentimento neutro. Possibilidade de upsell (cobranca automatica) no RM Educacional. Interlocutor: Coordenador Administrativo (influenciador). Budget mencionado: R$ 8.000,00 por mes.",
            "Coordenador Administrativo (influenciador)", "R$ 8.000,00 por mes", 0.60, 0.05, metricas(3, 3, 1, 2, 0, 1, 1, 0, 0, 0, 1),
            TRANSCRICOES[3]["RESUMO_EXECUTIVO"], ts("2026-08-14 16:31:48"), "Qualificar necessidade, budget e prazo antes de enviar proposta.",
            "Neutro (avaliacao cautelosa)",
            '{"elogios":["O RM Educacional atende a secretaria"],"reclamacoes":["o portal do aluno e lento nas matriculas"],"duvidasJornada":[],"proximosPassos":["Vamos avaliar com calma"],"observacoes":[]}',
            '{"locutores":2,"turnos":3,"sentencas":5,"palavras":52,"participacaoVendedorPct":45,"participacaoClientePct":55,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}'),
    analise(5, U[4], "NEGATIVO", "Sentimento negativo (perda de confianca apos incidentes no PDV). Concorrente Oracle NetSuite em negociacao. Interlocutor: Diretor de Operacoes (decisor). Budget nao mencionado.",
            "Diretor de Operacoes (decisor)", "Nao identificado", 0.40, -0.70, metricas(4, 4, 3, 1, 0, 3, 1, 1, 2, 2, 2),
            TRANSCRICOES[4]["RESUMO_EXECUTIVO"], ts("2026-08-20 08:05:22"), "Envolver a diretoria de CS e apresentar plano de comunicacao de incidentes antes da renovacao.",
            "Negativo (perda de confianca / concorrencia)",
            '{"elogios":[],"reclamacoes":["O PDV travou duas vezes na Black Friday passada","ninguem nos avisou da correcao"],"duvidasJornada":[],"proximosPassos":["Precisamos de garantias antes de renovar"],"observacoes":["Oracle NetSuite com proposta agressiva"]}',
            '{"locutores":4,"turnos":4,"sentencas":8,"palavras":74,"participacaoVendedorPct":25,"participacaoClientePct":75,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Vendedor ouviu mais do que falou","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}'),
    analise(6, U[2], "POSITIVO", "Sentimento positivo (cliente promotor). Boa probabilidade de cross-sell (TOTVS Techfin - antecipacao de recebiveis). Interlocutor: CFO (decisor final). Budget nao mencionado.",
            "CFO (decisor final)", "Nao identificado", 0.85, 0.80, metricas(3, 3, 0, 0, 3, 0, 1, 0, 0, 1, 1),
            TRANSCRICOES[5]["RESUMO_EXECUTIVO"], ts("2026-08-25 17:12:30"), "Enviar proposta ou agendar demonstracao com os decisores nos proximos dias.",
            "Positivo (satisfacao com o Datasul)",
            '{"elogios":["O Datasul esta redondo na operacao, o time adora"],"reclamacoes":[],"duvidasJornada":[],"proximosPassos":["Pode mandar a proposta"],"observacoes":[]}',
            '{"locutores":2,"turnos":4,"sentencas":6,"palavras":58,"participacaoVendedorPct":35,"participacaoClientePct":65,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"media"}'),
]

PRODUTOS = [
    (1, 0, "TOTVS Protheus", "ATUAL", "revisar o uso do Protheus no financeiro"),
    (1, 1, "TOTVS Fluig", "OPORTUNIDADE", "O Fluig resolve exatamente esse fluxo de aprovacao"),
    (2, 0, "TOTVS RM", "ATUAL", "chamado do RM aberto e ninguem retorna"),
    (2, 1, "Senior", "CONCORRENTE", "A Senior ja nos apresentou uma proposta"),
    (3, 0, "Bling", "CONCORRENTE", "Hoje usamos o Bling, mas ele nao acompanha nosso crescimento"),
    (3, 1, "TOTVS Protheus", "OPORTUNIDADE", "O Protheus tem o modulo de distribuicao e logistica"),
    (4, 0, "TOTVS RM", "ATUAL", "O RM Educacional atende a secretaria"),
    (5, 0, "TOTVS Varejo", "ATUAL", "O PDV travou duas vezes na Black Friday passada"),
    (5, 1, "Oracle NetSuite", "CONCORRENTE", "A Oracle NetSuite esta nos procurando com uma proposta agressiva"),
    (6, 0, "TOTVS Datasul", "ATUAL", "O Datasul esta redondo na operacao"),
    (6, 1, "TOTVS Techfin", "OPORTUNIDADE", "O TOTVS Techfin faz antecipacao integrada ao ERP"),
]
OPORTUNIDADES = [
    (1, 0, "UPSELL", "Fluxo de aprovacao de compras com TOTVS Fluig integrado ao Protheus.", "TOTVS Fluig", 0.78, "R$ 60.000,00"),
    (2, 0, "UPSELL", "Retencao com plano de melhoria no suporte do RM; sem nova venda antes de resolver o chamado.", "TOTVS RM", 0.25, "Nao estimado"),
    (3, 0, "NOVO_PRODUTO", "Implantacao do Protheus Distribuicao e Logistica substituindo o Bling.", "TOTVS Protheus", 0.81, "R$ 120.000,00"),
    (4, 0, "UPSELL", "Modulo de cobranca automatica do RM Educacional.", "TOTVS RM", 0.55, "R$ 8.000,00 por mes"),
    (5, 0, "UPSELL", "Monitoramento proativo do PDV incluido na renovacao do TOTVS Varejo.", "TOTVS Varejo", 0.42, "Nao estimado"),
    (6, 0, "CROSS_SELL", "Antecipacao de recebiveis com TOTVS Techfin integrada ao Datasul.", "TOTVS Techfin", 0.74, "Nao estimado"),
]
RISCOS = [
    (1, 0, "INSATISFACAO", "Insatisfacao com Backoffice: aprovacao de compras ainda e por e-mail.", "MEDIO", None),
    (2, 0, "CHURN", "Cliente verbalizou cancelamento: se nao resolver ate o fim do mes, vamos cancelar o contrato.", "ALTO", None),
    (2, 1, "CONCORRENCIA", "Concorrente Senior apresentou proposta e a diretoria avalia a troca.", "MEDIO", "Senior"),
    (4, 0, "INSATISFACAO", "Insatisfacao com o portal do aluno: lento nas matriculas.", "BAIXO", None),
    (5, 0, "PERDA_CONFIANCA", "Cliente sinaliza perda de confianca na relacao: sinceramente, perdemos a confianca.", "ALTO", None),
    (5, 1, "CONCORRENCIA", "Oracle NetSuite abordou o cliente com proposta agressiva.", "MEDIO", "Oracle NetSuite"),
]

ALERTAS = [
    dict(ID=AL[1], OWNER_ID=U[2], ANALISE_ID=A[1], TIPO="OPORTUNIDADE_ALTA", TITULO="Oportunidade de UPSELL: TOTVS Fluig", CATEGORIA="OPORTUNIDADE_ALTA",
         MENSAGEM="Oportunidade de UPSELL com alta probabilidade: aprovacao de compras com TOTVS Fluig (budget R$ 60.000,00).", MOTIVO=None, RECOMENDACAO=None,
         OCORRENCIAS=None, PRIORIDADE="MEDIO", STATUS="NOVO", DATA_CRIACAO=ts("2026-08-05 08:03:13")),
    dict(ID=AL[2], OWNER_ID=U[2], ANALISE_ID=A[2], TIPO="RISCO_CHURN", TITULO="RISCO_CHURN", CATEGORIA="RISCO_CHURN",
         MENSAGEM="Risco CHURN detectado para cliente Clinica Vida Plena: Cliente verbalizou cancelamento. Trecho: \"vamos cancelar o contrato\".", MOTIVO=None, RECOMENDACAO=None,
         OCORRENCIAS=None, PRIORIDADE="ALTO", STATUS="NOVO", DATA_CRIACAO=ts("2026-08-05 08:04:41")),
    dict(ID=AL[3], OWNER_ID=U[2], ANALISE_ID=A[2], TIPO="CONSOLIDADO_CHURN", TITULO="Risco de churn", CATEGORIA="Churn",
         MENSAGEM="Cliente verbalizou cancelamento, troca ou nao renovacao. Ocorrencias: 2. Recomendacao: Acionar plano de retencao imediato com lideranca comercial.",
         MOTIVO="Cliente verbalizou cancelamento, troca ou nao renovacao.", RECOMENDACAO="Acionar plano de retencao imediato com lideranca comercial.",
         OCORRENCIAS=2, PRIORIDADE="ALTO", STATUS="NOVO", DATA_CRIACAO=ts("2026-08-05 08:04:41")),
    dict(ID=AL[4], OWNER_ID=U[2], ANALISE_ID=A[2], TIPO="CONCORRENTE_DETECTADO", TITULO="CONCORRENTE_DETECTADO", CATEGORIA="CONCORRENTE_DETECTADO",
         MENSAGEM="Concorrente Senior mencionado pelo cliente Clinica Vida Plena: \"A Senior ja nos apresentou uma proposta\".", MOTIVO=None, RECOMENDACAO=None,
         OCORRENCIAS=None, PRIORIDADE="MEDIO", STATUS="NOVO", DATA_CRIACAO=ts("2026-08-05 08:04:42")),
    dict(ID=AL[5], OWNER_ID=U[3], ANALISE_ID=A[3], TIPO="OPORTUNIDADE_ALTA", TITULO="Oportunidade de NOVO_PRODUTO: TOTVS Protheus", CATEGORIA="OPORTUNIDADE_ALTA",
         MENSAGEM="Oportunidade de NOVO_PRODUTO com alta probabilidade: Protheus Distribuicao e Logistica (verba aprovada R$ 120.000,00).", MOTIVO=None, RECOMENDACAO=None,
         OCORRENCIAS=None, PRIORIDADE="MEDIO", STATUS="NOVO", DATA_CRIACAO=ts("2026-08-12 09:12:06")),
    dict(ID=AL[6], OWNER_ID=U[4], ANALISE_ID=A[5], TIPO="RISCO_PERDA_CONFIANCA", TITULO="RISCO_PERDA_CONFIANCA", CATEGORIA="RISCO_PERDA_CONFIANCA",
         MENSAGEM="Risco PERDA_CONFIANCA detectado para cliente Supermercados Bom Preco: \"Sinceramente, perdemos a confianca\".", MOTIVO=None, RECOMENDACAO=None,
         OCORRENCIAS=None, PRIORIDADE="ALTO", STATUS="NOVO", DATA_CRIACAO=ts("2026-08-20 08:05:23")),
    dict(ID=AL[7], OWNER_ID=U[4], ANALISE_ID=A[5], TIPO="CONSOLIDADO_CONCORRENCIA", TITULO="Concorrente citado", CATEGORIA="Concorrencia",
         MENSAGEM="Oracle NetSuite em negociacao com o cliente. Ocorrencias: 2. Recomendacao: Contraproposta de renovacao com garantias de SLA.",
         MOTIVO="Oracle NetSuite em negociacao com o cliente.", RECOMENDACAO="Contraproposta de renovacao com garantias de SLA.",
         OCORRENCIAS=2, PRIORIDADE="MEDIO", STATUS="NOVO", DATA_CRIACAO=ts("2026-08-20 08:05:23")),
    dict(ID=AL[8], OWNER_ID=U[2], ANALISE_ID=A[6], TIPO="OPORTUNIDADE_ALTA", TITULO="Oportunidade de CROSS_SELL: TOTVS Techfin", CATEGORIA="OPORTUNIDADE_ALTA",
         MENSAGEM="Oportunidade de CROSS_SELL com alta probabilidade: antecipacao de recebiveis com TOTVS Techfin.", MOTIVO=None, RECOMENDACAO=None,
         OCORRENCIAS=None, PRIORIDADE="MEDIO", STATUS="NOVO", DATA_CRIACAO=ts("2026-08-25 17:12:31")),
]

FEEDBACKS = [
    dict(ID=FB[1], OWNER_ID=U[2], TRANSCRICAO_ID=T[1], ANALISE_ID=A[1], UTIL=1, AJUSTE="Persona correta; o budget de R$ 60 mil e anual, nao mensal.", CREATED_AT=ts("2026-08-05 10:00:00")),
    dict(ID=FB[2], OWNER_ID=U[2], TRANSCRICAO_ID=T[2], ANALISE_ID=A[2], UTIL=1, AJUSTE="Risco de churn confirmado com o cliente; alerta foi essencial.", CREATED_AT=ts("2026-08-06 09:30:00")),
    dict(ID=FB[3], OWNER_ID=U[3], TRANSCRICAO_ID=T[3], ANALISE_ID=A[3], UTIL=1, AJUSTE=None, CREATED_AT=ts("2026-08-12 11:00:00")),
    dict(ID=FB[4], OWNER_ID=U[3], TRANSCRICAO_ID=T[4], ANALISE_ID=A[4], UTIL=0, AJUSTE="A lentidao do portal e um risco de insatisfacao MEDIO, nao BAIXO.", CREATED_AT=ts("2026-08-15 08:20:00")),
    dict(ID=FB[5], OWNER_ID=U[1], TRANSCRICAO_ID=T[5], ANALISE_ID=A[5], UTIL=0, AJUSTE="Feedback duplicado enviado por engano.", CREATED_AT=ts("2026-08-20 09:00:00")),
    dict(ID=FB[6], OWNER_ID=U[4], TRANSCRICAO_ID=T[6], ANALISE_ID=A[6], UTIL=1, AJUSTE="Cross-sell de Techfin identificado corretamente; proposta enviada no mesmo dia.", CREATED_AT=ts("2026-08-26 08:15:00")),
]


def gerar():
    p = []
    p.append("""-- =====================================================================================
--  {solucao}
--  Script DML - Carga de dados (INSERT), evolucao dos processos (UPDATE) e exclusoes (DELETE)
--
--  Equipe: {equipe}
{integrantes}
--
--  Challenge FIAP x TOTVS 2026 - Database Design - Sprint 3
--
--  Pre-requisito: executar antes o script 01_DDL (tabelas, sequence, triggers).
--  Os identificadores (UUID) sao fixos para o script ser legivel e reexecutavel.
--  Os dados sao ficticios: empresas, pessoas e reunioes foram inventadas para o Challenge,
--  seguindo o vocabulario real da solucao (produtos TOTVS, concorrentes, tipos de alerta).
--
--  Secoes:
--    1. INSERT  - popula todas as {n} tabelas (minimo de 5 registros por tabela)
--    2. UPDATE  - evolucao da transcricao (PENDENTE > PROCESSANDO > PROCESSADA / ERRO),
--                 do alerta (NOVO > VISTO > RESOLVIDO), do feedback e das sessoes
--    3. DELETE  - exclusoes com integridade referencial (ON DELETE CASCADE / SET NULL)
--    4. CONFERENCIA - contagem de registros por tabela
-- =====================================================================================
""".format(solucao=SOLUCAO, equipe=EQUIPE, integrantes="\n".join("--     %s - %s" % (n, rm) for n, rm in INTEGRANTES), n=len(TABELAS)))

    p.append("\n-- =====================================================================================\n--  1. INSERT - CARGA INICIAL\n-- =====================================================================================\n")

    p.append(bloco("APP_USERS - 5 usuarios (1 ADMIN, 4 USER; 1 desativado). Senha de todos: Senha@123 (hash BCrypt)"))
    for u in USUARIOS:
        p.append(ins("APP_USERS", **u, PASSWORD_HASH=BCRYPT_DEMO))

    p.append(bloco("REFRESH_TOKENS - 6 sessoes (1 revogada, 1 expirada)"))
    for r in TOKENS:
        p.append(ins("REFRESH_TOKENS", **r))

    p.append(bloco("TRANSCRICOES - 9 reunioes (6 PROCESSADA, 1 PENDENTE, 1 PROCESSANDO, 1 ERRO). NUM_PROTOCOLO vem da sequence por DEFAULT.\n--   ANALISE_ID fica NULL aqui (FK circular 1:1) e e preenchido depois que as analises existirem"))
    for t in TRANSCRICOES:
        p.append(ins("TRANSCRICOES", **{k: v for k, v in t.items() if k != "ANALISE_ID"}))

    p.append(bloco("TRANSCRICAO_STATUS_HIST - registro de criacao de cada reuniao (as mudancas seguintes vem do trigger)"))
    for t in TRANSCRICOES:
        p.append(ins("TRANSCRICAO_STATUS_HIST", TRANSCRICAO_ID=t["ID"], STATUS_ANTERIOR=None, STATUS_NOVO="PENDENTE",
                     ALTERADO_EM=t["DATA_CRIACAO"], ALTERADO_POR="APLICACAO", OBSERVACAO="Registro criado (%s)" % t["ORIGEM_TRANSCRICAO"]))

    p.append(bloco("TRANSCRICAO_WARNINGS - avisos de importacao"))
    for tid, pos, w in WARNINGS:
        p.append(ins("TRANSCRICAO_WARNINGS", TRANSCRICAO_ID=tid, POSICAO=pos, WARNING=w))

    p.append(bloco("TRANSCRICAO_RISCOS_CONS / TRANSCRICAO_OPORT_CONS / TRANSCRICAO_ALERTAS_CONS / TRANSCRICAO_ITENS_CRITICOS - listas consolidadas"))
    for i, pos, cat, sev, occ, ev, rec in RISCOS_CONS:
        p.append(ins("TRANSCRICAO_RISCOS_CONS", TRANSCRICAO_ID=T[i], POSICAO=pos, CATEGORIA=cat, SEVERIDADE=sev, OCORRENCIAS=occ, EVIDENCIAS=ev, RECOMENDACAO=rec))
    for i, pos, cat, sc, occ, ev, rec in OPORT_CONS:
        p.append(ins("TRANSCRICAO_OPORT_CONS", TRANSCRICAO_ID=T[i], POSICAO=pos, CATEGORIA=cat, SCORE=sc, OCORRENCIAS=occ, EVIDENCIAS=ev, RECOMENDACAO=rec))
    for i, pos, tit, cat, sev, occ, mot, rec in ALERTAS_CONS:
        p.append(ins("TRANSCRICAO_ALERTAS_CONS", TRANSCRICAO_ID=T[i], POSICAO=pos, TITULO=tit, CATEGORIA=cat, SEVERIDADE=sev, OCORRENCIAS=occ, MOTIVO=mot, RECOMENDACAO=rec))
    for i, pos, tipo, cat, trecho, sev in ITENS_CRITICOS:
        p.append(ins("TRANSCRICAO_ITENS_CRITICOS", TRANSCRICAO_ID=T[i], POSICAO=pos, TIPO=tipo, CATEGORIA=cat, TRECHO=trecho, SEVERIDADE=sev))

    p.append(bloco("ANALISES - 6 analises da LeanIA (uma por reuniao processada)"))
    for a in ANALISES:
        p.append(ins("ANALISES", **a))

    p.append(bloco("TRANSCRICOES.ANALISE_ID - vincula cada reuniao processada a sua analise atual (relacionamento 1:1)"))
    for t in TRANSCRICOES:
        if t.get("ANALISE_ID"):
            p.append("UPDATE TRANSCRICOES SET ANALISE_ID = '%s' WHERE ID = '%s';\n" % (t["ANALISE_ID"], t["ID"]))

    p.append(bloco("ANALISE_PRODUTOS / ANALISE_OPORTUNIDADES / ANALISE_RISCOS - detalhes da analise"))
    for i, pos, nome, tipo, ctx in PRODUTOS:
        p.append(ins("ANALISE_PRODUTOS", ANALISE_ID=A[i], POSICAO=pos, NOME_PRODUTO=nome, TIPO=tipo, CONTEXTO=ctx))
    for i, pos, tipo, desc, prod, sc, val in OPORTUNIDADES:
        p.append(ins("ANALISE_OPORTUNIDADES", ANALISE_ID=A[i], POSICAO=pos, TIPO=tipo, DESCRICAO=desc, PRODUTO_RELACIONADO=prod, SCORE=sc, VALOR_ESTIMADO=val))
    for i, pos, tipo, desc, sev, conc in RISCOS:
        p.append(ins("ANALISE_RISCOS", ANALISE_ID=A[i], POSICAO=pos, TIPO=tipo, DESCRICAO=desc, SEVERIDADE=sev, CONCORRENTE_MENCIONADO=conc))

    p.append(bloco("ANALISE_RISCOS_CONS / ANALISE_OPORT_CONS / ANALISE_ALERTAS_CONS / ANALISE_ITENS_CRITICOS - consolidados gerados pela analise"))
    for i, pos, cat, sev, occ, ev, rec in RISCOS_CONS:
        p.append(ins("ANALISE_RISCOS_CONS", ANALISE_ID=A[i], POSICAO=pos, CATEGORIA=cat, SEVERIDADE=sev, OCORRENCIAS=occ, EVIDENCIAS=ev, RECOMENDACAO=rec))
    for i, pos, cat, sc, occ, ev, rec in OPORT_CONS:
        p.append(ins("ANALISE_OPORT_CONS", ANALISE_ID=A[i], POSICAO=pos, CATEGORIA=cat, SCORE=sc, OCORRENCIAS=occ, EVIDENCIAS=ev, RECOMENDACAO=rec))
    for i, pos, tit, cat, sev, occ, mot, rec in ALERTAS_CONS:
        p.append(ins("ANALISE_ALERTAS_CONS", ANALISE_ID=A[i], POSICAO=pos, TITULO=tit, CATEGORIA=cat, SEVERIDADE=sev, OCORRENCIAS=occ, MOTIVO=mot, RECOMENDACAO=rec))
    for i, pos, tipo, cat, trecho, sev in ITENS_CRITICOS:
        p.append(ins("ANALISE_ITENS_CRITICOS", ANALISE_ID=A[i], POSICAO=pos, TIPO=tipo, CATEGORIA=cat, TRECHO=trecho, SEVERIDADE=sev))

    p.append(bloco("ALERTAS - 8 alertas acionaveis (todos NOVO na criacao)"))
    for al in ALERTAS:
        p.append(ins("ALERTAS", **al))

    p.append(bloco("ALERTA_STATUS_HIST - registro de criacao de cada alerta (as mudancas seguintes vem do trigger)"))
    for al in ALERTAS:
        p.append(ins("ALERTA_STATUS_HIST", ALERTA_ID=al["ID"], STATUS_ANTERIOR=None, STATUS_NOVO="NOVO",
                     ALTERADO_EM=al["DATA_CRIACAO"], ALTERADO_POR="LEANIA", OBSERVACAO="Alerta gerado pela analise"))

    p.append(bloco("ANALISE_FEEDBACKS - 6 avaliacoes dos usuarios"))
    for f in FEEDBACKS:
        p.append(ins("ANALISE_FEEDBACKS", **f))

    p.append("\nCOMMIT;\n")

    # ------------------------------------------------------------------ UPDATE
    p.append("""
-- =====================================================================================
--  2. UPDATE - EVOLUCAO DOS PROCESSOS
-- =====================================================================================
""")
    p.append(bloco("2.1 Transcricao 'Construtora Alvorada': PENDENTE > PROCESSANDO > PROCESSADA (com a analise gerada)"))
    p.append("-- a) a fila da LeanIA pega a reuniao: PENDENTE > PROCESSANDO (o trigger grava o historico)\n")
    p.append("UPDATE TRANSCRICOES\n   SET STATUS = 'PROCESSANDO'\n WHERE ID = '%s';\n\n" % T[7])
    p.append("-- b) a LeanIA devolve a analise: inserimos a analise e seus detalhes\n")
    a7 = dict(ID=A[7], OWNER_ID=U[3], TRANSCRICAO_ID=T[7], SENTIMENTO_GERAL="POSITIVO",
              RESUMO="Sentimento positivo. Boa probabilidade de venda de novo produto (Protheus Construcao e Projetos). Interlocutor: Gerente de Obras (influenciador tecnico). Budget nao mencionado.",
              PERSONA_IDENTIFICADA="Gerente de Obras (influenciador tecnico)", BUDGET_IDENTIFICADO="Nao identificado",
              SCORE_CONFIANCA_VENDEDOR=0.70, SCORE_SENTIMENTO=0.55, **metricas(3, 3, 1, 0, 2, 1, 1, 0, 0, 1, 1),
              RESUMO_EXECUTIVO="Lead com dor clara (controle de obras em planilha) e pedido de demonstracao. Agendar demo do Protheus Construcao e Projetos.",
              DATA_ANALISE=Raw("SYSTIMESTAMP"), PROXIMA_ACAO="Enviar proposta ou agendar demonstracao com os decisores nos proximos dias.",
              SENTIMENTO_DETALHE="Positivo (interesse na demonstracao)",
              INSIGHTS_JSON='{"elogios":[],"reclamacoes":["perdemos prazo de medicao"],"duvidasJornada":[],"proximosPassos":["Queremos ver uma demonstracao ainda este mes"],"observacoes":[]}',
              INDICADORES_JSON='{"locutores":2,"turnos":3,"sentencas":5,"palavras":49,"participacaoVendedorPct":40,"participacaoClientePct":60,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}')
    p.append(ins("ANALISES", **a7))
    p.append(ins("ANALISE_PRODUTOS", ANALISE_ID=A[7], POSICAO=0, NOME_PRODUTO="TOTVS Protheus", TIPO="OPORTUNIDADE", CONTEXTO="O Protheus Construcao e Projetos integra medicao, compras e financeiro"))
    p.append(ins("ANALISE_OPORTUNIDADES", ANALISE_ID=A[7], POSICAO=0, TIPO="NOVO_PRODUTO", DESCRICAO="Implantacao do Protheus Construcao e Projetos no lugar das planilhas.", PRODUTO_RELACIONADO="TOTVS Protheus", SCORE=0.70, VALOR_ESTIMADO="Nao estimado"))
    p.append(ins("ANALISE_RISCOS", ANALISE_ID=A[7], POSICAO=0, TIPO="INSATISFACAO", DESCRICAO="Perda de prazo de medicao por controle em planilha (dor do processo atual).", SEVERIDADE="BAIXO", CONCORRENTE_MENCIONADO=None))
    p.append(ins("ANALISE_OPORT_CONS", ANALISE_ID=A[7], POSICAO=0, CATEGORIA="Novo produto", SCORE=0.70, OCORRENCIAS=1, EVIDENCIAS='["Queremos ver uma demonstracao ainda este mes"]', RECOMENDACAO="Agendar demonstracao do Protheus Construcao e Projetos."))
    p.append(ins("ANALISE_ALERTAS_CONS", ANALISE_ID=A[7], POSICAO=0, TITULO="Oportunidade alta", CATEGORIA="Novo produto", SEVERIDADE="MEDIO", OCORRENCIAS=1, MOTIVO="Lead pediu demonstracao com prazo.", RECOMENDACAO="Agendar demonstracao ainda este mes."))
    p.append(ins("ANALISE_ITENS_CRITICOS", ANALISE_ID=A[7], POSICAO=0, TIPO="oportunidade", CATEGORIA="Novo produto", TRECHO="Queremos ver uma demonstracao ainda este mes.", SEVERIDADE="MEDIO"))
    p.append(ins("TRANSCRICAO_OPORT_CONS", TRANSCRICAO_ID=T[7], POSICAO=0, CATEGORIA="Novo produto", SCORE=0.70, OCORRENCIAS=1, EVIDENCIAS='["Queremos ver uma demonstracao ainda este mes"]', RECOMENDACAO="Agendar demonstracao do Protheus Construcao e Projetos."))
    p.append(ins("TRANSCRICAO_ALERTAS_CONS", TRANSCRICAO_ID=T[7], POSICAO=0, TITULO="Oportunidade alta", CATEGORIA="Novo produto", SEVERIDADE="MEDIO", OCORRENCIAS=1, MOTIVO="Lead pediu demonstracao com prazo.", RECOMENDACAO="Agendar demonstracao ainda este mes."))
    p.append(ins("TRANSCRICAO_ITENS_CRITICOS", TRANSCRICAO_ID=T[7], POSICAO=0, TIPO="oportunidade", CATEGORIA="Novo produto", TRECHO="Queremos ver uma demonstracao ainda este mes.", SEVERIDADE="MEDIO"))
    p.append("\n-- c) a transcricao passa a PROCESSADA, recebe a analise atual, as metricas e o resumo executivo\n")
    p.append("""UPDATE TRANSCRICOES
   SET STATUS = 'PROCESSADA',
       DATA_PROCESSAMENTO = SYSTIMESTAMP,
       ANALISE_ID = '%s',
       MET_TOTAL_PARTES = 3, MET_PARTES_ANALISADAS = 3, MET_PARTES_NEGATIVAS = 1, MET_PARTES_NEUTRAS = 0,
       MET_PARTES_POSITIVAS = 2, MET_RISCOS_DETECTADOS = 1, MET_OPORTUNIDADES_DETECTADAS = 1,
       MET_CHURNS_POTENCIAIS = 0, MET_MENCOES_CRITICAS = 0, MET_ALERTAS_CONSOLIDADOS = 1, MET_CHUNKS_PROCESSADOS = 1,
       RESUMO_EXECUTIVO = 'Lead com dor clara (controle de obras em planilha) e pedido de demonstracao. Agendar demo do Protheus Construcao e Projetos.'
 WHERE ID = '%s';

-- d) o alerta da nova oportunidade e gerado
""" % (A[7], T[7]))
    p.append(ins("ALERTAS", ID="eeeeeeee-0000-4000-8000-000000000009", OWNER_ID=U[3], ANALISE_ID=A[7], TIPO="OPORTUNIDADE_ALTA",
                 TITULO="Oportunidade de NOVO_PRODUTO: TOTVS Protheus", CATEGORIA="OPORTUNIDADE_ALTA",
                 MENSAGEM="Oportunidade de NOVO_PRODUTO com alta probabilidade: Protheus Construcao e Projetos (demonstracao solicitada).",
                 MOTIVO=None, RECOMENDACAO=None, OCORRENCIAS=None, PRIORIDADE="MEDIO", STATUS="NOVO", DATA_CRIACAO=Raw("SYSTIMESTAMP")))
    p.append("\n-- conferencia: o historico mostra PENDENTE > PROCESSANDO > PROCESSADA\nSELECT STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR\n  FROM TRANSCRICAO_STATUS_HIST\n WHERE TRANSCRICAO_ID = '%s'\n ORDER BY ID;\n" % T[7])

    p.append(bloco("2.2 Transcricao 'Agro Serra Verde': PROCESSANDO > ERRO (LeanIA indisponivel) e observacao no historico"))
    p.append("UPDATE TRANSCRICOES\n   SET STATUS = 'ERRO'\n WHERE ID = '%s';\n\n" % T[8])
    p.append("UPDATE TRANSCRICAO_STATUS_HIST\n   SET OBSERVACAO = 'Timeout na LeanIA apos 3 tentativas; reenfileirar pelo job de recuperacao'\n WHERE TRANSCRICAO_ID = '%s'\n   AND STATUS_NOVO = 'ERRO';\n" % T[8])

    p.append(bloco("2.3 Alertas: NOVO > VISTO > RESOLVIDO (tratamento do risco de churn) e NOVO > VISTO"))
    p.append("UPDATE ALERTAS SET STATUS = 'VISTO'     WHERE ID = '%s';\n" % AL[2])
    p.append("UPDATE ALERTAS SET STATUS = 'RESOLVIDO' WHERE ID = '%s';\n" % AL[2])
    p.append("UPDATE ALERTAS SET STATUS = 'VISTO'     WHERE ID IN ('%s', '%s');\n\n" % (AL[3], AL[6]))
    p.append("UPDATE ALERTA_STATUS_HIST\n   SET OBSERVACAO = 'Reuniao de retencao realizada em 08/08/2026; cliente manteve o contrato do RM'\n WHERE ALERTA_ID = '%s'\n   AND STATUS_NOVO = 'RESOLVIDO';\n" % AL[2])
    p.append("\n-- conferencia: ciclo completo do alerta de churn\nSELECT STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, OBSERVACAO\n  FROM ALERTA_STATUS_HIST\n WHERE ALERTA_ID = '%s'\n ORDER BY ID;\n" % AL[2])

    p.append(bloco("2.4 Feedback: usuario revisa a avaliacao (nao util > util) e complementa o ajuste"))
    p.append("UPDATE ANALISE_FEEDBACKS\n   SET UTIL = 1,\n       AJUSTE = 'Apos revisao: a severidade MEDIO faz sentido no periodo de matriculas. Analise util.'\n WHERE ID = '%s';\n" % FB[4])

    p.append(bloco("2.5 Sessoes e usuarios: logout revoga o refresh token; analista de BI reativada; NPS atualizado apos nova pesquisa"))
    p.append("UPDATE REFRESH_TOKENS SET REVOKED_AT = SYSTIMESTAMP WHERE ID = '%s' AND REVOKED_AT IS NULL;\n" % RT[2])
    p.append("UPDATE APP_USERS SET IS_ACTIVE = 1 WHERE ID = '%s';\n" % U[5])
    p.append("UPDATE TRANSCRICOES SET NOTA_NPS = 9, DT_ULTIMA_PESQUISA = '2026-09' WHERE ID = '%s';\n" % T[3])
    p.append("\nCOMMIT;\n")

    # ------------------------------------------------------------------ DELETE
    p.append("""
-- =====================================================================================
--  3. DELETE - EXCLUSOES COM INTEGRIDADE REFERENCIAL
-- =====================================================================================
""")
    p.append(bloco("3.1 Sessao expirada e revogada ha mais de 7 dias (limpeza de refresh tokens)"))
    p.append("DELETE FROM REFRESH_TOKENS\n WHERE ID = '%s'\n   AND EXPIRES_AT < SYSTIMESTAMP;\n" % RT[5])

    p.append(bloco("3.2 Feedback enviado por engano (nao util e marcado como duplicado)"))
    p.append("DELETE FROM ANALISE_FEEDBACKS\n WHERE ID = '%s'\n   AND UTIL = 0;\n" % FB[5])

    p.append(bloco("3.3 Transcricao com ERRO e texto vazio ('Hospital Sao Lucas'): o ON DELETE CASCADE remove avisos e historico"))
    p.append("-- antes: avisos e historico da reuniao\nSELECT (SELECT COUNT(*) FROM TRANSCRICAO_WARNINGS    WHERE TRANSCRICAO_ID = '%s') AS AVISOS,\n       (SELECT COUNT(*) FROM TRANSCRICAO_STATUS_HIST WHERE TRANSCRICAO_ID = '%s') AS HISTORICO\n  FROM DUAL;\n\n" % (T[9], T[9]))
    p.append("DELETE FROM TRANSCRICOES\n WHERE ID = '%s';\n\n" % T[9])
    p.append("-- depois: zero (removidos em cascata)\nSELECT (SELECT COUNT(*) FROM TRANSCRICAO_WARNINGS    WHERE TRANSCRICAO_ID = '%s') AS AVISOS,\n       (SELECT COUNT(*) FROM TRANSCRICAO_STATUS_HIST WHERE TRANSCRICAO_ID = '%s') AS HISTORICO\n  FROM DUAL;\n" % (T[9], T[9]))

    p.append(bloco("3.4 Reprocessamento: apagar a analise da 'Colegio Nova Geracao' zera TRANSCRICOES.ANALISE_ID (ON DELETE SET NULL)\n--     e ANALISE_FEEDBACKS.ANALISE_ID (SET NULL); os detalhes da analise saem em cascata"))
    p.append("DELETE FROM ANALISES\n WHERE ID = '%s';\n\n" % A[4])
    p.append("SELECT ID, CLIENTE, STATUS, ANALISE_ID FROM TRANSCRICOES WHERE ID = '%s';\n" % T[4])
    p.append("SELECT ID, UTIL, ANALISE_ID FROM ANALISE_FEEDBACKS WHERE TRANSCRICAO_ID = '%s';\n" % T[4])
    p.append("\nCOMMIT;\n")

    # ------------------------------------------------------------------ CONFERENCIA
    p.append("""
-- =====================================================================================
--  4. CONFERENCIA - registros por tabela apos a carga, as atualizacoes e as exclusoes
-- =====================================================================================
""")
    partes = ["SELECT '%s' AS TABELA, COUNT(*) AS REGISTROS FROM %s" % (t["nome"], t["nome"]) for t in TABELAS]
    p.append("\nUNION ALL\n".join(partes) + "\nORDER BY 1;\n")
    return "".join(p)


if __name__ == "__main__":
    print(gerar())
