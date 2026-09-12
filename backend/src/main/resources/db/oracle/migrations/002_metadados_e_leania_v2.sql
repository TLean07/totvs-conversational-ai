-- =====================================================================
--  Migracao 002 - metadados do cliente (JSON/CSV TOTVS) e LeanIA v2
--  Aplicar em schemas criados com o schema.sql anterior a esta versao.
--  Colunas novas sao todas opcionais: o backend antigo continua subindo.
-- =====================================================================
ALTER TABLE TRANSCRICOES ADD (
    UF                  VARCHAR2(10 CHAR),
    CNAE                VARCHAR2(20 CHAR),
    NOME_UNIDADE        VARCHAR2(200 CHAR),
    NOME_SEGMENTO       VARCHAR2(200 CHAR),
    FAIXA_FATURAMENTO   VARCHAR2(200 CHAR),
    TP_RECURSO          VARCHAR2(50 CHAR),
    NOTA_NPS            NUMBER(10,0),
    DT_ULTIMA_PESQUISA  VARCHAR2(50 CHAR)
);

COMMENT ON COLUMN TRANSCRICOES.NOME_SEGMENTO IS 'Segmento do cliente (NOME_SEGMENTO no JSON/CSV TOTVS)';
COMMENT ON COLUMN TRANSCRICOES.FAIXA_FATURAMENTO IS 'FAIXA_FATURAMENTO_CLIENTE ou FAIXA_FATURAMENTO_CLIENTE_EC';
COMMENT ON COLUMN TRANSCRICOES.TP_RECURSO IS 'Tipo da reuniao no CSV: VENDAS, SUPORTE, OPERACIONAL';
COMMENT ON COLUMN TRANSCRICOES.NOTA_NPS IS 'NPS informado pelo cliente na ultima pesquisa (0-10)';

ALTER TABLE ANALISES ADD (
    PROXIMA_ACAO        VARCHAR2(500 CHAR),
    SENTIMENTO_DETALHE  VARCHAR2(300 CHAR),
    INSIGHTS_JSON       CLOB,
    INDICADORES_JSON    CLOB
);

COMMENT ON COLUMN ANALISES.PROXIMA_ACAO IS 'Proxima acao comercial sugerida pela LeanIA';
COMMENT ON COLUMN ANALISES.SENTIMENTO_DETALHE IS 'Sentimento por area, ex.: Misto (Satisfeito com Backoffice / Frustrado com RH)';
COMMENT ON COLUMN ANALISES.INSIGHTS_JSON IS 'JSON: elogios, reclamacoes, duvidasJornada, proximosPassos, observacoes';
COMMENT ON COLUMN ANALISES.INDICADORES_JSON IS 'JSON: locutores, participacao vendedor/cliente, perguntas, escuta ativa';

CREATE INDEX IDX_TRANSC_SEGMENTO ON TRANSCRICOES (NOME_SEGMENTO);
CREATE INDEX IDX_TRANSC_UF ON TRANSCRICOES (UF);
