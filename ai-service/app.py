import hmac
import logging
import os
import re
import tempfile
import threading
import time
import urllib.request
import uuid
from contextvars import ContextVar
from pathlib import Path
from typing import Annotated

from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from cache import TTLCache
from config import load_settings
from providers.free_ai_provider import FreeAiProvider
from transcription.models import TranscriptionError
from transcription.service import TranscriptionService

request_id_var: ContextVar[str] = ContextVar("request_id", default="none")


class RequestIdFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.request_id = request_id_var.get()
        return True


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s requestId=%(request_id)s %(name)s - %(message)s",
)
for handler in logging.getLogger().handlers:
    handler.addFilter(RequestIdFilter())
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
    allow_headers=["Content-Type", "X-Internal-Token", "X-Request-ID"],
    expose_headers=["X-Request-ID"],
)

provider = FreeAiProvider(settings)
cache = TTLCache(settings.cache_ttl_seconds, settings.cache_max_entries)
transcription_service = TranscriptionService(settings)

# Keep-alive mutuo: o backend (Render free, hiberna em 15 min) pinga este Space e este Space pinga o
# backend, de modo que qualquer um dos dois que estiver de pe mantem o outro acordado.
KEEPALIVE_URL = os.getenv("KEEPALIVE_URL", "").strip()
KEEPALIVE_INTERVAL_SECONDS = max(60, int(os.getenv("KEEPALIVE_INTERVAL_SECONDS", "600") or "600"))


def _keepalive_loop() -> None:
    while True:
        time.sleep(KEEPALIVE_INTERVAL_SECONDS)
        try:
            with urllib.request.urlopen(KEEPALIVE_URL, timeout=120) as resp:
                logger.debug("keep-alive ok: %s (%s)", KEEPALIVE_URL, resp.status)
        except Exception as exc:  # backend dormindo ou rede instavel: tenta de novo no proximo ciclo
            logger.info("keep-alive falhou para %s: %s", KEEPALIVE_URL, exc.__class__.__name__)


if KEEPALIVE_URL:
    threading.Thread(target=_keepalive_loop, name="keepalive", daemon=True).start()
    logger.info("keep-alive ativo a cada %ss para %s", KEEPALIVE_INTERVAL_SECONDS, KEEPALIVE_URL)

ALLOWED_AUDIO_EXTENSIONS = {".wav", ".mp3", ".m4a"}
ALLOWED_AUDIO_TYPES = {
    "audio/wav",
    "audio/wave",
    "audio/x-wav",
    "audio/mpeg",
    "audio/mp3",
    "audio/mp4",
    "audio/x-m4a",
    "audio/m4a",
    "application/octet-stream",
}


@app.middleware("http")
async def request_id_middleware(request: Request, call_next):
    request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
    token = request_id_var.set(request_id[:80])
    try:
        response = await call_next(request)
        response.headers["X-Request-ID"] = request_id[:80]
        return response
    finally:
        request_id_var.reset(token)


class LeanIAContext(BaseModel):
    produtosFrequentes: list[str] = Field(default_factory=list)
    concorrentesFrequentes: list[str] = Field(default_factory=list)
    riscosFrequentes: list[str] = Field(default_factory=list)
    oportunidadesFrequentes: list[str] = Field(default_factory=list)
    ajustesRecentes: list[str] = Field(default_factory=list)


class TranscricaoRequest(BaseModel):
    texto: str = Field(min_length=1)
    contexto: LeanIAContext | None = None


def validar_token_interno(x_internal_token: Annotated[str | None, Header()] = None):
    if settings.require_token and settings.service_token and not hmac.compare_digest(x_internal_token or "", settings.service_token):
        raise HTTPException(status_code=401, detail="Nao autorizado")


def sanitizar_texto(texto: str) -> str:
    """Remove bytes nulos e espacos repetidos, mas preserva as quebras de linha:
    e nelas que a LeanIA separa os turnos de cada [LOCUTOR N]."""
    texto = texto.replace("\x00", " ").replace("\r\n", "\n").replace("\r", "\n")
    texto = re.sub(r"[ \t\f\v]+", " ", texto)
    texto = re.sub(r"\n{3,}", "\n\n", texto)
    return texto.strip()


def sanitize_filename(filename: str | None) -> str:
    value = filename or "audio"
    value = Path(value).name
    value = re.sub(r"[^A-Za-z0-9._-]+", "_", value).strip("._")
    return value or "audio"


def validate_audio_file(file: UploadFile):
    filename = sanitize_filename(file.filename)
    suffix = Path(filename).suffix.lower()
    if suffix not in ALLOWED_AUDIO_EXTENSIONS:
        raise HTTPException(
            status_code=415,
            detail={
                "errorCode": "UNSUPPORTED_AUDIO_FORMAT",
                "message": "Formato de audio nao suportado. Envie .wav, .mp3 ou .m4a.",
                "details": suffix or "sem extensao",
                "fallbackAvailable": True,
            },
        )
    if file.content_type and file.content_type not in ALLOWED_AUDIO_TYPES:
        raise HTTPException(
            status_code=415,
            detail={
                "errorCode": "UNSUPPORTED_AUDIO_MIME",
                "message": "Tipo MIME de audio nao suportado.",
                "details": file.content_type,
                "fallbackAvailable": True,
            },
        )
    return filename, suffix


async def persist_upload_temporarily(file: UploadFile, suffix: str) -> Path:
    max_bytes = settings.parakeet_max_audio_mb * 1024 * 1024
    written = 0
    fd, temp_name = tempfile.mkstemp(prefix="leania-audio-", suffix=suffix)
    temp_path = Path(temp_name)
    try:
        with os.fdopen(fd, "wb") as tmp:
            while chunk := await file.read(1024 * 1024):
                written += len(chunk)
                if written > max_bytes:
                    raise TranscriptionError(
                        "AUDIO_TOO_LARGE",
                        f"Audio excede o limite de {settings.parakeet_max_audio_mb} MB.",
                        f"{written} bytes recebidos.",
                        status_code=413,
                    )
                tmp.write(chunk)
        return temp_path
    except Exception:
        temp_path.unlink(missing_ok=True)
        raise


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


@app.post("/transcrever")
async def transcrever_audio(
    file: UploadFile = File(...),
    language: str | None = Form(default=None),
    meetingId: str | None = Form(default=None),
    forceModel: str | None = Form(default=None),
    timestamps: bool | None = Form(default=None),
    diarization: bool | None = Form(default=False),
    _: None = Depends(validar_token_interno),
):
    filename, suffix = validate_audio_file(file)
    started = time.perf_counter()
    temp_path: Path | None = None
    try:
        temp_path = await persist_upload_temporarily(file, suffix)
        if diarization:
            logger.info("Diarizacao solicitada para meetingId=%s, mas ainda nao esta habilitada", meetingId or "none")
        result = transcription_service.transcribe(temp_path, language, forceModel, timestamps)
        logger.info(
            "Audio transcrito com provider=%s model=%s elapsedMs=%s meetingId=%s",
            settings.transcription_provider,
            result.model,
            int((time.perf_counter() - started) * 1000),
            meetingId or "none",
        )
        payload = result.to_dict()
        payload["warnings"] = [
            *payload.get("warnings", []),
            "Transcricao automatica deve ser revisada antes de decisoes comerciais criticas.",
        ]
        return payload
    except TranscriptionError as exc:
        logger.warning("Transcricao indisponivel para arquivo=%s code=%s", filename, exc.error_code)
        raise HTTPException(status_code=exc.status_code, detail=exc.to_dict()) from exc
    finally:
        if temp_path:
            temp_path.unlink(missing_ok=True)


@app.get("/health")
def health():
    return {
        "status": "ok",
        **provider.status(),
        "transcription": transcription_service.status(),
        "cache": cache.stats() if settings.cache_enabled else {"enabled": False},
    }
