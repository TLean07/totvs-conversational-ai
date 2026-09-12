<h1 align="center">TOTVS Conversational Intelligence</h1>
<p align="center">Plataforma de IA para analise de transcricoes de reunioes comerciais, extraindo inteligencia acionavel para vendas e retencao de clientes</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Python-3.11-3776AB?logo=python&logoColor=white" alt="Python">
  <img src="https://img.shields.io/badge/LeanIA-Adaptive%20AI-FFD21E" alt="LeanIA">
  <img src="https://img.shields.io/badge/FastAPI-009688?logo=fastapi&logoColor=white" alt="FastAPI">
  <img src="https://img.shields.io/badge/Next.js-16-000000?logo=nextdotjs&logoColor=white" alt="Next.js">
  <img src="https://img.shields.io/badge/Oracle-Database%2019c-F80000?logo=oracle&logoColor=white" alt="Oracle Database">
  <img src="https://img.shields.io/badge/Vercel-Deploy-000000?logo=vercel&logoColor=white" alt="Vercel">
  <img src="https://img.shields.io/badge/FIAP-Challenge%20TOTVS%202026-E4002B" alt="FIAP">
</p>

---

### Sobre o Projeto

O **TOTVS Conversational Intelligence** e uma solucao desenvolvida para o Challenge FIAP x TOTVS 2026, que transforma transcricoes brutas de reunioes de vendas e CS em inteligencia acionavel. A plataforma utiliza a **LeanIA**, um microsservico de IA gratuito e adaptativo com HuggingFace Transformers, fallback local e processamento de linguagem natural para extrair automaticamente:

- **Oportunidades de vendas** — Upsell, cross-sell e novos produtos
- **Riscos de churn** — Sinais de insatisfacao e mencoes a concorrentes
- **Mapeamento de produtos** — Produtos TOTVS em uso, oportunidades e concorrentes citados
- **Analise de sentimento** — Classificacao do tom geral da conversa (modelo BERT multilingual)
- **Alertas automaticos** — Notificacoes prioritarias para o Diretor Comercial

O objetivo e que a TOTVS consiga analisar milhares de reunioes diarias e identificar, em tempo real, onde estao as oportunidades de receita e os riscos de perda de clientes.

---

### Funcionalidades

- **Ingestao de transcricoes** — Envio de texto bruto de reunioes via API REST ou dashboard
- **Analise com LeanIA** — Modelo BERT multilingual gratuito, regras de negocio especializadas e contexto adaptativo por historico
- **Deteccao de oportunidades** — Identificacao de gatilhos de compra com score de probabilidade
- **Sinalizacao de riscos** — Alertas de churn, concorrencia e insatisfacao com severidade
- **Mapeamento de ecossistema** — Produtos TOTVS (Protheus, RM, Fluig) e concorrentes (SAP, Senior, Sankhya)
- **Dashboard interativo** — Graficos de sentimento, riscos por tipo, cards de metricas e alertas
- **Sistema de alertas** — Priorizacao automatica com acoes de marcar como visto/resolvido
- **Identificacao de personas** — Cargo e papel do interlocutor na reuniao
- **Deteccao de budget** — Captura de valores monetarios mencionados
- **100% gratuito** — Sem dependencia de APIs pagas, roda inteiramente local

---

### Importacao JSON TOTVS e Granularidade

O fluxo de importacao por JSON mantem o banco limpo sem perder riqueza analitica:

```text
1 JSON TOTVS
   |
   |-- leitura em streaming, sem carregar o arquivo inteiro em memoria
   |-- parsing dos registros internos do JSON
   |-- calculo de metricas agregadas
   |-- geracao de riscos, oportunidades, itens criticos e alertas consolidados
   v
1 documento em transcricoes
```

Regra principal:

- **1 arquivo JSON importado = 1 unico registro principal em `TRANSCRICOES`**
- O sistema **nao cria 1 registro por linha, parte, chunk ou conversa**
- As partes internas sao analisadas em memoria e viram colunas agregadas (`MET_*`) e tabelas filhas (`TRANSCRICAO_*`) do registro unico
- O texto completo fica em coluna CLOB; o sistema tambem salva estatisticas e trechos representativos

Campos consolidados adicionados a transcricao e a analise:

| Campo | Objetivo |
|---|---|
| `metricasAgregadas.totalPartes` | Quantidade de registros encontrados no JSON |
| `metricasAgregadas.partesAnalisadas` | Partes usadas no calculo consolidado |
| `metricasAgregadas.partesNegativas` | Volume interno de sinais negativos |
| `metricasAgregadas.riscosDetectados` | Riscos encontrados nas partes internas |
| `metricasAgregadas.oportunidadesDetectadas` | Oportunidades encontradas nas partes internas |
| `metricasAgregadas.churnsPotenciais` | Indicadores internos de churn |
| `metricasAgregadas.mencoesCriticas` | Mencoes operacionais ou comerciais criticas |
| `metricasAgregadas.alertasConsolidados` | Alertas gerados por recorrencia/categoria |
| `riscosConsolidados` | Riscos agrupados por categoria, severidade e ocorrencias |
| `oportunidadesConsolidadas` | Oportunidades agrupadas por categoria, score e ocorrencias |
| `alertasConsolidados` | Alertas com titulo, categoria, severidade, motivo e recomendacao |
| `itensCriticos` | Evidencias/trechos mais importantes para revisao |

Com isso, o dashboard continua exibindo **Transcricoes = quantidade de registros**, mas cards e graficos usam os dados consolidados do JSON sem multiplicar linhas no Oracle.

---

### Arquitetura

```
Reunioes → Audios → Transcricoes + Metadados
                         |
             [LeanIA - Microsservico Python Gratuito]
              HuggingFace BERT (Sentimento)
              Regras de negocio (Entidades)
                         |
                  [Java Spring Boot]
                   API REST + JPA
                         |
                  [DB - Oracle 19c]
                   Dados Extraidos
                         |
                [Analytics + Regras]
           Analise de Riscos e Oportunidades
                    /         \
            Oportunidades    Riscos
                    \         /
          Mensagens + Dashboards + Alertas
               [Next.js - Vercel]
```

Documentacao tecnica adicional:

- [Arquitetura com diagrama](docs/architecture.md)
- [Politica de retencao de dados](docs/data-retention-policy.md)

---

### Estrutura do Projeto

```
challenge-totvs-conversational-intelligence/
|
|-- ai-service/                           # Microsservico Python (IA local)
|   |-- app.py                            # FastAPI - endpoints /analisar e /health
|   |-- analyzer.py                       # Motor de NLP (HuggingFace + regras)
|   +-- requirements.txt                  # Dependencias Python
|
|-- backend/                              # Java Spring Boot (API REST)
|   |-- pom.xml
|   +-- src/main/java/br/com/fiap/challenge/
|       |-- ChallengeApplication.java
|       |-- config/
|       |   |-- CorsConfig.java           # CORS para Vercel + localhost
|       |   |-- DataLoader.java           # Dados de exemplo (profile: demo)
|       |   +-- GlobalExceptionHandler.java
|       |-- controller/
|       |   |-- TranscricaoController.java # POST/GET/DELETE transcricoes
|       |   |-- DashboardController.java   # GET dashboard consolidado
|       |   +-- AlertaController.java      # GET/PATCH alertas
|       |-- dto/                           # Request/Response objects
|       |-- model/
|       |   |-- Transcricao.java
|       |   |-- AnaliseResultado.java
|       |   |-- ProdutoIdentificado.java
|       |   |-- Oportunidade.java
|       |   |-- Risco.java
|       |   +-- Alerta.java
|       |-- repository/                    # Spring Data JPA
|       |-- service/
|       |   |-- LeanIAService.java         # Client HTTP gratuito com retry, fallback e contexto adaptativo
|       |   |-- TranscricaoService.java
|       |   |-- AnaliseService.java
|       |   |-- AlertaService.java
|       |   +-- DashboardService.java
|       +-- enums/
|
+-- frontend/                             # Next.js + React + Tailwind (Vercel)
    +-- src/
        |-- app/
        |   |-- page.tsx                   # Dashboard principal
        |   |-- transcricoes/page.tsx      # Lista + formulario de transcricoes
        |   |-- transcricoes/[id]/page.tsx # Detalhe da analise
        |   +-- alertas/page.tsx           # Central de alertas
        |-- components/
        |   |-- Navbar.tsx
        |   |-- StatsCard.tsx
        |   |-- AlertList.tsx
        |   |-- SentimentBadge.tsx
        |   +-- SeveridadeBadge.tsx
        +-- lib/
            |-- api.ts                     # Client HTTP para o backend
            +-- types.ts                   # Tipos TypeScript
```

---

### Tecnologias Utilizadas

| Tecnologia | Uso |
| --- | --- |
| **Java 17** | Linguagem principal do backend |
| **Spring Boot 3.3** | Framework web, JPA, validacao |
| **Oracle Database 19c** | Banco relacional (instancia FIAP) acessado via Spring Data JPA/Hibernate |
| **Python 3.11** | Microsservico de IA / NLP |
| **FastAPI** | Framework web do microsservico Python |
| **HuggingFace Transformers** | Modelo BERT multilingual para analise de sentimento |
| **Lombok** | Reducao de boilerplate no Java |
| **Next.js 16** | Framework React para o frontend |
| **Tailwind CSS** | Estilizacao do dashboard |
| **Recharts** | Graficos interativos (pizza, barras) |
| **Vercel** | Deploy do frontend |
| **Maven** | Build e gerenciamento de dependencias |

---

### Como Executar

**Pre-requisitos:**
- JDK 17+
- Maven 3.9+
- Python 3.10+
- Node.js 18+
- Acesso ao Oracle Database da FIAP (`oracle.fiap.com.br:1521/orcl`) com usuario RM

**1. Clone o repositorio:**
```bash
git clone https://github.com/TLean07/challenge-totvs-conversational-intelligence.git
cd challenge-totvs-conversational-intelligence
```

**2. Microsservico de IA (Python):**
```bash
cd ai-service
python -m venv venv
venv\Scripts\activate        # Windows
# source venv/bin/activate   # Linux/Mac
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```
Na primeira execucao, o modelo BERT sera baixado automaticamente (~700MB). Acesse `http://localhost:8000/health` para verificar.

Transcricao automatica de audio com Parakeet e opcional e vem desativada por padrao para manter o ambiente gratuito leve:

```bash
export TRANSCRIPTION_PROVIDER=manual
export PARAKEET_ENABLED=false
export PARAKEET_MODEL="nvidia/parakeet-tdt-0.6b-v3"
export PARAKEET_MAX_AUDIO_MB=100
export PARAKEET_MAX_DURATION_SECONDS=3600
export PARAKEET_LANGUAGE=pt
```

Para habilitar Parakeet, configure `TRANSCRIPTION_PROVIDER=parakeet` e `PARAKEET_ENABLED=true` em um ambiente que tenha o provider ASR compativel instalado. Se o provider nao estiver disponivel, o sistema retorna erro amigavel e continua permitindo texto/JSON manual.

**3. Backend (Java Spring Boot):**

Crie as tabelas uma unica vez no seu schema Oracle (SQL Developer ou SQLcl) executando
`backend/src/main/resources/db/oracle/schema.sql`. O backend valida o schema na subida
(`spring.jpa.hibernate.ddl-auto=validate`) e nunca cria ou altera tabelas sozinho.

Credenciais do Oracle somente por variavel de ambiente:
```bash
export ORACLE_URL="jdbc:oracle:thin:@//oracle.fiap.com.br:1521/orcl"
export ORACLE_USER="RM000000"
export ORACLE_PASSWORD="senha-do-oracle"
```

Variaveis de seguranca obrigatorias para o backend:
```bash
export APP_JWT_SECRET="string-aleatoria-com-32-caracteres-ou-mais"
export APP_ADMIN_EMAIL="admin@example.com"
export APP_ADMIN_PASSWORD="senha-forte-com-12-caracteres-ou-mais"
export AI_SERVICE_TOKEN="token-interno-com-32-caracteres-ou-mais"
```

O usuario de aluno da FIAP permite 20 sessoes simultaneas e encerra sessoes ociosas apos 30 minutos; o pool (HikariCP) ja esta configurado para isso.

**Importacao TOTVS**: `POST /api/transcricoes/import/totvs` aceita o export em JSON (array, NDJSON ou objeto
aninhado) e o CSV de reunioes (`reunioes_transcricoes_mockado.csv`, em ISO-8859-1 ou UTF-8, separado por `,` ou `;`).
O formato e detectado pelo conteudo; UF, CNAE, unidade, segmento, faixa de faturamento, tipo de recurso e NPS sao
persistidos junto com a transcricao.

**LeanIA v2**: a analise identifica produtos TOTVS (atual/oportunidade), concorrentes, riscos, oportunidades com
score, persona, budget, sentimento por area, proxima acao, insights "alem do obvio" (elogios, reclamacoes, duvidas
da jornada, proximos passos, observacoes) e indicadores da conversa (participacao vendedor/cliente, perguntas,
escuta ativa). O motor roda no `ai-service` Python e tem uma copia em Java no backend para quando o servico nao
responde. O exemplo do briefing do Challenge e o teste de referencia (`LeanIALocalAnalyzerTest` e
`ai-service/tests/test_analyzer.py`).

O backend mantem um cache em memoria (Caffeine) das leituras de dashboard, listas, insights e alertas por usuario:
dentro da janela `CACHE_TTL_SECONDS` (padrao 120 s) as chamadas repetidas nao tocam o Oracle, e qualquer escrita
(nova transcricao, importacao, reprocessamento, exclusao, alerta visto, feedback) limpa o cache na hora.

Execute:
```bash
cd backend
mvn spring-boot:run
```

Para carregar dados de exemplo automaticamente:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

**4. Frontend (Next.js):**
```bash
cd frontend
npm install
npm run dev
```
Acesse `http://localhost:3000`

---

### Deploy em Producao (Gratuito)

A aplicacao roda em 3 servicos na nuvem, todos com planos gratuitos:

```
[Vercel]                [Render]                 [HuggingFace Spaces]
 Frontend   ──REST──►   Backend Java  ──REST──►   AI Service Python
 Next.js                Spring Boot               FastAPI + BERT
                            |
                  [Oracle DB - FIAP]
```

#### Passo 0 — Oracle Database (FIAP)

1. Conecte no SQL Developer com o usuario RM do aluno em `oracle.fiap.com.br:1521` (SID/servico `ORCL`)
2. Execute o script `backend/src/main/resources/db/oracle/schema.sql` para criar as tabelas, indices e FKs
3. Guarde `ORACLE_URL`, `ORACLE_USER` e `ORACLE_PASSWORD` — voce usara no Render
4. O host precisa ser alcancavel pelo provedor de deploy; se a rede da FIAP bloquear, rode o backend localmente ou via Docker Compose

#### Passo 1 — AI Service no Hugging Face Spaces

1. Crie uma conta em [huggingface.co](https://huggingface.co)
2. Va em **New Space** → escolha **Docker** como SDK
3. Clone o Space e copie os arquivos da pasta `ai-service/`:
```bash
git clone https://huggingface.co/spaces/SEU_USUARIO/totvs-ai-service
cp ai-service/* totvs-ai-service/
cd totvs-ai-service
git add . && git commit -m "Deploy AI service" && git push
```
4. Aguarde o build (~5 min). A URL sera: `https://SEU_USUARIO-totvs-ai-service.hf.space`
5. Teste: `https://SEU_USUARIO-totvs-ai-service.hf.space/health`

#### Passo 2 — Backend no Render

1. Crie uma conta em [render.com](https://render.com)
2. Va em **New** → **Web Service** → conecte seu repositorio GitHub
3. Configure:
   - **Root Directory:** `backend`
   - **Runtime:** Docker
   - **Plan:** Free
4. Adicione as **Environment Variables:**
   | Variavel | Valor |
   |---|---|
   | `ORACLE_URL` | `jdbc:oracle:thin:@//oracle.fiap.com.br:1521/orcl` |
   | `ORACLE_USER` | Usuario RM do aluno |
   | `ORACLE_PASSWORD` | Senha do Oracle |
   | `AI_SERVICE_URL` | `https://SEU_USUARIO-totvs-ai-service.hf.space` |
   | `CORS_ALLOWED_ORIGINS` | `https://SEU-APP.vercel.app` |
   | `AI_SERVICE_TOKEN` | Token interno forte compartilhado com o servico de IA |
   | `APP_JWT_SECRET` | Segredo forte para assinatura dos access tokens |
   | `APP_ADMIN_EMAIL` | E-mail do admin inicial |
   | `APP_ADMIN_PASSWORD` | Senha forte do admin inicial |
   | `APP_COOKIE_SECURE` | `true` em producao |
   | `APP_COOKIE_SAME_SITE` | `None` quando frontend e backend usam dominios diferentes |
5. Clique em **Create Web Service**. A URL sera: `https://SEU-APP.onrender.com`

#### Passo 3 — Frontend no Vercel

1. Crie uma conta em [vercel.com](https://vercel.com)
2. Va em **New Project** → importe o repositorio GitHub
3. Configure:
   - **Root Directory:** `frontend`
   - **Framework Preset:** Next.js
4. Adicione a **Environment Variable:**
   | Variavel | Valor |
   |---|---|
   | `NEXT_PUBLIC_API_URL` | `https://SEU-APP.onrender.com/api` |
5. Clique em **Deploy**

#### Resumo das URLs finais

| Servico | URL |
|---|---|
| Frontend | `https://SEU-APP.vercel.app` |
| Backend | `https://SEU-APP.onrender.com` |
| AI Service | `https://SEU_USUARIO-totvs-ai-service.hf.space` |

> **Nota:** No plano gratuito do Render, o servico "dorme" apos 15 min de inatividade. A primeira requisicao apos isso leva ~30s para acordar. O HuggingFace Spaces tambem pode dormir, mas acorda automaticamente.

---

### Endpoints da API

| Metodo | Endpoint | Descricao |
| --- | --- | --- |
| `POST` | `/api/transcricoes` | Envia transcricao e processa com IA |
| `POST` | `/api/transcricoes/import/totvs` | Importa JSON TOTVS como 1 transcricao consolidada |
| `POST` | `/api/transcricoes/audio` | Envia audio `.wav`, `.mp3` ou `.m4a`, transcreve no ai-service e analisa com LeanIA |
| `GET` | `/api/transcricoes` | Lista todas as transcricoes |
| `GET` | `/api/transcricoes/{id}` | Detalhe de uma transcricao + analise |
| `GET` | `/api/transcricoes/cliente/{nome}` | Busca por cliente |
| `DELETE` | `/api/transcricoes/{id}` | Remove uma transcricao |
| `GET` | `/api/dashboard` | Dados consolidados do dashboard |
| `GET` | `/api/alertas` | Lista todos os alertas |
| `GET` | `/api/alertas/novos` | Lista alertas nao lidos |
| `PATCH` | `/api/alertas/{id}/visto` | Marca alerta como visto |
| `PATCH` | `/api/alertas/{id}/resolvido` | Marca alerta como resolvido |

**Microsservico de IA:**

| Metodo | Endpoint | Descricao |
| --- | --- | --- |
| `POST` | `/analisar` | Analisa uma transcricao e retorna insights |
| `POST` | `/transcrever` | Transcreve audio com provider opcional Parakeet |
| `GET` | `/health` | Verifica se o modelo esta carregado |

---

### Exemplo de Uso

**Enviando uma transcricao via API:**
```json
POST /api/transcricoes
{
  "textoOriginal": "Bom dia, Joao. O nosso Protheus esta atendendo o backoffice, mas o time de RH esta sofrendo com a folha manual. O pessoal viu uma demo da Senior e gostou...",
  "cliente": "Empresa ABC Ltda",
  "vendedor": "Joao Silva",
  "participantes": "Joao Silva (TOTVS), Carlos Mendes (ABC)",
  "duracaoMinutos": 45
}
```

**Resposta com analise completa:**
```json
{
  "id": 1,
  "status": "PROCESSADA",
  "analise": {
    "sentimentoGeral": "MISTO",
    "resumo": "Cliente utiliza TOTVS Protheus. Sentimento misto na conversa. 1 oportunidade(s) de venda identificada(s). ATENCAO: 1 risco(s) de alta severidade. Concorrentes mencionados: Senior.",
    "personaIdentificada": "Gestor Operacional",
    "budgetIdentificado": "R$ 50 mil",
    "produtos": [...],
    "oportunidades": [...],
    "riscos": [...],
    "alertas": [...]
  }
}
```

**Importando JSON TOTVS:**

```http
POST /api/transcricoes/import/totvs
Content-Type: multipart/form-data

file=@ANON_transcricao.json
```

**Resposta da importacao JSON:**

```json
{
  "totalRegistros": 200,
  "importados": 1,
  "ignorados": 0,
  "avisos": [
    "Foi criada 1 transcricao consolidada para este JSON, evitando centenas de documentos na colecao transcricoes.",
    "Os registros do JSON foram tratados como partes da mesma transcricao TOTVS."
  ]
}
```

**Enviando audio para transcricao opcional:**

```http
POST /api/transcricoes/audio
Content-Type: multipart/form-data

file=@reuniao.wav
cliente=Empresa ABC
vendedor=Consultor TOTVS
participantes=Cliente, Consultor TOTVS
```

O backend valida extensao, MIME e tamanho, envia o arquivo ao `ai-service /transcrever`, salva o texto retornado como uma transcricao com `origemTranscricao=parakeet` e enfileira a analise normal da LeanIA. O audio bruto nao e salvo em pasta publica; apenas metadados como nome do arquivo, modelo, idioma, duracao e avisos sao mantidos no documento.

Se Parakeet estiver desativado, ausente ou falhar:

```json
{
  "errorCode": "PARAKEET_NOT_INSTALLED",
  "message": "Parakeet nao esta instalado neste ambiente. Use importacao manual de texto/JSON ou instale o provider ASR.",
  "fallbackAvailable": true
}
```

Nesse caso, o fluxo de texto e JSON continua funcionando normalmente.

Depois do processamento, a transcricao consolidada passa a conter metricas como:

```json
{
  "metricasAgregadas": {
    "totalPartes": 200,
    "partesAnalisadas": 200,
    "partesNegativas": 37,
    "partesNeutras": 120,
    "partesPositivas": 43,
    "riscosDetectados": 37,
    "oportunidadesDetectadas": 12,
    "churnsPotenciais": 8,
    "mencoesCriticas": 25,
    "alertasConsolidados": 5
  }
}
```

---

### Como Funciona a IA Local

O microsservico Python utiliza uma abordagem hibrida:

1. **Analise de Sentimento** — Modelo `nlptown/bert-base-multilingual-uncased-sentiment` (HuggingFace), treinado em 6 idiomas incluindo portugues. Classifica o texto em uma escala de 1-5 estrelas, convertida para POSITIVO/NEGATIVO/NEUTRO/MISTO.

2. **Extracao de Entidades** — Regras de negocio especializadas que identificam:
   - 15+ produtos TOTVS (Protheus, RM, Fluig, Datasul, etc.)
   - 12+ concorrentes (SAP, Senior, Sankhya, Linx, etc.)
   - 30+ modulos/areas de produto (RH, Fiscal, Compras, etc.)
   - Padroes de valor monetario (R$ X.XXX)
   - Cargos e personas (Diretor, Gerente, CEO, etc.)

3. **Deteccao de Oportunidades** — Analise de padroes linguisticos que indicam intencao de compra, com scoring de probabilidade (0.0 a 1.0).

4. **Deteccao de Riscos** — Identificacao de sinais de insatisfacao, mencoes a concorrentes e indicadores de churn, com classificacao de severidade (ALTO/MEDIO/BAIXO).

5. **Analise Consolidada de JSON** - Para arquivos grandes, a LeanIA e o backend extraem metricas agregadas dos registros internos sem criar linhas extras no Oracle. O dashboard usa esses agregados para manter numeros consolidados mesmo com apenas 1 registro por JSON.

---

### Autor

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/TLean07">
        <img src="https://github.com/TLean07.png" width="100px;" alt="TLean07"/>
        <br />
        <sub><b>TLean07</b></sub>
      </a>
      <br />
      <sub>RM 561344</sub>
    </td>
  </tr>
</table>
