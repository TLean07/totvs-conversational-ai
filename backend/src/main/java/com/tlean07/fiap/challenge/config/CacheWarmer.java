package com.tlean07.fiap.challenge.config;

import com.tlean07.fiap.challenge.enums.UserRole;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.service.DashboardService;
import com.tlean07.fiap.challenge.service.TranscricaoInsightsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mantem o dashboard e os insights do administrador aquecidos no cache.
 *
 * Na CPU do plano free da Render (0,1 vCPU) montar o dashboard e os insights de centenas de
 * reunioes leva de 1 a 2 minutos; se o usuario e quem paga esse tempo, a tela parece travada.
 * Este job recalcula as duas respostas em segundo plano, como um administrador sintetico, um
 * pouco antes de o cache expirar (TTL de 10 min), e logo apos a subida da aplicacao. Todos os
 * administradores compartilham a mesma chave de cache ({@link CacheConfig.CacheKeys#ADMIN_KEY}).
 * Desligue com WARMUP_ENABLED=false.
 */
@Component
public class CacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmer.class);
    private static final AuthenticatedUser ADMIN_SINTETICO =
            new AuthenticatedUser("system-warmup", "warmup@leania.local", "LeanIA Warmup", UserRole.ADMIN);

    private final DashboardService dashboardService;
    private final TranscricaoInsightsService insightsService;
    private final boolean enabled;

    public CacheWarmer(DashboardService dashboardService,
                       TranscricaoInsightsService insightsService,
                       @Value("${warmup.enabled:true}") boolean enabled) {
        this.dashboardService = dashboardService;
        this.insightsService = insightsService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${warmup.interval-ms:570000}", initialDelayString = "${warmup.initial-delay-ms:5000}")
    public void aquecer() {
        if (!enabled) {
            return;
        }
        long inicio = System.currentTimeMillis();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ADMIN_SINTETICO, null, List.of()));
        try {
            dashboardService.getDashboard();
            insightsService.listarTodos();
            log.info("Cache do dashboard/insights aquecido em {} ms", System.currentTimeMillis() - inicio);
        } catch (Exception e) {
            log.info("Aquecimento do cache falhou ({}); a proxima requisicao real fara o trabalho", e.getClass().getSimpleName());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
