package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlean07.fiap.challenge.dto.AiAnalysisDTO;
import com.tlean07.fiap.challenge.dto.LeanIAContextDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Porta de entrada da LeanIA. Tenta o microsservico Python (que pode usar o modelo local de
 * sentimento do Hugging Face) e, se ele nao responder, roda o {@link LeanIALocalAnalyzer},
 * que implementa as mesmas regras em Java. O resultado tem o mesmo contrato nos dois casos.
 */
@Service
public class LeanIAService {

    private static final Logger log = LoggerFactory.getLogger(LeanIAService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LeanIALocalAnalyzer localAnalyzer;
    private final String aiServiceUrl;
    private final String aiServiceToken;
    private final int maxRetries;

    public LeanIAService(ObjectMapper objectMapper,
                         RestTemplateBuilder restTemplateBuilder,
                         LeanIALocalAnalyzer localAnalyzer,
                         @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
                         @Value("${ai.service.token:}") String aiServiceToken,
                         @Value("${ai.service.max-retries:1}") int maxRetries,
                         @Value("${ai.service.read-timeout-seconds:120}") int readTimeoutSeconds) {
        // Reunioes reais tem ~35 mil caracteres; o modelo de sentimento leva mais de 30 s em CPU.
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(Math.max(30, readTimeoutSeconds)))
                .build();
        this.objectMapper = objectMapper;
        this.localAnalyzer = localAnalyzer;
        this.aiServiceUrl = aiServiceUrl;
        this.aiServiceToken = aiServiceToken;
        this.maxRetries = Math.max(0, maxRetries);
    }

    public AiAnalysisDTO analisarTranscricao(String textoTranscricao, LeanIAContextDTO contexto) {
        Exception lastError = null;
        int attempts = maxRetries + 1;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                AiAnalysisDTO remoto = callLeanIA(textoTranscricao, contexto);
                return completarComMotorLocal(remoto, textoTranscricao, contexto);
            } catch (Exception e) {
                lastError = e;
                log.warn("LeanIA indisponivel na tentativa {}/{}: {}",
                        attempt, attempts, e.getClass().getSimpleName());
            }
        }

        log.warn("Usando motor local da LeanIA. Motivo: {}",
                lastError == null ? "desconhecido" : lastError.getClass().getSimpleName());
        return localAnalyzer.analisar(textoTranscricao, contexto);
    }

    private AiAnalysisDTO callLeanIA(String textoTranscricao, LeanIAContextDTO contexto) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (aiServiceToken != null && !aiServiceToken.isBlank()) {
            headers.set("X-Internal-Token", aiServiceToken);
        }
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            headers.set("X-Request-ID", requestId);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("texto", textoTranscricao);
        if (contexto != null) {
            body.put("contexto", contexto);
        }
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                aiServiceUrl + "/analisar", request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Resposta invalida da LeanIA: " + response.getStatusCode());
        }

        return objectMapper.readValue(response.getBody(), AiAnalysisDTO.class);
    }

    /**
     * Um ai-service de versao anterior nao devolve os blocos novos (proxima acao, insights,
     * indicadores). Nesse caso o motor local preenche so o que faltou, sem sobrescrever o resto.
     */
    private AiAnalysisDTO completarComMotorLocal(AiAnalysisDTO remoto, String texto, LeanIAContextDTO contexto) {
        if (remoto == null) {
            return localAnalyzer.analisar(texto, contexto);
        }
        boolean faltaAlgo = remoto.getProximaAcao() == null || remoto.getInsightsAdicionais() == null
                || remoto.getIndicadoresQualidade() == null || remoto.getSentimentoDetalhe() == null;
        if (!faltaAlgo) {
            return remoto;
        }
        AiAnalysisDTO local = localAnalyzer.analisar(texto, contexto);
        if (remoto.getProximaAcao() == null) {
            remoto.setProximaAcao(local.getProximaAcao());
        }
        if (remoto.getSentimentoDetalhe() == null) {
            remoto.setSentimentoDetalhe(local.getSentimentoDetalhe());
        }
        if (remoto.getInsightsAdicionais() == null) {
            remoto.setInsightsAdicionais(local.getInsightsAdicionais());
        }
        if (remoto.getIndicadoresQualidade() == null) {
            remoto.setIndicadoresQualidade(local.getIndicadoresQualidade());
        }
        return remoto;
    }
}
