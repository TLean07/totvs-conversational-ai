"""
Microsservico de IA para Inteligencia Conversacional
Analisa transcricoes de reunioes usando HuggingFace + spaCy
"""

import hmac
import os
from typing import Annotated

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from analyzer import TranscricaoAnalyzer

app = FastAPI(
    title="TOTVS Conversational Intelligence - AI Service",
    description="Microsservico de NLP para analise de transcricoes",
    version="1.0.0",
)

AI_SERVICE_TOKEN = os.getenv("AI_SERVICE_TOKEN", "")
AI_SERVICE_REQUIRE_TOKEN = os.getenv("AI_SERVICE_REQUIRE_TOKEN", "true").lower() == "true"
ALLOWED_ORIGINS = [
    origin.strip()
    for origin in os.getenv("AI_CORS_ALLOWED_ORIGINS", "").split(",")
    if origin.strip()
]

if AI_SERVICE_REQUIRE_TOKEN and not AI_SERVICE_TOKEN:
    raise RuntimeError("AI_SERVICE_TOKEN deve ser configurado quando AI_SERVICE_REQUIRE_TOKEN=true")

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS or ["http://localhost:8080"],
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "X-Internal-Token"],
)

analyzer = TranscricaoAnalyzer()


class TranscricaoRequest(BaseModel):
    texto: str = Field(min_length=1, max_length=20000)


def validar_token_interno(x_internal_token: Annotated[str | None, Header()] = None):
    if AI_SERVICE_TOKEN and not hmac.compare_digest(x_internal_token or "", AI_SERVICE_TOKEN):
        raise HTTPException(status_code=401, detail="Nao autorizado")


@app.post("/analisar")
def analisar_transcricao(request: TranscricaoRequest, _: None = Depends(validar_token_interno)):
    resultado = analyzer.analisar(request.texto)
    return resultado


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": analyzer.is_ready()}
