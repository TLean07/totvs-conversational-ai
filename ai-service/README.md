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
| `AI_SERVICE_TOKEN` | Secret | Mesmo valor configurado no backend Render |
| `AI_SERVICE_REQUIRE_TOKEN` | Variable | `true` em producao, `false` para teste local |
| `AI_CORS_ALLOWED_ORIGINS` | Variable | `https://challenge-totvs-backend.onrender.com` |
| `AI_PROVIDER` | Variable | `auto`, `transformers` ou `fallback` |
| `AI_MODEL_NAME` | Variable | Modelo local gratuito de sentimento |
| `AI_CACHE_ENABLED` | Variable | `true` |

LeanIA e o AI Service gratuito e adaptativo para analise de transcricoes de reunioes comerciais.

## Analise consolidada de CSV

O backend pode enviar para a LeanIA uma transcricao consolidada vinda de CSV. Nesse modo, o CSV inteiro continua sendo tratado como **1 unica transcricao**, mas a resposta da IA traz granularidade interna para o dashboard.

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
