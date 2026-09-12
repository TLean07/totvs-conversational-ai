package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.config.CacheConfig.CacheNames;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processa a analise de uma transcricao em segundo plano.
 *
 * Resiliencia: falhas de banco (pool esgotado, sessao derrubada pela FIAP) sao tentadas de novo com
 * espera; se mesmo assim falhar, a transcricao vai para ERRO em vez de ficar em PROCESSANDO para
 * sempre. O {@link AnalysisRecoveryScheduler} reenfileira o que ficou preso em PROCESSANDO (por
 * exemplo, apos um reinicio da instancia no meio de uma importacao grande).
 */
@Service
public class TranscricaoAnalysisJob {

    private static final Logger log = LoggerFactory.getLogger(TranscricaoAnalysisJob.class);
    private static final int MAX_TENTATIVAS = 3;
    private static final long[] ESPERA_MS = {5_000L, 15_000L, 30_000L};

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final AlertaRepository alertaRepository;
    private final AnaliseService analiseService;

    /** Ids enfileirados ou em execucao, para o job de recuperacao nao duplicar trabalho. */
    private final Set<String> pendentes = ConcurrentHashMap.newKeySet();

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
        pendentes.add(transcricaoId);
        try {
            executarComRetentativas(transcricaoId);
        } finally {
            pendentes.remove(transcricaoId);
        }
    }

    private void executarComRetentativas(String transcricaoId) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                executar(transcricaoId);
                return;
            } catch (DataAccessException | CannotCreateTransactionException e) {
                log.warn("Banco indisponivel ao processar {} (tentativa {}/{}): {}",
                        transcricaoId, tentativa, MAX_TENTATIVAS, e.getClass().getSimpleName());
                if (tentativa == MAX_TENTATIVAS) {
                    marcarErro(transcricaoId);
                    return;
                }
                dormir(ESPERA_MS[Math.min(tentativa - 1, ESPERA_MS.length - 1)]);
            }
        }
    }

    private void executar(String transcricaoId) {
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
        } catch (DataAccessException | CannotCreateTransactionException e) {
            throw e; // deixa a retentativa decidir
        } catch (Exception e) {
            log.warn("Falha ao processar transcricao {}: {}", transcricaoId, e.getClass().getSimpleName());
            transcricao.setStatus(StatusTranscricao.ERRO);
        }

        transcricaoRepository.save(transcricao);
    }

    private void marcarErro(String transcricaoId) {
        try {
            transcricaoRepository.findById(transcricaoId).ifPresent(t -> {
                t.setStatus(StatusTranscricao.ERRO);
                transcricaoRepository.save(t);
            });
        } catch (Exception e) {
            log.warn("Nao foi possivel marcar {} como ERRO: {}", transcricaoId, e.getClass().getSimpleName());
        }
    }

    /** Se a transcricao ja esta enfileirada ou em execucao nesta instancia. */
    public boolean isPendente(String transcricaoId) {
        return pendentes.contains(transcricaoId);
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

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
