-- =====================================================================================
--  Migracao 003 - Database Design Sprint 3 (aplicar em schemas criados com o schema.sql anterior)
--  * SEQ_TRANSCRICAO_PROTOCOLO + TRANSCRICOES.NUM_PROTOCOLO (numera as reunioes existentes por
--    ordem de criacao, depois vira DEFAULT NOT NULL UNIQUE)
--  * TRANSCRICAO_STATUS_HIST / ALERTA_STATUS_HIST (IDENTITY) + triggers de historico
--  * COMMENT ON em todas as tabelas e colunas
--  Idempotencia: nao e; rode uma unica vez. Para um schema novo use o schema.sql.
-- =====================================================================================

CREATE SEQUENCE SEQ_TRANSCRICAO_PROTOCOLO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

ALTER TABLE TRANSCRICOES ADD (NUM_PROTOCOLO NUMBER(10,0));

-- numera as reunioes ja cadastradas na ordem em que foram criadas
BEGIN
    FOR r IN (SELECT ID FROM TRANSCRICOES ORDER BY DATA_CRIACAO NULLS LAST, ID) LOOP
        UPDATE TRANSCRICOES SET NUM_PROTOCOLO = SEQ_TRANSCRICAO_PROTOCOLO.NEXTVAL WHERE ID = r.ID;
    END LOOP;
    COMMIT;
END;
/

ALTER TABLE TRANSCRICOES MODIFY (NUM_PROTOCOLO DEFAULT SEQ_TRANSCRICAO_PROTOCOLO.NEXTVAL NOT NULL);
ALTER TABLE TRANSCRICOES ADD CONSTRAINT UK_TRANSCRICOES_PROTOCOLO UNIQUE (NUM_PROTOCOLO);

CREATE TABLE TRANSCRICAO_STATUS_HIST (
    ID                NUMBER(19,0) GENERATED ALWAYS AS IDENTITY,
    TRANSCRICAO_ID    VARCHAR2(36 CHAR) NOT NULL,
    STATUS_ANTERIOR   VARCHAR2(20 CHAR),
    STATUS_NOVO       VARCHAR2(20 CHAR) NOT NULL CHECK (STATUS_NOVO IN ('PENDENTE','PROCESSANDO','PROCESSADA','ERRO')),
    ALTERADO_EM       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    ALTERADO_POR      VARCHAR2(100 CHAR) DEFAULT USER,
    OBSERVACAO        VARCHAR2(500 CHAR),
    CONSTRAINT PK_TRANSC_STATUS_HIST PRIMARY KEY (ID),
    CONSTRAINT FK_TRANSC_STATUS_HIST FOREIGN KEY (TRANSCRICAO_ID) REFERENCES TRANSCRICOES (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_TRANSC_STATUS_HIST ON TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, ALTERADO_EM);

CREATE TABLE ALERTA_STATUS_HIST (
    ID                NUMBER(19,0) GENERATED ALWAYS AS IDENTITY,
    ALERTA_ID         VARCHAR2(36 CHAR) NOT NULL,
    STATUS_ANTERIOR   VARCHAR2(20 CHAR),
    STATUS_NOVO       VARCHAR2(20 CHAR) NOT NULL CHECK (STATUS_NOVO IN ('NOVO','VISTO','RESOLVIDO')),
    ALTERADO_EM       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    ALTERADO_POR      VARCHAR2(100 CHAR) DEFAULT USER,
    OBSERVACAO        VARCHAR2(500 CHAR),
    CONSTRAINT PK_ALERTA_STATUS_HIST PRIMARY KEY (ID),
    CONSTRAINT FK_ALERTA_STATUS_HIST FOREIGN KEY (ALERTA_ID) REFERENCES ALERTAS (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_ALERTA_STATUS_HIST ON ALERTA_STATUS_HIST (ALERTA_ID, ALTERADO_EM);

-- registro de criacao para o historico das reunioes e alertas ja existentes
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
SELECT ID, NULL, NVL(STATUS, 'PENDENTE'), NVL(DATA_CRIACAO, SYSTIMESTAMP), 'MIGRACAO_003', 'Registro anterior a migracao 003'
  FROM TRANSCRICOES;
INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
SELECT ID, NULL, NVL(STATUS, 'NOVO'), NVL(DATA_CRIACAO, SYSTIMESTAMP), 'MIGRACAO_003', 'Registro anterior a migracao 003'
  FROM ALERTAS;
COMMIT;

-- Registra em TRANSCRICAO_STATUS_HIST toda mudanca de STATUS da transcricao
CREATE OR REPLACE TRIGGER TRG_TRANSCRICOES_STATUS_HIST
    AFTER UPDATE OF STATUS ON TRANSCRICOES
    FOR EACH ROW
    WHEN (NVL(OLD.STATUS, '-') <> NVL(NEW.STATUS, '-'))
BEGIN
    INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO)
    VALUES (:NEW.ID, :OLD.STATUS, :NEW.STATUS);
END;
/

-- Registra em ALERTA_STATUS_HIST toda mudanca de STATUS do alerta
CREATE OR REPLACE TRIGGER TRG_ALERTAS_STATUS_HIST
    AFTER UPDATE OF STATUS ON ALERTAS
    FOR EACH ROW
    WHEN (NVL(OLD.STATUS, '-') <> NVL(NEW.STATUS, '-'))
BEGIN
    INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO)
    VALUES (:NEW.ID, :OLD.STATUS, :NEW.STATUS);
END;
/

-- comentarios de todas as tabelas e colunas
COMMENT ON TABLE  APP_USERS IS 'Usuarios que acessam a plataforma (login por e-mail e senha com hash BCrypt)';
COMMENT ON COLUMN APP_USERS.ID IS 'Identificador UUID gerado pela aplicacao (chave primaria)';
COMMENT ON COLUMN APP_USERS.EMAIL IS 'E-mail de login; unico no sistema (UK_APP_USERS_EMAIL)';
COMMENT ON COLUMN APP_USERS.USER_NAME IS 'Nome de exibicao do usuario';
COMMENT ON COLUMN APP_USERS.PASSWORD_HASH IS 'Hash BCrypt da senha; a senha em claro nunca e armazenada';
COMMENT ON COLUMN APP_USERS.USER_ROLE IS 'Perfil de acesso: ADMIN (ve tudo) ou USER (ve apenas o que criou)';
COMMENT ON COLUMN APP_USERS.IS_ACTIVE IS '1 = usuario ativo (pode logar); 0 = desativado';
COMMENT ON COLUMN APP_USERS.CREATED_AT IS 'Data/hora de criacao do usuario';

COMMENT ON TABLE  REFRESH_TOKENS IS 'Sessoes de refresh do JWT (cookie HttpOnly). Apenas o hash SHA-256 do token e persistido; REVOKED_AT preenchido invalida o token';
COMMENT ON COLUMN REFRESH_TOKENS.ID IS 'Identificador UUID da sessao (chave primaria)';
COMMENT ON COLUMN REFRESH_TOKENS.USER_ID IS 'Usuario dono da sessao (APP_USERS.ID)';
COMMENT ON COLUMN REFRESH_TOKENS.TOKEN_HASH IS 'SHA-256 (hex) do refresh token; unico (UK_REFRESH_TOKENS_HASH)';
COMMENT ON COLUMN REFRESH_TOKENS.CREATED_AT IS 'Data/hora de emissao do token';
COMMENT ON COLUMN REFRESH_TOKENS.EXPIRES_AT IS 'Data/hora de expiracao do token';
COMMENT ON COLUMN REFRESH_TOKENS.REVOKED_AT IS 'Data/hora de revogacao (logout ou rotacao); NULL = token valido';

COMMENT ON TABLE  TRANSCRICOES IS 'Transcricao de reuniao comercial (manual, JSON/CSV TOTVS ou audio). 1 reuniao = 1 linha; o texto integral fica em CLOB';
COMMENT ON COLUMN TRANSCRICOES.ID IS 'Identificador UUID gerado pela aplicacao (chave primaria)';
COMMENT ON COLUMN TRANSCRICOES.NUM_PROTOCOLO IS 'Numero de protocolo sequencial e legivel (SEQ_TRANSCRICAO_PROTOCOLO); unico';
COMMENT ON COLUMN TRANSCRICOES.OWNER_ID IS 'Usuario que cadastrou/importou a reuniao (APP_USERS.ID)';
COMMENT ON COLUMN TRANSCRICOES.TEXTO_ORIGINAL IS 'Texto integral da transcricao usado na analise';
COMMENT ON COLUMN TRANSCRICOES.PARTICIPANTES IS 'Participantes informados (nomes ou LOCUTOR N quando anonimizado)';
COMMENT ON COLUMN TRANSCRICOES.DATA_REUNIAO IS 'Data/hora em que a reuniao aconteceu';
COMMENT ON COLUMN TRANSCRICOES.DURACAO_MINUTOS IS 'Duracao da reuniao em minutos';
COMMENT ON COLUMN TRANSCRICOES.CLIENTE IS 'Nome (ou codigo CODT) do cliente';
COMMENT ON COLUMN TRANSCRICOES.VENDEDOR IS 'Vendedor/consultor TOTVS responsavel pela reuniao';
COMMENT ON COLUMN TRANSCRICOES.ORIGEM_TRANSCRICAO IS 'Origem do texto: manual, json, csv ou parakeet (transcricao de audio)';
COMMENT ON COLUMN TRANSCRICOES.MEETING_ID IS 'ID_MEETING do JSON/CSV TOTVS; unico quando informado (NULL nao entra no indice unico)';
COMMENT ON COLUMN TRANSCRICOES.MEETING_DATE IS 'DT_MEETING original do arquivo TOTVS (texto, como veio na fonte)';
COMMENT ON COLUMN TRANSCRICOES.FORMATO IS 'FORMATO_MEETING do arquivo TOTVS (VIDEO, LINK...)';
COMMENT ON COLUMN TRANSCRICOES.MEETING_STATUS IS 'STATUS_MEETING do arquivo TOTVS (ex.: COMPLETED)';
COMMENT ON COLUMN TRANSCRICOES.MEETING_DURATION IS 'Duracao original no formato hh:mm:ss do arquivo TOTVS';
COMMENT ON COLUMN TRANSCRICOES.CODT IS 'Codigo do cliente na TOTVS (CODT)';
COMMENT ON COLUMN TRANSCRICOES.FLG_EXTERNO IS '1 = reuniao externa (com cliente); 0 = interna';
COMMENT ON COLUMN TRANSCRICOES.CREATED_AT_SOURCE IS 'DT_CRIACAO do registro na fonte TOTVS';
COMMENT ON COLUMN TRANSCRICOES.TRANSCRIPTION_TEXT IS 'Texto bruto da fonte (ANON_TRANSCRICAO ou saida do modelo de audio) antes de normalizacao';
COMMENT ON COLUMN TRANSCRICOES.SOURCE_FILE_NAME IS 'Nome do arquivo importado (JSON, CSV ou audio)';
COMMENT ON COLUMN TRANSCRICOES.IMPORT_BATCH_ID IS 'UUID do lote de importacao; agrupa as reunioes de um mesmo arquivo';
COMMENT ON COLUMN TRANSCRICOES.IMPORTED_AT IS 'Data/hora da importacao do arquivo';
COMMENT ON COLUMN TRANSCRICOES.MISSING_MEETING_ID IS '1 = a fonte nao trouxe ID_MEETING (MEETING_ID gerado pela aplicacao)';
COMMENT ON COLUMN TRANSCRICOES.AUDIO_FILE_NAME IS 'Nome do arquivo de audio quando a origem e transcricao de audio';
COMMENT ON COLUMN TRANSCRICOES.TRANSCRIPTION_MODEL IS 'Modelo usado na transcricao do audio (ex.: parakeet)';
COMMENT ON COLUMN TRANSCRICOES.TRANSCRIPTION_LANGUAGE IS 'Idioma detectado/informado na transcricao do audio (ex.: pt)';
COMMENT ON COLUMN TRANSCRICOES.AUDIO_DURATION_SECONDS IS 'Duracao do audio em segundos';
COMMENT ON COLUMN TRANSCRICOES.TRANSCRIPTION_PROCESSING_MS IS 'Tempo de processamento da transcricao do audio em milissegundos';
COMMENT ON COLUMN TRANSCRICOES.UF IS 'UF do cliente (metadado TOTVS)';
COMMENT ON COLUMN TRANSCRICOES.CNAE IS 'CNAE principal do cliente (metadado TOTVS)';
COMMENT ON COLUMN TRANSCRICOES.NOME_UNIDADE IS 'Unidade TOTVS responsavel pela conta (ex.: TOTVS SAUDE)';
COMMENT ON COLUMN TRANSCRICOES.NOME_SEGMENTO IS 'Segmento do cliente (SERVICOS, SAUDE, MANUFATURA, VAREJO...)';
COMMENT ON COLUMN TRANSCRICOES.FAIXA_FATURAMENTO IS 'Faixa de faturamento do cliente (FAIXA_FATURAMENTO_CLIENTE)';
COMMENT ON COLUMN TRANSCRICOES.TP_RECURSO IS 'Tipo do contato na fonte: lead, customer, VENDAS, SUPORTE, OPERACIONAL';
COMMENT ON COLUMN TRANSCRICOES.NOTA_NPS IS 'Nota NPS (0-10) informada pelo cliente na ultima pesquisa';
COMMENT ON COLUMN TRANSCRICOES.DT_ULTIMA_PESQUISA IS 'Competencia (AAAA-MM) da ultima pesquisa NPS';
COMMENT ON COLUMN TRANSCRICOES.STATUS IS 'Estado do processamento pela LeanIA: PENDENTE, PROCESSANDO, PROCESSADA, ERRO';
COMMENT ON COLUMN TRANSCRICOES.DATA_CRIACAO IS 'Data/hora de criacao do registro';
COMMENT ON COLUMN TRANSCRICOES.DATA_PROCESSAMENTO IS 'Data/hora em que a analise foi concluida';
COMMENT ON COLUMN TRANSCRICOES.MET_TOTAL_PARTES IS 'Metricas agregadas: quantidade de partes (turnos/registros) encontradas na reuniao';
COMMENT ON COLUMN TRANSCRICOES.MET_PARTES_ANALISADAS IS 'Metricas agregadas: partes efetivamente analisadas pela LeanIA';
COMMENT ON COLUMN TRANSCRICOES.MET_PARTES_NEGATIVAS IS 'Metricas agregadas: partes com sentimento negativo';
COMMENT ON COLUMN TRANSCRICOES.MET_PARTES_NEUTRAS IS 'Metricas agregadas: partes com sentimento neutro';
COMMENT ON COLUMN TRANSCRICOES.MET_PARTES_POSITIVAS IS 'Metricas agregadas: partes com sentimento positivo';
COMMENT ON COLUMN TRANSCRICOES.MET_RISCOS_DETECTADOS IS 'Metricas agregadas: total de riscos detectados';
COMMENT ON COLUMN TRANSCRICOES.MET_OPORTUNIDADES_DETECTADAS IS 'Metricas agregadas: total de oportunidades detectadas';
COMMENT ON COLUMN TRANSCRICOES.MET_CHURNS_POTENCIAIS IS 'Metricas agregadas: sinais de churn (cancelamento/troca) encontrados';
COMMENT ON COLUMN TRANSCRICOES.MET_MENCOES_CRITICAS IS 'Metricas agregadas: mencoes criticas (operacionais ou comerciais)';
COMMENT ON COLUMN TRANSCRICOES.MET_ALERTAS_CONSOLIDADOS IS 'Metricas agregadas: alertas consolidados gerados por recorrencia';
COMMENT ON COLUMN TRANSCRICOES.MET_CHUNKS_PROCESSADOS IS 'Metricas agregadas: blocos (chunks) de texto enviados a LeanIA';
COMMENT ON COLUMN TRANSCRICOES.RESUMO_EXECUTIVO IS 'Resumo executivo consolidado da reuniao (copiado da analise atual)';
COMMENT ON COLUMN TRANSCRICOES.ANALISE_ID IS 'Analise atual da transcricao (ANALISES.ID); unica e fica NULL quando a analise e removida';

COMMENT ON TABLE  TRANSCRICAO_WARNINGS IS 'Avisos gerados na importacao/transcricao da reuniao (ex.: MISSING_ID_MEETING, INVALID_DT_MEETING)';
COMMENT ON COLUMN TRANSCRICAO_WARNINGS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_WARNINGS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_WARNINGS.WARNING IS 'Codigo ou texto do aviso';

COMMENT ON TABLE  TRANSCRICAO_RISCOS_CONS IS 'Riscos consolidados da reuniao agrupados por categoria (copia da analise atual, usada pelo dashboard)';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.CATEGORIA IS 'Categoria do risco: Churn, Concorrencia, Insatisfacao, Perda de confianca...';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.SEVERIDADE IS 'Severidade consolidada: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.OCORRENCIAS IS 'Quantidade de vezes que o risco apareceu na reuniao';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.EVIDENCIAS IS 'Lista JSON de ate 5 trechos curtos que evidenciam o risco';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.RECOMENDACAO IS 'Recomendacao de acao comercial para mitigar o risco';

COMMENT ON TABLE  TRANSCRICAO_OPORT_CONS IS 'Oportunidades consolidadas da reuniao agrupadas por categoria (copia da analise atual, usada pelo dashboard)';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.CATEGORIA IS 'Categoria da oportunidade: Upsell, Cross-sell, Novo produto, Melhoria no atendimento...';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.SCORE IS 'Probabilidade estimada da oportunidade (0 a 1)';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.OCORRENCIAS IS 'Quantidade de sinais da oportunidade na reuniao';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.EVIDENCIAS IS 'Lista JSON de ate 5 trechos curtos que evidenciam a oportunidade';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.RECOMENDACAO IS 'Recomendacao de acao comercial para aproveitar a oportunidade';

COMMENT ON TABLE  TRANSCRICAO_ALERTAS_CONS IS 'Alertas consolidados da reuniao (titulo, categoria, severidade, motivo e recomendacao)';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.TITULO IS 'Titulo curto do alerta (ex.: Risco de churn)';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.CATEGORIA IS 'Categoria do alerta (Churn, Concorrencia, Sentimento negativo...)';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.OCORRENCIAS IS 'Quantidade de ocorrencias que originaram o alerta';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.MOTIVO IS 'Motivo do alerta em linguagem de negocio';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.RECOMENDACAO IS 'Acao recomendada ao time comercial';

COMMENT ON TABLE  TRANSCRICAO_ITENS_CRITICOS IS 'Trechos mais importantes da reuniao para revisao humana (evidencias de risco ou oportunidade)';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.TIPO IS 'Tipo do item: risco ou oportunidade';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.CATEGORIA IS 'Categoria do item (Churn, Concorrencia, Upsell...)';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.TRECHO IS 'Trecho literal da transcricao (ate 300 caracteres)';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';

COMMENT ON TABLE  TRANSCRICAO_STATUS_HIST IS 'Historico das mudancas de STATUS da transcricao (PENDENTE > PROCESSANDO > PROCESSADA/ERRO). Alimentado automaticamente pelo trigger TRG_TRANSCRICOES_STATUS_HIST';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.ID IS 'Identificador numerico gerado por IDENTITY (chave primaria)';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.TRANSCRICAO_ID IS 'Transcricao que mudou de status (TRANSCRICOES.ID)';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.STATUS_ANTERIOR IS 'Status antes da mudanca; NULL no registro de criacao';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.STATUS_NOVO IS 'Status depois da mudanca';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.ALTERADO_EM IS 'Data/hora da mudanca';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.ALTERADO_POR IS 'Usuario Oracle da sessao que fez a mudanca';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.OBSERVACAO IS 'Observacao livre (motivo do erro, reprocessamento, etc.)';

COMMENT ON TABLE  ANALISES IS 'Resultado da analise da LeanIA para uma transcricao: sentimento, persona, budget, scores, metricas, proxima acao e insights';
COMMENT ON COLUMN ANALISES.ID IS 'Identificador UUID gerado pela aplicacao (chave primaria)';
COMMENT ON COLUMN ANALISES.OWNER_ID IS 'Usuario dono da transcricao analisada (APP_USERS.ID)';
COMMENT ON COLUMN ANALISES.TRANSCRICAO_ID IS 'Transcricao analisada (TRANSCRICOES.ID)';
COMMENT ON COLUMN ANALISES.SENTIMENTO_GERAL IS 'Sentimento geral da conversa: POSITIVO, NEGATIVO, NEUTRO ou MISTO';
COMMENT ON COLUMN ANALISES.RESUMO IS 'Resumo textual da analise gerado pela LeanIA';
COMMENT ON COLUMN ANALISES.PERSONA_IDENTIFICADA IS 'Persona/cargo do interlocutor (ex.: Gestor de TI (influenciador tecnico))';
COMMENT ON COLUMN ANALISES.BUDGET_IDENTIFICADO IS 'Valor de budget mencionado na reuniao (ex.: R$ 25.000,00 por mes)';
COMMENT ON COLUMN ANALISES.SCORE_CONFIANCA_VENDEDOR IS 'Confianca do vendedor percebida na conversa (0 a 1)';
COMMENT ON COLUMN ANALISES.SCORE_SENTIMENTO IS 'Score numerico do sentimento (-1 negativo a +1 positivo)';
COMMENT ON COLUMN ANALISES.MET_TOTAL_PARTES IS 'Metricas agregadas: quantidade de partes (turnos/registros) encontradas na reuniao';
COMMENT ON COLUMN ANALISES.MET_PARTES_ANALISADAS IS 'Metricas agregadas: partes efetivamente analisadas pela LeanIA';
COMMENT ON COLUMN ANALISES.MET_PARTES_NEGATIVAS IS 'Metricas agregadas: partes com sentimento negativo';
COMMENT ON COLUMN ANALISES.MET_PARTES_NEUTRAS IS 'Metricas agregadas: partes com sentimento neutro';
COMMENT ON COLUMN ANALISES.MET_PARTES_POSITIVAS IS 'Metricas agregadas: partes com sentimento positivo';
COMMENT ON COLUMN ANALISES.MET_RISCOS_DETECTADOS IS 'Metricas agregadas: total de riscos detectados';
COMMENT ON COLUMN ANALISES.MET_OPORTUNIDADES_DETECTADAS IS 'Metricas agregadas: total de oportunidades detectadas';
COMMENT ON COLUMN ANALISES.MET_CHURNS_POTENCIAIS IS 'Metricas agregadas: sinais de churn (cancelamento/troca) encontrados';
COMMENT ON COLUMN ANALISES.MET_MENCOES_CRITICAS IS 'Metricas agregadas: mencoes criticas (operacionais ou comerciais)';
COMMENT ON COLUMN ANALISES.MET_ALERTAS_CONSOLIDADOS IS 'Metricas agregadas: alertas consolidados gerados por recorrencia';
COMMENT ON COLUMN ANALISES.MET_CHUNKS_PROCESSADOS IS 'Metricas agregadas: blocos (chunks) de texto enviados a LeanIA';
COMMENT ON COLUMN ANALISES.RESUMO_EXECUTIVO IS 'Resumo executivo para o diretor comercial';
COMMENT ON COLUMN ANALISES.DATA_ANALISE IS 'Data/hora em que a analise foi gerada';
COMMENT ON COLUMN ANALISES.PROXIMA_ACAO IS 'Proxima acao comercial sugerida pela LeanIA';
COMMENT ON COLUMN ANALISES.SENTIMENTO_DETALHE IS 'Sentimento por area, ex.: Misto (Satisfeito com Backoffice / Frustrado com RH)';
COMMENT ON COLUMN ANALISES.INSIGHTS_JSON IS 'JSON: elogios, reclamacoes, duvidasJornada, proximosPassos, observacoes';
COMMENT ON COLUMN ANALISES.INDICADORES_JSON IS 'JSON: locutores, participacao vendedor/cliente, perguntas, escuta ativa, qualidade da transcricao';

COMMENT ON TABLE  ANALISE_PRODUTOS IS 'Produtos identificados na reuniao: em uso (ATUAL), oportunidade ou concorrente';
COMMENT ON COLUMN ANALISE_PRODUTOS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_PRODUTOS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_PRODUTOS.NOME_PRODUTO IS 'Nome do produto (TOTVS Protheus, TOTVS RM, TOTVS Fluig, SAP, Senior, Omie...)';
COMMENT ON COLUMN ANALISE_PRODUTOS.TIPO IS 'ATUAL (cliente usa), OPORTUNIDADE (pode comprar) ou CONCORRENTE';
COMMENT ON COLUMN ANALISE_PRODUTOS.CONTEXTO IS 'Trecho da conversa em que o produto foi citado';

COMMENT ON TABLE  ANALISE_OPORTUNIDADES IS 'Oportunidades de venda detectadas na reuniao (upsell, cross-sell, novo produto)';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.TIPO IS 'UPSELL, CROSS_SELL ou NOVO_PRODUTO';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.DESCRICAO IS 'Descricao da oportunidade em linguagem de negocio';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.PRODUTO_RELACIONADO IS 'Produto/modulo TOTVS relacionado (ou A definir)';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.SCORE IS 'Probabilidade estimada de fechamento (0 a 1)';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.VALOR_ESTIMADO IS 'Valor estimado mencionado (ex.: R$ 50.000,00) ou Nao estimado';

COMMENT ON TABLE  ANALISE_RISCOS IS 'Riscos detectados na reuniao (churn, concorrencia, insatisfacao, perda de confianca)';
COMMENT ON COLUMN ANALISE_RISCOS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_RISCOS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_RISCOS.TIPO IS 'CHURN, CONCORRENCIA, INSATISFACAO ou PERDA_CONFIANCA';
COMMENT ON COLUMN ANALISE_RISCOS.DESCRICAO IS 'Descricao do risco com o trecho que o evidencia';
COMMENT ON COLUMN ANALISE_RISCOS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN ANALISE_RISCOS.CONCORRENTE_MENCIONADO IS 'Concorrente citado quando TIPO = CONCORRENCIA (SAP, Senior, Omie, Bling...)';

COMMENT ON TABLE  ANALISE_RISCOS_CONS IS 'Riscos consolidados por categoria gerados pela analise (origem da copia em TRANSCRICAO_RISCOS_CONS)';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.CATEGORIA IS 'Categoria do risco consolidado';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.SEVERIDADE IS 'Severidade consolidada: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.OCORRENCIAS IS 'Quantidade de ocorrencias do risco';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.EVIDENCIAS IS 'Lista JSON de trechos que evidenciam o risco';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.RECOMENDACAO IS 'Recomendacao de acao para mitigar o risco';

COMMENT ON TABLE  ANALISE_OPORT_CONS IS 'Oportunidades consolidadas por categoria geradas pela analise (origem da copia em TRANSCRICAO_OPORT_CONS)';
COMMENT ON COLUMN ANALISE_OPORT_CONS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_OPORT_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_OPORT_CONS.CATEGORIA IS 'Categoria da oportunidade consolidada';
COMMENT ON COLUMN ANALISE_OPORT_CONS.SCORE IS 'Probabilidade estimada (0 a 1)';
COMMENT ON COLUMN ANALISE_OPORT_CONS.OCORRENCIAS IS 'Quantidade de sinais da oportunidade';
COMMENT ON COLUMN ANALISE_OPORT_CONS.EVIDENCIAS IS 'Lista JSON de trechos que evidenciam a oportunidade';
COMMENT ON COLUMN ANALISE_OPORT_CONS.RECOMENDACAO IS 'Recomendacao de acao comercial';

COMMENT ON TABLE  ANALISE_ALERTAS_CONS IS 'Alertas consolidados gerados pela analise (origem da copia em TRANSCRICAO_ALERTAS_CONS e dos ALERTAS do tipo CONSOLIDADO_*)';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.TITULO IS 'Titulo curto do alerta';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.CATEGORIA IS 'Categoria do alerta';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.OCORRENCIAS IS 'Quantidade de ocorrencias';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.MOTIVO IS 'Motivo do alerta';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.RECOMENDACAO IS 'Acao recomendada';

COMMENT ON TABLE  ANALISE_ITENS_CRITICOS IS 'Trechos criticos apontados pela analise (origem da copia em TRANSCRICAO_ITENS_CRITICOS)';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.TIPO IS 'Tipo do item: risco ou oportunidade';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.CATEGORIA IS 'Categoria do item';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.TRECHO IS 'Trecho literal da transcricao';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';

COMMENT ON TABLE  ALERTAS IS 'Alertas acionaveis para o time comercial (churn, concorrente, oportunidade alta, consolidados) com ciclo NOVO > VISTO > RESOLVIDO';
COMMENT ON COLUMN ALERTAS.ID IS 'Identificador UUID gerado pela aplicacao (chave primaria)';
COMMENT ON COLUMN ALERTAS.OWNER_ID IS 'Usuario dono da transcricao que originou o alerta (APP_USERS.ID)';
COMMENT ON COLUMN ALERTAS.ANALISE_ID IS 'Analise que gerou o alerta (ANALISES.ID)';
COMMENT ON COLUMN ALERTAS.TIPO IS 'Tipo: RISCO_CHURN, RISCO_INSATISFACAO, RISCO_CONCORRENCIA, RISCO_PERDA_CONFIANCA, CONCORRENTE_DETECTADO, OPORTUNIDADE_ALTA, CHURN_IMINENTE, CONSOLIDADO_*';
COMMENT ON COLUMN ALERTAS.TITULO IS 'Titulo exibido no painel de alertas';
COMMENT ON COLUMN ALERTAS.CATEGORIA IS 'Categoria de negocio (Churn, Concorrencia, Sentimento negativo, OPORTUNIDADE_ALTA...)';
COMMENT ON COLUMN ALERTAS.MENSAGEM IS 'Mensagem completa do alerta com o trecho que o justifica';
COMMENT ON COLUMN ALERTAS.MOTIVO IS 'Motivo resumido (alertas consolidados)';
COMMENT ON COLUMN ALERTAS.RECOMENDACAO IS 'Acao recomendada (alertas consolidados)';
COMMENT ON COLUMN ALERTAS.OCORRENCIAS IS 'Quantidade de ocorrencias que originaram o alerta';
COMMENT ON COLUMN ALERTAS.PRIORIDADE IS 'Prioridade: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN ALERTAS.STATUS IS 'Ciclo de tratamento: NOVO, VISTO ou RESOLVIDO';
COMMENT ON COLUMN ALERTAS.DATA_CRIACAO IS 'Data/hora de geracao do alerta';

COMMENT ON TABLE  ALERTA_STATUS_HIST IS 'Historico das mudancas de STATUS do alerta (NOVO > VISTO > RESOLVIDO). Alimentado automaticamente pelo trigger TRG_ALERTAS_STATUS_HIST';
COMMENT ON COLUMN ALERTA_STATUS_HIST.ID IS 'Identificador numerico gerado por IDENTITY (chave primaria)';
COMMENT ON COLUMN ALERTA_STATUS_HIST.ALERTA_ID IS 'Alerta que mudou de status (ALERTAS.ID)';
COMMENT ON COLUMN ALERTA_STATUS_HIST.STATUS_ANTERIOR IS 'Status antes da mudanca; NULL no registro de criacao';
COMMENT ON COLUMN ALERTA_STATUS_HIST.STATUS_NOVO IS 'Status depois da mudanca';
COMMENT ON COLUMN ALERTA_STATUS_HIST.ALTERADO_EM IS 'Data/hora da mudanca';
COMMENT ON COLUMN ALERTA_STATUS_HIST.ALTERADO_POR IS 'Usuario Oracle da sessao que fez a mudanca';
COMMENT ON COLUMN ALERTA_STATUS_HIST.OBSERVACAO IS 'Observacao livre (quem tratou, resultado do contato, etc.)';

COMMENT ON TABLE  ANALISE_FEEDBACKS IS 'Avaliacao (util/ajuste) dada pelo usuario a uma analise. Alimenta o contexto adaptativo da LeanIA';
COMMENT ON COLUMN ANALISE_FEEDBACKS.ID IS 'Identificador UUID gerado pela aplicacao (chave primaria)';
COMMENT ON COLUMN ANALISE_FEEDBACKS.OWNER_ID IS 'Usuario que avaliou (APP_USERS.ID)';
COMMENT ON COLUMN ANALISE_FEEDBACKS.TRANSCRICAO_ID IS 'Transcricao avaliada (TRANSCRICOES.ID)';
COMMENT ON COLUMN ANALISE_FEEDBACKS.ANALISE_ID IS 'Analise avaliada (ANALISES.ID); fica NULL se a analise for refeita';
COMMENT ON COLUMN ANALISE_FEEDBACKS.UTIL IS '1 = analise considerada util; 0 = nao util';
COMMENT ON COLUMN ANALISE_FEEDBACKS.AJUSTE IS 'Texto livre com a correcao/ajuste sugerido pelo usuario';
COMMENT ON COLUMN ANALISE_FEEDBACKS.CREATED_AT IS 'Data/hora do feedback';

