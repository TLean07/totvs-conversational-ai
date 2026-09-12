---
title: LeanIA - TOTVS Conversational Intelligence
emoji: 🧠
colorFrom: blue
colorTo: green
sdk: docker
app_port: 7860
---

## Variaveis de ambiente no Hugging Face Space

Configure em **Settings -> Variables and secrets**:

| Nome | Tipo | Valor |
|---|---|---|
| `KEEPALIVE_URL` | Variable | `https://challenge-totvs-backend-oracle.onrender.com/health` (o Space pinga o backend a cada 10 min; o backend pinga o Space) |
| `AI_SERVICE_TOKEN` | Secret | Mesmo valor configurado no backend Render |
| `AI_SERVICE_REQUIRE_TOKEN` | Variable | `true` em producao, `false` para teste local |
| `AI_CORS_ALLOWED_ORIGINS` | Variable | `https://challenge-totvs-backend.onrender.com` |
| `AI_PROVIDER` | Variable | `auto`, `transformers` ou `fallback` |
| `AI_MODEL_NAME` | Variable | Modelo local gratuito de sentimento |
| `AI_CACHE_ENABLED` | Variable | `true` |
| `TRANSCRIPTION_PROVIDER` | Variable | `manual`, `parakeet` ou `auto` |
| `PARAKEET_ENABLED` | Variable | `false` por padrao; use `true` apenas quando o ambiente tiver o provider ASR instalado |
| `PARAKEET_MODEL` | Variable | `nvidia/parakeet-tdt-0.6b-v3` ou outro modelo compativel |
| `PARAKEET_MAX_AUDIO_MB` | Variable | Limite de tamanho do audio, exemplo `100` |
| `PARAKEET_MAX_DURATION_SECONDS` | Variable | Limite de duracao, exemplo `3600` |

LeanIA e o AI Service gratuito e adaptativo para analise de transcricoes de reunioes comerciais.

## Motor de analise (v2)

`analyzer.py` processa a transcricao em etapas: locutores (`[LOCUTOR N]:`) e turnos, papel de cada
locutor (vendedor x cliente), sentencas e oracoes, anotacao de areas/produtos/concorrentes, sentimento
por sentenca com lexico pt-BR (negacao, intensificadores, aspecto), e entao extrai produtos TOTVS
(atual/oportunidade/concorrente), riscos (churn, concorrencia, insatisfacao, perda de confianca),
oportunidades (upsell/cross-sell/novo produto com score), persona, budget, confianca no vendedor,
proxima acao, insights "alem do obvio" (elogios, reclamacoes, duvidas da jornada, proximos passos,
observacoes) e indicadores de qualidade da conversa (participacao, perguntas, escuta ativa).

O exemplo do briefing do Challenge (pagina 15) e o teste de referencia: `python tests/test_analyzer.py`.
`AI_MAX_CHARS` padrao subiu para 250000 porque as reunioes reais tem ~35 mil caracteres.
O backend Java tem um motor equivalente (`LeanIALocalAnalyzer`) usado quando este servico nao responde.

## Transcricao opcional com Parakeet

O endpoint `POST /transcrever` permite receber audio de reuniao e gerar uma transcricao antes da analise da LeanIA.

Fluxo:

```text
Audio da reuniao -> /transcrever -> texto transcrito -> backend -> LeanIA /analisar -> dashboard
```

Parakeet fica isolado como provider opcional. O `requirements.txt` padrao nao instala dependencias pesadas de ASR para manter o Space gratuito leve. Para usar Parakeet de fato, configure `TRANSCRIPTION_PROVIDER=parakeet`, `PARAKEET_ENABLED=true` e instale o provider compativel no ambiente, como `nemo_toolkit[asr]` e dependencias de runtime adequadas ao hardware.

Se Parakeet estiver desativado, ausente ou falhar, o endpoint retorna erro estruturado com `fallbackAvailable=true`. O backend/frontend continuam permitindo importacao manual de texto ou JSON.

Formatos aceitos pelo contrato:

- `.wav`
- `.mp3`
- `.m4a`

Resposta de sucesso:

```json
{
  "transcript": "...",
  "language": "pt",
  "durationSeconds": 120.4,
  "model": "nvidia/parakeet-tdt-0.6b-v3",
  "processingTimeMs": 3200,
  "segments": [],
  "confidence": null,
  "warnings": []
}
```

## Analise consolidada de JSON

O backend pode enviar para a LeanIA uma transcricao consolidada vinda de JSON. Nesse modo, o JSON inteiro continua sendo tratado como **1 unica transcricao**, mas a resposta da IA traz granularidade interna para o dashboard.

A resposta estruturada inclui:

| Campo | Descricao |
|---|---|
| `resumoExecutivo` | Sintese executiva da reuniao/lote analisado |
| `sentimentoGeral` | Classificacao consolidada do sentimento |
| `scoreSentimento` | Score numerico agregado do sentimento |
| `metricasAgregadas` | Totais internos como partes analisadas, negativas, riscos, churns e alertas |
| `riscosConsolidados` | Riscos agrupados por categoria, severidade e ocorrencias |
| `oportunidadesConsolidadas` | Oportunidades agrupadas por categoria, score e ocorrencias |
| `alertasConsolidados` | Alertas consolidados com motivo e recomendacao |
| `itensCriticos` | Trechos/evidencias mais relevantes para revisao |

Exemplo resumido:

```json
{
  "sentimentoGeral": "negativo",
  "scoreSentimento": -0.72,
  "metricasAgregadas": {
    "totalPartes": 200,
    "partesAnalisadas": 200,
    "partesNegativas": 37,
    "riscosDetectados": 37,
    "oportunidadesDetectadas": 12,
    "churnsPotenciais": 8,
    "mencoesCriticas": 25,
    "alertasConsolidados": 5
  },
  "riscosConsolidados": [
    {
      "categoria": "Insatisfacao",
      "severidade": "ALTO",
      "ocorrencias": 18,
      "evidencias": ["Cliente relatou atraso recorrente no atendimento."],
      "recomendacao": "Acionar atendimento prioritario e registrar plano de recuperacao."
    }
  ]
}
```

Quando o modelo local gratuito nao estiver disponivel, a LeanIA usa fallback por regras locais para manter a analise funcionando sem custo e sem depender de APIs pagas.

## Modos de operacao

Modos suportados:

- `auto`: tenta modelo local/self-hosted via Hugging Face Transformers e usa fallback por regras se falhar.
- `transformers`: prioriza o modelo configurado em `AI_MODEL_NAME`.
- `fallback`: usa apenas regras locais/offline, sem baixar modelo e sem chamar servico externo.

A LeanIA tambem aceita um `contexto` opcional enviado pelo backend com padroes aprendidos do historico: produtos frequentes, concorrentes frequentes, riscos recorrentes e oportunidades recorrentes. Isso melhora as proximas analises sem treinar modelo pago.

Para rodar localmente sem custo:

```bash
pip install -r requirements.txt
AI_SERVICE_REQUIRE_TOKEN=false uvicorn app:app --reload --port 8000
```
