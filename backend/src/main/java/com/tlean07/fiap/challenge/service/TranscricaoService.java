package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.*;
import com.tlean07.fiap.challenge.dto.TranscricaoRequest;
import com.tlean07.fiap.challenge.dto.TranscricaoResponse;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TranscricaoService {

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final AnaliseService analiseService;

    public TranscricaoService(TranscricaoRepository transcricaoRepository,
                              AnaliseResultadoRepository analiseRepository,
                              AnaliseService analiseService) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
        this.analiseService = analiseService;
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

        try {
            AnaliseResultado analise = analiseService.processarAnalise(transcricao);
            transcricao.setAnaliseResultado(analise);
            transcricao.setStatus(StatusTranscricao.PROCESSADA);
            transcricao.setDataProcessamento(LocalDateTime.now());
        } catch (Exception e) {
            transcricao.setStatus(StatusTranscricao.ERRO);
        }

        transcricao = transcricaoRepository.save(transcricao);
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
        transcricaoRepository.delete(transcricao);
    }

    private Transcricao findVisibleTranscricao(String id) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return (user.isAdmin()
                ? transcricaoRepository.findById(id)
                : transcricaoRepository.findByIdAndOwnerId(id, user.id()))
                .orElseThrow(() -> new AccessDeniedException("Transcricao nao encontrada"));
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
            AuthenticatedUser user = SecurityUtils.currentUser();
            (user.isAdmin()
                    ? analiseRepository.findByTranscricaoId(t.getId())
                    : analiseRepository.findByTranscricaoIdAndOwnerId(t.getId(), user.id()))
                    .ifPresent(a -> builder.analise(analiseService.toResponse(a)));
        }

        return builder.build();
    }
}
