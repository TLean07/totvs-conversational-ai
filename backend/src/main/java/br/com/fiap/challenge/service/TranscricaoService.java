package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.*;
import br.com.fiap.challenge.enums.StatusTranscricao;
import br.com.fiap.challenge.model.Transcricao;
import br.com.fiap.challenge.repository.TranscricaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TranscricaoService {

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseService analiseService;

    public TranscricaoService(TranscricaoRepository transcricaoRepository, AnaliseService analiseService) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseService = analiseService;
    }

    @Transactional
    public TranscricaoResponse criarEAnalisar(TranscricaoRequest request) {
        Transcricao transcricao = Transcricao.builder()
                .textoOriginal(request.getTextoOriginal())
                .participantes(request.getParticipantes())
                .duracaoMinutos(request.getDuracaoMinutos())
                .cliente(request.getCliente())
                .vendedor(request.getVendedor())
                .status(StatusTranscricao.PENDENTE)
                .build();

        if (request.getDataReuniao() != null && !request.getDataReuniao().isBlank()) {
            transcricao.setDataReuniao(LocalDateTime.parse(request.getDataReuniao(),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        transcricao = transcricaoRepository.save(transcricao);

        transcricao.setStatus(StatusTranscricao.PROCESSANDO);
        transcricao = transcricaoRepository.save(transcricao);

        try {
            analiseService.processarAnalise(transcricao);
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

    public TranscricaoResponse buscarPorId(Long id) {
        Transcricao transcricao = transcricaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transcrição não encontrada: " + id));
        return toResponse(transcricao);
    }

    public List<TranscricaoResponse> buscarPorCliente(String cliente) {
        return transcricaoRepository.findByClienteContainingIgnoreCase(cliente)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deletar(Long id) {
        transcricaoRepository.deleteById(id);
    }

    public TranscricaoResponse toResponse(Transcricao t) {
        TranscricaoResponse response = TranscricaoResponse.builder()
                .id(t.getId())
                .textoOriginal(t.getTextoOriginal())
                .participantes(t.getParticipantes())
                .dataReuniao(t.getDataReuniao())
                .duracaoMinutos(t.getDuracaoMinutos())
                .cliente(t.getCliente())
                .vendedor(t.getVendedor())
                .status(t.getStatus())
                .dataCriacao(t.getDataCriacao())
                .dataProcessamento(t.getDataProcessamento())
                .build();

        if (t.getAnaliseResultado() != null) {
            response.setAnalise(analiseService.toResponse(t.getAnaliseResultado()));
        }

        return response;
    }
}
