package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.config.CacheConfig.CacheNames;
import com.tlean07.fiap.challenge.dto.DashboardResponse;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.repository.AlertaRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository.ContagemRow;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository.DashboardRow;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Numeros do dashboard, todos vindos de agregacoes no banco: sentimento e metricas por analise
 * (uma linha por analise, sem CLOB) e contagens de riscos/oportunidades por tipo (group by).
 * Na CPU do plano free, hidratar as listas de detalhe de centenas de analises levava minutos.
 */
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

    @Cacheable(cacheNames = CacheNames.DASHBOARD, key = "@cacheKeys.user()")
    public DashboardResponse getDashboard() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        boolean admin = user.isAdmin();
        String owner = user.id();

        List<DashboardRow> analises = admin
                ? analiseRepository.findDashboardRows()
                : analiseRepository.findDashboardRowsByOwnerId(owner);

        Map<String, Long> sentimentoDistribuicao = new LinkedHashMap<>();
        long totalPartes = 0, partesAnalisadas = 0, partesNegativas = 0, partesPositivas = 0, partesNeutras = 0;
        long churnsPotenciais = 0, mencoesCriticas = 0, alertasConsolidados = 0;
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

        // Riscos: listas consolidadas quando existem; riscos brutos so das analises sem consolidacao
        Map<String, Long> riscoPorTipo = new LinkedHashMap<>();
        somar(riscoPorTipo, admin ? analiseRepository.contarRiscosConsolidadosPorCategoria()
                : analiseRepository.contarRiscosConsolidadosPorCategoria(owner), this::normalizeRiskKey);
        somar(riscoPorTipo, admin ? analiseRepository.contarRiscosSemConsolidadoPorTipo()
                : analiseRepository.contarRiscosSemConsolidadoPorTipo(owner), Function.identity());
        long totalRiscos = riscoPorTipo.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Long> oportunidadePorTipo = new LinkedHashMap<>();
        somar(oportunidadePorTipo, admin ? analiseRepository.contarOportunidadesConsolidadasPorCategoria()
                : analiseRepository.contarOportunidadesConsolidadasPorCategoria(owner), this::normalizeOpportunityKey);
        somar(oportunidadePorTipo, admin ? analiseRepository.contarOportunidadesSemConsolidadoPorTipo()
                : analiseRepository.contarOportunidadesSemConsolidadoPorTipo(owner), Function.identity());
        long totalOportunidades = oportunidadePorTipo.values().stream().mapToLong(Long::longValue).sum();

        long alertasNovos = alertasConsolidados > 0
                ? alertasConsolidados
                : admin
                ? alertaRepository.countByStatus(StatusAlerta.NOVO)
                : alertaRepository.countByOwnerIdAndStatus(owner, StatusAlerta.NOVO);

        return DashboardResponse.builder()
                .totalTranscricoes(admin ? transcricaoRepository.count() : transcricaoRepository.countByOwnerId(owner))
                .transcricoesProcessadas(admin
                        ? transcricaoRepository.countByStatus(StatusTranscricao.PROCESSADA)
                        : transcricaoRepository.countByOwnerIdAndStatus(owner, StatusTranscricao.PROCESSADA))
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

    private void somar(Map<String, Long> destino, List<ContagemRow> linhas, Function<String, String> chave) {
        for (ContagemRow linha : linhas) {
            increment(destino, chave.apply(linha.getChave()), linha.getTotal() == null ? 0 : linha.getTotal());
        }
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
}
