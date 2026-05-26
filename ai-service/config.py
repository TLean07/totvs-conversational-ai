"""
Configuracao central do AI Service.
Somente usa variaveis locais/ambiente e nao depende de APIs pagas.
"""

from dataclasses import dataclass
import os


def _bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "sim", "on"}


def _int_env(name: str, default: int) -> int:
    try:
        return int(os.getenv(name, str(default)))
    except ValueError:
        return default


@dataclass(frozen=True)
class Settings:
    service_token: str
    require_token: bool
    allowed_origins: list[str]
    provider: str
    model_name: str
    max_chars: int
    cache_enabled: bool
    cache_ttl_seconds: int
    cache_max_entries: int
    sentiment_max_chunks: int
    transcription_provider: str
    parakeet_enabled: bool
    parakeet_model: str
    parakeet_device: str
    parakeet_max_audio_mb: int
    parakeet_max_duration_seconds: int
    parakeet_language: str
    parakeet_use_timestamps: bool
    parakeet_cache_dir: str


def load_settings() -> Settings:
    allowed_origins = [
        origin.strip()
        for origin in os.getenv("AI_CORS_ALLOWED_ORIGINS", "").split(",")
        if origin.strip()
    ]

    provider = os.getenv("AI_PROVIDER", "auto").strip().lower()
    if provider not in {"auto", "transformers", "fallback"}:
        provider = "auto"

    return Settings(
        service_token=os.getenv("AI_SERVICE_TOKEN", ""),
        require_token=_bool_env("AI_SERVICE_REQUIRE_TOKEN", True),
        allowed_origins=allowed_origins,
        provider=provider,
        model_name=os.getenv(
            "AI_MODEL_NAME",
            "nlptown/bert-base-multilingual-uncased-sentiment",
        ),
        max_chars=_int_env("AI_MAX_CHARS", 20000),
        cache_enabled=_bool_env("AI_CACHE_ENABLED", True),
        cache_ttl_seconds=_int_env("AI_CACHE_TTL_SECONDS", 3600),
        cache_max_entries=_int_env("AI_CACHE_MAX_ENTRIES", 256),
        sentiment_max_chunks=_int_env("AI_SENTIMENT_MAX_CHUNKS", 8),
        transcription_provider=os.getenv("TRANSCRIPTION_PROVIDER", "manual").strip().lower(),
        parakeet_enabled=_bool_env("PARAKEET_ENABLED", False),
        parakeet_model=os.getenv("PARAKEET_MODEL", "nvidia/parakeet-tdt-0.6b-v3"),
        parakeet_device=os.getenv("PARAKEET_DEVICE", "auto").strip().lower(),
        parakeet_max_audio_mb=_int_env("PARAKEET_MAX_AUDIO_MB", 100),
        parakeet_max_duration_seconds=_int_env("PARAKEET_MAX_DURATION_SECONDS", 3600),
        parakeet_language=os.getenv("PARAKEET_LANGUAGE", "pt"),
        parakeet_use_timestamps=_bool_env("PARAKEET_USE_TIMESTAMPS", False),
        parakeet_cache_dir=os.getenv("PARAKEET_CACHE_DIR", ""),
    )
