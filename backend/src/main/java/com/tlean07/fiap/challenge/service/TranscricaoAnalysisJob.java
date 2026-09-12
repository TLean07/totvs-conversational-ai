package com.tlean07.fiap.challenge.service;

import org.springframework.cache.annotation.CacheEvict;
import com.tlean07.fiap.challenge.config.CacheConfig.CacheNames;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Alerta;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TranscricaoAnalysisJob {

    private static final Logger log = LoggerFactory.getLogger(TranscricaoAnalysisJob.class);

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final AlertaRepository alertaRepository;
    private final AnaliseService analiseService;

    public TranscricaoAnalysisJob(TranscricaoRepository transcricaoRepository,
                                  AnaliseResultadoRepository analiseRepository,
                                  AlertaRepository alertaRepository,
                                  AnaliseService analiseService) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
        this.alertaRepository = alertaRepository;
        this.analiseService = analiseService;
    }

    @Async
    @CacheEvict(cacheNames = {CacheNames.DASHBOARD, CacheNames.TRANSCRICOES, CacheNames.TRANSCRICAO, CacheNames.INSIGHTS, CacheNames.ALERTAS, CacheNames.LEANIA_CONTEXTO}, allEntries = true)
    public void processar(String transcricaoId) {
        Transcricao transcricao = transcricaoRepository.findById(transcricaoId).orElse(null);
        if (transcricao == null) {
            log.warn("Transcricao {} nao encontrada para processamento", transcricaoId);
            return;
        }

        try {
            limparAnaliseAnterior(transcricao);
            AnaliseResultado analise = analiseService.processarAnalise(transcricao);
            transcricao.setAnaliseResultado(analise);
            transcricao.setStatus(StatusTranscricao.PROCESSADA);
            transcricao.setDataProcessamento(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Falha ao processar transcricao {}: {}", transcricaoId, e.getClass().getSimpleName());
            transcricao.setStatus(StatusTranscricao.ERRO);
        }

        transcricaoRepository.save(transcricao);
    }

    private void limparAnaliseAnterior(Transcricao transcricao) {
        AnaliseResultado existente = transcricao.getAnaliseResultado();
        if (existente != null && existente.getId() != null) {
            alertaRepository.deleteByAnaliseId(existente.getId());
            analiseRepository.deleteById(existente.getId());
            transcricao.setAnaliseResultado(null);
        }
        analiseRepository.findByTranscricaoId(transcricao.getId()).ifPresent(analise -> {
            alertaRepository.deleteByAnaliseId(analise.getId());
            analiseRepository.delete(analise);
        });
    }
}
