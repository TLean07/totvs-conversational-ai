<h1 align="center">TOTVS Conversational Intelligence</h1>
<p align="center">Plataforma de IA para analise de transcricoes de reunioes comerciais, extraindo inteligencia acionavel para vendas e retencao de clientes</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Python-3.11-3776AB?logo=python&logoColor=white" alt="Python">
  <img src="https://img.shields.io/badge/LeanIA-Adaptive%20AI-FFD21E" alt="LeanIA">
  <img src="https://img.shields.io/badge/FastAPI-009688?logo=fastapi&logoColor=white" alt="FastAPI">
  <img src="https://img.shields.io/badge/Next.js-16-000000?logo=nextdotjs&logoColor=white" alt="Next.js">
  <img src="https://img.shields.io/badge/MongoDB-Atlas-47A248?logo=mongodb&logoColor=white" alt="MongoDB">
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

### Importacao CSV TOTVS e Granularidade

O fluxo de importacao por CSV mantem o banco limpo sem perder riqueza analitica:

```text
1 CSV TOTVS
   |
   |-- leitura em streaming, sem carregar o arquivo inteiro em memoria
   |-- parsing das partes internas do CSV
   |-- calculo de metricas agregadas
   |-- geracao de riscos, oportunidades, itens criticos e alertas consolidados
   v
1 documento em transcricoes
```

Regra principal:

- **1 arquivo CSV importado = 1 unico documento principal em `transcricoes`**
- O sistema **nao cria 1 documento por linha, parte, chunk ou conversa**
- As partes internas sao analisadas em memoria e viram campos agregados dentro do documento unico
- O CSV bruto completo nao e salvo quando excede o limite pratico do MongoDB; o sistema salva uma transcricao consolidada, estatisticas e trechos representativos

Campos consolidados adicionados ao documento e a analise:

| Campo | Objetivo |
|---|---|
| `metricasAgregadas.totalPartes` | Quantidade de partes encontradas no CSV |
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

Com isso, o dashboard continua exibindo **Transcricoes = quantidade de documentos**, mas cards como riscos, oportunidades, churn, sentimento negativo, partes analisadas e alertas passam a usar os sinais internos agregados do CSV.

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
                    [DB - MongoDB]
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
| **MongoDB Atlas** | Banco de dados NoSQL na nuvem (gratis) |
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
- MongoDB (local ou MongoDB Atlas gratuito)

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

**3. Backend (Java Spring Boot):**

Configure o MongoDB em `backend/src/main/resources/application.properties`:
```properties
spring.data.mongodb.uri=mongodb+srv://USUARIO:SENHA@cluster.mongodb.net/challenge-totvs
```
Ou via variavel de ambiente: `export MONGODB_URI=mongodb+srv://...`

Variaveis de seguranca obrigatorias para o backend:
```bash
export APP_JWT_SECRET="string-aleatoria-com-32-caracteres-ou-mais"
export APP_ADMIN_EMAIL="admin@example.com"
export APP_ADMIN_PASSWORD="senha-forte-com-12-caracteres-ou-mais"
export AI_SERVICE_TOKEN="token-interno-com-32-caracteres-ou-mais"
```

Para rodar local sem Atlas, basta ter o MongoDB rodando em `localhost:27017` (o padrao ja funciona).

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
                    [MongoDB Atlas]
```

#### Passo 0 — MongoDB Atlas (Banco de dados gratuito)

1. Crie uma conta em [mongodb.com/atlas](https://www.mongodb.com/atlas)
2. Crie um **Free Cluster** (M0 - gratuito, 512MB)
3. Em **Database Access**, crie um usuario com senha
4. Em **Network Access**, adicione `0.0.0.0/0` (permite acesso de qualquer IP)
5. Em **Connect** → **Drivers**, copie a connection string:
   ```
   mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/challenge-totvs
   ```
6. Guarde essa URI — voce usara no Render

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
   | `MONGODB_URI` | `mongodb+srv://USUARIO:SENHA@cluster.mongodb.net/challenge-totvs` |
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
| `POST` | `/api/transcricoes/import/totvs` | Importa CSV TOTVS como 1 transcricao consolidada |
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

**Importando CSV TOTVS:**

```http
POST /api/transcricoes/import/totvs
Content-Type: multipart/form-data

file=@transcricao_totvs.csv
```

**Resposta da importacao CSV:**

```json
{
  "totalRegistros": 200,
  "importados": 1,
  "ignorados": 0,
  "avisos": [
    "Foi criada 1 transcricao consolidada para este CSV, evitando centenas de documentos na colecao transcricoes.",
    "Os registros do CSV foram tratados como partes da mesma transcricao TOTVS."
  ]
}
```

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

5. **Analise Consolidada de CSV** - Para arquivos grandes, a LeanIA e o backend extraem metricas agregadas das partes internas sem criar documentos extras no MongoDB. O dashboard usa esses agregados para manter numeros ricos mesmo com apenas 1 documento por CSV.

---

### Autor

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/TLean07">
        <img src="https://github.com/TLean07.png" width="100px;" alt="TLean07"/>
        <br />
        <sub><b>Leandro Afonso Silva Santos Junior</b></sub>
      </a>
      <br />
      <sub>RM 561344</sub>
    </td>
  </tr>
</table>
