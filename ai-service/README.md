---
title: TOTVS Conversational Intelligence - AI Service
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
| `AI_SERVICE_REQUIRE_TOKEN` | Variable | `true` |
| `AI_CORS_ALLOWED_ORIGINS` | Variable | `https://challenge-totvs-backend.onrender.com` |

AI Service para analise de transcricoes de reunioes comerciais usando HuggingFace Transformers.
