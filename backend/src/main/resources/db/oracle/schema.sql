-- =====================================================================================
--  TOTVS Inteligencia Conversacional
--  Script DDL - Estrutura do banco de dados (Oracle Database 19c)
--
--  Equipe: Company 404
--     Bruno Gama Masseiras - RM562526
--     Kauai Rosa de Assis Rocha - RM563256
--     Lucas Gilmar Pedro da Silva - RM566231
--     Leandro Afonso Silva Santos Junior - RM561344
--     Murilo da Silva Basto - RM565445
--
--  Challenge FIAP x TOTVS 2026 - Database Design - Sprint 3
--
--  Conteudo: sequence, tabelas (CREATE TABLE), constraints (PK, FK, UNIQUE, CHECK, NOT NULL),
--            coluna IDENTITY, indices, triggers de historico e COMMENT em todas as tabelas
--            e colunas.
--
--  ARQUIVO GERADO por docs/database-design/sprint3/gerador (python build.py); nao edite a mao.
--  Os tipos seguem o que o Hibernate espera (spring.jpa.hibernate.ddl-auto=validate).
--  Como executar: SQL Developer (abrir o arquivo e rodar como script, F5) ou
--                 SQLcl: sql usuario/senha@host:1521/servico @01_DDL_COMPANY404_TOTVS.sql
--  Ordem: as tabelas sao criadas do pai para o filho; a FK circular entre TRANSCRICOES e
--         ANALISES (1:1) e adicionada com ALTER TABLE ao final da secao de tabelas.
--  Para recriar do zero, execute antes a secao "LIMPEZA" (comentada) no fim do arquivo.
-- =====================================================================================

-- =====================================================================================
--  1. SEQUENCES
--  Os identificadores principais (ID) sao UUIDs gerados pela aplicacao (compatibilidade
--  com o backend e com a migracao do MongoDB). A sequence abaixo da um numero de
--  protocolo legivel e sequencial para a transcricao; a coluna recebe o valor por DEFAULT.
--  As tabelas de historico usam coluna IDENTITY (autonumeracao nativa do Oracle 12c+).
-- =====================================================================================
-- Numero de protocolo sequencial e legivel das transcricoes (NUM_PROTOCOLO). O ID continua sendo UUID gerado pela aplicacao.
CREATE SEQUENCE SEQ_TRANSCRICAO_PROTOCOLO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;


-- =====================================================================================
--  2. TABELAS, CONSTRAINTS, INDICES E COMENTARIOS
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- SEGURANCA E ACESSO
-- -------------------------------------------------------------------------------------

-- Usuarios que acessam a plataforma (login por e-mail e senha com hash BCrypt)
CREATE TABLE APP_USERS (
    ID              VARCHAR2(36 CHAR) NOT NULL,
    EMAIL           VARCHAR2(320 CHAR) NOT NULL,
    USER_NAME       VARCHAR2(200 CHAR),
    PASSWORD_HASH   VARCHAR2(200 CHAR),
    USER_ROLE       VARCHAR2(20 CHAR) CHECK (USER_ROLE IN ('ADMIN','USER')),
    IS_ACTIVE       NUMBER(1,0) NOT NULL CHECK (IS_ACTIVE IN (0,1)),
    CREATED_AT      TIMESTAMP(6),
    CONSTRAINT PK_APP_USERS PRIMARY KEY (ID),
    CONSTRAINT UK_APP_USERS_EMAIL UNIQUE (EMAIL)
);

COMMENT ON TABLE  APP_USERS IS 'Usuarios que acessam a plataforma (login por e-mail e senha com hash BCrypt)';
COMMENT ON COLUMN APP_USERS.ID IS 'Identificador UUID gerado pela aplicacao (chave primaria)';
COMMENT ON COLUMN APP_USERS.EMAIL IS 'E-mail de login; unico no sistema (UK_APP_USERS_EMAIL)';
COMMENT ON COLUMN APP_USERS.USER_NAME IS 'Nome de exibicao do usuario';
COMMENT ON COLUMN APP_USERS.PASSWORD_HASH IS 'Hash BCrypt da senha; a senha em claro nunca e armazenada';
COMMENT ON COLUMN APP_USERS.USER_ROLE IS 'Perfil de acesso: ADMIN (ve tudo) ou USER (ve apenas o que criou)';
COMMENT ON COLUMN APP_USERS.IS_ACTIVE IS '1 = usuario ativo (pode logar); 0 = desativado';
COMMENT ON COLUMN APP_USERS.CREATED_AT IS 'Data/hora de criacao do usuario';

-- Sessoes de refresh do JWT (cookie HttpOnly). Apenas o hash SHA-256 do token e persistido; REVOKED_AT preenchido invalida o token
CREATE TABLE REFRESH_TOKENS (
    ID           VARCHAR2(36 CHAR) NOT NULL,
    USER_ID      VARCHAR2(36 CHAR) NOT NULL,
    TOKEN_HASH   VARCHAR2(64 CHAR) NOT NULL,
    CREATED_AT   TIMESTAMP(6),
    EXPIRES_AT   TIMESTAMP(6),
    REVOKED_AT   TIMESTAMP(6),
    CONSTRAINT PK_REFRESH_TOKENS PRIMARY KEY (ID),
    CONSTRAINT UK_REFRESH_TOKENS_HASH UNIQUE (TOKEN_HASH),
    CONSTRAINT FK_REFRESH_TOKENS_USER FOREIGN KEY (USER_ID) REFERENCES APP_USERS (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_REFRESH_TOKENS_USER ON REFRESH_TOKENS (USER_ID);

COMMENT ON TABLE  REFRESH_TOKENS IS 'Sessoes de refresh do JWT (cookie HttpOnly). Apenas o hash SHA-256 do token e persistido; REVOKED_AT preenchido invalida o token';
COMMENT ON COLUMN REFRESH_TOKENS.ID IS 'Identificador UUID da sessao (chave primaria)';
COMMENT ON COLUMN REFRESH_TOKENS.USER_ID IS 'Usuario dono da sessao (APP_USERS.ID)';
COMMENT ON COLUMN REFRESH_TOKENS.TOKEN_HASH IS 'SHA-256 (hex) do refresh token; unico (UK_REFRESH_TOKENS_HASH)';
COMMENT ON COLUMN REFRESH_TOKENS.CREATED_AT IS 'Data/hora de emissao do token';
COMMENT ON COLUMN REFRESH_TOKENS.EXPIRES_AT IS 'Data/hora de expiracao do token';
COMMENT ON COLUMN REFRESH_TOKENS.REVOKED_AT IS 'Data/hora de revogacao (logout ou rotacao); NULL = token valido';

-- -------------------------------------------------------------------------------------
-- TRANSCRICOES
-- -------------------------------------------------------------------------------------

-- Transcricao de reuniao comercial (manual, JSON/CSV TOTVS ou audio). 1 reuniao = 1 linha; o texto integral fica em CLOB
CREATE TABLE TRANSCRICOES (
    ID                             VARCHAR2(36 CHAR) NOT NULL,
    NUM_PROTOCOLO                  NUMBER(10,0) DEFAULT SEQ_TRANSCRICAO_PROTOCOLO.NEXTVAL NOT NULL,
    OWNER_ID                       VARCHAR2(36 CHAR),
    TEXTO_ORIGINAL                 CLOB,
    PARTICIPANTES                  VARCHAR2(1000 CHAR),
    DATA_REUNIAO                   TIMESTAMP(6),
    DURACAO_MINUTOS                NUMBER(10,0),
    CLIENTE                        VARCHAR2(200 CHAR),
    VENDEDOR                       VARCHAR2(200 CHAR),
    ORIGEM_TRANSCRICAO             VARCHAR2(30 CHAR),
    MEETING_ID                     VARCHAR2(100 CHAR),
    MEETING_DATE                   VARCHAR2(50 CHAR),
    FORMATO                        VARCHAR2(50 CHAR),
    MEETING_STATUS                 VARCHAR2(50 CHAR),
    MEETING_DURATION               VARCHAR2(30 CHAR),
    CODT                           VARCHAR2(50 CHAR),
    FLG_EXTERNO                    NUMBER(1,0) CHECK (FLG_EXTERNO IN (0,1)),
    CREATED_AT_SOURCE              VARCHAR2(50 CHAR),
    TRANSCRIPTION_TEXT             CLOB,
    SOURCE_FILE_NAME               VARCHAR2(200 CHAR),
    IMPORT_BATCH_ID                VARCHAR2(36 CHAR),
    IMPORTED_AT                    TIMESTAMP(6),
    MISSING_MEETING_ID             NUMBER(1,0) NOT NULL CHECK (MISSING_MEETING_ID IN (0,1)),
    AUDIO_FILE_NAME                VARCHAR2(200 CHAR),
    TRANSCRIPTION_MODEL            VARCHAR2(100 CHAR),
    TRANSCRIPTION_LANGUAGE         VARCHAR2(10 CHAR),
    AUDIO_DURATION_SECONDS         FLOAT(53),
    TRANSCRIPTION_PROCESSING_MS    NUMBER(10,0),
    UF                             VARCHAR2(10 CHAR),
    CNAE                           VARCHAR2(20 CHAR),
    NOME_UNIDADE                   VARCHAR2(200 CHAR),
    NOME_SEGMENTO                  VARCHAR2(200 CHAR),
    FAIXA_FATURAMENTO              VARCHAR2(200 CHAR),
    TP_RECURSO                     VARCHAR2(50 CHAR),
    NOTA_NPS                       NUMBER(10,0),
    DT_ULTIMA_PESQUISA             VARCHAR2(50 CHAR),
    STATUS                         VARCHAR2(20 CHAR) CHECK (STATUS IN ('PENDENTE','PROCESSANDO','PROCESSADA','ERRO')),
    DATA_CRIACAO                   TIMESTAMP(6),
    DATA_PROCESSAMENTO             TIMESTAMP(6),
    MET_TOTAL_PARTES               NUMBER(10,0),
    MET_PARTES_ANALISADAS          NUMBER(10,0),
    MET_PARTES_NEGATIVAS           NUMBER(10,0),
    MET_PARTES_NEUTRAS             NUMBER(10,0),
    MET_PARTES_POSITIVAS           NUMBER(10,0),
    MET_RISCOS_DETECTADOS          NUMBER(10,0),
    MET_OPORTUNIDADES_DETECTADAS   NUMBER(10,0),
    MET_CHURNS_POTENCIAIS          NUMBER(10,0),
    MET_MENCOES_CRITICAS           NUMBER(10,0),
    MET_ALERTAS_CONSOLIDADOS       NUMBER(10,0),
    MET_CHUNKS_PROCESSADOS         NUMBER(10,0),
    RESUMO_EXECUTIVO               CLOB,
    ANALISE_ID                     VARCHAR2(36 CHAR),
    CONSTRAINT PK_TRANSCRICOES PRIMARY KEY (ID),
    CONSTRAINT UK_TRANSCRICOES_MEETING_ID UNIQUE (MEETING_ID),
    CONSTRAINT UK_TRANSCRICOES_PROTOCOLO UNIQUE (NUM_PROTOCOLO),
    CONSTRAINT UK_TRANSCRICOES_ANALISE UNIQUE (ANALISE_ID),
    CONSTRAINT FK_TRANSCRICOES_OWNER FOREIGN KEY (OWNER_ID) REFERENCES APP_USERS (ID)
);

CREATE INDEX IDX_TRANSC_OWNER_CRIACAO ON TRANSCRICOES (OWNER_ID, DATA_CRIACAO);
CREATE INDEX IDX_TRANSC_OWNER_STATUS ON TRANSCRICOES (OWNER_ID, STATUS);
CREATE INDEX IDX_TRANSC_CLIENTE ON TRANSCRICOES (CLIENTE);
CREATE INDEX IDX_TRANSC_STATUS ON TRANSCRICOES (STATUS);
CREATE INDEX IDX_TRANSC_DATA_CRIACAO ON TRANSCRICOES (DATA_CRIACAO);
CREATE INDEX IDX_TRANSC_IMPORT_BATCH ON TRANSCRICOES (IMPORT_BATCH_ID);
CREATE INDEX IDX_TRANSC_SEGMENTO ON TRANSCRICOES (NOME_SEGMENTO);
CREATE INDEX IDX_TRANSC_UF ON TRANSCRICOES (UF);

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

-- Avisos gerados na importacao/transcricao da reuniao (ex.: MISSING_ID_MEETING, INVALID_DT_MEETING)
CREATE TABLE TRANSCRICAO_WARNINGS (
    TRANSCRICAO_ID   VARCHAR2(36 CHAR) NOT NULL,
    POSICAO          NUMBER(10,0) NOT NULL,
    WARNING          VARCHAR2(200 CHAR),
    CONSTRAINT PK_TRANSCRICAO_WARNINGS PRIMARY KEY (POSICAO, TRANSCRICAO_ID),
    CONSTRAINT FK_TRANSCRICAO_WARNINGS FOREIGN KEY (TRANSCRICAO_ID) REFERENCES TRANSCRICOES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  TRANSCRICAO_WARNINGS IS 'Avisos gerados na importacao/transcricao da reuniao (ex.: MISSING_ID_MEETING, INVALID_DT_MEETING)';
COMMENT ON COLUMN TRANSCRICAO_WARNINGS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_WARNINGS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_WARNINGS.WARNING IS 'Codigo ou texto do aviso';

-- Riscos consolidados da reuniao agrupados por categoria (copia da analise atual, usada pelo dashboard)
CREATE TABLE TRANSCRICAO_RISCOS_CONS (
    TRANSCRICAO_ID   VARCHAR2(36 CHAR) NOT NULL,
    POSICAO          NUMBER(10,0) NOT NULL,
    CATEGORIA        VARCHAR2(200 CHAR),
    SEVERIDADE       VARCHAR2(10 CHAR) CHECK (SEVERIDADE IN ('ALTO','MEDIO','BAIXO')),
    OCORRENCIAS      NUMBER(10,0),
    EVIDENCIAS       CLOB,
    RECOMENDACAO     CLOB,
    CONSTRAINT PK_TRANSC_RISCOS_CONS PRIMARY KEY (POSICAO, TRANSCRICAO_ID),
    CONSTRAINT FK_TRANSC_RISCOS_CONS FOREIGN KEY (TRANSCRICAO_ID) REFERENCES TRANSCRICOES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  TRANSCRICAO_RISCOS_CONS IS 'Riscos consolidados da reuniao agrupados por categoria (copia da analise atual, usada pelo dashboard)';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.CATEGORIA IS 'Categoria do risco: Churn, Concorrencia, Insatisfacao, Perda de confianca...';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.SEVERIDADE IS 'Severidade consolidada: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.OCORRENCIAS IS 'Quantidade de vezes que o risco apareceu na reuniao';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.EVIDENCIAS IS 'Lista JSON de ate 5 trechos curtos que evidenciam o risco';
COMMENT ON COLUMN TRANSCRICAO_RISCOS_CONS.RECOMENDACAO IS 'Recomendacao de acao comercial para mitigar o risco';

-- Oportunidades consolidadas da reuniao agrupadas por categoria (copia da analise atual, usada pelo dashboard)
CREATE TABLE TRANSCRICAO_OPORT_CONS (
    TRANSCRICAO_ID   VARCHAR2(36 CHAR) NOT NULL,
    POSICAO          NUMBER(10,0) NOT NULL,
    CATEGORIA        VARCHAR2(200 CHAR),
    SCORE            FLOAT(53),
    OCORRENCIAS      NUMBER(10,0),
    EVIDENCIAS       CLOB,
    RECOMENDACAO     CLOB,
    CONSTRAINT PK_TRANSC_OPORT_CONS PRIMARY KEY (POSICAO, TRANSCRICAO_ID),
    CONSTRAINT FK_TRANSC_OPORT_CONS FOREIGN KEY (TRANSCRICAO_ID) REFERENCES TRANSCRICOES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  TRANSCRICAO_OPORT_CONS IS 'Oportunidades consolidadas da reuniao agrupadas por categoria (copia da analise atual, usada pelo dashboard)';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.CATEGORIA IS 'Categoria da oportunidade: Upsell, Cross-sell, Novo produto, Melhoria no atendimento...';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.SCORE IS 'Probabilidade estimada da oportunidade (0 a 1)';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.OCORRENCIAS IS 'Quantidade de sinais da oportunidade na reuniao';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.EVIDENCIAS IS 'Lista JSON de ate 5 trechos curtos que evidenciam a oportunidade';
COMMENT ON COLUMN TRANSCRICAO_OPORT_CONS.RECOMENDACAO IS 'Recomendacao de acao comercial para aproveitar a oportunidade';

-- Alertas consolidados da reuniao (titulo, categoria, severidade, motivo e recomendacao)
CREATE TABLE TRANSCRICAO_ALERTAS_CONS (
    TRANSCRICAO_ID   VARCHAR2(36 CHAR) NOT NULL,
    POSICAO          NUMBER(10,0) NOT NULL,
    TITULO           VARCHAR2(500 CHAR),
    CATEGORIA        VARCHAR2(200 CHAR),
    SEVERIDADE       VARCHAR2(10 CHAR) CHECK (SEVERIDADE IN ('ALTO','MEDIO','BAIXO')),
    OCORRENCIAS      NUMBER(10,0),
    MOTIVO           CLOB,
    RECOMENDACAO     CLOB,
    CONSTRAINT PK_TRANSC_ALERTAS_CONS PRIMARY KEY (POSICAO, TRANSCRICAO_ID),
    CONSTRAINT FK_TRANSC_ALERTAS_CONS FOREIGN KEY (TRANSCRICAO_ID) REFERENCES TRANSCRICOES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  TRANSCRICAO_ALERTAS_CONS IS 'Alertas consolidados da reuniao (titulo, categoria, severidade, motivo e recomendacao)';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.TITULO IS 'Titulo curto do alerta (ex.: Risco de churn)';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.CATEGORIA IS 'Categoria do alerta (Churn, Concorrencia, Sentimento negativo...)';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.OCORRENCIAS IS 'Quantidade de ocorrencias que originaram o alerta';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.MOTIVO IS 'Motivo do alerta em linguagem de negocio';
COMMENT ON COLUMN TRANSCRICAO_ALERTAS_CONS.RECOMENDACAO IS 'Acao recomendada ao time comercial';

-- Trechos mais importantes da reuniao para revisao humana (evidencias de risco ou oportunidade)
CREATE TABLE TRANSCRICAO_ITENS_CRITICOS (
    TRANSCRICAO_ID   VARCHAR2(36 CHAR) NOT NULL,
    POSICAO          NUMBER(10,0) NOT NULL,
    TIPO             VARCHAR2(100 CHAR),
    CATEGORIA        VARCHAR2(200 CHAR),
    TRECHO           CLOB,
    SEVERIDADE       VARCHAR2(10 CHAR) CHECK (SEVERIDADE IN ('ALTO','MEDIO','BAIXO')),
    CONSTRAINT PK_TRANSC_ITENS_CRITICOS PRIMARY KEY (POSICAO, TRANSCRICAO_ID),
    CONSTRAINT FK_TRANSC_ITENS_CRITICOS FOREIGN KEY (TRANSCRICAO_ID) REFERENCES TRANSCRICOES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  TRANSCRICAO_ITENS_CRITICOS IS 'Trechos mais importantes da reuniao para revisao humana (evidencias de risco ou oportunidade)';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.TRANSCRICAO_ID IS 'Referencia a TRANSCRICOES.ID (dono da lista)';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.TIPO IS 'Tipo do item: risco ou oportunidade';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.CATEGORIA IS 'Categoria do item (Churn, Concorrencia, Upsell...)';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.TRECHO IS 'Trecho literal da transcricao (ate 300 caracteres)';
COMMENT ON COLUMN TRANSCRICAO_ITENS_CRITICOS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';

-- Historico das mudancas de STATUS da transcricao (PENDENTE > PROCESSANDO > PROCESSADA/ERRO). Alimentado automaticamente pelo trigger TRG_TRANSCRICOES_STATUS_HIST
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

COMMENT ON TABLE  TRANSCRICAO_STATUS_HIST IS 'Historico das mudancas de STATUS da transcricao (PENDENTE > PROCESSANDO > PROCESSADA/ERRO). Alimentado automaticamente pelo trigger TRG_TRANSCRICOES_STATUS_HIST';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.ID IS 'Identificador numerico gerado por IDENTITY (chave primaria)';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.TRANSCRICAO_ID IS 'Transcricao que mudou de status (TRANSCRICOES.ID)';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.STATUS_ANTERIOR IS 'Status antes da mudanca; NULL no registro de criacao';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.STATUS_NOVO IS 'Status depois da mudanca';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.ALTERADO_EM IS 'Data/hora da mudanca';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.ALTERADO_POR IS 'Usuario Oracle da sessao que fez a mudanca';
COMMENT ON COLUMN TRANSCRICAO_STATUS_HIST.OBSERVACAO IS 'Observacao livre (motivo do erro, reprocessamento, etc.)';

-- -------------------------------------------------------------------------------------
-- ANALISES
-- -------------------------------------------------------------------------------------

-- Resultado da analise da LeanIA para uma transcricao: sentimento, persona, budget, scores, metricas, proxima acao e insights
CREATE TABLE ANALISES (
    ID                             VARCHAR2(36 CHAR) NOT NULL,
    OWNER_ID                       VARCHAR2(36 CHAR),
    TRANSCRICAO_ID                 VARCHAR2(36 CHAR),
    SENTIMENTO_GERAL               VARCHAR2(20 CHAR) CHECK (SENTIMENTO_GERAL IN ('POSITIVO','NEGATIVO','NEUTRO','MISTO')),
    RESUMO                         CLOB,
    PERSONA_IDENTIFICADA           VARCHAR2(500 CHAR),
    BUDGET_IDENTIFICADO            VARCHAR2(500 CHAR),
    SCORE_CONFIANCA_VENDEDOR       FLOAT(53),
    SCORE_SENTIMENTO               FLOAT(53),
    MET_TOTAL_PARTES               NUMBER(10,0),
    MET_PARTES_ANALISADAS          NUMBER(10,0),
    MET_PARTES_NEGATIVAS           NUMBER(10,0),
    MET_PARTES_NEUTRAS             NUMBER(10,0),
    MET_PARTES_POSITIVAS           NUMBER(10,0),
    MET_RISCOS_DETECTADOS          NUMBER(10,0),
    MET_OPORTUNIDADES_DETECTADAS   NUMBER(10,0),
    MET_CHURNS_POTENCIAIS          NUMBER(10,0),
    MET_MENCOES_CRITICAS           NUMBER(10,0),
    MET_ALERTAS_CONSOLIDADOS       NUMBER(10,0),
    MET_CHUNKS_PROCESSADOS         NUMBER(10,0),
    RESUMO_EXECUTIVO               CLOB,
    DATA_ANALISE                   TIMESTAMP(6),
    PROXIMA_ACAO                   VARCHAR2(500 CHAR),
    SENTIMENTO_DETALHE             VARCHAR2(300 CHAR),
    INSIGHTS_JSON                  CLOB,
    INDICADORES_JSON               CLOB,
    CONSTRAINT PK_ANALISES PRIMARY KEY (ID),
    CONSTRAINT FK_ANALISES_OWNER FOREIGN KEY (OWNER_ID) REFERENCES APP_USERS (ID),
    CONSTRAINT FK_ANALISES_TRANSCRICAO FOREIGN KEY (TRANSCRICAO_ID) REFERENCES TRANSCRICOES (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_ANALISES_OWNER ON ANALISES (OWNER_ID);
CREATE INDEX IDX_ANALISES_TRANSCRICAO ON ANALISES (TRANSCRICAO_ID);
CREATE INDEX IDX_ANALISES_DATA ON ANALISES (DATA_ANALISE);

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

-- Produtos identificados na reuniao: em uso (ATUAL), oportunidade ou concorrente
CREATE TABLE ANALISE_PRODUTOS (
    ANALISE_ID     VARCHAR2(36 CHAR) NOT NULL,
    POSICAO        NUMBER(10,0) NOT NULL,
    NOME_PRODUTO   VARCHAR2(300 CHAR),
    TIPO           VARCHAR2(20 CHAR) CHECK (TIPO IN ('ATUAL','OPORTUNIDADE','CONCORRENTE')),
    CONTEXTO       CLOB,
    CONSTRAINT PK_ANALISE_PRODUTOS PRIMARY KEY (POSICAO, ANALISE_ID),
    CONSTRAINT FK_ANALISE_PRODUTOS FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  ANALISE_PRODUTOS IS 'Produtos identificados na reuniao: em uso (ATUAL), oportunidade ou concorrente';
COMMENT ON COLUMN ANALISE_PRODUTOS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_PRODUTOS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_PRODUTOS.NOME_PRODUTO IS 'Nome do produto (TOTVS Protheus, TOTVS RM, TOTVS Fluig, SAP, Senior, Omie...)';
COMMENT ON COLUMN ANALISE_PRODUTOS.TIPO IS 'ATUAL (cliente usa), OPORTUNIDADE (pode comprar) ou CONCORRENTE';
COMMENT ON COLUMN ANALISE_PRODUTOS.CONTEXTO IS 'Trecho da conversa em que o produto foi citado';

-- Oportunidades de venda detectadas na reuniao (upsell, cross-sell, novo produto)
CREATE TABLE ANALISE_OPORTUNIDADES (
    ANALISE_ID            VARCHAR2(36 CHAR) NOT NULL,
    POSICAO               NUMBER(10,0) NOT NULL,
    TIPO                  VARCHAR2(20 CHAR) CHECK (TIPO IN ('UPSELL','CROSS_SELL','NOVO_PRODUTO')),
    DESCRICAO             CLOB,
    PRODUTO_RELACIONADO   VARCHAR2(300 CHAR),
    SCORE                 FLOAT(53),
    VALOR_ESTIMADO        VARCHAR2(200 CHAR),
    CONSTRAINT PK_ANALISE_OPORTUNIDADES PRIMARY KEY (POSICAO, ANALISE_ID),
    CONSTRAINT FK_ANALISE_OPORTUNIDADES FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  ANALISE_OPORTUNIDADES IS 'Oportunidades de venda detectadas na reuniao (upsell, cross-sell, novo produto)';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.TIPO IS 'UPSELL, CROSS_SELL ou NOVO_PRODUTO';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.DESCRICAO IS 'Descricao da oportunidade em linguagem de negocio';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.PRODUTO_RELACIONADO IS 'Produto/modulo TOTVS relacionado (ou A definir)';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.SCORE IS 'Probabilidade estimada de fechamento (0 a 1)';
COMMENT ON COLUMN ANALISE_OPORTUNIDADES.VALOR_ESTIMADO IS 'Valor estimado mencionado (ex.: R$ 50.000,00) ou Nao estimado';

-- Riscos detectados na reuniao (churn, concorrencia, insatisfacao, perda de confianca)
CREATE TABLE ANALISE_RISCOS (
    ANALISE_ID               VARCHAR2(36 CHAR) NOT NULL,
    POSICAO                  NUMBER(10,0) NOT NULL,
    TIPO                     VARCHAR2(20 CHAR) CHECK (TIPO IN ('CHURN','CONCORRENCIA','INSATISFACAO','PERDA_CONFIANCA')),
    DESCRICAO                CLOB,
    SEVERIDADE               VARCHAR2(10 CHAR) CHECK (SEVERIDADE IN ('ALTO','MEDIO','BAIXO')),
    CONCORRENTE_MENCIONADO   VARCHAR2(300 CHAR),
    CONSTRAINT PK_ANALISE_RISCOS PRIMARY KEY (POSICAO, ANALISE_ID),
    CONSTRAINT FK_ANALISE_RISCOS FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  ANALISE_RISCOS IS 'Riscos detectados na reuniao (churn, concorrencia, insatisfacao, perda de confianca)';
COMMENT ON COLUMN ANALISE_RISCOS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_RISCOS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_RISCOS.TIPO IS 'CHURN, CONCORRENCIA, INSATISFACAO ou PERDA_CONFIANCA';
COMMENT ON COLUMN ANALISE_RISCOS.DESCRICAO IS 'Descricao do risco com o trecho que o evidencia';
COMMENT ON COLUMN ANALISE_RISCOS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN ANALISE_RISCOS.CONCORRENTE_MENCIONADO IS 'Concorrente citado quando TIPO = CONCORRENCIA (SAP, Senior, Omie, Bling...)';

-- Riscos consolidados por categoria gerados pela analise (origem da copia em TRANSCRICAO_RISCOS_CONS)
CREATE TABLE ANALISE_RISCOS_CONS (
    ANALISE_ID     VARCHAR2(36 CHAR) NOT NULL,
    POSICAO        NUMBER(10,0) NOT NULL,
    CATEGORIA      VARCHAR2(200 CHAR),
    SEVERIDADE     VARCHAR2(10 CHAR) CHECK (SEVERIDADE IN ('ALTO','MEDIO','BAIXO')),
    OCORRENCIAS    NUMBER(10,0),
    EVIDENCIAS     CLOB,
    RECOMENDACAO   CLOB,
    CONSTRAINT PK_ANALISE_RISCOS_CONS PRIMARY KEY (POSICAO, ANALISE_ID),
    CONSTRAINT FK_ANALISE_RISCOS_CONS FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  ANALISE_RISCOS_CONS IS 'Riscos consolidados por categoria gerados pela analise (origem da copia em TRANSCRICAO_RISCOS_CONS)';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.CATEGORIA IS 'Categoria do risco consolidado';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.SEVERIDADE IS 'Severidade consolidada: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.OCORRENCIAS IS 'Quantidade de ocorrencias do risco';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.EVIDENCIAS IS 'Lista JSON de trechos que evidenciam o risco';
COMMENT ON COLUMN ANALISE_RISCOS_CONS.RECOMENDACAO IS 'Recomendacao de acao para mitigar o risco';

-- Oportunidades consolidadas por categoria geradas pela analise (origem da copia em TRANSCRICAO_OPORT_CONS)
CREATE TABLE ANALISE_OPORT_CONS (
    ANALISE_ID     VARCHAR2(36 CHAR) NOT NULL,
    POSICAO        NUMBER(10,0) NOT NULL,
    CATEGORIA      VARCHAR2(200 CHAR),
    SCORE          FLOAT(53),
    OCORRENCIAS    NUMBER(10,0),
    EVIDENCIAS     CLOB,
    RECOMENDACAO   CLOB,
    CONSTRAINT PK_ANALISE_OPORT_CONS PRIMARY KEY (POSICAO, ANALISE_ID),
    CONSTRAINT FK_ANALISE_OPORT_CONS FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  ANALISE_OPORT_CONS IS 'Oportunidades consolidadas por categoria geradas pela analise (origem da copia em TRANSCRICAO_OPORT_CONS)';
COMMENT ON COLUMN ANALISE_OPORT_CONS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_OPORT_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_OPORT_CONS.CATEGORIA IS 'Categoria da oportunidade consolidada';
COMMENT ON COLUMN ANALISE_OPORT_CONS.SCORE IS 'Probabilidade estimada (0 a 1)';
COMMENT ON COLUMN ANALISE_OPORT_CONS.OCORRENCIAS IS 'Quantidade de sinais da oportunidade';
COMMENT ON COLUMN ANALISE_OPORT_CONS.EVIDENCIAS IS 'Lista JSON de trechos que evidenciam a oportunidade';
COMMENT ON COLUMN ANALISE_OPORT_CONS.RECOMENDACAO IS 'Recomendacao de acao comercial';

-- Alertas consolidados gerados pela analise (origem da copia em TRANSCRICAO_ALERTAS_CONS e dos ALERTAS do tipo CONSOLIDADO_*)
CREATE TABLE ANALISE_ALERTAS_CONS (
    ANALISE_ID     VARCHAR2(36 CHAR) NOT NULL,
    POSICAO        NUMBER(10,0) NOT NULL,
    TITULO         VARCHAR2(500 CHAR),
    CATEGORIA      VARCHAR2(200 CHAR),
    SEVERIDADE     VARCHAR2(10 CHAR) CHECK (SEVERIDADE IN ('ALTO','MEDIO','BAIXO')),
    OCORRENCIAS    NUMBER(10,0),
    MOTIVO         CLOB,
    RECOMENDACAO   CLOB,
    CONSTRAINT PK_ANALISE_ALERTAS_CONS PRIMARY KEY (POSICAO, ANALISE_ID),
    CONSTRAINT FK_ANALISE_ALERTAS_CONS FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  ANALISE_ALERTAS_CONS IS 'Alertas consolidados gerados pela analise (origem da copia em TRANSCRICAO_ALERTAS_CONS e dos ALERTAS do tipo CONSOLIDADO_*)';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.TITULO IS 'Titulo curto do alerta';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.CATEGORIA IS 'Categoria do alerta';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.OCORRENCIAS IS 'Quantidade de ocorrencias';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.MOTIVO IS 'Motivo do alerta';
COMMENT ON COLUMN ANALISE_ALERTAS_CONS.RECOMENDACAO IS 'Acao recomendada';

-- Trechos criticos apontados pela analise (origem da copia em TRANSCRICAO_ITENS_CRITICOS)
CREATE TABLE ANALISE_ITENS_CRITICOS (
    ANALISE_ID   VARCHAR2(36 CHAR) NOT NULL,
    POSICAO      NUMBER(10,0) NOT NULL,
    TIPO         VARCHAR2(100 CHAR),
    CATEGORIA    VARCHAR2(200 CHAR),
    TRECHO       CLOB,
    SEVERIDADE   VARCHAR2(10 CHAR) CHECK (SEVERIDADE IN ('ALTO','MEDIO','BAIXO')),
    CONSTRAINT PK_ANALISE_ITENS_CRITICOS PRIMARY KEY (POSICAO, ANALISE_ID),
    CONSTRAINT FK_ANALISE_ITENS_CRITICOS FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE CASCADE
);

COMMENT ON TABLE  ANALISE_ITENS_CRITICOS IS 'Trechos criticos apontados pela analise (origem da copia em TRANSCRICAO_ITENS_CRITICOS)';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.ANALISE_ID IS 'Referencia a ANALISES.ID (dono da lista)';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.POSICAO IS 'Posicao do item na lista (0..n); preserva a ordem gerada pela LeanIA';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.TIPO IS 'Tipo do item: risco ou oportunidade';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.CATEGORIA IS 'Categoria do item';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.TRECHO IS 'Trecho literal da transcricao';
COMMENT ON COLUMN ANALISE_ITENS_CRITICOS.SEVERIDADE IS 'Severidade: ALTO, MEDIO ou BAIXO';

-- -------------------------------------------------------------------------------------
-- ALERTAS E FEEDBACK
-- -------------------------------------------------------------------------------------

-- Alertas acionaveis para o time comercial (churn, concorrente, oportunidade alta, consolidados) com ciclo NOVO > VISTO > RESOLVIDO
CREATE TABLE ALERTAS (
    ID             VARCHAR2(36 CHAR) NOT NULL,
    OWNER_ID       VARCHAR2(36 CHAR),
    ANALISE_ID     VARCHAR2(36 CHAR),
    TIPO           VARCHAR2(100 CHAR),
    TITULO         VARCHAR2(500 CHAR),
    CATEGORIA      VARCHAR2(200 CHAR),
    MENSAGEM       CLOB,
    MOTIVO         CLOB,
    RECOMENDACAO   CLOB,
    OCORRENCIAS    NUMBER(10,0),
    PRIORIDADE     VARCHAR2(10 CHAR) CHECK (PRIORIDADE IN ('ALTO','MEDIO','BAIXO')),
    STATUS         VARCHAR2(20 CHAR) CHECK (STATUS IN ('NOVO','VISTO','RESOLVIDO')),
    DATA_CRIACAO   TIMESTAMP(6),
    CONSTRAINT PK_ALERTAS PRIMARY KEY (ID),
    CONSTRAINT FK_ALERTAS_OWNER FOREIGN KEY (OWNER_ID) REFERENCES APP_USERS (ID),
    CONSTRAINT FK_ALERTAS_ANALISE FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_ALERTAS_STATUS_PRIO_DATA ON ALERTAS (STATUS, PRIORIDADE, DATA_CRIACAO);
CREATE INDEX IDX_ALERTAS_OWNER_STATUS ON ALERTAS (OWNER_ID, STATUS, PRIORIDADE, DATA_CRIACAO);
CREATE INDEX IDX_ALERTAS_ANALISE ON ALERTAS (ANALISE_ID);

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

-- Historico das mudancas de STATUS do alerta (NOVO > VISTO > RESOLVIDO). Alimentado automaticamente pelo trigger TRG_ALERTAS_STATUS_HIST
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

COMMENT ON TABLE  ALERTA_STATUS_HIST IS 'Historico das mudancas de STATUS do alerta (NOVO > VISTO > RESOLVIDO). Alimentado automaticamente pelo trigger TRG_ALERTAS_STATUS_HIST';
COMMENT ON COLUMN ALERTA_STATUS_HIST.ID IS 'Identificador numerico gerado por IDENTITY (chave primaria)';
COMMENT ON COLUMN ALERTA_STATUS_HIST.ALERTA_ID IS 'Alerta que mudou de status (ALERTAS.ID)';
COMMENT ON COLUMN ALERTA_STATUS_HIST.STATUS_ANTERIOR IS 'Status antes da mudanca; NULL no registro de criacao';
COMMENT ON COLUMN ALERTA_STATUS_HIST.STATUS_NOVO IS 'Status depois da mudanca';
COMMENT ON COLUMN ALERTA_STATUS_HIST.ALTERADO_EM IS 'Data/hora da mudanca';
COMMENT ON COLUMN ALERTA_STATUS_HIST.ALTERADO_POR IS 'Usuario Oracle da sessao que fez a mudanca';
COMMENT ON COLUMN ALERTA_STATUS_HIST.OBSERVACAO IS 'Observacao livre (quem tratou, resultado do contato, etc.)';

-- Avaliacao (util/ajuste) dada pelo usuario a uma analise. Alimenta o contexto adaptativo da LeanIA
CREATE TABLE ANALISE_FEEDBACKS (
    ID               VARCHAR2(36 CHAR) NOT NULL,
    OWNER_ID         VARCHAR2(36 CHAR),
    TRANSCRICAO_ID   VARCHAR2(36 CHAR),
    ANALISE_ID       VARCHAR2(36 CHAR),
    UTIL             NUMBER(1,0) NOT NULL CHECK (UTIL IN (0,1)),
    AJUSTE           CLOB,
    CREATED_AT       TIMESTAMP(6),
    CONSTRAINT PK_ANALISE_FEEDBACKS PRIMARY KEY (ID),
    CONSTRAINT FK_FEEDBACKS_OWNER FOREIGN KEY (OWNER_ID) REFERENCES APP_USERS (ID),
    CONSTRAINT FK_FEEDBACKS_TRANSCRICAO FOREIGN KEY (TRANSCRICAO_ID) REFERENCES TRANSCRICOES (ID) ON DELETE CASCADE,
    CONSTRAINT FK_FEEDBACKS_ANALISE FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE SET NULL
);

CREATE INDEX IDX_FEEDBACK_OWNER ON ANALISE_FEEDBACKS (OWNER_ID);
CREATE INDEX IDX_FEEDBACK_TRANSCRICAO ON ANALISE_FEEDBACKS (TRANSCRICAO_ID);
CREATE INDEX IDX_FEEDBACK_ANALISE ON ANALISE_FEEDBACKS (ANALISE_ID);
CREATE INDEX IDX_FEEDBACK_CREATED ON ANALISE_FEEDBACKS (CREATED_AT);

COMMENT ON TABLE  ANALISE_FEEDBACKS IS 'Avaliacao (util/ajuste) dada pelo usuario a uma analise. Alimenta o contexto adaptativo da LeanIA';
COMMENT ON COLUMN ANALISE_FEEDBACKS.ID IS 'Identificador UUID gerado pela aplicacao (chave primaria)';
COMMENT ON COLUMN ANALISE_FEEDBACKS.OWNER_ID IS 'Usuario que avaliou (APP_USERS.ID)';
COMMENT ON COLUMN ANALISE_FEEDBACKS.TRANSCRICAO_ID IS 'Transcricao avaliada (TRANSCRICOES.ID)';
COMMENT ON COLUMN ANALISE_FEEDBACKS.ANALISE_ID IS 'Analise avaliada (ANALISES.ID); fica NULL se a analise for refeita';
COMMENT ON COLUMN ANALISE_FEEDBACKS.UTIL IS '1 = analise considerada util; 0 = nao util';
COMMENT ON COLUMN ANALISE_FEEDBACKS.AJUSTE IS 'Texto livre com a correcao/ajuste sugerido pelo usuario';
COMMENT ON COLUMN ANALISE_FEEDBACKS.CREATED_AT IS 'Data/hora do feedback';

-- -------------------------------------------------------------------------------------
-- FK CIRCULAR (1:1): a transcricao aponta para a sua analise atual e a analise aponta
-- para a transcricao. A restricao so pode ser criada depois que as duas tabelas existem.
-- ON DELETE SET NULL: ao apagar a analise, a transcricao continua existindo sem analise.
-- -------------------------------------------------------------------------------------
ALTER TABLE TRANSCRICOES ADD CONSTRAINT FK_TRANSCRICOES_ANALISE
    FOREIGN KEY (ANALISE_ID) REFERENCES ANALISES (ID) ON DELETE SET NULL;

-- =====================================================================================
--  3. TRIGGERS DE HISTORICO
--  Toda mudanca de STATUS em TRANSCRICOES e ALERTAS gera uma linha nas tabelas de
--  historico (auditoria da evolucao do processo). O ID do historico vem de IDENTITY.
-- =====================================================================================
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


-- =====================================================================================
--  4. CONFERENCIA
-- =====================================================================================
SELECT object_type, COUNT(*) AS quantidade
  FROM user_objects
 WHERE object_type IN ('TABLE', 'SEQUENCE', 'TRIGGER', 'INDEX')
 GROUP BY object_type
 ORDER BY object_type;

SELECT table_name, comments FROM user_tab_comments WHERE comments IS NOT NULL ORDER BY table_name;

/* =====================================================================================
   LIMPEZA (opcional) - remove todos os objetos deste script para recriar do zero.
   Descomente e execute antes do DDL caso o schema ja contenha as tabelas.
   DROP TABLE ANALISE_FEEDBACKS CASCADE CONSTRAINTS PURGE;
   DROP TABLE ALERTA_STATUS_HIST CASCADE CONSTRAINTS PURGE;
   DROP TABLE ALERTAS CASCADE CONSTRAINTS PURGE;
   DROP TABLE ANALISE_ITENS_CRITICOS CASCADE CONSTRAINTS PURGE;
   DROP TABLE ANALISE_ALERTAS_CONS CASCADE CONSTRAINTS PURGE;
   DROP TABLE ANALISE_OPORT_CONS CASCADE CONSTRAINTS PURGE;
   DROP TABLE ANALISE_RISCOS_CONS CASCADE CONSTRAINTS PURGE;
   DROP TABLE ANALISE_RISCOS CASCADE CONSTRAINTS PURGE;
   DROP TABLE ANALISE_OPORTUNIDADES CASCADE CONSTRAINTS PURGE;
   DROP TABLE ANALISE_PRODUTOS CASCADE CONSTRAINTS PURGE;
   DROP TABLE ANALISES CASCADE CONSTRAINTS PURGE;
   DROP TABLE TRANSCRICAO_STATUS_HIST CASCADE CONSTRAINTS PURGE;
   DROP TABLE TRANSCRICAO_ITENS_CRITICOS CASCADE CONSTRAINTS PURGE;
   DROP TABLE TRANSCRICAO_ALERTAS_CONS CASCADE CONSTRAINTS PURGE;
   DROP TABLE TRANSCRICAO_OPORT_CONS CASCADE CONSTRAINTS PURGE;
   DROP TABLE TRANSCRICAO_RISCOS_CONS CASCADE CONSTRAINTS PURGE;
   DROP TABLE TRANSCRICAO_WARNINGS CASCADE CONSTRAINTS PURGE;
   DROP TABLE TRANSCRICOES CASCADE CONSTRAINTS PURGE;
   DROP TABLE REFRESH_TOKENS CASCADE CONSTRAINTS PURGE;
   DROP TABLE APP_USERS CASCADE CONSTRAINTS PURGE;
   DROP SEQUENCE SEQ_TRANSCRICAO_PROTOCOLO;
   ===================================================================================== */
