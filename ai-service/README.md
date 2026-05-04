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
