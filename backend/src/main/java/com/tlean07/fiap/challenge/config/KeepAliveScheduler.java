package com.tlean07.fiap.challenge.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Keep-alive mutuo entre o backend (Render free) e a LeanIA (Hugging Face Space free).
 *
 * Os dois planos gratuitos hibernam sem trafego: a Render apos 15 minutos, o Space apos 48 horas,
 * e a instancia da Render leva ~2 minutos para subir de novo. Este job chama o {@code /health} da
 * LeanIA a cada 10 minutos, e o ai-service faz o mesmo contra este backend (KEEPALIVE_URL), de modo
 * que qualquer um dos dois que estiver de pe mantem o outro acordado. Desligue com KEEPALIVE_ENABLED=false.
 */
@Component
@EnableScheduling
public class KeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveScheduler.class);

    private final RestTemplate restTemplate;
    private final boolean enabled;
    private final String url;

    public KeepAliveScheduler(RestTemplateBuilder restTemplateBuilder,
                              @Value("${keepalive.enabled:true}") boolean enabled,
                              @Value("${keepalive.url:}") String url,
                              @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
        this.enabled = enabled;
        this.url = url == null || url.isBlank() ? aiServiceUrl.replaceAll("/+$", "") + "/health" : url;
    }

    @Scheduled(fixedDelayString = "${keepalive.interval-ms:600000}", initialDelayString = "${keepalive.initial-delay-ms:90000}")
    public void ping() {
        if (!enabled) {
            return;
        }
        try {
            restTemplate.getForEntity(url, String.class);
            log.debug("keep-alive ok: {}", url);
        } catch (Exception e) {
            // Espaco dormindo ou rede instavel: a proxima tentativa acorda de novo, sem alarde no log.
            log.info("keep-alive falhou para {}: {}", url, e.getClass().getSimpleName());
        }
    }
}
