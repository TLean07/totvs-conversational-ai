from analyzer import TranscricaoAnalyzer


class LocalAiProvider:
    """Provider gratuito com modelo local/self-hosted via Hugging Face Transformers."""

    name = "transformers"

    def __init__(self, model_name: str, max_chunks: int):
        self._analyzer = TranscricaoAnalyzer(
            model_name=model_name,
            enable_transformers=True,
            max_sentiment_chunks=max_chunks,
        )

    def analyze(self, text: str, context: dict | None = None) -> dict:
        return self._analyzer.analisar(text, context)

    def is_ready(self) -> bool:
        return self._analyzer.is_ready()
