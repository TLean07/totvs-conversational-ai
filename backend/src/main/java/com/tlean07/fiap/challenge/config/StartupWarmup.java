package com.tlean07.fiap.challenge.config;

import com.tlean07.fiap.challenge.enums.StatusAlerta;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Aquece as consultas do dashboard logo depois que a aplicacao sobe.
 *
 * No plano free da Render (0,1 CPU) a primeira execucao de cada consulta paga a compilacao dos
 * planos do Hibernate e o JIT da JVM: o primeiro dashboard apos um deploy levava ~2 minutos e o
 * site parecia travado. Rodar as mesmas consultas uma vez em segundo plano, sem usuario, deixa a
 * primeira requisicao real na casa de 1 segundo. Desligue com WARMUP_ENABLED=false.
 */
@Component
public class StartupWarmup {

    private static final Logger log = LoggerFactory.getLogger(StartupWarmup.class);

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final AlertaRepository alertaRepository;
    private final boolean enabled;

    public StartupWarmup(TranscricaoRepository transcricaoRepository,
                         AnaliseResultadoRepository analiseRepository,
                         AlertaRepository alertaRepository,
                         @Value("${warmup.enabled:true}") boolean enabled) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
        this.alertaRepository = alertaRepository;
        this.enabled = enabled;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void aquecer() {
        if (!enabled) {
            return;
        }
        long inicio = System.currentTimeMillis();
        try {
            transcricaoRepository.count();
            transcricaoRepository.findInsightRows();
            transcricaoRepository.findTop5ByOrderByDataCriacaoDesc();
            analiseRepository.findDashboardRows();
            analiseRepository.findRiscoRows();
            analiseRepository.findOportunidadeRows();
            analiseRepository.findRiscoConsolidadoRows();
            analiseRepository.findOportunidadeConsolidadaRows();
            alertaRepository.findTop10ByStatusOrderByPrioridadeAscDataCriacaoDesc(StatusAlerta.NOVO);
            alertaRepository.countByStatus(StatusAlerta.NOVO);
            log.info("Aquecimento das consultas do dashboard concluido em {} ms", System.currentTimeMillis() - inicio);
        } catch (Exception e) {
            log.info("Aquecimento das consultas falhou ({}); a primeira requisicao real fara o trabalho", e.getClass().getSimpleName());
        }
    }
}
