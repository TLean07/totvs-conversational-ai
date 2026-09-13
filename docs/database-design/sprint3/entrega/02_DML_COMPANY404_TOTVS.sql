-- =====================================================================================
--  TOTVS Inteligencia Conversacional
--  Script DML - Carga de dados (INSERT), evolucao dos processos (UPDATE) e exclusoes (DELETE)
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
--  Pre-requisito: executar antes o script 01_DDL (tabelas, sequence, triggers).
--  Os identificadores (UUID) sao fixos para o script ser legivel e reexecutavel.
--  Os dados sao ficticios: empresas, pessoas e reunioes foram inventadas para o Challenge,
--  seguindo o vocabulario real da solucao (produtos TOTVS, concorrentes, tipos de alerta).
--
--  Secoes:
--    1. INSERT  - popula todas as 20 tabelas (minimo de 5 registros por tabela)
--    2. UPDATE  - evolucao da transcricao (PENDENTE > PROCESSANDO > PROCESSADA / ERRO),
--                 do alerta (NOVO > VISTO > RESOLVIDO), do feedback e das sessoes
--    3. DELETE  - exclusoes com integridade referencial (ON DELETE CASCADE / SET NULL)
--    4. CONFERENCIA - contagem de registros por tabela
-- =====================================================================================

-- =====================================================================================
--  1. INSERT - CARGA INICIAL
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- APP_USERS - 5 usuarios (1 ADMIN, 4 USER; 1 desativado). Senha de todos: Senha@123 (hash BCrypt)
-- -------------------------------------------------------------------------------------
INSERT INTO APP_USERS (ID, EMAIL, USER_NAME, USER_ROLE, IS_ACTIVE, CREATED_AT, PASSWORD_HASH)
VALUES ('aaaaaaaa-0000-4000-8000-000000000001', 'diretoria.comercial@company404.com.br', 'Helena Prado (Diretora Comercial)', 'ADMIN', 1, TO_TIMESTAMP('2026-07-01 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), '$2b$10$KaMclFmqsp2mIPTj8pv8su6Dp4uWI2q9/KlTqW963mOtkD4CUlK/K');
INSERT INTO APP_USERS (ID, EMAIL, USER_NAME, USER_ROLE, IS_ACTIVE, CREATED_AT, PASSWORD_HASH)
VALUES ('aaaaaaaa-0000-4000-8000-000000000002', 'rafael.moura@company404.com.br', 'Rafael Moura (Account Executive)', 'USER', 1, TO_TIMESTAMP('2026-07-01 08:05:00', 'YYYY-MM-DD HH24:MI:SS'), '$2b$10$KaMclFmqsp2mIPTj8pv8su6Dp4uWI2q9/KlTqW963mOtkD4CUlK/K');
INSERT INTO APP_USERS (ID, EMAIL, USER_NAME, USER_ROLE, IS_ACTIVE, CREATED_AT, PASSWORD_HASH)
VALUES ('aaaaaaaa-0000-4000-8000-000000000003', 'juliana.castro@company404.com.br', 'Juliana Castro (Gerente de Vendas)', 'USER', 1, TO_TIMESTAMP('2026-07-01 08:10:00', 'YYYY-MM-DD HH24:MI:SS'), '$2b$10$KaMclFmqsp2mIPTj8pv8su6Dp4uWI2q9/KlTqW963mOtkD4CUlK/K');
INSERT INTO APP_USERS (ID, EMAIL, USER_NAME, USER_ROLE, IS_ACTIVE, CREATED_AT, PASSWORD_HASH)
VALUES ('aaaaaaaa-0000-4000-8000-000000000004', 'marcos.lima@company404.com.br', 'Marcos Lima (CS Manager)', 'USER', 1, TO_TIMESTAMP('2026-07-02 09:00:00', 'YYYY-MM-DD HH24:MI:SS'), '$2b$10$KaMclFmqsp2mIPTj8pv8su6Dp4uWI2q9/KlTqW963mOtkD4CUlK/K');
INSERT INTO APP_USERS (ID, EMAIL, USER_NAME, USER_ROLE, IS_ACTIVE, CREATED_AT, PASSWORD_HASH)
VALUES ('aaaaaaaa-0000-4000-8000-000000000005', 'patricia.nunes@company404.com.br', 'Patricia Nunes (Analista de BI)', 'USER', 0, TO_TIMESTAMP('2026-07-02 09:30:00', 'YYYY-MM-DD HH24:MI:SS'), '$2b$10$KaMclFmqsp2mIPTj8pv8su6Dp4uWI2q9/KlTqW963mOtkD4CUlK/K');

-- -------------------------------------------------------------------------------------
-- REFRESH_TOKENS - 6 sessoes (1 revogada, 1 expirada)
-- -------------------------------------------------------------------------------------
INSERT INTO REFRESH_TOKENS (ID, USER_ID, TOKEN_HASH, CREATED_AT, EXPIRES_AT, REVOKED_AT)
VALUES ('bbbbbbbb-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000001', '3f1c9b6a2e7d4c5b8a9f0e1d2c3b4a5968778695a4b3c2d1e0f9a8b7c6d5e4f3', TO_TIMESTAMP('2026-09-01 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-09-08 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO REFRESH_TOKENS (ID, USER_ID, TOKEN_HASH, CREATED_AT, EXPIRES_AT, REVOKED_AT)
VALUES ('bbbbbbbb-0000-4000-8000-000000000002', 'aaaaaaaa-0000-4000-8000-000000000002', '8b2d4f6a1c3e5a7b9d0f2e4c6a8b0d1f3e5a7c9b1d3f5a7c9e1b3d5f7a9c1e3b', TO_TIMESTAMP('2026-09-01 09:15:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-09-08 09:15:00', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO REFRESH_TOKENS (ID, USER_ID, TOKEN_HASH, CREATED_AT, EXPIRES_AT, REVOKED_AT)
VALUES ('bbbbbbbb-0000-4000-8000-000000000003', 'aaaaaaaa-0000-4000-8000-000000000002', 'c4e6a8b0d2f4a6c8e0b2d4f6a8c0e2b4d6f8a0c2e4b6d8f0a2c4e6b8d0f2a4c6', TO_TIMESTAMP('2026-08-20 10:00:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-08-27 10:00:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-08-21 18:40:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO REFRESH_TOKENS (ID, USER_ID, TOKEN_HASH, CREATED_AT, EXPIRES_AT, REVOKED_AT)
VALUES ('bbbbbbbb-0000-4000-8000-000000000004', 'aaaaaaaa-0000-4000-8000-000000000003', 'd5f7a9b1c3e5a7b9d1f3a5c7e9b1d3f5a7c9e1b3d5f7a9c1e3b5d7f9a1c3e5b7', TO_TIMESTAMP('2026-09-02 07:50:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-09-09 07:50:00', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO REFRESH_TOKENS (ID, USER_ID, TOKEN_HASH, CREATED_AT, EXPIRES_AT, REVOKED_AT)
VALUES ('bbbbbbbb-0000-4000-8000-000000000005', 'aaaaaaaa-0000-4000-8000-000000000004', 'e6a8b0c2d4f6a8b0c2e4d6f8a0b2c4e6d8f0a2b4c6e8d0f2a4b6c8e0d2f4a6b8', TO_TIMESTAMP('2026-08-10 14:00:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-08-17 14:00:00', 'YYYY-MM-DD HH24:MI:SS'), NULL);
INSERT INTO REFRESH_TOKENS (ID, USER_ID, TOKEN_HASH, CREATED_AT, EXPIRES_AT, REVOKED_AT)
VALUES ('bbbbbbbb-0000-4000-8000-000000000006', 'aaaaaaaa-0000-4000-8000-000000000003', 'f7b9c1d3e5a7b9c1d3f5a7b9c1e3d5f7a9b1c3e5d7f9a1b3c5e7d9f1a3b5c7d9', TO_TIMESTAMP('2026-09-05 11:20:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-09-12 11:20:00', 'YYYY-MM-DD HH24:MI:SS'), NULL);

-- -------------------------------------------------------------------------------------
-- TRANSCRICOES - 9 reunioes (6 PROCESSADA, 1 PENDENTE, 1 PROCESSANDO, 1 ERRO). NUM_PROTOCOLO vem da sequence por DEFAULT.
--   ANALISE_ID fica NULL aqui (FK circular 1:1) e e preenchido depois que as analises existirem
-- -------------------------------------------------------------------------------------
INSERT INTO TRANSCRICOES (ID, MISSING_MEETING_ID, STATUS, OWNER_ID, CLIENTE, VENDEDOR, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, TEXTO_ORIGINAL, ORIGEM_TRANSCRICAO, MEETING_ID, MEETING_DATE, FORMATO, MEETING_STATUS, MEETING_DURATION, CODT, FLG_EXTERNO, CREATED_AT_SOURCE, TRANSCRIPTION_TEXT, SOURCE_FILE_NAME, IMPORT_BATCH_ID, IMPORTED_AT, UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO, NOTA_NPS, DT_ULTIMA_PESQUISA, DATA_CRIACAO, DATA_PROCESSAMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO)
VALUES ('cccccccc-0000-4000-8000-000000000001', 0, 'PROCESSADA', 'aaaaaaaa-0000-4000-8000-000000000002', 'Metalurgica Andrade', 'Rafael Moura', 'LOCUTOR 1, LOCUTOR 2, LOCUTOR 3', TO_TIMESTAMP('2026-08-04 09:30:00', 'YYYY-MM-DD HH24:MI:SS'), 52, '[LOCUTOR 1]: Bom dia, pessoal. Hoje queria revisar o uso do Protheus no financeiro. [LOCUTOR 2]: O modulo financeiro atende bem, mas a aprovacao de compras ainda e por e-mail. [LOCUTOR 1]: O Fluig resolve exatamente esse fluxo de aprovacao. [LOCUTOR 2]: Interessante, precisamos de uma proposta ate o fim do mes. [LOCUTOR 3]: O orcamento previsto para automacao e de uns R$ 60 mil.', 'json', '2026080401', '2026-08-04 09:30:00', 'VIDEO', 'COMPLETED', '00:52:10', 'TFAND1', 1, '2026-08-04 12:05:44', '[LOCUTOR 1]: Bom dia, pessoal. Hoje queria revisar o uso do Protheus no financeiro...', 'ANON_transcricao_agosto.json', '99999999-0000-4000-8000-000000000001', TO_TIMESTAMP('2026-08-05 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'RS', '2511000', 'TOTVS RIO GRANDE DO SUL', 'MANUFATURA', '04.SMB I - De R$ 20.000.001 a R$ 60.000.000', 'customer', 8, '2026-06', TO_TIMESTAMP('2026-08-05 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-08-05 08:03:12', 'YYYY-MM-DD HH24:MI:SS'), 3, 3, 1, 1, 1, 1, 1, 0, 1, 1, 1, 'Cliente satisfeito com o Protheus e com forte interesse no Fluig para aprovacoes. Budget de R$ 60 mil. Enviar proposta ate o fim do mes.');
INSERT INTO TRANSCRICOES (ID, MISSING_MEETING_ID, STATUS, OWNER_ID, CLIENTE, VENDEDOR, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, TEXTO_ORIGINAL, ORIGEM_TRANSCRICAO, MEETING_ID, MEETING_DATE, FORMATO, MEETING_STATUS, MEETING_DURATION, CODT, FLG_EXTERNO, CREATED_AT_SOURCE, TRANSCRIPTION_TEXT, SOURCE_FILE_NAME, IMPORT_BATCH_ID, IMPORTED_AT, UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO, NOTA_NPS, DT_ULTIMA_PESQUISA, DATA_CRIACAO, DATA_PROCESSAMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO)
VALUES ('cccccccc-0000-4000-8000-000000000002', 0, 'PROCESSADA', 'aaaaaaaa-0000-4000-8000-000000000002', 'Clinica Vida Plena', 'Rafael Moura', 'LOCUTOR 1, LOCUTOR 2', TO_TIMESTAMP('2026-08-06 15:00:00', 'YYYY-MM-DD HH24:MI:SS'), 38, '[LOCUTOR 2]: Estamos ha tres semanas com o chamado do RM aberto e ninguem retorna. [LOCUTOR 1]: Entendo a frustracao, vou escalar hoje. [LOCUTOR 2]: A Senior ja nos apresentou uma proposta e a diretoria esta pensando em trocar. Se nao resolver ate o fim do mes, vamos cancelar o contrato.', 'json', '2026080602', '2026-08-06 15:00:00', 'VIDEO', 'COMPLETED', '00:38:44', 'TFVID2', 1, '2026-08-06 17:20:10', '[LOCUTOR 2]: Estamos ha tres semanas com o chamado do RM aberto e ninguem retorna...', 'ANON_transcricao_agosto.json', '99999999-0000-4000-8000-000000000001', TO_TIMESTAMP('2026-08-05 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'SP', '8630501', 'TOTVS SAUDE', 'SAUDE', '02.MICRO II - De R$ 500.001 a R$ 10.000.000', 'customer', 3, '2026-07', TO_TIMESTAMP('2026-08-05 08:00:01', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-08-05 08:04:40', 'YYYY-MM-DD HH24:MI:SS'), 3, 3, 3, 0, 0, 3, 0, 2, 2, 2, 1, 'Cliente insatisfeito com o suporte do RM, com proposta da Senior na mesa e ameaca de cancelamento. Acionar plano de retencao imediato.');
INSERT INTO TRANSCRICOES (ID, MISSING_MEETING_ID, STATUS, OWNER_ID, CLIENTE, VENDEDOR, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, TEXTO_ORIGINAL, ORIGEM_TRANSCRICAO, MEETING_ID, MEETING_DATE, FORMATO, MEETING_STATUS, MEETING_DURATION, CODT, FLG_EXTERNO, CREATED_AT_SOURCE, TRANSCRIPTION_TEXT, SOURCE_FILE_NAME, IMPORT_BATCH_ID, IMPORTED_AT, UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO, NOTA_NPS, DT_ULTIMA_PESQUISA, DATA_CRIACAO, DATA_PROCESSAMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO)
VALUES ('cccccccc-0000-4000-8000-000000000003', 0, 'PROCESSADA', 'aaaaaaaa-0000-4000-8000-000000000003', 'Distribuidora Horizonte', 'Juliana Castro', 'Juliana Castro, Carlos Menezes (Diretor)', TO_TIMESTAMP('2026-08-11 10:00:00', 'YYYY-MM-DD HH24:MI:SS'), 45, 'Carlos: Hoje usamos o Bling, mas ele nao acompanha nosso crescimento no estoque. Juliana: O Protheus tem o modulo de distribuicao e logistica integrado ao fiscal. Carlos: Gostei. Temos verba aprovada de R$ 120 mil para este ano e queremos implantar antes do quarto trimestre.', 'csv', 'CSV-000731', '11/08/2026', 'LINK', 'COMPLETED', '00:45:00', 'TFHOR3', 1, '2026-08-11 12:00:00', 'Carlos: Hoje usamos o Bling, mas ele nao acompanha nosso crescimento no estoque...', 'transcricoes_mock_totvs.csv', '99999999-0000-4000-8000-000000000002', TO_TIMESTAMP('2026-08-12 09:10:00', 'YYYY-MM-DD HH24:MI:SS'), 'MG', '4639701', 'TOTVS MINAS GERAIS', 'DISTRIBUICAO', '03.MICRO III - De R$ 10.000.001 a R$ 20.000.000', 'lead', NULL, NULL, TO_TIMESTAMP('2026-08-12 09:10:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-08-12 09:12:05', 'YYYY-MM-DD HH24:MI:SS'), 3, 3, 0, 1, 2, 0, 2, 0, 0, 1, 1, 'Lead com verba aprovada de R$ 120 mil e dor clara em estoque. Alta probabilidade de novo produto (Protheus).');
INSERT INTO TRANSCRICOES (ID, MISSING_MEETING_ID, STATUS, OWNER_ID, CLIENTE, VENDEDOR, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, TEXTO_ORIGINAL, ORIGEM_TRANSCRICAO, MEETING_ID, MEETING_DATE, FORMATO, MEETING_STATUS, MEETING_DURATION, CODT, FLG_EXTERNO, CREATED_AT_SOURCE, TRANSCRIPTION_TEXT, SOURCE_FILE_NAME, IMPORT_BATCH_ID, IMPORTED_AT, UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO, NOTA_NPS, DT_ULTIMA_PESQUISA, DATA_CRIACAO, DATA_PROCESSAMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO)
VALUES ('cccccccc-0000-4000-8000-000000000004', 0, 'PROCESSADA', 'aaaaaaaa-0000-4000-8000-000000000003', 'Colegio Nova Geracao', 'Juliana Castro', 'Juliana Castro, Beatriz Rocha (Coordenadora Administrativa)', TO_TIMESTAMP('2026-08-14 14:00:00', 'YYYY-MM-DD HH24:MI:SS'), 30, 'Beatriz: O RM Educacional atende a secretaria, mas o portal do aluno e lento nas matriculas. Juliana: Podemos revisar a infraestrutura e apresentar o modulo de cobranca automatica. Beatriz: Nosso limite e de R$ 8 mil por mes. Vamos avaliar com calma.', 'manual', NULL, NULL, NULL, NULL, NULL, NULL, 1, NULL, NULL, NULL, NULL, NULL, 'PR', '8513900', 'TOTVS PARANA', 'EDUCACIONAL', '02.MICRO II - De R$ 500.001 a R$ 10.000.000', 'customer', 7, '2026-05', TO_TIMESTAMP('2026-08-14 16:30:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-08-14 16:31:48', 'YYYY-MM-DD HH24:MI:SS'), 3, 3, 1, 2, 0, 1, 1, 0, 0, 0, 1, 'Cliente neutro; reclamacao pontual de lentidao no portal. Oportunidade de upsell (cobranca automatica) com budget de R$ 8 mil/mes.');
INSERT INTO TRANSCRICOES (ID, MISSING_MEETING_ID, STATUS, OWNER_ID, CLIENTE, VENDEDOR, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, TEXTO_ORIGINAL, ORIGEM_TRANSCRICAO, MEETING_ID, MEETING_DATE, FORMATO, MEETING_STATUS, MEETING_DURATION, CODT, FLG_EXTERNO, CREATED_AT_SOURCE, TRANSCRIPTION_TEXT, SOURCE_FILE_NAME, IMPORT_BATCH_ID, IMPORTED_AT, UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO, NOTA_NPS, DT_ULTIMA_PESQUISA, DATA_CRIACAO, DATA_PROCESSAMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO)
VALUES ('cccccccc-0000-4000-8000-000000000005', 0, 'PROCESSADA', 'aaaaaaaa-0000-4000-8000-000000000004', 'Supermercados Bom Preco', 'Marcos Lima', 'LOCUTOR 1, LOCUTOR 2, LOCUTOR 3, LOCUTOR 4', TO_TIMESTAMP('2026-08-19 11:00:00', 'YYYY-MM-DD HH24:MI:SS'), 64, '[LOCUTOR 3]: O PDV travou duas vezes na Black Friday passada e ninguem nos avisou da correcao. [LOCUTOR 1]: A correcao foi publicada, mas falhamos na comunicacao. [LOCUTOR 3]: Sinceramente, perdemos a confianca. A Oracle NetSuite esta nos procurando com uma proposta agressiva. [LOCUTOR 4]: Precisamos de garantias antes de renovar.', 'json', '2026081905', '2026-08-19 11:00:00', 'VIDEO', 'COMPLETED', '01:04:38', 'TFBOM5', 1, '2026-08-19 13:30:00', '[LOCUTOR 3]: O PDV travou duas vezes na Black Friday passada...', 'ANON_transcricao_agosto.json', '99999999-0000-4000-8000-000000000001', TO_TIMESTAMP('2026-08-05 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'BA', '4711302', 'TOTVS NORDESTE', 'VAREJO', '06.SMB III - De R$ 120.000.001 a R$ 240.000.000', 'customer', 4, '2026-07', TO_TIMESTAMP('2026-08-20 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-08-20 08:05:22', 'YYYY-MM-DD HH24:MI:SS'), 4, 4, 3, 1, 0, 3, 1, 1, 2, 2, 2, 'Cliente com perda de confianca apos falhas no PDV e concorrente (Oracle NetSuite) em negociacao. Renovacao em risco.');
INSERT INTO TRANSCRICOES (ID, MISSING_MEETING_ID, STATUS, OWNER_ID, CLIENTE, VENDEDOR, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, TEXTO_ORIGINAL, ORIGEM_TRANSCRICAO, MEETING_ID, MEETING_DATE, FORMATO, MEETING_STATUS, MEETING_DURATION, CODT, FLG_EXTERNO, CREATED_AT_SOURCE, TRANSCRIPTION_TEXT, SOURCE_FILE_NAME, IMPORT_BATCH_ID, IMPORTED_AT, AUDIO_FILE_NAME, TRANSCRIPTION_MODEL, TRANSCRIPTION_LANGUAGE, AUDIO_DURATION_SECONDS, TRANSCRIPTION_PROCESSING_MS, UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO, NOTA_NPS, DT_ULTIMA_PESQUISA, DATA_CRIACAO, DATA_PROCESSAMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO)
VALUES ('cccccccc-0000-4000-8000-000000000006', 0, 'PROCESSADA', 'aaaaaaaa-0000-4000-8000-000000000002', 'Transportadora Rota Sul', 'Rafael Moura', 'Rafael Moura, Fernanda Dias (CFO)', TO_TIMESTAMP('2026-08-25 16:00:00', 'YYYY-MM-DD HH24:MI:SS'), 41, 'Fernanda: O Datasul esta redondo na operacao, o time adora. Nosso gargalo hoje e capital de giro para antecipar recebiveis. Rafael: O TOTVS Techfin faz antecipacao integrada ao ERP. Fernanda: Otimo, vamos avancar. Pode mandar a proposta.', 'parakeet', 'AUD-2026-0825', NULL, NULL, NULL, NULL, NULL, 1, NULL, 'fernanda o datasul esta redondo na operacao o time adora nosso gargalo hoje e capital de giro...', 'reuniao_rota_sul.mp3', NULL, NULL, 'reuniao_rota_sul.mp3', 'parakeet', 'pt', 2461.8, 48310, 'SC', '4930202', 'TOTVS SANTA CATARINA', 'LOGISTICA', '05.SMB II - De R$ 60.000.001 a R$ 120.000.000', 'customer', 9, '2026-06', TO_TIMESTAMP('2026-08-25 17:10:00', 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP('2026-08-25 17:12:30', 'YYYY-MM-DD HH24:MI:SS'), 3, 3, 0, 0, 3, 0, 1, 0, 0, 1, 1, 'Cliente promotor; cross-sell de TOTVS Techfin (antecipacao de recebiveis) com decisao favoravel da CFO. Enviar proposta.');
INSERT INTO TRANSCRICOES (ID, MISSING_MEETING_ID, STATUS, OWNER_ID, CLIENTE, VENDEDOR, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, TEXTO_ORIGINAL, ORIGEM_TRANSCRICAO, MEETING_ID, FLG_EXTERNO, UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO, DATA_CRIACAO)
VALUES ('cccccccc-0000-4000-8000-000000000007', 1, 'PENDENTE', 'aaaaaaaa-0000-4000-8000-000000000003', 'Construtora Alvorada', 'Juliana Castro', 'Juliana Castro, Paulo Siqueira (Gerente de Obras)', TO_TIMESTAMP('2026-09-08 09:00:00', 'YYYY-MM-DD HH24:MI:SS'), 35, 'Paulo: Controlamos as obras em planilha e perdemos prazo de medicao. Juliana: O Protheus Construcao e Projetos integra medicao, compras e financeiro. Paulo: Queremos ver uma demonstracao ainda este mes.', 'manual', NULL, 1, 'GO', '4120400', 'TOTVS CENTRO-OESTE', 'CONSTRUCAO E PROJETOS', '04.SMB I - De R$ 20.000.001 a R$ 60.000.000', 'lead', TO_TIMESTAMP('2026-09-08 11:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO TRANSCRICOES (ID, MISSING_MEETING_ID, STATUS, OWNER_ID, CLIENTE, VENDEDOR, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, TEXTO_ORIGINAL, ORIGEM_TRANSCRICAO, MEETING_ID, MEETING_DATE, FORMATO, MEETING_STATUS, MEETING_DURATION, CODT, FLG_EXTERNO, SOURCE_FILE_NAME, IMPORT_BATCH_ID, IMPORTED_AT, UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO, NOTA_NPS, DT_ULTIMA_PESQUISA, DATA_CRIACAO)
VALUES ('cccccccc-0000-4000-8000-000000000008', 0, 'PROCESSANDO', 'aaaaaaaa-0000-4000-8000-000000000004', 'Agro Serra Verde', 'Marcos Lima', 'LOCUTOR 1, LOCUTOR 2', TO_TIMESTAMP('2026-09-09 08:30:00', 'YYYY-MM-DD HH24:MI:SS'), 27, '[LOCUTOR 1]: A safra foi boa e queremos integrar a balanca ao ERP. [LOCUTOR 2]: O TOTVS Agro faz essa integracao com o financeiro.', 'json', '2026090908', '09/09/26 08:30', 'VIDEO', 'COMPLETED', '00:27:05', 'TFAGR8', 1, 'ANON_transcricao_setembro.json', '99999999-0000-4000-8000-000000000002', TO_TIMESTAMP('2026-09-10 07:45:00', 'YYYY-MM-DD HH24:MI:SS'), 'MS', '0111301', 'TOTVS CENTRO-OESTE', 'AGROINDUSTRIA', '07.UPM I - De R$ 240.000.001 a R$ 480.000.000', 'customer', 8, '2026-08', TO_TIMESTAMP('2026-09-10 07:45:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO TRANSCRICOES (ID, MISSING_MEETING_ID, STATUS, OWNER_ID, CLIENTE, VENDEDOR, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, TEXTO_ORIGINAL, ORIGEM_TRANSCRICAO, MEETING_ID, MEETING_DATE, FORMATO, MEETING_STATUS, CODT, FLG_EXTERNO, SOURCE_FILE_NAME, IMPORT_BATCH_ID, IMPORTED_AT, UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO, DATA_CRIACAO)
VALUES ('cccccccc-0000-4000-8000-000000000009', 0, 'ERRO', 'aaaaaaaa-0000-4000-8000-000000000002', 'Hospital Sao Lucas', 'Rafael Moura', NULL, NULL, NULL, '(arquivo de transcricao vazio)', 'json', '2026091009', NULL, 'VIDEO', 'COMPLETED', 'TFSLU9', 1, 'ANON_transcricao_setembro.json', '99999999-0000-4000-8000-000000000002', TO_TIMESTAMP('2026-09-10 07:45:00', 'YYYY-MM-DD HH24:MI:SS'), 'RJ', '8610101', 'TOTVS SAUDE', 'SAUDE', '06.SMB III - De R$ 120.000.001 a R$ 240.000.000', 'customer', TO_TIMESTAMP('2026-09-10 07:45:01', 'YYYY-MM-DD HH24:MI:SS'));

-- -------------------------------------------------------------------------------------
-- TRANSCRICAO_STATUS_HIST - registro de criacao de cada reuniao (as mudancas seguintes vem do trigger)
-- -------------------------------------------------------------------------------------
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('cccccccc-0000-4000-8000-000000000001', NULL, 'PENDENTE', TO_TIMESTAMP('2026-08-05 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'APLICACAO', 'Registro criado (json)');
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('cccccccc-0000-4000-8000-000000000002', NULL, 'PENDENTE', TO_TIMESTAMP('2026-08-05 08:00:01', 'YYYY-MM-DD HH24:MI:SS'), 'APLICACAO', 'Registro criado (json)');
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('cccccccc-0000-4000-8000-000000000003', NULL, 'PENDENTE', TO_TIMESTAMP('2026-08-12 09:10:00', 'YYYY-MM-DD HH24:MI:SS'), 'APLICACAO', 'Registro criado (csv)');
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('cccccccc-0000-4000-8000-000000000004', NULL, 'PENDENTE', TO_TIMESTAMP('2026-08-14 16:30:00', 'YYYY-MM-DD HH24:MI:SS'), 'APLICACAO', 'Registro criado (manual)');
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('cccccccc-0000-4000-8000-000000000005', NULL, 'PENDENTE', TO_TIMESTAMP('2026-08-20 08:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'APLICACAO', 'Registro criado (json)');
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('cccccccc-0000-4000-8000-000000000006', NULL, 'PENDENTE', TO_TIMESTAMP('2026-08-25 17:10:00', 'YYYY-MM-DD HH24:MI:SS'), 'APLICACAO', 'Registro criado (parakeet)');
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('cccccccc-0000-4000-8000-000000000007', NULL, 'PENDENTE', TO_TIMESTAMP('2026-09-08 11:00:00', 'YYYY-MM-DD HH24:MI:SS'), 'APLICACAO', 'Registro criado (manual)');
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('cccccccc-0000-4000-8000-000000000008', NULL, 'PENDENTE', TO_TIMESTAMP('2026-09-10 07:45:00', 'YYYY-MM-DD HH24:MI:SS'), 'APLICACAO', 'Registro criado (json)');
INSERT INTO TRANSCRICAO_STATUS_HIST (TRANSCRICAO_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('cccccccc-0000-4000-8000-000000000009', NULL, 'PENDENTE', TO_TIMESTAMP('2026-09-10 07:45:01', 'YYYY-MM-DD HH24:MI:SS'), 'APLICACAO', 'Registro criado (json)');

-- -------------------------------------------------------------------------------------
-- TRANSCRICAO_WARNINGS - avisos de importacao
-- -------------------------------------------------------------------------------------
INSERT INTO TRANSCRICAO_WARNINGS (TRANSCRICAO_ID, POSICAO, WARNING)
VALUES ('cccccccc-0000-4000-8000-000000000003', 0, 'INVALID_NOTA_NPS');
INSERT INTO TRANSCRICAO_WARNINGS (TRANSCRICAO_ID, POSICAO, WARNING)
VALUES ('cccccccc-0000-4000-8000-000000000003', 1, 'INVALID_DT_MEETING');
INSERT INTO TRANSCRICAO_WARNINGS (TRANSCRICAO_ID, POSICAO, WARNING)
VALUES ('cccccccc-0000-4000-8000-000000000006', 0, 'Audio sem metadados de reuniao (data e participantes informados manualmente)');
INSERT INTO TRANSCRICAO_WARNINGS (TRANSCRICAO_ID, POSICAO, WARNING)
VALUES ('cccccccc-0000-4000-8000-000000000007', 0, 'MISSING_ID_MEETING');
INSERT INTO TRANSCRICAO_WARNINGS (TRANSCRICAO_ID, POSICAO, WARNING)
VALUES ('cccccccc-0000-4000-8000-000000000008', 0, 'INVALID_DT_MEETING');
INSERT INTO TRANSCRICAO_WARNINGS (TRANSCRICAO_ID, POSICAO, WARNING)
VALUES ('cccccccc-0000-4000-8000-000000000008', 1, 'INVALID_DT_CRIACAO');
INSERT INTO TRANSCRICAO_WARNINGS (TRANSCRICAO_ID, POSICAO, WARNING)
VALUES ('cccccccc-0000-4000-8000-000000000009', 0, 'INVALID_DT_MEETING');
INSERT INTO TRANSCRICAO_WARNINGS (TRANSCRICAO_ID, POSICAO, WARNING)
VALUES ('cccccccc-0000-4000-8000-000000000009', 1, 'INVALID_DT_CRIACAO');
INSERT INTO TRANSCRICAO_WARNINGS (TRANSCRICAO_ID, POSICAO, WARNING)
VALUES ('cccccccc-0000-4000-8000-000000000009', 2, 'Texto da transcricao vazio: analise nao executada');

-- -------------------------------------------------------------------------------------
-- TRANSCRICAO_RISCOS_CONS / TRANSCRICAO_OPORT_CONS / TRANSCRICAO_ALERTAS_CONS / TRANSCRICAO_ITENS_CRITICOS - listas consolidadas
-- -------------------------------------------------------------------------------------
INSERT INTO TRANSCRICAO_RISCOS_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000001', 0, 'Insatisfacao', 'MEDIO', 1, '["a aprovacao de compras ainda e por e-mail"]', 'Mostrar o ganho de tempo com o fluxo de aprovacao do Fluig.');
INSERT INTO TRANSCRICAO_RISCOS_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000002', 0, 'Churn', 'ALTO', 2, '["vamos cancelar o contrato", "a diretoria esta pensando em trocar"]', 'Acionar plano de retencao imediato com lideranca comercial.');
INSERT INTO TRANSCRICAO_RISCOS_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000002', 1, 'Concorrencia', 'MEDIO', 1, '["A Senior ja nos apresentou uma proposta"]', 'Levantar diferenciais frente a Senior e reforcar o roadmap do RM.');
INSERT INTO TRANSCRICAO_RISCOS_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000004', 0, 'Insatisfacao', 'BAIXO', 1, '["o portal do aluno e lento nas matriculas"]', 'Agendar diagnostico de infraestrutura antes do periodo de matriculas.');
INSERT INTO TRANSCRICAO_RISCOS_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000005', 0, 'Perda de confianca', 'ALTO', 1, '["Sinceramente, perdemos a confianca"]', 'Envolver a diretoria de CS e apresentar plano de comunicacao de incidentes.');
INSERT INTO TRANSCRICAO_RISCOS_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000005', 1, 'Concorrencia', 'MEDIO', 2, '["A Oracle NetSuite esta nos procurando", "proposta agressiva"]', 'Preparar contraproposta de renovacao com garantias de SLA.');
INSERT INTO TRANSCRICAO_OPORT_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000001', 0, 'Upsell', 0.78, 2, '["O Fluig resolve exatamente esse fluxo", "precisamos de uma proposta ate o fim do mes"]', 'Enviar proposta do Fluig com foco em aprovacao de compras.');
INSERT INTO TRANSCRICAO_OPORT_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000002', 0, 'Melhoria no atendimento', 0.6, 1, '["vou escalar hoje"]', 'Resolver o chamado e registrar retorno formal ao cliente.');
INSERT INTO TRANSCRICAO_OPORT_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000003', 0, 'Novo produto', 0.81, 2, '["Temos verba aprovada de R$ 120 mil", "implantar antes do quarto trimestre"]', 'Apresentar Protheus Distribuicao e Logistica com cronograma de implantacao.');
INSERT INTO TRANSCRICAO_OPORT_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000004', 0, 'Upsell', 0.55, 1, '["apresentar o modulo de cobranca automatica"]', 'Enviar simulacao de economia com cobranca automatica.');
INSERT INTO TRANSCRICAO_OPORT_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000005', 0, 'Upsell', 0.42, 1, '["Precisamos de garantias antes de renovar"]', 'Incluir monitoramento proativo do PDV na renovacao.');
INSERT INTO TRANSCRICAO_OPORT_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000006', 0, 'Cross-sell', 0.74, 2, '["capital de giro para antecipar recebiveis", "Pode mandar a proposta"]', 'Enviar proposta do TOTVS Techfin de antecipacao de recebiveis.');
INSERT INTO TRANSCRICAO_ALERTAS_CONS (TRANSCRICAO_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000001', 0, 'Oportunidade alta', 'Upsell', 'MEDIO', 2, 'Cliente pediu proposta do Fluig com prazo definido.', 'Enviar proposta ate o fim do mes.');
INSERT INTO TRANSCRICAO_ALERTAS_CONS (TRANSCRICAO_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000002', 0, 'Risco de churn', 'Churn', 'ALTO', 2, 'Cliente verbalizou cancelamento e troca de fornecedor.', 'Acionar plano de retencao imediato com lideranca comercial.');
INSERT INTO TRANSCRICAO_ALERTAS_CONS (TRANSCRICAO_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000002', 1, 'Concorrente citado', 'Concorrencia', 'MEDIO', 1, 'Senior apresentou proposta ao cliente.', 'Reforcar diferenciais e roadmap do RM.');
INSERT INTO TRANSCRICAO_ALERTAS_CONS (TRANSCRICAO_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000005', 0, 'Perda de confianca', 'Perda de confianca', 'ALTO', 1, 'Cliente declarou ter perdido a confianca apos incidentes.', 'Plano de comunicacao de incidentes e reuniao com diretoria de CS.');
INSERT INTO TRANSCRICAO_ALERTAS_CONS (TRANSCRICAO_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000005', 1, 'Concorrente citado', 'Concorrencia', 'MEDIO', 2, 'Oracle NetSuite em negociacao com o cliente.', 'Contraproposta de renovacao com garantias de SLA.');
INSERT INTO TRANSCRICAO_ALERTAS_CONS (TRANSCRICAO_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000006', 0, 'Oportunidade alta', 'Cross-sell', 'MEDIO', 2, 'CFO aprovou avancar com antecipacao de recebiveis.', 'Enviar proposta do TOTVS Techfin.');
INSERT INTO TRANSCRICAO_ITENS_CRITICOS (TRANSCRICAO_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('cccccccc-0000-4000-8000-000000000001', 0, 'oportunidade', 'Upsell', 'O Fluig resolve exatamente esse fluxo de aprovacao.', 'MEDIO');
INSERT INTO TRANSCRICAO_ITENS_CRITICOS (TRANSCRICAO_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('cccccccc-0000-4000-8000-000000000002', 0, 'risco', 'Churn', 'Se nao resolver ate o fim do mes, vamos cancelar o contrato.', 'ALTO');
INSERT INTO TRANSCRICAO_ITENS_CRITICOS (TRANSCRICAO_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('cccccccc-0000-4000-8000-000000000002', 1, 'risco', 'Concorrencia', 'A Senior ja nos apresentou uma proposta e a diretoria esta pensando em trocar.', 'MEDIO');
INSERT INTO TRANSCRICAO_ITENS_CRITICOS (TRANSCRICAO_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('cccccccc-0000-4000-8000-000000000003', 0, 'oportunidade', 'Novo produto', 'Temos verba aprovada de R$ 120 mil para este ano.', 'MEDIO');
INSERT INTO TRANSCRICAO_ITENS_CRITICOS (TRANSCRICAO_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('cccccccc-0000-4000-8000-000000000005', 0, 'risco', 'Perda de confianca', 'Sinceramente, perdemos a confianca.', 'ALTO');
INSERT INTO TRANSCRICAO_ITENS_CRITICOS (TRANSCRICAO_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('cccccccc-0000-4000-8000-000000000005', 1, 'risco', 'Concorrencia', 'A Oracle NetSuite esta nos procurando com uma proposta agressiva.', 'MEDIO');
INSERT INTO TRANSCRICAO_ITENS_CRITICOS (TRANSCRICAO_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('cccccccc-0000-4000-8000-000000000006', 0, 'oportunidade', 'Cross-sell', 'Nosso gargalo hoje e capital de giro para antecipar recebiveis.', 'MEDIO');

-- -------------------------------------------------------------------------------------
-- ANALISES - 6 analises da LeanIA (uma por reuniao processada)
-- -------------------------------------------------------------------------------------
INSERT INTO ANALISES (ID, OWNER_ID, TRANSCRICAO_ID, SENTIMENTO_GERAL, RESUMO, PERSONA_IDENTIFICADA, BUDGET_IDENTIFICADO, SCORE_CONFIANCA_VENDEDOR, SCORE_SENTIMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO, DATA_ANALISE, PROXIMA_ACAO, SENTIMENTO_DETALHE, INSIGHTS_JSON, INDICADORES_JSON)
VALUES ('dddddddd-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000002', 'cccccccc-0000-4000-8000-000000000001', 'MISTO', 'Sentimento misto (satisfeito com Backoffice / frustrado com aprovacoes manuais). Boa probabilidade de venda do modulo de processos (TOTVS Fluig). Interlocutor: Gestor Financeiro (decisor tecnico). Budget mencionado: R$ 60.000,00.', 'Gestor Financeiro (decisor tecnico)', 'R$ 60.000,00', 0.72, 0.15, 3, 3, 1, 1, 1, 1, 1, 0, 1, 1, 1, 'Cliente satisfeito com o Protheus e com forte interesse no Fluig para aprovacoes. Budget de R$ 60 mil. Enviar proposta ate o fim do mes.', TO_TIMESTAMP('2026-08-05 08:03:12', 'YYYY-MM-DD HH24:MI:SS'), 'Enviar proposta ou agendar demonstracao com os decisores nos proximos dias.', 'Misto (Satisfeito com Backoffice / Frustrado com aprovacoes)', '{"elogios":["O modulo financeiro atende bem"],"reclamacoes":["a aprovacao de compras ainda e por e-mail"],"duvidasJornada":[],"proximosPassos":["precisamos de uma proposta ate o fim do mes"],"observacoes":[]}', '{"locutores":3,"turnos":5,"sentencas":7,"palavras":78,"participacaoVendedorPct":45,"participacaoClientePct":55,"perguntasVendedor":1,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}');
INSERT INTO ANALISES (ID, OWNER_ID, TRANSCRICAO_ID, SENTIMENTO_GERAL, RESUMO, PERSONA_IDENTIFICADA, BUDGET_IDENTIFICADO, SCORE_CONFIANCA_VENDEDOR, SCORE_SENTIMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO, DATA_ANALISE, PROXIMA_ACAO, SENTIMENTO_DETALHE, INSIGHTS_JSON, INDICADORES_JSON)
VALUES ('dddddddd-0000-4000-8000-000000000002', 'aaaaaaaa-0000-4000-8000-000000000002', 'cccccccc-0000-4000-8000-000000000002', 'NEGATIVO', 'Sentimento negativo (insatisfacao recorrente com suporte). Risco alto de churn com concorrente (Senior) na mesa. Interlocutor: Gestor Administrativo (decisor). Budget nao mencionado.', 'Gestor Administrativo (decisor)', 'Nao identificado', 0.48, -0.85, 3, 3, 3, 0, 0, 3, 0, 2, 2, 2, 1, 'Cliente insatisfeito com o suporte do RM, com proposta da Senior na mesa e ameaca de cancelamento. Acionar plano de retencao imediato.', TO_TIMESTAMP('2026-08-05 08:04:40', 'YYYY-MM-DD HH24:MI:SS'), 'Acionar plano de retencao imediato com lideranca comercial.', 'Negativo (insatisfacao com suporte)', '{"elogios":[],"reclamacoes":["ha tres semanas com o chamado do RM aberto e ninguem retorna"],"duvidasJornada":[],"proximosPassos":["vou escalar hoje"],"observacoes":["ameaca de cancelamento"]}', '{"locutores":2,"turnos":4,"sentencas":6,"palavras":64,"participacaoVendedorPct":30,"participacaoClientePct":70,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Vendedor ouviu mais do que falou","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}');
INSERT INTO ANALISES (ID, OWNER_ID, TRANSCRICAO_ID, SENTIMENTO_GERAL, RESUMO, PERSONA_IDENTIFICADA, BUDGET_IDENTIFICADO, SCORE_CONFIANCA_VENDEDOR, SCORE_SENTIMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO, DATA_ANALISE, PROXIMA_ACAO, SENTIMENTO_DETALHE, INSIGHTS_JSON, INDICADORES_JSON)
VALUES ('dddddddd-0000-4000-8000-000000000003', 'aaaaaaaa-0000-4000-8000-000000000003', 'cccccccc-0000-4000-8000-000000000003', 'POSITIVO', 'Sentimento positivo. Alta probabilidade de venda de novo produto (TOTVS Protheus - Distribuicao e Logistica). Interlocutor: Socio/Executivo (decisor final). Budget mencionado: R$ 120.000,00.', 'Socio/Executivo (decisor final)', 'R$ 120.000,00', 0.81, 0.72, 3, 3, 0, 1, 2, 0, 2, 0, 0, 1, 1, 'Lead com verba aprovada de R$ 120 mil e dor clara em estoque. Alta probabilidade de novo produto (Protheus).', TO_TIMESTAMP('2026-08-12 09:12:05', 'YYYY-MM-DD HH24:MI:SS'), 'Enviar proposta ou agendar demonstracao com os decisores nos proximos dias.', 'Positivo (entusiasmo com a solucao)', '{"elogios":["Gostei"],"reclamacoes":["ele nao acompanha nosso crescimento no estoque"],"duvidasJornada":[],"proximosPassos":["implantar antes do quarto trimestre"],"observacoes":[]}', '{"locutores":2,"turnos":4,"sentencas":6,"palavras":66,"participacaoVendedorPct":40,"participacaoClientePct":60,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}');
INSERT INTO ANALISES (ID, OWNER_ID, TRANSCRICAO_ID, SENTIMENTO_GERAL, RESUMO, PERSONA_IDENTIFICADA, BUDGET_IDENTIFICADO, SCORE_CONFIANCA_VENDEDOR, SCORE_SENTIMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO, DATA_ANALISE, PROXIMA_ACAO, SENTIMENTO_DETALHE, INSIGHTS_JSON, INDICADORES_JSON)
VALUES ('dddddddd-0000-4000-8000-000000000004', 'aaaaaaaa-0000-4000-8000-000000000003', 'cccccccc-0000-4000-8000-000000000004', 'NEUTRO', 'Sentimento neutro. Possibilidade de upsell (cobranca automatica) no RM Educacional. Interlocutor: Coordenador Administrativo (influenciador). Budget mencionado: R$ 8.000,00 por mes.', 'Coordenador Administrativo (influenciador)', 'R$ 8.000,00 por mes', 0.6, 0.05, 3, 3, 1, 2, 0, 1, 1, 0, 0, 0, 1, 'Cliente neutro; reclamacao pontual de lentidao no portal. Oportunidade de upsell (cobranca automatica) com budget de R$ 8 mil/mes.', TO_TIMESTAMP('2026-08-14 16:31:48', 'YYYY-MM-DD HH24:MI:SS'), 'Qualificar necessidade, budget e prazo antes de enviar proposta.', 'Neutro (avaliacao cautelosa)', '{"elogios":["O RM Educacional atende a secretaria"],"reclamacoes":["o portal do aluno e lento nas matriculas"],"duvidasJornada":[],"proximosPassos":["Vamos avaliar com calma"],"observacoes":[]}', '{"locutores":2,"turnos":3,"sentencas":5,"palavras":52,"participacaoVendedorPct":45,"participacaoClientePct":55,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}');
INSERT INTO ANALISES (ID, OWNER_ID, TRANSCRICAO_ID, SENTIMENTO_GERAL, RESUMO, PERSONA_IDENTIFICADA, BUDGET_IDENTIFICADO, SCORE_CONFIANCA_VENDEDOR, SCORE_SENTIMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO, DATA_ANALISE, PROXIMA_ACAO, SENTIMENTO_DETALHE, INSIGHTS_JSON, INDICADORES_JSON)
VALUES ('dddddddd-0000-4000-8000-000000000005', 'aaaaaaaa-0000-4000-8000-000000000004', 'cccccccc-0000-4000-8000-000000000005', 'NEGATIVO', 'Sentimento negativo (perda de confianca apos incidentes no PDV). Concorrente Oracle NetSuite em negociacao. Interlocutor: Diretor de Operacoes (decisor). Budget nao mencionado.', 'Diretor de Operacoes (decisor)', 'Nao identificado', 0.4, -0.7, 4, 4, 3, 1, 0, 3, 1, 1, 2, 2, 2, 'Cliente com perda de confianca apos falhas no PDV e concorrente (Oracle NetSuite) em negociacao. Renovacao em risco.', TO_TIMESTAMP('2026-08-20 08:05:22', 'YYYY-MM-DD HH24:MI:SS'), 'Envolver a diretoria de CS e apresentar plano de comunicacao de incidentes antes da renovacao.', 'Negativo (perda de confianca / concorrencia)', '{"elogios":[],"reclamacoes":["O PDV travou duas vezes na Black Friday passada","ninguem nos avisou da correcao"],"duvidasJornada":[],"proximosPassos":["Precisamos de garantias antes de renovar"],"observacoes":["Oracle NetSuite com proposta agressiva"]}', '{"locutores":4,"turnos":4,"sentencas":8,"palavras":74,"participacaoVendedorPct":25,"participacaoClientePct":75,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Vendedor ouviu mais do que falou","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}');
INSERT INTO ANALISES (ID, OWNER_ID, TRANSCRICAO_ID, SENTIMENTO_GERAL, RESUMO, PERSONA_IDENTIFICADA, BUDGET_IDENTIFICADO, SCORE_CONFIANCA_VENDEDOR, SCORE_SENTIMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO, DATA_ANALISE, PROXIMA_ACAO, SENTIMENTO_DETALHE, INSIGHTS_JSON, INDICADORES_JSON)
VALUES ('dddddddd-0000-4000-8000-000000000006', 'aaaaaaaa-0000-4000-8000-000000000002', 'cccccccc-0000-4000-8000-000000000006', 'POSITIVO', 'Sentimento positivo (cliente promotor). Boa probabilidade de cross-sell (TOTVS Techfin - antecipacao de recebiveis). Interlocutor: CFO (decisor final). Budget nao mencionado.', 'CFO (decisor final)', 'Nao identificado', 0.85, 0.8, 3, 3, 0, 0, 3, 0, 1, 0, 0, 1, 1, 'Cliente promotor; cross-sell de TOTVS Techfin (antecipacao de recebiveis) com decisao favoravel da CFO. Enviar proposta.', TO_TIMESTAMP('2026-08-25 17:12:30', 'YYYY-MM-DD HH24:MI:SS'), 'Enviar proposta ou agendar demonstracao com os decisores nos proximos dias.', 'Positivo (satisfacao com o Datasul)', '{"elogios":["O Datasul esta redondo na operacao, o time adora"],"reclamacoes":[],"duvidasJornada":[],"proximosPassos":["Pode mandar a proposta"],"observacoes":[]}', '{"locutores":2,"turnos":4,"sentencas":6,"palavras":58,"participacaoVendedorPct":35,"participacaoClientePct":65,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"media"}');

-- -------------------------------------------------------------------------------------
-- TRANSCRICOES.ANALISE_ID - vincula cada reuniao processada a sua analise atual (relacionamento 1:1)
-- -------------------------------------------------------------------------------------
UPDATE TRANSCRICOES SET ANALISE_ID = 'dddddddd-0000-4000-8000-000000000001' WHERE ID = 'cccccccc-0000-4000-8000-000000000001';
UPDATE TRANSCRICOES SET ANALISE_ID = 'dddddddd-0000-4000-8000-000000000002' WHERE ID = 'cccccccc-0000-4000-8000-000000000002';
UPDATE TRANSCRICOES SET ANALISE_ID = 'dddddddd-0000-4000-8000-000000000003' WHERE ID = 'cccccccc-0000-4000-8000-000000000003';
UPDATE TRANSCRICOES SET ANALISE_ID = 'dddddddd-0000-4000-8000-000000000004' WHERE ID = 'cccccccc-0000-4000-8000-000000000004';
UPDATE TRANSCRICOES SET ANALISE_ID = 'dddddddd-0000-4000-8000-000000000005' WHERE ID = 'cccccccc-0000-4000-8000-000000000005';
UPDATE TRANSCRICOES SET ANALISE_ID = 'dddddddd-0000-4000-8000-000000000006' WHERE ID = 'cccccccc-0000-4000-8000-000000000006';

-- -------------------------------------------------------------------------------------
-- ANALISE_PRODUTOS / ANALISE_OPORTUNIDADES / ANALISE_RISCOS - detalhes da analise
-- -------------------------------------------------------------------------------------
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000001', 0, 'TOTVS Protheus', 'ATUAL', 'revisar o uso do Protheus no financeiro');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000001', 1, 'TOTVS Fluig', 'OPORTUNIDADE', 'O Fluig resolve exatamente esse fluxo de aprovacao');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 0, 'TOTVS RM', 'ATUAL', 'chamado do RM aberto e ninguem retorna');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 1, 'Senior', 'CONCORRENTE', 'A Senior ja nos apresentou uma proposta');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000003', 0, 'Bling', 'CONCORRENTE', 'Hoje usamos o Bling, mas ele nao acompanha nosso crescimento');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000003', 1, 'TOTVS Protheus', 'OPORTUNIDADE', 'O Protheus tem o modulo de distribuicao e logistica');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000004', 0, 'TOTVS RM', 'ATUAL', 'O RM Educacional atende a secretaria');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 0, 'TOTVS Varejo', 'ATUAL', 'O PDV travou duas vezes na Black Friday passada');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 1, 'Oracle NetSuite', 'CONCORRENTE', 'A Oracle NetSuite esta nos procurando com uma proposta agressiva');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000006', 0, 'TOTVS Datasul', 'ATUAL', 'O Datasul esta redondo na operacao');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000006', 1, 'TOTVS Techfin', 'OPORTUNIDADE', 'O TOTVS Techfin faz antecipacao integrada ao ERP');
INSERT INTO ANALISE_OPORTUNIDADES (ANALISE_ID, POSICAO, TIPO, DESCRICAO, PRODUTO_RELACIONADO, SCORE, VALOR_ESTIMADO)
VALUES ('dddddddd-0000-4000-8000-000000000001', 0, 'UPSELL', 'Fluxo de aprovacao de compras com TOTVS Fluig integrado ao Protheus.', 'TOTVS Fluig', 0.78, 'R$ 60.000,00');
INSERT INTO ANALISE_OPORTUNIDADES (ANALISE_ID, POSICAO, TIPO, DESCRICAO, PRODUTO_RELACIONADO, SCORE, VALOR_ESTIMADO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 0, 'UPSELL', 'Retencao com plano de melhoria no suporte do RM; sem nova venda antes de resolver o chamado.', 'TOTVS RM', 0.25, 'Nao estimado');
INSERT INTO ANALISE_OPORTUNIDADES (ANALISE_ID, POSICAO, TIPO, DESCRICAO, PRODUTO_RELACIONADO, SCORE, VALOR_ESTIMADO)
VALUES ('dddddddd-0000-4000-8000-000000000003', 0, 'NOVO_PRODUTO', 'Implantacao do Protheus Distribuicao e Logistica substituindo o Bling.', 'TOTVS Protheus', 0.81, 'R$ 120.000,00');
INSERT INTO ANALISE_OPORTUNIDADES (ANALISE_ID, POSICAO, TIPO, DESCRICAO, PRODUTO_RELACIONADO, SCORE, VALOR_ESTIMADO)
VALUES ('dddddddd-0000-4000-8000-000000000004', 0, 'UPSELL', 'Modulo de cobranca automatica do RM Educacional.', 'TOTVS RM', 0.55, 'R$ 8.000,00 por mes');
INSERT INTO ANALISE_OPORTUNIDADES (ANALISE_ID, POSICAO, TIPO, DESCRICAO, PRODUTO_RELACIONADO, SCORE, VALOR_ESTIMADO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 0, 'UPSELL', 'Monitoramento proativo do PDV incluido na renovacao do TOTVS Varejo.', 'TOTVS Varejo', 0.42, 'Nao estimado');
INSERT INTO ANALISE_OPORTUNIDADES (ANALISE_ID, POSICAO, TIPO, DESCRICAO, PRODUTO_RELACIONADO, SCORE, VALOR_ESTIMADO)
VALUES ('dddddddd-0000-4000-8000-000000000006', 0, 'CROSS_SELL', 'Antecipacao de recebiveis com TOTVS Techfin integrada ao Datasul.', 'TOTVS Techfin', 0.74, 'Nao estimado');
INSERT INTO ANALISE_RISCOS (ANALISE_ID, POSICAO, TIPO, DESCRICAO, SEVERIDADE, CONCORRENTE_MENCIONADO)
VALUES ('dddddddd-0000-4000-8000-000000000001', 0, 'INSATISFACAO', 'Insatisfacao com Backoffice: aprovacao de compras ainda e por e-mail.', 'MEDIO', NULL);
INSERT INTO ANALISE_RISCOS (ANALISE_ID, POSICAO, TIPO, DESCRICAO, SEVERIDADE, CONCORRENTE_MENCIONADO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 0, 'CHURN', 'Cliente verbalizou cancelamento: se nao resolver ate o fim do mes, vamos cancelar o contrato.', 'ALTO', NULL);
INSERT INTO ANALISE_RISCOS (ANALISE_ID, POSICAO, TIPO, DESCRICAO, SEVERIDADE, CONCORRENTE_MENCIONADO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 1, 'CONCORRENCIA', 'Concorrente Senior apresentou proposta e a diretoria avalia a troca.', 'MEDIO', 'Senior');
INSERT INTO ANALISE_RISCOS (ANALISE_ID, POSICAO, TIPO, DESCRICAO, SEVERIDADE, CONCORRENTE_MENCIONADO)
VALUES ('dddddddd-0000-4000-8000-000000000004', 0, 'INSATISFACAO', 'Insatisfacao com o portal do aluno: lento nas matriculas.', 'BAIXO', NULL);
INSERT INTO ANALISE_RISCOS (ANALISE_ID, POSICAO, TIPO, DESCRICAO, SEVERIDADE, CONCORRENTE_MENCIONADO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 0, 'PERDA_CONFIANCA', 'Cliente sinaliza perda de confianca na relacao: sinceramente, perdemos a confianca.', 'ALTO', NULL);
INSERT INTO ANALISE_RISCOS (ANALISE_ID, POSICAO, TIPO, DESCRICAO, SEVERIDADE, CONCORRENTE_MENCIONADO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 1, 'CONCORRENCIA', 'Oracle NetSuite abordou o cliente com proposta agressiva.', 'MEDIO', 'Oracle NetSuite');

-- -------------------------------------------------------------------------------------
-- ANALISE_RISCOS_CONS / ANALISE_OPORT_CONS / ANALISE_ALERTAS_CONS / ANALISE_ITENS_CRITICOS - consolidados gerados pela analise
-- -------------------------------------------------------------------------------------
INSERT INTO ANALISE_RISCOS_CONS (ANALISE_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000001', 0, 'Insatisfacao', 'MEDIO', 1, '["a aprovacao de compras ainda e por e-mail"]', 'Mostrar o ganho de tempo com o fluxo de aprovacao do Fluig.');
INSERT INTO ANALISE_RISCOS_CONS (ANALISE_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 0, 'Churn', 'ALTO', 2, '["vamos cancelar o contrato", "a diretoria esta pensando em trocar"]', 'Acionar plano de retencao imediato com lideranca comercial.');
INSERT INTO ANALISE_RISCOS_CONS (ANALISE_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 1, 'Concorrencia', 'MEDIO', 1, '["A Senior ja nos apresentou uma proposta"]', 'Levantar diferenciais frente a Senior e reforcar o roadmap do RM.');
INSERT INTO ANALISE_RISCOS_CONS (ANALISE_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000004', 0, 'Insatisfacao', 'BAIXO', 1, '["o portal do aluno e lento nas matriculas"]', 'Agendar diagnostico de infraestrutura antes do periodo de matriculas.');
INSERT INTO ANALISE_RISCOS_CONS (ANALISE_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 0, 'Perda de confianca', 'ALTO', 1, '["Sinceramente, perdemos a confianca"]', 'Envolver a diretoria de CS e apresentar plano de comunicacao de incidentes.');
INSERT INTO ANALISE_RISCOS_CONS (ANALISE_ID, POSICAO, CATEGORIA, SEVERIDADE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 1, 'Concorrencia', 'MEDIO', 2, '["A Oracle NetSuite esta nos procurando", "proposta agressiva"]', 'Preparar contraproposta de renovacao com garantias de SLA.');
INSERT INTO ANALISE_OPORT_CONS (ANALISE_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000001', 0, 'Upsell', 0.78, 2, '["O Fluig resolve exatamente esse fluxo", "precisamos de uma proposta ate o fim do mes"]', 'Enviar proposta do Fluig com foco em aprovacao de compras.');
INSERT INTO ANALISE_OPORT_CONS (ANALISE_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 0, 'Melhoria no atendimento', 0.6, 1, '["vou escalar hoje"]', 'Resolver o chamado e registrar retorno formal ao cliente.');
INSERT INTO ANALISE_OPORT_CONS (ANALISE_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000003', 0, 'Novo produto', 0.81, 2, '["Temos verba aprovada de R$ 120 mil", "implantar antes do quarto trimestre"]', 'Apresentar Protheus Distribuicao e Logistica com cronograma de implantacao.');
INSERT INTO ANALISE_OPORT_CONS (ANALISE_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000004', 0, 'Upsell', 0.55, 1, '["apresentar o modulo de cobranca automatica"]', 'Enviar simulacao de economia com cobranca automatica.');
INSERT INTO ANALISE_OPORT_CONS (ANALISE_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 0, 'Upsell', 0.42, 1, '["Precisamos de garantias antes de renovar"]', 'Incluir monitoramento proativo do PDV na renovacao.');
INSERT INTO ANALISE_OPORT_CONS (ANALISE_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000006', 0, 'Cross-sell', 0.74, 2, '["capital de giro para antecipar recebiveis", "Pode mandar a proposta"]', 'Enviar proposta do TOTVS Techfin de antecipacao de recebiveis.');
INSERT INTO ANALISE_ALERTAS_CONS (ANALISE_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000001', 0, 'Oportunidade alta', 'Upsell', 'MEDIO', 2, 'Cliente pediu proposta do Fluig com prazo definido.', 'Enviar proposta ate o fim do mes.');
INSERT INTO ANALISE_ALERTAS_CONS (ANALISE_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 0, 'Risco de churn', 'Churn', 'ALTO', 2, 'Cliente verbalizou cancelamento e troca de fornecedor.', 'Acionar plano de retencao imediato com lideranca comercial.');
INSERT INTO ANALISE_ALERTAS_CONS (ANALISE_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000002', 1, 'Concorrente citado', 'Concorrencia', 'MEDIO', 1, 'Senior apresentou proposta ao cliente.', 'Reforcar diferenciais e roadmap do RM.');
INSERT INTO ANALISE_ALERTAS_CONS (ANALISE_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 0, 'Perda de confianca', 'Perda de confianca', 'ALTO', 1, 'Cliente declarou ter perdido a confianca apos incidentes.', 'Plano de comunicacao de incidentes e reuniao com diretoria de CS.');
INSERT INTO ANALISE_ALERTAS_CONS (ANALISE_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000005', 1, 'Concorrente citado', 'Concorrencia', 'MEDIO', 2, 'Oracle NetSuite em negociacao com o cliente.', 'Contraproposta de renovacao com garantias de SLA.');
INSERT INTO ANALISE_ALERTAS_CONS (ANALISE_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000006', 0, 'Oportunidade alta', 'Cross-sell', 'MEDIO', 2, 'CFO aprovou avancar com antecipacao de recebiveis.', 'Enviar proposta do TOTVS Techfin.');
INSERT INTO ANALISE_ITENS_CRITICOS (ANALISE_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('dddddddd-0000-4000-8000-000000000001', 0, 'oportunidade', 'Upsell', 'O Fluig resolve exatamente esse fluxo de aprovacao.', 'MEDIO');
INSERT INTO ANALISE_ITENS_CRITICOS (ANALISE_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('dddddddd-0000-4000-8000-000000000002', 0, 'risco', 'Churn', 'Se nao resolver ate o fim do mes, vamos cancelar o contrato.', 'ALTO');
INSERT INTO ANALISE_ITENS_CRITICOS (ANALISE_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('dddddddd-0000-4000-8000-000000000002', 1, 'risco', 'Concorrencia', 'A Senior ja nos apresentou uma proposta e a diretoria esta pensando em trocar.', 'MEDIO');
INSERT INTO ANALISE_ITENS_CRITICOS (ANALISE_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('dddddddd-0000-4000-8000-000000000003', 0, 'oportunidade', 'Novo produto', 'Temos verba aprovada de R$ 120 mil para este ano.', 'MEDIO');
INSERT INTO ANALISE_ITENS_CRITICOS (ANALISE_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('dddddddd-0000-4000-8000-000000000005', 0, 'risco', 'Perda de confianca', 'Sinceramente, perdemos a confianca.', 'ALTO');
INSERT INTO ANALISE_ITENS_CRITICOS (ANALISE_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('dddddddd-0000-4000-8000-000000000005', 1, 'risco', 'Concorrencia', 'A Oracle NetSuite esta nos procurando com uma proposta agressiva.', 'MEDIO');
INSERT INTO ANALISE_ITENS_CRITICOS (ANALISE_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('dddddddd-0000-4000-8000-000000000006', 0, 'oportunidade', 'Cross-sell', 'Nosso gargalo hoje e capital de giro para antecipar recebiveis.', 'MEDIO');

-- -------------------------------------------------------------------------------------
-- ALERTAS - 8 alertas acionaveis (todos NOVO na criacao)
-- -------------------------------------------------------------------------------------
INSERT INTO ALERTAS (ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, MOTIVO, RECOMENDACAO, OCORRENCIAS, PRIORIDADE, STATUS, DATA_CRIACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000002', 'dddddddd-0000-4000-8000-000000000001', 'OPORTUNIDADE_ALTA', 'Oportunidade de UPSELL: TOTVS Fluig', 'OPORTUNIDADE_ALTA', 'Oportunidade de UPSELL com alta probabilidade: aprovacao de compras com TOTVS Fluig (budget R$ 60.000,00).', NULL, NULL, NULL, 'MEDIO', 'NOVO', TO_TIMESTAMP('2026-08-05 08:03:13', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ALERTAS (ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, MOTIVO, RECOMENDACAO, OCORRENCIAS, PRIORIDADE, STATUS, DATA_CRIACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000002', 'aaaaaaaa-0000-4000-8000-000000000002', 'dddddddd-0000-4000-8000-000000000002', 'RISCO_CHURN', 'RISCO_CHURN', 'RISCO_CHURN', 'Risco CHURN detectado para cliente Clinica Vida Plena: Cliente verbalizou cancelamento. Trecho: "vamos cancelar o contrato".', NULL, NULL, NULL, 'ALTO', 'NOVO', TO_TIMESTAMP('2026-08-05 08:04:41', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ALERTAS (ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, MOTIVO, RECOMENDACAO, OCORRENCIAS, PRIORIDADE, STATUS, DATA_CRIACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000003', 'aaaaaaaa-0000-4000-8000-000000000002', 'dddddddd-0000-4000-8000-000000000002', 'CONSOLIDADO_CHURN', 'Risco de churn', 'Churn', 'Cliente verbalizou cancelamento, troca ou nao renovacao. Ocorrencias: 2. Recomendacao: Acionar plano de retencao imediato com lideranca comercial.', 'Cliente verbalizou cancelamento, troca ou nao renovacao.', 'Acionar plano de retencao imediato com lideranca comercial.', 2, 'ALTO', 'NOVO', TO_TIMESTAMP('2026-08-05 08:04:41', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ALERTAS (ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, MOTIVO, RECOMENDACAO, OCORRENCIAS, PRIORIDADE, STATUS, DATA_CRIACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000004', 'aaaaaaaa-0000-4000-8000-000000000002', 'dddddddd-0000-4000-8000-000000000002', 'CONCORRENTE_DETECTADO', 'CONCORRENTE_DETECTADO', 'CONCORRENTE_DETECTADO', 'Concorrente Senior mencionado pelo cliente Clinica Vida Plena: "A Senior ja nos apresentou uma proposta".', NULL, NULL, NULL, 'MEDIO', 'NOVO', TO_TIMESTAMP('2026-08-05 08:04:42', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ALERTAS (ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, MOTIVO, RECOMENDACAO, OCORRENCIAS, PRIORIDADE, STATUS, DATA_CRIACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000005', 'aaaaaaaa-0000-4000-8000-000000000003', 'dddddddd-0000-4000-8000-000000000003', 'OPORTUNIDADE_ALTA', 'Oportunidade de NOVO_PRODUTO: TOTVS Protheus', 'OPORTUNIDADE_ALTA', 'Oportunidade de NOVO_PRODUTO com alta probabilidade: Protheus Distribuicao e Logistica (verba aprovada R$ 120.000,00).', NULL, NULL, NULL, 'MEDIO', 'NOVO', TO_TIMESTAMP('2026-08-12 09:12:06', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ALERTAS (ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, MOTIVO, RECOMENDACAO, OCORRENCIAS, PRIORIDADE, STATUS, DATA_CRIACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000006', 'aaaaaaaa-0000-4000-8000-000000000004', 'dddddddd-0000-4000-8000-000000000005', 'RISCO_PERDA_CONFIANCA', 'RISCO_PERDA_CONFIANCA', 'RISCO_PERDA_CONFIANCA', 'Risco PERDA_CONFIANCA detectado para cliente Supermercados Bom Preco: "Sinceramente, perdemos a confianca".', NULL, NULL, NULL, 'ALTO', 'NOVO', TO_TIMESTAMP('2026-08-20 08:05:23', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ALERTAS (ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, MOTIVO, RECOMENDACAO, OCORRENCIAS, PRIORIDADE, STATUS, DATA_CRIACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000007', 'aaaaaaaa-0000-4000-8000-000000000004', 'dddddddd-0000-4000-8000-000000000005', 'CONSOLIDADO_CONCORRENCIA', 'Concorrente citado', 'Concorrencia', 'Oracle NetSuite em negociacao com o cliente. Ocorrencias: 2. Recomendacao: Contraproposta de renovacao com garantias de SLA.', 'Oracle NetSuite em negociacao com o cliente.', 'Contraproposta de renovacao com garantias de SLA.', 2, 'MEDIO', 'NOVO', TO_TIMESTAMP('2026-08-20 08:05:23', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ALERTAS (ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, MOTIVO, RECOMENDACAO, OCORRENCIAS, PRIORIDADE, STATUS, DATA_CRIACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000008', 'aaaaaaaa-0000-4000-8000-000000000002', 'dddddddd-0000-4000-8000-000000000006', 'OPORTUNIDADE_ALTA', 'Oportunidade de CROSS_SELL: TOTVS Techfin', 'OPORTUNIDADE_ALTA', 'Oportunidade de CROSS_SELL com alta probabilidade: antecipacao de recebiveis com TOTVS Techfin.', NULL, NULL, NULL, 'MEDIO', 'NOVO', TO_TIMESTAMP('2026-08-25 17:12:31', 'YYYY-MM-DD HH24:MI:SS'));

-- -------------------------------------------------------------------------------------
-- ALERTA_STATUS_HIST - registro de criacao de cada alerta (as mudancas seguintes vem do trigger)
-- -------------------------------------------------------------------------------------
INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000001', NULL, 'NOVO', TO_TIMESTAMP('2026-08-05 08:03:13', 'YYYY-MM-DD HH24:MI:SS'), 'LEANIA', 'Alerta gerado pela analise');
INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000002', NULL, 'NOVO', TO_TIMESTAMP('2026-08-05 08:04:41', 'YYYY-MM-DD HH24:MI:SS'), 'LEANIA', 'Alerta gerado pela analise');
INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000003', NULL, 'NOVO', TO_TIMESTAMP('2026-08-05 08:04:41', 'YYYY-MM-DD HH24:MI:SS'), 'LEANIA', 'Alerta gerado pela analise');
INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000004', NULL, 'NOVO', TO_TIMESTAMP('2026-08-05 08:04:42', 'YYYY-MM-DD HH24:MI:SS'), 'LEANIA', 'Alerta gerado pela analise');
INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000005', NULL, 'NOVO', TO_TIMESTAMP('2026-08-12 09:12:06', 'YYYY-MM-DD HH24:MI:SS'), 'LEANIA', 'Alerta gerado pela analise');
INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000006', NULL, 'NOVO', TO_TIMESTAMP('2026-08-20 08:05:23', 'YYYY-MM-DD HH24:MI:SS'), 'LEANIA', 'Alerta gerado pela analise');
INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000007', NULL, 'NOVO', TO_TIMESTAMP('2026-08-20 08:05:23', 'YYYY-MM-DD HH24:MI:SS'), 'LEANIA', 'Alerta gerado pela analise');
INSERT INTO ALERTA_STATUS_HIST (ALERTA_ID, STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR, OBSERVACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000008', NULL, 'NOVO', TO_TIMESTAMP('2026-08-25 17:12:31', 'YYYY-MM-DD HH24:MI:SS'), 'LEANIA', 'Alerta gerado pela analise');

-- -------------------------------------------------------------------------------------
-- ANALISE_FEEDBACKS - 6 avaliacoes dos usuarios
-- -------------------------------------------------------------------------------------
INSERT INTO ANALISE_FEEDBACKS (ID, OWNER_ID, TRANSCRICAO_ID, ANALISE_ID, UTIL, AJUSTE, CREATED_AT)
VALUES ('ffffffff-0000-4000-8000-000000000001', 'aaaaaaaa-0000-4000-8000-000000000002', 'cccccccc-0000-4000-8000-000000000001', 'dddddddd-0000-4000-8000-000000000001', 1, 'Persona correta; o budget de R$ 60 mil e anual, nao mensal.', TO_TIMESTAMP('2026-08-05 10:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ANALISE_FEEDBACKS (ID, OWNER_ID, TRANSCRICAO_ID, ANALISE_ID, UTIL, AJUSTE, CREATED_AT)
VALUES ('ffffffff-0000-4000-8000-000000000002', 'aaaaaaaa-0000-4000-8000-000000000002', 'cccccccc-0000-4000-8000-000000000002', 'dddddddd-0000-4000-8000-000000000002', 1, 'Risco de churn confirmado com o cliente; alerta foi essencial.', TO_TIMESTAMP('2026-08-06 09:30:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ANALISE_FEEDBACKS (ID, OWNER_ID, TRANSCRICAO_ID, ANALISE_ID, UTIL, AJUSTE, CREATED_AT)
VALUES ('ffffffff-0000-4000-8000-000000000003', 'aaaaaaaa-0000-4000-8000-000000000003', 'cccccccc-0000-4000-8000-000000000003', 'dddddddd-0000-4000-8000-000000000003', 1, NULL, TO_TIMESTAMP('2026-08-12 11:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ANALISE_FEEDBACKS (ID, OWNER_ID, TRANSCRICAO_ID, ANALISE_ID, UTIL, AJUSTE, CREATED_AT)
VALUES ('ffffffff-0000-4000-8000-000000000004', 'aaaaaaaa-0000-4000-8000-000000000003', 'cccccccc-0000-4000-8000-000000000004', 'dddddddd-0000-4000-8000-000000000004', 0, 'A lentidao do portal e um risco de insatisfacao MEDIO, nao BAIXO.', TO_TIMESTAMP('2026-08-15 08:20:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ANALISE_FEEDBACKS (ID, OWNER_ID, TRANSCRICAO_ID, ANALISE_ID, UTIL, AJUSTE, CREATED_AT)
VALUES ('ffffffff-0000-4000-8000-000000000005', 'aaaaaaaa-0000-4000-8000-000000000001', 'cccccccc-0000-4000-8000-000000000005', 'dddddddd-0000-4000-8000-000000000005', 0, 'Feedback duplicado enviado por engano.', TO_TIMESTAMP('2026-08-20 09:00:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ANALISE_FEEDBACKS (ID, OWNER_ID, TRANSCRICAO_ID, ANALISE_ID, UTIL, AJUSTE, CREATED_AT)
VALUES ('ffffffff-0000-4000-8000-000000000006', 'aaaaaaaa-0000-4000-8000-000000000004', 'cccccccc-0000-4000-8000-000000000006', 'dddddddd-0000-4000-8000-000000000006', 1, 'Cross-sell de Techfin identificado corretamente; proposta enviada no mesmo dia.', TO_TIMESTAMP('2026-08-26 08:15:00', 'YYYY-MM-DD HH24:MI:SS'));

COMMIT;

-- =====================================================================================
--  2. UPDATE - EVOLUCAO DOS PROCESSOS
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 2.1 Transcricao 'Construtora Alvorada': PENDENTE > PROCESSANDO > PROCESSADA (com a analise gerada)
-- -------------------------------------------------------------------------------------
-- a) a fila da LeanIA pega a reuniao: PENDENTE > PROCESSANDO (o trigger grava o historico)
UPDATE TRANSCRICOES
   SET STATUS = 'PROCESSANDO'
 WHERE ID = 'cccccccc-0000-4000-8000-000000000007';

-- b) a LeanIA devolve a analise: inserimos a analise e seus detalhes
INSERT INTO ANALISES (ID, OWNER_ID, TRANSCRICAO_ID, SENTIMENTO_GERAL, RESUMO, PERSONA_IDENTIFICADA, BUDGET_IDENTIFICADO, SCORE_CONFIANCA_VENDEDOR, SCORE_SENTIMENTO, MET_TOTAL_PARTES, MET_PARTES_ANALISADAS, MET_PARTES_NEGATIVAS, MET_PARTES_NEUTRAS, MET_PARTES_POSITIVAS, MET_RISCOS_DETECTADOS, MET_OPORTUNIDADES_DETECTADAS, MET_CHURNS_POTENCIAIS, MET_MENCOES_CRITICAS, MET_ALERTAS_CONSOLIDADOS, MET_CHUNKS_PROCESSADOS, RESUMO_EXECUTIVO, DATA_ANALISE, PROXIMA_ACAO, SENTIMENTO_DETALHE, INSIGHTS_JSON, INDICADORES_JSON)
VALUES ('dddddddd-0000-4000-8000-000000000007', 'aaaaaaaa-0000-4000-8000-000000000003', 'cccccccc-0000-4000-8000-000000000007', 'POSITIVO', 'Sentimento positivo. Boa probabilidade de venda de novo produto (Protheus Construcao e Projetos). Interlocutor: Gerente de Obras (influenciador tecnico). Budget nao mencionado.', 'Gerente de Obras (influenciador tecnico)', 'Nao identificado', 0.7, 0.55, 3, 3, 1, 0, 2, 1, 1, 0, 0, 1, 1, 'Lead com dor clara (controle de obras em planilha) e pedido de demonstracao. Agendar demo do Protheus Construcao e Projetos.', SYSTIMESTAMP, 'Enviar proposta ou agendar demonstracao com os decisores nos proximos dias.', 'Positivo (interesse na demonstracao)', '{"elogios":[],"reclamacoes":["perdemos prazo de medicao"],"duvidasJornada":[],"proximosPassos":["Queremos ver uma demonstracao ainda este mes"],"observacoes":[]}', '{"locutores":2,"turnos":3,"sentencas":5,"palavras":49,"participacaoVendedorPct":40,"participacaoClientePct":60,"perguntasVendedor":0,"perguntasCliente":0,"escutaAtiva":"Equilibrado","tokensAnonimizados":0,"qualidadeTranscricao":"alta"}');
INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO)
VALUES ('dddddddd-0000-4000-8000-000000000007', 0, 'TOTVS Protheus', 'OPORTUNIDADE', 'O Protheus Construcao e Projetos integra medicao, compras e financeiro');
INSERT INTO ANALISE_OPORTUNIDADES (ANALISE_ID, POSICAO, TIPO, DESCRICAO, PRODUTO_RELACIONADO, SCORE, VALOR_ESTIMADO)
VALUES ('dddddddd-0000-4000-8000-000000000007', 0, 'NOVO_PRODUTO', 'Implantacao do Protheus Construcao e Projetos no lugar das planilhas.', 'TOTVS Protheus', 0.7, 'Nao estimado');
INSERT INTO ANALISE_RISCOS (ANALISE_ID, POSICAO, TIPO, DESCRICAO, SEVERIDADE, CONCORRENTE_MENCIONADO)
VALUES ('dddddddd-0000-4000-8000-000000000007', 0, 'INSATISFACAO', 'Perda de prazo de medicao por controle em planilha (dor do processo atual).', 'BAIXO', NULL);
INSERT INTO ANALISE_OPORT_CONS (ANALISE_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000007', 0, 'Novo produto', 0.7, 1, '["Queremos ver uma demonstracao ainda este mes"]', 'Agendar demonstracao do Protheus Construcao e Projetos.');
INSERT INTO ANALISE_ALERTAS_CONS (ANALISE_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('dddddddd-0000-4000-8000-000000000007', 0, 'Oportunidade alta', 'Novo produto', 'MEDIO', 1, 'Lead pediu demonstracao com prazo.', 'Agendar demonstracao ainda este mes.');
INSERT INTO ANALISE_ITENS_CRITICOS (ANALISE_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('dddddddd-0000-4000-8000-000000000007', 0, 'oportunidade', 'Novo produto', 'Queremos ver uma demonstracao ainda este mes.', 'MEDIO');
INSERT INTO TRANSCRICAO_OPORT_CONS (TRANSCRICAO_ID, POSICAO, CATEGORIA, SCORE, OCORRENCIAS, EVIDENCIAS, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000007', 0, 'Novo produto', 0.7, 1, '["Queremos ver uma demonstracao ainda este mes"]', 'Agendar demonstracao do Protheus Construcao e Projetos.');
INSERT INTO TRANSCRICAO_ALERTAS_CONS (TRANSCRICAO_ID, POSICAO, TITULO, CATEGORIA, SEVERIDADE, OCORRENCIAS, MOTIVO, RECOMENDACAO)
VALUES ('cccccccc-0000-4000-8000-000000000007', 0, 'Oportunidade alta', 'Novo produto', 'MEDIO', 1, 'Lead pediu demonstracao com prazo.', 'Agendar demonstracao ainda este mes.');
INSERT INTO TRANSCRICAO_ITENS_CRITICOS (TRANSCRICAO_ID, POSICAO, TIPO, CATEGORIA, TRECHO, SEVERIDADE)
VALUES ('cccccccc-0000-4000-8000-000000000007', 0, 'oportunidade', 'Novo produto', 'Queremos ver uma demonstracao ainda este mes.', 'MEDIO');

-- c) a transcricao passa a PROCESSADA, recebe a analise atual, as metricas e o resumo executivo
UPDATE TRANSCRICOES
   SET STATUS = 'PROCESSADA',
       DATA_PROCESSAMENTO = SYSTIMESTAMP,
       ANALISE_ID = 'dddddddd-0000-4000-8000-000000000007',
       MET_TOTAL_PARTES = 3, MET_PARTES_ANALISADAS = 3, MET_PARTES_NEGATIVAS = 1, MET_PARTES_NEUTRAS = 0,
       MET_PARTES_POSITIVAS = 2, MET_RISCOS_DETECTADOS = 1, MET_OPORTUNIDADES_DETECTADAS = 1,
       MET_CHURNS_POTENCIAIS = 0, MET_MENCOES_CRITICAS = 0, MET_ALERTAS_CONSOLIDADOS = 1, MET_CHUNKS_PROCESSADOS = 1,
       RESUMO_EXECUTIVO = 'Lead com dor clara (controle de obras em planilha) e pedido de demonstracao. Agendar demo do Protheus Construcao e Projetos.'
 WHERE ID = 'cccccccc-0000-4000-8000-000000000007';

-- d) o alerta da nova oportunidade e gerado
INSERT INTO ALERTAS (ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, MOTIVO, RECOMENDACAO, OCORRENCIAS, PRIORIDADE, STATUS, DATA_CRIACAO)
VALUES ('eeeeeeee-0000-4000-8000-000000000009', 'aaaaaaaa-0000-4000-8000-000000000003', 'dddddddd-0000-4000-8000-000000000007', 'OPORTUNIDADE_ALTA', 'Oportunidade de NOVO_PRODUTO: TOTVS Protheus', 'OPORTUNIDADE_ALTA', 'Oportunidade de NOVO_PRODUTO com alta probabilidade: Protheus Construcao e Projetos (demonstracao solicitada).', NULL, NULL, NULL, 'MEDIO', 'NOVO', SYSTIMESTAMP);

-- conferencia: o historico mostra PENDENTE > PROCESSANDO > PROCESSADA
SELECT STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, ALTERADO_POR
  FROM TRANSCRICAO_STATUS_HIST
 WHERE TRANSCRICAO_ID = 'cccccccc-0000-4000-8000-000000000007'
 ORDER BY ID;

-- -------------------------------------------------------------------------------------
-- 2.2 Transcricao 'Agro Serra Verde': PROCESSANDO > ERRO (LeanIA indisponivel) e observacao no historico
-- -------------------------------------------------------------------------------------
UPDATE TRANSCRICOES
   SET STATUS = 'ERRO'
 WHERE ID = 'cccccccc-0000-4000-8000-000000000008';

UPDATE TRANSCRICAO_STATUS_HIST
   SET OBSERVACAO = 'Timeout na LeanIA apos 3 tentativas; reenfileirar pelo job de recuperacao'
 WHERE TRANSCRICAO_ID = 'cccccccc-0000-4000-8000-000000000008'
   AND STATUS_NOVO = 'ERRO';

-- -------------------------------------------------------------------------------------
-- 2.3 Alertas: NOVO > VISTO > RESOLVIDO (tratamento do risco de churn) e NOVO > VISTO
-- -------------------------------------------------------------------------------------
UPDATE ALERTAS SET STATUS = 'VISTO'     WHERE ID = 'eeeeeeee-0000-4000-8000-000000000002';
UPDATE ALERTAS SET STATUS = 'RESOLVIDO' WHERE ID = 'eeeeeeee-0000-4000-8000-000000000002';
UPDATE ALERTAS SET STATUS = 'VISTO'     WHERE ID IN ('eeeeeeee-0000-4000-8000-000000000003', 'eeeeeeee-0000-4000-8000-000000000006');

UPDATE ALERTA_STATUS_HIST
   SET OBSERVACAO = 'Reuniao de retencao realizada em 08/08/2026; cliente manteve o contrato do RM'
 WHERE ALERTA_ID = 'eeeeeeee-0000-4000-8000-000000000002'
   AND STATUS_NOVO = 'RESOLVIDO';

-- conferencia: ciclo completo do alerta de churn
SELECT STATUS_ANTERIOR, STATUS_NOVO, ALTERADO_EM, OBSERVACAO
  FROM ALERTA_STATUS_HIST
 WHERE ALERTA_ID = 'eeeeeeee-0000-4000-8000-000000000002'
 ORDER BY ID;

-- -------------------------------------------------------------------------------------
-- 2.4 Feedback: usuario revisa a avaliacao (nao util > util) e complementa o ajuste
-- -------------------------------------------------------------------------------------
UPDATE ANALISE_FEEDBACKS
   SET UTIL = 1,
       AJUSTE = 'Apos revisao: a severidade MEDIO faz sentido no periodo de matriculas. Analise util.'
 WHERE ID = 'ffffffff-0000-4000-8000-000000000004';

-- -------------------------------------------------------------------------------------
-- 2.5 Sessoes e usuarios: logout revoga o refresh token; analista de BI reativada; NPS atualizado apos nova pesquisa
-- -------------------------------------------------------------------------------------
UPDATE REFRESH_TOKENS SET REVOKED_AT = SYSTIMESTAMP WHERE ID = 'bbbbbbbb-0000-4000-8000-000000000002' AND REVOKED_AT IS NULL;
UPDATE APP_USERS SET IS_ACTIVE = 1 WHERE ID = 'aaaaaaaa-0000-4000-8000-000000000005';
UPDATE TRANSCRICOES SET NOTA_NPS = 9, DT_ULTIMA_PESQUISA = '2026-09' WHERE ID = 'cccccccc-0000-4000-8000-000000000003';

COMMIT;

-- =====================================================================================
--  3. DELETE - EXCLUSOES COM INTEGRIDADE REFERENCIAL
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 3.1 Sessao expirada e revogada ha mais de 7 dias (limpeza de refresh tokens)
-- -------------------------------------------------------------------------------------
DELETE FROM REFRESH_TOKENS
 WHERE ID = 'bbbbbbbb-0000-4000-8000-000000000005'
   AND EXPIRES_AT < SYSTIMESTAMP;

-- -------------------------------------------------------------------------------------
-- 3.2 Feedback enviado por engano (nao util e marcado como duplicado)
-- -------------------------------------------------------------------------------------
DELETE FROM ANALISE_FEEDBACKS
 WHERE ID = 'ffffffff-0000-4000-8000-000000000005'
   AND UTIL = 0;

-- -------------------------------------------------------------------------------------
-- 3.3 Transcricao com ERRO e texto vazio ('Hospital Sao Lucas'): o ON DELETE CASCADE remove avisos e historico
-- -------------------------------------------------------------------------------------
-- antes: avisos e historico da reuniao
SELECT (SELECT COUNT(*) FROM TRANSCRICAO_WARNINGS    WHERE TRANSCRICAO_ID = 'cccccccc-0000-4000-8000-000000000009') AS AVISOS,
       (SELECT COUNT(*) FROM TRANSCRICAO_STATUS_HIST WHERE TRANSCRICAO_ID = 'cccccccc-0000-4000-8000-000000000009') AS HISTORICO
  FROM DUAL;

DELETE FROM TRANSCRICOES
 WHERE ID = 'cccccccc-0000-4000-8000-000000000009';

-- depois: zero (removidos em cascata)
SELECT (SELECT COUNT(*) FROM TRANSCRICAO_WARNINGS    WHERE TRANSCRICAO_ID = 'cccccccc-0000-4000-8000-000000000009') AS AVISOS,
       (SELECT COUNT(*) FROM TRANSCRICAO_STATUS_HIST WHERE TRANSCRICAO_ID = 'cccccccc-0000-4000-8000-000000000009') AS HISTORICO
  FROM DUAL;

-- -------------------------------------------------------------------------------------
-- 3.4 Reprocessamento: apagar a analise da 'Colegio Nova Geracao' zera TRANSCRICOES.ANALISE_ID (ON DELETE SET NULL)
--     e ANALISE_FEEDBACKS.ANALISE_ID (SET NULL); os detalhes da analise saem em cascata
-- -------------------------------------------------------------------------------------
DELETE FROM ANALISES
 WHERE ID = 'dddddddd-0000-4000-8000-000000000004';

SELECT ID, CLIENTE, STATUS, ANALISE_ID FROM TRANSCRICOES WHERE ID = 'cccccccc-0000-4000-8000-000000000004';
SELECT ID, UTIL, ANALISE_ID FROM ANALISE_FEEDBACKS WHERE TRANSCRICAO_ID = 'cccccccc-0000-4000-8000-000000000004';

COMMIT;

-- =====================================================================================
--  4. CONFERENCIA - registros por tabela apos a carga, as atualizacoes e as exclusoes
-- =====================================================================================
SELECT 'APP_USERS' AS TABELA, COUNT(*) AS REGISTROS FROM APP_USERS
UNION ALL
SELECT 'REFRESH_TOKENS' AS TABELA, COUNT(*) AS REGISTROS FROM REFRESH_TOKENS
UNION ALL
SELECT 'TRANSCRICOES' AS TABELA, COUNT(*) AS REGISTROS FROM TRANSCRICOES
UNION ALL
SELECT 'TRANSCRICAO_WARNINGS' AS TABELA, COUNT(*) AS REGISTROS FROM TRANSCRICAO_WARNINGS
UNION ALL
SELECT 'TRANSCRICAO_RISCOS_CONS' AS TABELA, COUNT(*) AS REGISTROS FROM TRANSCRICAO_RISCOS_CONS
UNION ALL
SELECT 'TRANSCRICAO_OPORT_CONS' AS TABELA, COUNT(*) AS REGISTROS FROM TRANSCRICAO_OPORT_CONS
UNION ALL
SELECT 'TRANSCRICAO_ALERTAS_CONS' AS TABELA, COUNT(*) AS REGISTROS FROM TRANSCRICAO_ALERTAS_CONS
UNION ALL
SELECT 'TRANSCRICAO_ITENS_CRITICOS' AS TABELA, COUNT(*) AS REGISTROS FROM TRANSCRICAO_ITENS_CRITICOS
UNION ALL
SELECT 'TRANSCRICAO_STATUS_HIST' AS TABELA, COUNT(*) AS REGISTROS FROM TRANSCRICAO_STATUS_HIST
UNION ALL
SELECT 'ANALISES' AS TABELA, COUNT(*) AS REGISTROS FROM ANALISES
UNION ALL
SELECT 'ANALISE_PRODUTOS' AS TABELA, COUNT(*) AS REGISTROS FROM ANALISE_PRODUTOS
UNION ALL
SELECT 'ANALISE_OPORTUNIDADES' AS TABELA, COUNT(*) AS REGISTROS FROM ANALISE_OPORTUNIDADES
UNION ALL
SELECT 'ANALISE_RISCOS' AS TABELA, COUNT(*) AS REGISTROS FROM ANALISE_RISCOS
UNION ALL
SELECT 'ANALISE_RISCOS_CONS' AS TABELA, COUNT(*) AS REGISTROS FROM ANALISE_RISCOS_CONS
UNION ALL
SELECT 'ANALISE_OPORT_CONS' AS TABELA, COUNT(*) AS REGISTROS FROM ANALISE_OPORT_CONS
UNION ALL
SELECT 'ANALISE_ALERTAS_CONS' AS TABELA, COUNT(*) AS REGISTROS FROM ANALISE_ALERTAS_CONS
UNION ALL
SELECT 'ANALISE_ITENS_CRITICOS' AS TABELA, COUNT(*) AS REGISTROS FROM ANALISE_ITENS_CRITICOS
UNION ALL
SELECT 'ALERTAS' AS TABELA, COUNT(*) AS REGISTROS FROM ALERTAS
UNION ALL
SELECT 'ALERTA_STATUS_HIST' AS TABELA, COUNT(*) AS REGISTROS FROM ALERTA_STATUS_HIST
UNION ALL
SELECT 'ANALISE_FEEDBACKS' AS TABELA, COUNT(*) AS REGISTROS FROM ANALISE_FEEDBACKS
ORDER BY 1;
