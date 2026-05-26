import logging
import os
import time
import wave
from pathlib import Path

from .models import TranscriptionError, TranscriptionResult

logger = logging.getLogger(__name__)


class ManualTranscriptionFallback:
    name = "manual"

    def transcribe(self, *_args, **_kwargs) -> TranscriptionResult:
        raise TranscriptionError(
            "TRANSCRIPTION_UNAVAILABLE",
            "Transcricao automatica indisponivel no momento. Voce ainda pode importar uma transcricao em texto ou JSON.",
            "Provider automatico desativado ou nao configurado.",
            status_code=503,
        )

    def status(self) -> dict:
        return {"provider": self.name, "available": False}


class ParakeetTranscriptionProvider:
    name = "parakeet"

    def __init__(self, settings):
        self.settings = settings
        self._model = None
        self._load_error = None

    def status(self) -> dict:
        available = self.settings.parakeet_enabled and self._load_error is None
        return {
            "provider": self.name,
            "enabled": self.settings.parakeet_enabled,
            "available": available,
            "model": self.settings.parakeet_model,
            "device": self.settings.parakeet_device,
            "loadError": self._load_error,
        }

    def transcribe(
        self,
        audio_path: Path,
        language: str | None = None,
        force_model: str | None = None,
        timestamps: bool | None = None,
    ) -> TranscriptionResult:
        if not self.settings.parakeet_enabled:
            raise TranscriptionError(
                "PARAKEET_DISABLED",
                "Transcricao automatica esta desativada. Importe texto ou JSON para continuar.",
                "PARAKEET_ENABLED=false",
                status_code=503,
            )

        duration = estimate_wav_duration(audio_path)
        if duration and duration > self.settings.parakeet_max_duration_seconds:
            raise TranscriptionError(
                "AUDIO_TOO_LONG",
                f"Audio excede o limite de {self.settings.parakeet_max_duration_seconds} segundos.",
                f"Duracao estimada: {duration:.1f}s",
                status_code=413,
            )

        model_name = force_model or self.settings.parakeet_model
        started = time.perf_counter()
        model = self._load_model(model_name)

        try:
            output = model.transcribe([str(audio_path)])
        except Exception as exc:
            logger.warning("Falha ao transcrever com Parakeet: %s", exc.__class__.__name__)
            raise TranscriptionError(
                "PARAKEET_TRANSCRIPTION_FAILED",
                "Nao foi possivel transcrever o audio automaticamente.",
                exc.__class__.__name__,
                status_code=502,
            ) from exc

        transcript = normalize_parakeet_output(output)
        if not transcript:
            raise TranscriptionError(
                "EMPTY_TRANSCRIPT",
                "O modelo nao retornou texto para este audio.",
                "Resultado vazio do provider Parakeet.",
                status_code=422,
            )

        warnings = []
        if duration is None:
            warnings.append("Duracao nao estimada automaticamente para este formato; valide o limite antes do envio em producao.")
        if timestamps or self.settings.parakeet_use_timestamps:
            warnings.append("Timestamps estao preparados no contrato, mas dependem do suporte do modelo/provider instalado.")

        return TranscriptionResult(
            transcript=transcript,
            language=language or self.settings.parakeet_language,
            duration_seconds=duration,
            model=model_name,
            processing_time_ms=int((time.perf_counter() - started) * 1000),
            segments=[],
            confidence=None,
            warnings=warnings,
        )

    def _load_model(self, model_name: str):
        if self._model is not None:
            return self._model
        if self.settings.parakeet_cache_dir:
            os.environ.setdefault("HF_HOME", self.settings.parakeet_cache_dir)

        try:
            from nemo.collections.asr.models import ASRModel
        except Exception as exc:
            self._load_error = "nemo_toolkit_asr_not_installed"
            raise TranscriptionError(
                "PARAKEET_NOT_INSTALLED",
                "Parakeet nao esta instalado neste ambiente. Use importacao manual de texto/JSON ou instale o provider ASR.",
                "Pacote nemo_toolkit[asr] indisponivel.",
                status_code=503,
            ) from exc

        try:
            model = ASRModel.from_pretrained(model_name=model_name)
            device = self.settings.parakeet_device
            if device and device != "auto":
                model = model.to(device)
            self._model = model
            self._load_error = None
            return self._model
        except Exception as exc:
            self._load_error = exc.__class__.__name__
            raise TranscriptionError(
                "PARAKEET_LOAD_FAILED",
                "Nao foi possivel carregar o modelo Parakeet.",
                exc.__class__.__name__,
                status_code=503,
            ) from exc


def estimate_wav_duration(audio_path: Path) -> float | None:
    if audio_path.suffix.lower() != ".wav":
        return None
    try:
        with wave.open(str(audio_path), "rb") as audio:
            frames = audio.getnframes()
            rate = audio.getframerate()
            return round(frames / float(rate), 2) if rate else None
    except wave.Error:
        return None


def normalize_parakeet_output(output) -> str:
    if output is None:
        return ""
    if isinstance(output, str):
        return output.strip()
    if isinstance(output, list):
        if not output:
            return ""
        first = output[0]
        if isinstance(first, str):
            return first.strip()
        text = getattr(first, "text", None)
        if text:
            return str(text).strip()
        return str(first).strip()
    text = getattr(output, "text", None)
    return str(text or output).strip()
