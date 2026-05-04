import hmac
import logging
import re
from typing import Annotated

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from cache import TTLCache
from config import load_settings
from providers.free_ai_provider import FreeAiProvider

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

settings = load_settings()

app = FastAPI(
    title="LeanIA - TOTVS Conversational Intelligence",
    description="IA gratuita e adaptativa para analise de transcricoes",
    version="1.1.0",
)

if settings.require_token and not settings.service_token:
    raise RuntimeError("AI_SERVICE_TOKEN deve ser configurado quando AI_SERVICE_REQUIRE_TOKEN=true")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins or ["http://localhost:8080"],
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "X-Internal-Token"],
)

provider = FreeAiProvider(settings)
cache = TTLCache(settings.cache_ttl_seconds, settings.cache_max_entries)


class LeanIAContext(BaseModel):
    produtosFrequentes: list[str] = Field(default_factory=list)
    concorrentesFrequentes: list[str] = Field(default_factory=list)
    riscosFrequentes: list[str] = Field(default_factory=list)
    oportunidadesFrequentes: list[str] = Field(default_factory=list)


class TranscricaoRequest(BaseModel):
    texto: str = Field(min_length=1)
    contexto: LeanIAContext | None = None


def validar_token_interno(x_internal_token: Annotated[str | None, Header()] = None):
    if settings.require_token and settings.service_token and not hmac.compare_digest(x_internal_token or "", settings.service_token):
        raise HTTPException(status_code=401, detail="Nao autorizado")


def sanitizar_texto(texto: str) -> str:
    texto = texto.replace("\x00", " ")
    texto = re.sub(r"\s+", " ", texto).strip()
    return texto


@app.post("/analisar")
def analisar_transcricao(request: TranscricaoRequest, _: None = Depends(validar_token_interno)):
    texto = sanitizar_texto(request.texto)
    if not texto:
        raise HTTPException(status_code=422, detail="Transcricao vazia")
    if len(texto) > settings.max_chars:
        raise HTTPException(
            status_code=422,
            detail=f"Transcricao excede o limite de {settings.max_chars} caracteres",
        )

    contexto = request.contexto.model_dump() if request.contexto else None
    cache_input = f"{texto}|{contexto or {}}"

    if settings.cache_enabled:
        cached = cache.get(cache_input)
        if cached:
            logger.info("Analise retornada do cache")
            return cached

    resultado = provider.analyze(texto, contexto)

    if settings.cache_enabled:
        cache.set(cache_input, resultado)

    return resultado


@app.get("/health")
def health():
    return {
        "status": "ok",
        **provider.status(),
        "cache": cache.stats() if settings.cache_enabled else {"enabled": False},
    }
