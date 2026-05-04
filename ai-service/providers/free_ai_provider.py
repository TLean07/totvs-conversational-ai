import logging

from config import Settings
from providers.fallback_ai_provider import FallbackAiProvider
from providers.local_ai_provider import LocalAiProvider

logger = logging.getLogger(__name__)


class FreeAiProvider:
    """
    Orquestra provedores 100% gratuitos.
    Ordem recomendada: modelo local/self-hosted -> fallback offline.
    """

    def __init__(self, settings: Settings):
        self.settings = settings
        self.fallback = FallbackAiProvider(settings.sentiment_max_chunks)
        self.local = None

        if settings.provider in {"auto", "transformers"}:
            self.local = LocalAiProvider(settings.model_name, settings.sentiment_max_chunks)

    def analyze(self, text: str, context: dict | None = None) -> dict:
        if self.settings.provider == "fallback" or self.local is None:
            return self.fallback.analyze(text, context)

        try:
            return self.local.analyze(text, context)
        except Exception as exc:
            logger.warning("Provider local indisponivel; usando fallback offline: %s", exc.__class__.__name__)
            return self.fallback.analyze(text, context)

    def status(self) -> dict:
        return {
            "provider": self.settings.provider,
            "local_provider": self.local.name if self.local else None,
            "local_model_loaded": self.local.is_ready() if self.local else False,
            "fallback_available": self.fallback.is_ready(),
        }
