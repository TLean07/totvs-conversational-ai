package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.DashboardResponse;
import br.com.fiap.challenge.enums.StatusAlerta;
import br.com.fiap.challenge.enums.StatusTranscricao;
import br.com.fiap.challenge.repository.*;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final RiscoRepository riscoRepository;
    private final AlertaRepository alertaRepository;
    private final AlertaService alertaService;
    private final TranscricaoService transcricaoService;

    public DashboardService(TranscricaoRepository transcricaoRepository,
                            AnaliseResultadoRepository analiseRepository,
                            OportunidadeRepository oportunidadeRepository,
                            RiscoRepository riscoRepository,
                            AlertaRepository alertaRepository,
                            AlertaService alertaService,
                            TranscricaoService transcricaoService) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
        this.oportunidadeRepository = oportunidadeRepository;
        this.riscoRepository = riscoRepository;
        this.alertaRepository = alertaRepository;
        this.alertaService = alertaService;
        this.transcricaoService = transcricaoService;
    }

    public DashboardResponse getDashboard() {
        Map<String, Long> sentimentoDistribuicao = analiseRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        a -> a.getSentimentoGeral().name(),
                        Collectors.counting()
                ));

        Map<String, Long> riscoPorTipo = riscoRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTipo().name(),
                        Collectors.counting()
                ));

        Map<String, Long> oportunidadePorTipo = oportunidadeRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        o -> o.getTipo().name(),
                        Collectors.counting()
                ));

        return DashboardResponse.builder()
                .totalTranscricoes(transcricaoRepository.count())
                .transcricoesProcessadas(transcricaoRepository.findByStatus(StatusTranscricao.PROCESSADA).size())
                .totalOportunidades(oportunidadeRepository.count())
                .totalRiscos(riscoRepository.count())
                .alertasNovos(alertaRepository.countByStatus(StatusAlerta.NOVO))
                .sentimentoDistribuicao(sentimentoDistribuicao)
                .riscoPorTipo(riscoPorTipo)
                .oportunidadePorTipo(oportunidadePorTipo)
                .alertasRecentes(alertaService.listarNovos().stream().limit(10).toList())
                .ultimasTranscricoes(transcricaoService.listarTodas().stream().limit(5).toList())
                .build();
    }
}
