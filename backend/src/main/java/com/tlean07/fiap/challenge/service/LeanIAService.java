package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlean07.fiap.challenge.dto.AiAnalysisDTO;
import com.tlean07.fiap.challenge.dto.LeanIAContextDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class LeanIAService {

    private static final Logger log = LoggerFactory.getLogger(LeanIAService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String aiServiceUrl;
    private final String aiServiceToken;
    private final int maxRetries;

    public LeanIAService(ObjectMapper objectMapper,
                         RestTemplateBuilder restTemplateBuilder,
                         @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
                         @Value("${ai.service.token:}") String aiServiceToken,
                         @Value("${ai.service.max-retries:1}") int maxRetries) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = objectMapper;
        this.aiServiceUrl = aiServiceUrl;
        this.aiServiceToken = aiServiceToken;
        this.maxRetries = Math.max(0, maxRetries);
    }

    public AiAnalysisDTO analisarTranscricao(String textoTranscricao, LeanIAContextDTO contexto) {
        Exception lastError = null;
        int attempts = maxRetries + 1;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return callLeanIA(textoTranscricao, contexto);
            } catch (Exception e) {
                lastError = e;
                log.warn("LeanIA indisponivel na tentativa {}/{}: {}",
                        attempt, attempts, e.getClass().getSimpleName());
            }
        }

        log.warn("Usando fallback local gratuito da LeanIA. Motivo: {}",
                lastError == null ? "desconhecido" : lastError.getClass().getSimpleName());
        return fallbackAnalysis(textoTranscricao);
    }

    private AiAnalysisDTO callLeanIA(String textoTranscricao, LeanIAContextDTO contexto) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (aiServiceToken != null && !aiServiceToken.isBlank()) {
            headers.set("X-Internal-Token", aiServiceToken);
        }

        Map<String, Object> body = new java.util.HashMap<>();
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

    private AiAnalysisDTO fallbackAnalysis(String textoTranscricao) {
        String texto = textoTranscricao == null ? "" : textoTranscricao.toLowerCase();
        boolean negativo = containsAny(texto, "problema", "erro", "lento", "trava", "cancelar", "trocar", "nao renovar");
        boolean positivo = containsAny(texto, "otimo", "excelente", "satisfeito", "funciona bem", "aprovamos");
        boolean concorrente = containsAny(texto, "sap", "oracle", "senior", "linx", "sankhya");

        String sentimento = positivo && negativo ? "MISTO" : negativo ? "NEGATIVO" : positivo ? "POSITIVO" : "NEUTRO";

        AiAnalysisDTO.AiAnalysisDTOBuilder builder = AiAnalysisDTO.builder()
                .sentimentoGeral(sentimento)
                .resumo("Analise basica gerada localmente pela LeanIA porque o provedor gratuito nao respondeu. Revise os insights antes de tomar decisao comercial.")
                .personaIdentificada("Nao identificado")
                .budgetIdentificado(extractBudget(textoTranscricao))
                .scoreConfiancaVendedor(negativo ? 0.35 : positivo ? 0.75 : 0.5)
                .produtos(List.of())
                .oportunidades(List.of())
                .riscos(List.of());

        if (negativo || concorrente) {
            AiAnalysisDTO.RiscoDTO risco = AiAnalysisDTO.RiscoDTO.builder()
                    .tipo(concorrente ? "CONCORRENCIA" : "INSATISFACAO")
                    .descricao("Fallback local da LeanIA detectou sinais que exigem acompanhamento do cliente.")
                    .severidade(concorrente ? "ALTO" : "MEDIO")
                    .concorrenteMencionado(concorrente ? "Concorrente mencionado" : null)
                    .build();
            builder.riscos(List.of(risco));
        }

        return builder.build();
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String extractBudget(String text) {
        if (text == null || text.isBlank()) {
            return "Nao identificado";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("R\\$\\s*[\\d.,]+|[\\d.,]+\\s*(mil|reais|milhao|milhoes)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(text);
        return matcher.find() ? matcher.group() : "Nao identificado";
    }
}
