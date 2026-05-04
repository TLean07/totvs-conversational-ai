package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.*;
import com.tlean07.fiap.challenge.dto.TranscricaoRequest;
import com.tlean07.fiap.challenge.dto.TranscricaoResponse;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.AnaliseFeedback;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.AnaliseFeedbackRepository;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TranscricaoService {

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final AnaliseFeedbackRepository feedbackRepository;
    private final AlertaRepository alertaRepository;
    private final AnaliseService analiseService;
    private final TranscricaoAnalysisJob analysisJob;
    private final AuditService auditService;

    public TranscricaoService(TranscricaoRepository transcricaoRepository,
                              AnaliseResultadoRepository analiseRepository,
                              AnaliseFeedbackRepository feedbackRepository,
                              AlertaRepository alertaRepository,
                              AnaliseService analiseService,
                              TranscricaoAnalysisJob analysisJob,
                              AuditService auditService) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
        this.feedbackRepository = feedbackRepository;
        this.alertaRepository = alertaRepository;
        this.analiseService = analiseService;
        this.analysisJob = analysisJob;
        this.auditService = auditService;
    }

    public TranscricaoResponse criarEAnalisar(TranscricaoRequest request) {
        return criarEAnalisar(request, SecurityUtils.currentUser().id());
    }

    public TranscricaoResponse criarEAnalisar(TranscricaoRequest request, String ownerId) {
        Transcricao transcricao = Transcricao.builder()
                .ownerId(ownerId)
                .textoOriginal(request.getTextoOriginal())
                .participantes(request.getParticipantes())
                .duracaoMinutos(request.getDuracaoMinutos())
                .cliente(request.getCliente())
                .vendedor(request.getVendedor())
                .status(StatusTranscricao.PENDENTE)
                .dataCriacao(LocalDateTime.now())
                .build();

        if (request.getDataReuniao() != null && !request.getDataReuniao().isBlank()) {
            transcricao.setDataReuniao(LocalDateTime.parse(request.getDataReuniao(),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        transcricao = transcricaoRepository.save(transcricao);

        transcricao.setStatus(StatusTranscricao.PROCESSANDO);
        transcricao = transcricaoRepository.save(transcricao);
        analysisJob.processar(transcricao.getId());
        auditService.record("transcricao.create", ownerId, transcricao.getId(), "queued");
        return toResponse(transcricao);
    }

    public List<TranscricaoResponse> listarTodas() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<Transcricao> transcricoes = user.isAdmin()
                ? transcricaoRepository.findAllByOrderByDataCriacaoDesc()
                : transcricaoRepository.findByOwnerIdOrderByDataCriacaoDesc(user.id());

        return transcricoes
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse<TranscricaoResponse> listarPaginado(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(50, size));
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "dataCriacao"));
        AuthenticatedUser user = SecurityUtils.currentUser();
        Page<Transcricao> result = user.isAdmin()
                ? transcricaoRepository.findAllByOrderByDataCriacaoDesc(pageRequest)
                : transcricaoRepository.findByOwnerIdOrderByDataCriacaoDesc(user.id(), pageRequest);

        return PageResponse.<TranscricaoResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public TranscricaoResponse buscarPorId(String id) {
        Transcricao transcricao = findVisibleTranscricao(id);
        return toResponse(transcricao);
    }

    public List<TranscricaoResponse> buscarPorCliente(String cliente) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<Transcricao> transcricoes = user.isAdmin()
                ? transcricaoRepository.findByClienteContainingIgnoreCase(cliente)
                : transcricaoRepository.findByOwnerIdAndClienteContainingIgnoreCase(user.id(), cliente);

        return transcricoes
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deletar(String id) {
        Transcricao transcricao = findVisibleTranscricao(id);
        limparAnaliseEAlertas(transcricao);
        feedbackRepository.deleteByTranscricaoId(transcricao.getId());
        transcricaoRepository.delete(transcricao);
        auditService.record("transcricao.delete", SecurityUtils.currentUser().id(), id, "success");
    }

    public TranscricaoResponse reprocessar(String id) {
        Transcricao transcricao = findVisibleTranscricao(id);
        transcricao.setStatus(StatusTranscricao.PROCESSANDO);
        transcricao.setDataProcessamento(null);
        transcricao = transcricaoRepository.save(transcricao);
        analysisJob.processar(transcricao.getId());
        auditService.record("transcricao.reprocess", SecurityUtils.currentUser().id(), id, "queued");
        return toResponse(transcricao);
    }

    public AnaliseFeedbackResponse registrarFeedback(String id, AnaliseFeedbackRequest request) {
        Transcricao transcricao = findVisibleTranscricao(id);
        AnaliseResultado analise = transcricao.getAnaliseResultado();
        if (analise == null) {
            analise = analiseRepository.findByTranscricaoId(transcricao.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Transcricao ainda nao possui analise"));
        }

        AnaliseFeedback feedback = AnaliseFeedback.builder()
                .ownerId(transcricao.getOwnerId())
                .transcricaoId(transcricao.getId())
                .analiseId(analise.getId())
                .util(request.isUtil())
                .ajuste(request.getAjuste() == null ? "" : request.getAjuste().trim())
                .createdAt(LocalDateTime.now())
                .build();

        AnaliseFeedback saved = feedbackRepository.save(feedback);
        auditService.record("analise.feedback", SecurityUtils.currentUser().id(), id, "success");
        return AnaliseFeedbackResponse.builder()
                .id(saved.getId())
                .transcricaoId(saved.getTranscricaoId())
                .analiseId(saved.getAnaliseId())
                .util(saved.isUtil())
                .ajuste(saved.getAjuste())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public List<AnaliseFeedbackResponse> listarFeedbacks(String id) {
        Transcricao transcricao = findVisibleTranscricao(id);
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<AnaliseFeedback> feedbacks = user.isAdmin()
                ? feedbackRepository.findByTranscricaoIdOrderByCreatedAtDesc(transcricao.getId())
                : feedbackRepository.findByTranscricaoIdAndOwnerIdOrderByCreatedAtDesc(transcricao.getId(), user.id());

        return feedbacks.stream()
                .map(this::toFeedbackResponse)
                .toList();
    }

    private Transcricao findVisibleTranscricao(String id) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return (user.isAdmin()
                ? transcricaoRepository.findById(id)
                : transcricaoRepository.findByIdAndOwnerId(id, user.id()))
                .orElseThrow(() -> new AccessDeniedException("Transcricao nao encontrada"));
    }

    private AnaliseFeedbackResponse toFeedbackResponse(AnaliseFeedback feedback) {
        return AnaliseFeedbackResponse.builder()
                .id(feedback.getId())
                .transcricaoId(feedback.getTranscricaoId())
                .analiseId(feedback.getAnaliseId())
                .util(feedback.isUtil())
                .ajuste(feedback.getAjuste())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    private void limparAnaliseEAlertas(Transcricao transcricao) {
        if (transcricao.getAnaliseResultado() != null && transcricao.getAnaliseResultado().getId() != null) {
            alertaRepository.deleteByAnaliseId(transcricao.getAnaliseResultado().getId());
            analiseRepository.deleteById(transcricao.getAnaliseResultado().getId());
        }
        analiseRepository.findByTranscricaoId(transcricao.getId()).ifPresent(analise -> {
            alertaRepository.deleteByAnaliseId(analise.getId());
            analiseRepository.delete(analise);
        });
    }

    public TranscricaoResponse toResponse(Transcricao t) {
        TranscricaoResponse.TranscricaoResponseBuilder builder = TranscricaoResponse.builder()
                .id(t.getId())
                .textoOriginal(t.getTextoOriginal())
                .participantes(t.getParticipantes())
                .dataReuniao(t.getDataReuniao())
                .duracaoMinutos(t.getDuracaoMinutos())
                .cliente(t.getCliente())
                .vendedor(t.getVendedor())
                .status(t.getStatus())
                .dataCriacao(t.getDataCriacao())
                .dataProcessamento(t.getDataProcessamento());

        // Buscar analise se existir
        if (t.getAnaliseResultado() != null) {
            builder.analise(analiseService.toResponse(t.getAnaliseResultado()));
        } else {
            analiseRepository.findByTranscricaoId(t.getId())
                    .ifPresent(a -> builder.analise(analiseService.toResponse(a)));
        }

        return builder.build();
    }
}
