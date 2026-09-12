# Memory — ChallengeTotvs

## Overview
<!-- Describe the project scope and goals here -->

## Architecture decisions
<!-- Key decisions made during development -->

## Current state
<!-- What has been done, what is in progress, what is next -->

## JSON transcription multi-record import

### Problem

The previous TOTVS JSON importer intentionally aggregated every JSON object into one `Transcricao` document. This mixed independent meetings and made listing, history, filters, dashboard metrics, and per-meeting analysis incorrect.

### Supported formats

- A root array of meeting objects.
- A root object containing a meeting list under `transcricoes` or any other key.
- A single meeting object.
- Nested objects containing arrays of meeting objects.
- Multiple top-level JSON objects separated by whitespace/newlines, as used by `ANON_transcricao.json`.

The parser treats a meeting object as one transcription. It never splits the text by speaker or by individual speech.

### Detection and mapping

`TotvsTranscriptionJsonParser` reads the upload as a stream. Root arrays are consumed item by item, which avoids loading the complete 48 MB sample into memory. Nested arrays are detected when their objects contain `ANON_TRANSCRICAO`.

Each valid item maps:

- `ID_MEETING` to `meetingId`.
- `DT_MEETING` to `meetingDate` and, when valid, `dataReuniao`.
- `FORMATO_MEETING` to `format`.
- `STATUS_MEETING` to `meetingStatus` (the existing `status` remains the application processing status).
- `DURACAO_MEETING` to `duration` and `duracaoMinutos`.
- `CODT` to `codt`.
- `FLG_EXTERNO` to `external`.
- `DT_CRIACAO` to `createdAtSource`.
- `ANON_TRANSCRICAO` to `transcriptionText` and the existing `textoOriginal` field.
- Upload metadata to `sourceFileName`, `importBatchId`, and `importedAt`.

Missing `ID_MEETING` values receive a `generated-<uuid>` identifier and set `missingMeetingId=true`. Invalid source dates are preserved as strings, reported as item warnings, and do not stop the batch.

### Duplicate rule and result

`meetingId` has a sparse unique MongoDB index. The importer also checks the repository and IDs already seen in the same file before saving. Existing IDs are skipped by default.

The endpoint keeps the existing route and multipart upload flow. Its response now includes `success`, `sourceFile`, `batchId`, `found`, `imported`, `skipped`, `failed`, and one status item per detected meeting. Legacy counters remain in the response for compatibility.

### Verification

Automated tests cover a single object, a three-item root array, a list under an arbitrary root key, a database duplicate, and an invalid item mixed with valid items.

The real `ANON_transcricao.json` file was tested through the streaming importer on June 15, 2026:

- File size: 48,441,117 bytes.
- Meetings detected: 1,174.
- Unique `ID_MEETING` values imported into an empty mocked repository: 1,126.
- Duplicate occurrences skipped within the file: 48.
- Invalid/empty `ANON_TRANSCRICAO` items: 0.

### Limitations

- Duplicate handling defaults to skip; overwrite mode is not implemented.
- The detailed response contains one item per meeting, so very large imports produce a correspondingly large response.
- Existing MongoDB documents remain readable because all new fields are additive. Automatic index creation must be allowed so the sparse unique `meetingId` index is created.

## Migracao MongoDB -> Oracle Database (2026-09-11)

### Decisao

O backend passou a usar Spring Data JPA + Hibernate 6.5 com o driver `ojdbc11` contra o Oracle 19c da FIAP
(`oracle.fiap.com.br:1521/orcl`). O `spring-boot-starter-data-mongodb` foi removido. Credenciais entram apenas
por `ORACLE_URL`, `ORACLE_USER` e `ORACLE_PASSWORD`.

### Schema

- Script versionado em `backend/src/main/resources/db/oracle/schema.sql` (e `drop.sql`), executado uma vez no
  SQL Developer/SQLcl. O app sobe com `spring.jpa.hibernate.ddl-auto=validate` e nunca altera o schema.
- 18 tabelas: `APP_USERS`, `REFRESH_TOKENS`, `TRANSCRICOES` (+ `TRANSCRICAO_WARNINGS`, `_RISCOS_CONS`,
  `_OPORT_CONS`, `_ALERTAS_CONS`, `_ITENS_CRITICOS`), `ANALISES` (+ `ANALISE_PRODUTOS`, `_OPORTUNIDADES`,
  `_RISCOS`, `_RISCOS_CONS`, `_OPORT_CONS`, `_ALERTAS_CONS`, `_ITENS_CRITICOS`), `ALERTAS`, `ANALISE_FEEDBACKS`.
- Listas embutidas do Mongo viraram tabelas filhas com coluna `POSICAO` (ordem da lista). Textos livres da IA
  e as transcricoes sao CLOB (VARCHAR2 limitado a 4000 bytes). `EVIDENCIAS` e uma lista JSON em CLOB.
- Integridade: `ANALISES.TRANSCRICAO_ID`, `ALERTAS.ANALISE_ID` e tabelas filhas com ON DELETE CASCADE;
  `TRANSCRICOES.ANALISE_ID` e `ANALISE_FEEDBACKS.ANALISE_ID` com ON DELETE SET NULL; `OWNER_ID` referencia
  `APP_USERS`. `MEETING_ID` unico (NULL nao entra no indice, equivalente ao indice sparse do Mongo).

### Mapeamento e armadilhas

- IDs continuam String (UUID gerado em `@PrePersist`), entao servicos, DTOs e frontend nao mudaram.
- Colecoes sao EAGER com `@Fetch(SELECT)` + `@BatchSize(100)` porque o job `@Async` le entidades fora de sessao
  (`open-in-view=false`).
- `AnaliseService` copia listas/metricas da transcricao antes de persistir a analise (evita "shared references
  to a collection" do Hibernate).
- `TranscricaoService.limparAnaliseEAlertas` zera `analiseResultado` antes do delete: com JPA o merge da
  entidade destacada tentaria recarregar a analise ja removida.
- Deletes derivados dos repositorios levam `@Transactional`; o importador trata `DataIntegrityViolationException`.
- Limites do usuario FIAP: 20 sessoes, 30 min ocioso, 4 h por conexao. Pool HikariCP com 5 conexoes e
  `max-lifetime` de 25 min.

### Verificacao

Testes unitarios verdes (17, 1 pulado). Validado contra o Oracle real: subida com `validate`, login, criacao
com texto de 12.200 caracteres (CLOB), analise assincrona, alertas, dashboard, feedback, paginacao,
reprocessamento (analise antiga removida, feedback com `ANALISE_ID` nulo) e exclusao em cascata. Os dados
de validacao foram removidos ao final; o schema esta vazio.

### Dados antigos

Os documentos do MongoDB Atlas nao foram migrados automaticamente. Reimporte os JSON TOTVS pelo endpoint
`/api/transcricoes/import/totvs` ou recadastre as transcricoes manuais.

### Deploys por branch (2026-09-12)

| Branch | Frontend (Vercel) | Backend (Render) |
| --- | --- | --- |
| `main` (MongoDB) | projeto `totvs-conversational-ai` -> https://totvs-conversational-ai.vercel.app | `challenge-totvs-backend` (starter) -> https://challenge-totvs-backend.onrender.com |
| `oracle` (Oracle FIAP) | projeto `totvs-conversational-ai-oracle` -> https://totvs-conversational-ai-oracle.vercel.app | `challenge-totvs-backend-oracle` (free, auto-deploy da branch `oracle`) -> https://challenge-totvs-backend-oracle.onrender.com |

- Vercel nao esta conectada ao GitHub: o deploy e feito pela CLI. A pasta `frontend/` esta vinculada ao projeto
  `main`; a raiz do repositorio esta vinculada ao projeto `oracle` (Root Directory `frontend` definido no projeto).
  Ou seja: `cd frontend && vercel --prod` publica o frontend da `main`; `vercel --prod` na raiz publica o da `oracle`.
- Render faz auto-deploy a cada push: `main` -> backend main, `oracle` -> backend oracle. Variaveis de ambiente
  sao editadas no dashboard ou pela API; `render restart` NAO recarrega variaveis, apenas um deploy novo.
- O plano free da Render hiberna apos inatividade; a primeira requisicao pode levar mais de 1 minuto.

### Cache de leitura no backend (2026-09-12)

Spring Cache + Caffeine (`config/CacheConfig.java`, `spring.cache.*` no `application.properties`).
Caches: `dashboard`, `transcricoes`, `transcricao`, `insights`, `alertas`, `usuarios`, `leania-contexto`;
TTL `CACHE_TTL_SECONDS` (padrao 120 s), `CACHE_MAX_ENTRIES` (padrao 1000). Toda chave inclui o usuario
autenticado (`@cacheKeys.user()` = id:papel) porque as consultas filtram por dono.

Invalidacao: `@CacheEvict(allEntries)` em criar/importar/audio/reprocessar/apagar/feedback (TranscricaoService),
marcar alerta (AlertaService), usuarios (UserService) e no fim de `TranscricaoAnalysisJob.processar` (o status
sai de PROCESSANDO). Medido localmente com `logging.level.org.hibernate.SQL=DEBUG`: dashboard 8 consultas na
1a chamada e 0 nas seguintes; `paged`, `alertas/novos` e `insights` idem; apos POST/PATCH/DELETE a chamada
seguinte volta ao banco. Anotacoes sao inertes nos testes unitarios (Mockito), nada mudou nos construtores.

## LeanIA v2 e importadores (2026-09-12)

### Objetivo
Reproduzir a "Entrega Esperada" do briefing (PDF, pagina 15) e os diferenciais da pagina 16 sem LLM pago.
Texto do briefing -> Protheus (ATUAL), RM (OPORTUNIDADE), Senior (CONCORRENTE); risco CONCORRENCIA ALTO
"para RH"; UPSELL Folha de Pagamento (TOTVS RM) score 0,9 e valor R$ 50.000,00; persona Gestor Operacional;
budget R$ 50.000,00; sentimento MISTO "Satisfeito com Backoffice / Frustrado com RH"; observacoes sobre CFO,
discricao do valor e ciclo de ROI. Validado por teste unitario (Python e Java) e pela API contra o Oracle.

### Motor
- `ai-service/analyzer.py` reescrito (pipeline por locutor/sentenca/oracao; catalogos de produtos, concorrentes
  com desambiguacao por contexto, areas -> modulo/produto; lexico com negacao e intensificadores; churn so com
  objeto de contrato/sistema; desejo de compra so em 1a pessoa e nao negado). ~230 ms por reuniao real.
- `app.py`: `sanitizar_texto` preserva quebras de linha (os turnos dependem delas). `AI_MAX_CHARS` 20000 -> 250000
  (o limite antigo rejeitava a maioria das reunioes reais e empurrava tudo para o fallback).
- Backend: `LeanIALocalAnalyzer` (mesmas regras em Java) substitui o fallback de 40 linhas; `LeanIAService`
  completa blocos que um ai-service antigo nao devolver; read timeout 30 s -> `ai.service.read-timeout-seconds` (120).
- Contrato `AiAnalysisDTO` ganhou `proximaAcao`, `sentimentoDetalhe`, `insightsAdicionais`, `indicadoresQualidade`
  (opcionais). Persistidos em ANALISES: PROXIMA_ACAO, SENTIMENTO_DETALHE, INSIGHTS_JSON, INDICADORES_JSON (CLOB via
  `JsonMapConverter`). Frontend mostra "Proxima acao", "Alem do obvio" e "Qualidade da conversa".

### Importadores
- `/api/transcricoes/import/totvs` aceita JSON (array, NDJSON, aninhado) e CSV; o formato e detectado pelo conteudo.
- `TotvsTranscriptionCsvParser` (commons-csv): UTF-8 com/sem BOM ou Windows-1252 (o CSV mock e ISO-8859-1),
  delimitador `,`/`;`/tab, cabecalho normalizado, `FAIXA_FATURAMENTO_CLIENTE` -> alias `_EC`.
- Metadados agora persistidos em TRANSCRICOES: UF, CNAE, NOME_UNIDADE, NOME_SEGMENTO, FAIXA_FATURAMENTO, TP_RECURSO,
  NOTA_NPS, DT_ULTIMA_PESQUISA (migracao `db/oracle/migrations/002_metadados_e_leania_v2.sql`, ja aplicada no schema).
- Dados de referencia: CSV mock = 500 reunioes com 4 textos repetidos; JSON real = 1.174 reunioes, 1.126 IDs unicos,
  mediana 34 mil caracteres, RM/Fluig/integracao dominam, quase nenhum concorrente citado.

### Pendencias
- O Space do Hugging Face (`lean7-challenge.hf.space`) precisa receber o novo `ai-service` (push manual); ate la o
  backend na Render usa o motor Java local quando o Space responder com o contrato antigo ou falhar.
- App Android ignora os campos novos (Gson) e continua funcionando; nao exibe os blocos novos.

### Keep-alive mutuo Render <-> Hugging Face (2026-09-12)

Render free hiberna apos 15 min sem trafego (boot de ~2 min); o Space free dorme apos 48 h. O repositorio e privado,
entao GitHub Actions a cada 10 min estouraria os 2.000 min/mes gratuitos. Solucao sem conta externa:
`config/KeepAliveScheduler` (backend) chama `${ai.service.url}/health` a cada 10 min (`KEEPALIVE_ENABLED`,
`KEEPALIVE_URL`, `KEEPALIVE_INTERVAL_MS`) e o `app.py` do ai-service pinga `KEEPALIVE_URL` (variavel do Space =
`https://challenge-totvs-backend-oracle.onrender.com/health`) a cada `KEEPALIVE_INTERVAL_SECONDS` (600).
Qualquer um dos dois de pe mantem o outro acordado; se ambos dormirem ao mesmo tempo, a primeira requisicao de
usuario reacende a cadeia. Custo: ~720 das 750 horas/mes gratuitas da Render, entao nenhum outro servico free
pode ficar ligado no mesmo workspace.

### Importacao grande em producao: pool esgotado e PROCESSANDO presos (2026-09-12)

Sintoma: importar o JSON de 1.174 reunioes na Render free deixou 561 transcricoes em PROCESSANDO e o dashboard
carregando sem fim. Causa: 4 threads de analise contra um pool Oracle de 3 conexoes ("Connection is not available,
waiting=4"); o job assincrono falhava ao abrir transacao e a transcricao nunca saia de PROCESSANDO; o dashboard
ainda carregava o texto completo de todas as transcricoes (findAllForInsights) e esperava conexao.

Correcoes: `ANALYSIS_THREADS` (padrao 2) < `ORACLE_POOL_SIZE` (5 na Render); `TranscricaoAnalysisJob` com
retentativas em falha de banco, ERRO no fim e job de recuperacao a cada 3 min que reenfileira ate 100
PROCESSANDO que nao estejam na fila desta instancia; insights via projecao `InsightRow` (id, cliente) sem CLOB;
importacao pre-carrega `findAllMeetingIds()` (uma consulta por lote em vez de uma por reuniao).

Licoes do mesmo incidente: (1) o Space tinha a variavel `AI_MAX_CHARS=20000` definida nas configuracoes, que
sobrepoe o default do config.py; toda reuniao real voltava 422 e o backend caia no motor Java (variavel agora
250000 via HfApi.add_space_variable). (2) `@Async` chamado de dentro da propria classe nao passa pelo proxy:
a recuperacao rodava tudo em serie na thread `scheduling-1`; por isso `AnalysisRecoveryScheduler` e um bean
separado que chama `analysisJob.processar`.
(3) O marcador de pendente precisa ser registrado ao ENFILEIRAR, nao ao iniciar: `processar(id)` e sincrono
(`pendentes.add` + `self.processarAsync` via proxy `@Lazy`), senao a recuperacao reenfileira as mesmas 100 a cada
3 min e duas threads colidem na mesma transcricao. (4) `LEANIA_CONTEXTO` nao e mais invalidado por analise e
`buildContext` limita a 100 registros no banco: era o que fazia cada analise custar ~40 s numa fila grande.
(5) Com 620 analises o dashboard estourava 180 s: carregava todas as entidades AnaliseResultado (CLOBs + 8 tabelas
filhas) duas vezes por requisicao. Agora `AnaliseResultadoRepository` expoe projecoes (`DashboardRow` com sentimento e
metricas; `RiscoRow`/`OportunidadeRow`/consolidados via `join a.riscos r` devolvendo o embeddable) e o dashboard
fecha em ~1 s. Regra: nunca carregar a entidade completa em massa; projecao por tabela filha.
