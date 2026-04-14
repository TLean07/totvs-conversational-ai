package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.*;
import com.tlean07.fiap.challenge.dto.TranscricaoRequest;
import com.tlean07.fiap.challenge.dto.TranscricaoResponse;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
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
        Transcricao transcricao = Transcricao.builder()
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
        return transcricaoRepository.findAllByOrderByDataCriacaoDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TranscricaoResponse buscarPorId(String id) {
        Transcricao transcricao = transcricaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transcricao nao encontrada: " + id));
        return toResponse(transcricao);
    }

    public List<TranscricaoResponse> buscarPorCliente(String cliente) {
        return transcricaoRepository.findByClienteContainingIgnoreCase(cliente)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deletar(String id) {
        transcricaoRepository.deleteById(id);
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
