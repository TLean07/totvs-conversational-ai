package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.DashboardResponse;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.repository.*;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final AlertaRepository alertaRepository;
    private final AlertaService alertaService;
    private final TranscricaoService transcricaoService;

    public DashboardService(TranscricaoRepository transcricaoRepository,
                            AnaliseResultadoRepository analiseRepository,
                            AlertaRepository alertaRepository,
                            AlertaService alertaService,
                            TranscricaoService transcricaoService) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
        this.alertaRepository = alertaRepository;
        this.alertaService = alertaService;
        this.transcricaoService = transcricaoService;
    }

    public DashboardResponse getDashboard() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<AnaliseResultado> analises = user.isAdmin()
                ? analiseRepository.findAll()
                : analiseRepository.findAllByOwnerIdOrderByDataAnaliseDesc(user.id());

        Map<String, Long> sentimentoDistribuicao = analises.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getSentimentoGeral().name(),
                        Collectors.counting()
                ));

        Map<String, Long> riscoPorTipo = analises.stream()
                .flatMap(a -> a.getRiscos().stream())
                .collect(Collectors.groupingBy(
                        r -> r.getTipo().name(),
                        Collectors.counting()
                ));

        Map<String, Long> oportunidadePorTipo = analises.stream()
                .flatMap(a -> a.getOportunidades().stream())
                .collect(Collectors.groupingBy(
                        o -> o.getTipo().name(),
                        Collectors.counting()
                ));

        long totalOportunidades = analises.stream()
                .mapToLong(a -> a.getOportunidades().size())
                .sum();

        long totalRiscos = analises.stream()
                .mapToLong(a -> a.getRiscos().size())
                .sum();

        return DashboardResponse.builder()
                .totalTranscricoes(user.isAdmin()
                        ? transcricaoRepository.count()
                        : transcricaoRepository.countByOwnerId(user.id()))
                .transcricoesProcessadas(user.isAdmin()
                        ? transcricaoRepository.findByStatus(StatusTranscricao.PROCESSADA).size()
                        : transcricaoRepository.findByOwnerIdAndStatus(user.id(), StatusTranscricao.PROCESSADA).size())
                .totalOportunidades(totalOportunidades)
                .totalRiscos(totalRiscos)
                .alertasNovos(user.isAdmin()
                        ? alertaRepository.countByStatus(StatusAlerta.NOVO)
                        : alertaRepository.countByOwnerIdAndStatus(user.id(), StatusAlerta.NOVO))
                .sentimentoDistribuicao(sentimentoDistribuicao)
                .riscoPorTipo(riscoPorTipo)
                .oportunidadePorTipo(oportunidadePorTipo)
                .alertasRecentes(alertaService.listarNovos().stream().limit(10).toList())
                .ultimasTranscricoes(transcricaoService.listarTodas().stream().limit(5).toList())
                .build();
    }
}
