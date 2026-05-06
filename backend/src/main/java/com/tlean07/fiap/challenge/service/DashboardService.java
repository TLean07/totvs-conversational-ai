package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.DashboardResponse;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.MetricasAgregadas;
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

        Map<String, Long> sentimentoDistribuicao = new java.util.LinkedHashMap<>();
        Map<String, Long> riscoPorTipo = new java.util.LinkedHashMap<>();
        Map<String, Long> oportunidadePorTipo = new java.util.LinkedHashMap<>();

        long totalOportunidades = 0;
        long totalRiscos = 0;
        long totalPartes = 0;
        long partesAnalisadas = 0;
        long partesNegativas = 0;
        long partesPositivas = 0;
        long partesNeutras = 0;
        long churnsPotenciais = 0;
        long mencoesCriticas = 0;
        long alertasConsolidados = 0;

        for (AnaliseResultado analise : analises) {
            MetricasAgregadas metricas = analise.getMetricasAgregadas();
            if (metricas != null && metricas.getPartesAnalisadas() > 0) {
                increment(sentimentoDistribuicao, "NEGATIVO", metricas.getPartesNegativas());
                increment(sentimentoDistribuicao, "POSITIVO", metricas.getPartesPositivas());
                increment(sentimentoDistribuicao, "NEUTRO", metricas.getPartesNeutras());
                totalOportunidades += metricas.getOportunidadesDetectadas();
                totalRiscos += metricas.getRiscosDetectados();
                totalPartes += metricas.getTotalPartes();
                partesAnalisadas += metricas.getPartesAnalisadas();
                partesNegativas += metricas.getPartesNegativas();
                partesPositivas += metricas.getPartesPositivas();
                partesNeutras += metricas.getPartesNeutras();
                churnsPotenciais += metricas.getChurnsPotenciais();
                mencoesCriticas += metricas.getMencoesCriticas();
                alertasConsolidados += metricas.getAlertasConsolidados();

                safeList(analise.getRiscosConsolidados()).forEach(risco ->
                        increment(riscoPorTipo, normalizeRiskKey(risco.getCategoria()), risco.getOcorrencias()));
                safeList(analise.getOportunidadesConsolidadas()).forEach(oportunidade ->
                        increment(oportunidadePorTipo, normalizeOpportunityKey(oportunidade.getCategoria()), oportunidade.getOcorrencias()));
            } else {
                increment(sentimentoDistribuicao, analise.getSentimentoGeral() == null ? "NEUTRO" : analise.getSentimentoGeral().name(), 1);
                safeList(analise.getRiscos()).forEach(risco -> increment(riscoPorTipo, risco.getTipo().name(), 1));
                safeList(analise.getOportunidades()).forEach(oportunidade -> increment(oportunidadePorTipo, oportunidade.getTipo().name(), 1));
                totalOportunidades += safeList(analise.getOportunidades()).size();
                totalRiscos += safeList(analise.getRiscos()).size();
            }
        }

        long alertasNovos = alertasConsolidados > 0
                ? alertasConsolidados
                : user.isAdmin()
                ? alertaRepository.countByStatus(StatusAlerta.NOVO)
                : alertaRepository.countByOwnerIdAndStatus(user.id(), StatusAlerta.NOVO);

        return DashboardResponse.builder()
                .totalTranscricoes(user.isAdmin()
                        ? transcricaoRepository.count()
                        : transcricaoRepository.countByOwnerId(user.id()))
                .transcricoesProcessadas(user.isAdmin()
                        ? transcricaoRepository.findByStatus(StatusTranscricao.PROCESSADA).size()
                        : transcricaoRepository.findByOwnerIdAndStatus(user.id(), StatusTranscricao.PROCESSADA).size())
                .totalOportunidades(totalOportunidades)
                .totalRiscos(totalRiscos)
                .alertasNovos(alertasNovos)
                .totalPartes(totalPartes)
                .partesAnalisadas(partesAnalisadas)
                .partesNegativas(partesNegativas)
                .partesPositivas(partesPositivas)
                .partesNeutras(partesNeutras)
                .churnsPotenciais(churnsPotenciais)
                .mencoesCriticas(mencoesCriticas)
                .alertasConsolidados(alertasConsolidados)
                .sentimentoDistribuicao(sentimentoDistribuicao)
                .riscoPorTipo(riscoPorTipo)
                .oportunidadePorTipo(oportunidadePorTipo)
                .alertasRecentes(alertaService.listarNovos().stream().limit(10).toList())
                .ultimasTranscricoes(transcricaoService.listarTodas().stream().limit(5).toList())
                .build();
    }

    private void increment(Map<String, Long> map, String key, long value) {
        if (value <= 0) {
            return;
        }
        map.merge(key, value, Long::sum);
    }

    private String normalizeRiskKey(String categoria) {
        if (categoria == null) return "INSATISFACAO";
        String normalized = categoria.toUpperCase();
        if (normalized.contains("CHURN")) return "CHURN";
        if (normalized.contains("CONCORR")) return "CONCORRENCIA";
        if (normalized.contains("CONFIAN")) return "PERDA_CONFIANCA";
        return "INSATISFACAO";
    }

    private String normalizeOpportunityKey(String categoria) {
        if (categoria == null) return "NOVO_PRODUTO";
        String normalized = categoria.toUpperCase();
        if (normalized.contains("UPSELL") || normalized.contains("MODULO") || normalized.contains("LICENCA")) return "UPSELL";
        if (normalized.contains("CROSS")) return "CROSS_SELL";
        return "NOVO_PRODUTO";
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
