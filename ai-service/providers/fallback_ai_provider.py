from analyzer import TranscricaoAnalyzer


class FallbackAiProvider:
    """Analise gratuita/offline baseada em regras locais."""

    name = "fallback"

    def __init__(self, max_chunks: int):
        self._analyzer = TranscricaoAnalyzer(
            enable_transformers=False,
            max_sentiment_chunks=max_chunks,
        )

    def analyze(self, text: str, context: dict | None = None) -> dict:
        return self._analyzer.analisar(text, context)

    def is_ready(self) -> bool:
        return True
