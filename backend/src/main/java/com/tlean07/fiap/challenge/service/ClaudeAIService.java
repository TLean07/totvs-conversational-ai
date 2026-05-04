package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.ClaudeAnaliseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class ClaudeAIService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAIService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String aiServiceUrl;
    private final String aiServiceToken;

    public ClaudeAIService(ObjectMapper objectMapper,
                           RestTemplateBuilder restTemplateBuilder,
                           @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
                           @Value("${ai.service.token:}") String aiServiceToken) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = objectMapper;
        this.aiServiceUrl = aiServiceUrl;
        this.aiServiceToken = aiServiceToken;
    }

    public ClaudeAnaliseDTO analisarTranscricao(String textoTranscricao) {
        try {
            log.info("Enviando transcricao para microsservico de IA em {}", aiServiceUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (aiServiceToken != null && !aiServiceToken.isBlank()) {
                headers.set("X-Internal-Token", aiServiceToken);
            }

            Map<String, String> body = Map.of("texto", textoTranscricao);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiServiceUrl + "/analisar", request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Resposta invalida do microsservico de IA: " + response.getStatusCode());
            }

            return objectMapper.readValue(response.getBody(), ClaudeAnaliseDTO.class);

        } catch (Exception e) {
            log.error("Erro ao analisar transcricao com microsservico de IA: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na analise com IA: " + e.getMessage(), e);
        }
    }
}
