from pathlib import Path

from .models import TranscriptionResult
from .providers import ManualTranscriptionFallback, ParakeetTranscriptionProvider


class TranscriptionService:
    def __init__(self, settings):
        self.settings = settings
        self.parakeet = ParakeetTranscriptionProvider(settings)
        self.manual = ManualTranscriptionFallback()

    def transcribe(
        self,
        audio_path: Path,
        language: str | None = None,
        force_model: str | None = None,
        timestamps: bool | None = None,
    ) -> TranscriptionResult:
        provider = self.settings.transcription_provider
        if provider == "parakeet" or (provider == "auto" and self.settings.parakeet_enabled):
            return self.parakeet.transcribe(audio_path, language, force_model, timestamps)
        return self.manual.transcribe(audio_path, language, force_model, timestamps)

    def status(self) -> dict:
        return {
            "provider": self.settings.transcription_provider,
            "parakeet": self.parakeet.status(),
        }
