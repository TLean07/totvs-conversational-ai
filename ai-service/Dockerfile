FROM python:3.11-slim

WORKDIR /app
RUN addgroup --system app && adduser --system --ingroup app app
ENV HF_HOME=/app/.cache/huggingface

# Instalar dependencias
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Baixar modelo gratuito na build quando possivel (evita download no startup)
ARG AI_MODEL_NAME=nlptown/bert-base-multilingual-uncased-sentiment
RUN python -c "from transformers import pipeline; pipeline('sentiment-analysis', model='${AI_MODEL_NAME}')"

COPY . .
RUN chown -R app:app /app
USER app

# HuggingFace Spaces usa porta 7860
EXPOSE 7860

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7860"]
