package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.config.CacheConfig.CacheNames;
import com.tlean07.fiap.challenge.dto.DashboardResponse;
import com.tlean07.fiap.challenge.dto.TranscricaoInsightsResponse;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository.DashboardRow;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Numeros do dashboard. Le so projecoes (sentimento e metricas por analise; listas por tabela filha):
 * carregar as entidades completas de centenas de analises levava minutos no plano free.
 */
@Service
public class DashboardService {

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;
    private final AlertaRepository alertaRepository;
    private final AlertaService alertaService;
    private final TranscricaoService transcricaoService;
    private final TranscricaoInsightsService transcricaoInsightsService;

    public DashboardService(TranscricaoRepository transcricaoRepository,
                            AnaliseResultadoRepository analiseRepository,
                            AlertaRepository alertaRepository,
                            AlertaService alertaService,
                            TranscricaoService transcricaoService,
                            TranscricaoInsightsService transcricaoInsightsService) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
        this.alertaRepository = alertaRepository;
        this.alertaService = alertaService;
        this.transcricaoService = transcricaoService;
        this.transcricaoInsightsService = transcricaoInsightsService;
    }

    @Cacheable(cacheNames = CacheNames.DASHBOARD, key = "@cacheKeys.user()")
    public DashboardResponse getDashboard() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        List<DashboardRow> analises = user.isAdmin()
                ? analiseRepository.findDashboardRows()
                : analiseRepository.findDashboardRowsByOwnerId(user.id());
        List<TranscricaoInsightsResponse> insights = transcricaoInsightsService.listarTodos();

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

        for (DashboardRow analise : analises) {
            increment(sentimentoDistribuicao, analise.getSentimentoGeral() == null ? "NEUTRO" : analise.getSentimentoGeral().name(), 1);
            if (valor(analise.getPartesAnalisadas()) > 0) {
                totalPartes += valor(analise.getTotalPartes());
                partesAnalisadas += valor(analise.getPartesAnalisadas());
                partesNegativas += valor(analise.getPartesNegativas());
                partesPositivas += valor(analise.getPartesPositivas());
                partesNeutras += valor(analise.getPartesNeutras());
                churnsPotenciais += valor(analise.getChurnsPotenciais());
                mencoesCriticas += valor(analise.getMencoesCriticas());
                alertasConsolidados += valor(analise.getAlertasConsolidados());
            }
        }

        for (TranscricaoInsightsResponse insight : insights) {
            if (!safeList(insight.getRiscosConsolidados()).isEmpty()) {
                totalRiscos += insight.getRiscosConsolidados().size();
                insight.getRiscosConsolidados().forEach(risco ->
                        increment(riscoPorTipo, normalizeRiskKey(risco.getCategoria()), 1));
            } else {
                totalRiscos += safeList(insight.getRiscos()).size();
                safeList(insight.getRiscos()).forEach(risco ->
                        increment(riscoPorTipo, risco.getTipo().name(), 1));
            }

            if (!safeList(insight.getOportunidadesConsolidadas()).isEmpty()) {
                totalOportunidades += insight.getOportunidadesConsolidadas().size();
                insight.getOportunidadesConsolidadas().forEach(oportunidade ->
                        increment(oportunidadePorTipo, normalizeOpportunityKey(oportunidade.getCategoria()), 1));
            } else {
                totalOportunidades += safeList(insight.getOportunidades()).size();
                safeList(insight.getOportunidades()).forEach(oportunidade ->
                        increment(oportunidadePorTipo, oportunidade.getTipo().name(), 1));
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
                        ? transcricaoRepository.countByStatus(StatusTranscricao.PROCESSADA)
                        : transcricaoRepository.countByOwnerIdAndStatus(user.id(), StatusTranscricao.PROCESSADA))
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
                .alertasRecentes(alertaService.listarNovosDashboard())
                .ultimasTranscricoes(transcricaoService.listarRecentesDashboard())
                .build();
    }

    private static long valor(Integer v) {
        return v == null ? 0 : v;
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
