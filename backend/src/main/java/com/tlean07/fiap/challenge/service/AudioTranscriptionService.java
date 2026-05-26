package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlean07.fiap.challenge.dto.TranscriptionResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Service
public class AudioTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(AudioTranscriptionService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".wav", ".mp3", ".m4a");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/wav",
            "audio/wave",
            "audio/x-wav",
            "audio/mpeg",
            "audio/mp3",
            "audio/mp4",
            "audio/x-m4a",
            "audio/m4a",
            "application/octet-stream"
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String aiServiceUrl;
    private final String aiServiceToken;
    private final long maxAudioBytes;
    private final int maxDurationSeconds;
    private final String defaultLanguage;

    public AudioTranscriptionService(ObjectMapper objectMapper,
                                     RestTemplateBuilder restTemplateBuilder,
                                     @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
                                     @Value("${ai.service.token:}") String aiServiceToken,
                                     @Value("${transcription.max-audio-mb:100}") int maxAudioMb,
                                     @Value("${transcription.max-duration-seconds:3600}") int maxDurationSeconds,
                                     @Value("${transcription.language:pt}") String defaultLanguage) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofMinutes(10))
                .build();
        this.objectMapper = objectMapper;
        this.aiServiceUrl = aiServiceUrl;
        this.aiServiceToken = aiServiceToken;
        this.maxAudioBytes = Math.max(1, maxAudioMb) * 1024L * 1024L;
        this.maxDurationSeconds = Math.max(1, maxDurationSeconds);
        this.defaultLanguage = defaultLanguage;
    }

    public TranscriptionResultDTO transcrever(MultipartFile file, String meetingId) {
        validateAudio(file);
        try {
            TranscriptionResultDTO result = callAiService(file, meetingId);
            if (result.getDurationSeconds() != null && result.getDurationSeconds() > maxDurationSeconds) {
                throw new IllegalArgumentException("Audio excede o limite de duracao de " + maxDurationSeconds + " segundos.");
            }
            if (result.getTranscript() == null || result.getTranscript().isBlank()) {
                throw new IllegalArgumentException("A transcricao automatica retornou texto vazio.");
            }
            return result;
        } catch (HttpStatusCodeException e) {
            log.warn("AI Service recusou transcricao de audio: status={} bodyLength={}", e.getStatusCode(), safeLength(e.getResponseBodyAsString()));
            throw new IllegalStateException(extractFriendlyError(e.getResponseBodyAsString()), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Falha ao transcrever audio com ai-service: {}", e.getClass().getSimpleName());
            throw new IllegalStateException("Transcricao automatica indisponivel no momento. Voce ainda pode importar uma transcricao em texto ou JSON.", e);
        }
    }

    private TranscriptionResultDTO callAiService(MultipartFile file, String meetingId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (aiServiceToken != null && !aiServiceToken.isBlank()) {
            headers.set("X-Internal-Token", aiServiceToken);
        }
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            headers.set("X-Request-ID", requestId);
        }

        ByteArrayResource audioResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return sanitizeFileName(file.getOriginalFilename());
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", audioResource);
        body.add("language", defaultLanguage);
        if (meetingId != null && !meetingId.isBlank()) {
            body.add("meetingId", meetingId);
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
                aiServiceUrl + "/transcrever",
                new HttpEntity<>(body, headers),
                String.class
        );
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Resposta invalida do AI Service ao transcrever audio.");
        }
        return objectMapper.readValue(response.getBody(), TranscriptionResultDTO.class);
    }

    private void validateAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de audio obrigatorio.");
        }
        if (file.getSize() > maxAudioBytes) {
            throw new IllegalArgumentException("Audio excede o limite de " + (maxAudioBytes / 1024 / 1024) + " MB.");
        }
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Formato de audio nao suportado. Envie .wav, .mp3 ou .m4a.");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Tipo de audio nao suportado: " + contentType);
        }
    }

    private String extractFriendlyError(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode detail = root.get("detail");
            if (detail != null && detail.hasNonNull("message")) {
                return detail.get("message").asText();
            }
            if (detail != null && detail.isTextual()) {
                return detail.asText();
            }
        } catch (Exception ignored) {
        }
        return "Transcricao automatica indisponivel no momento. Voce ainda pode importar uma transcricao em texto ou JSON.";
    }

    private String sanitizeFileName(String value) {
        String fallback = "audio.wav";
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.replaceAll("[\\\\/]", "_").replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot).toLowerCase(Locale.ROOT) : "";
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
