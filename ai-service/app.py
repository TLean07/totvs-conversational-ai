"""
Microsservico de IA para Inteligencia Conversacional
Analisa transcricoes de reunioes usando HuggingFace + spaCy
"""

from fastapi import FastAPI
from pydantic import BaseModel
from analyzer import TranscricaoAnalyzer

app = FastAPI(
    title="TOTVS Conversational Intelligence - AI Service",
    description="Microsservico de NLP para analise de transcricoes",
    version="1.0.0",
)

analyzer = TranscricaoAnalyzer()


class TranscricaoRequest(BaseModel):
    texto: str


@app.post("/analisar")
def analisar_transcricao(request: TranscricaoRequest):
    resultado = analyzer.analisar(request.texto)
    return resultado


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": analyzer.is_ready()}
