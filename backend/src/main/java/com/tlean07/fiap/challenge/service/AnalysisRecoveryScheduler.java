package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reenfileira transcricoes que ficaram em PROCESSANDO sem estarem na fila desta instancia
 * (reinicio, deploy ou pool esgotado durante uma importacao grande).
 *
 * Fica em um bean separado de proposito: chamar {@code processar} de dentro do proprio
 * {@link TranscricaoAnalysisJob} nao passa pelo proxy do Spring e o {@code @Async} e ignorado,
 * o que executava tudo em serie na thread do agendador.
 */
@Component
public class AnalysisRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRecoveryScheduler.class);

    private final TranscricaoRepository transcricaoRepository;
    private final TranscricaoAnalysisJob analysisJob;

    public AnalysisRecoveryScheduler(TranscricaoRepository transcricaoRepository, TranscricaoAnalysisJob analysisJob) {
        this.transcricaoRepository = transcricaoRepository;
        this.analysisJob = analysisJob;
    }

    @Scheduled(fixedDelayString = "${analysis.recovery-interval-ms:180000}",
            initialDelayString = "${analysis.recovery-initial-delay-ms:120000}")
    public void recuperarPresas() {
        List<Transcricao> presas;
        try {
            presas = transcricaoRepository.findTop100ByStatusOrderByDataCriacaoAsc(StatusTranscricao.PROCESSANDO);
        } catch (Exception e) {
            log.info("Recuperacao adiada, banco indisponivel: {}", e.getClass().getSimpleName());
            return;
        }
        int reenfileiradas = 0;
        for (Transcricao t : presas) {
            if (analysisJob.isPendente(t.getId())) {
                continue;
            }
            analysisJob.processar(t.getId());
            reenfileiradas++;
        }
        if (reenfileiradas > 0) {
            log.info("Recuperacao: {} transcricao(oes) em PROCESSANDO reenfileirada(s)", reenfileiradas);
        }
    }
}
