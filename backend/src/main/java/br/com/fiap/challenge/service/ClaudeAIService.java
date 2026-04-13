package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.ClaudeAnaliseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ClaudeAIService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAIService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String aiServiceUrl;

    public ClaudeAIService(ObjectMapper objectMapper,
                           @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.aiServiceUrl = aiServiceUrl;
    }

    public ClaudeAnaliseDTO analisarTranscricao(String textoTranscricao) {
        try {
            log.info("Enviando transcricao para microsservico de IA em {}", aiServiceUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

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
