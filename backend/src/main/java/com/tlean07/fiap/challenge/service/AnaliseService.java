package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.*;
import com.tlean07.fiap.challenge.enums.*;
import com.tlean07.fiap.challenge.model.*;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnaliseService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseService.class);

    private final LeanIAService leanIAService;
    private final LeanIALearningService leanIALearningService;
    private final AnaliseResultadoRepository analiseRepository;
    private final AlertaService alertaService;

    public AnaliseService(LeanIAService leanIAService,
                          LeanIALearningService leanIALearningService,
                          AnaliseResultadoRepository analiseRepository,
                          AlertaService alertaService) {
        this.leanIAService = leanIAService;
        this.leanIALearningService = leanIALearningService;
        this.analiseRepository = analiseRepository;
        this.alertaService = alertaService;
    }

    public AnaliseResultado processarAnalise(Transcricao transcricao) {
        log.info("Iniciando analise da transcricao ID: {}", transcricao.getId());

        LeanIAContextDTO contexto = leanIALearningService.buildContext(transcricao.getOwnerId());
        AiAnalysisDTO dto = leanIAService.analisarTranscricao(transcricao.getTextoOriginal(), contexto);

        AnaliseResultado analise = AnaliseResultado.builder()
                .ownerId(transcricao.getOwnerId())
                .transcricaoId(transcricao.getId())
                .sentimentoGeral(parseEnum(Sentimento.class, dto.getSentimentoGeral(), Sentimento.NEUTRO))
                .resumo(defaultText(dto.getResumo(), "Analise gerada com dados insuficientes."))
                .personaIdentificada(defaultText(dto.getPersonaIdentificada(), "Nao identificado"))
                .budgetIdentificado(defaultText(dto.getBudgetIdentificado(), "Nao identificado"))
                .scoreConfiancaVendedor(clampScore(dto.getScoreConfiancaVendedor()))
                .scoreSentimento(dto.getScoreSentimento())
                .metricasAgregadas(resolveMetrics(transcricao, dto))
                .resumoExecutivo(defaultText(transcricao.getResumoExecutivo(), dto.getResumo()))
                .riscosConsolidados(resolveConsolidatedRisks(transcricao, dto))
                .oportunidadesConsolidadas(resolveConsolidatedOpportunities(transcricao, dto))
                .alertasConsolidados(resolveConsolidatedAlerts(transcricao, dto))
                .itensCriticos(resolveCriticalItems(transcricao, dto))
                .dataAnalise(LocalDateTime.now())
                .build();

        for (AiAnalysisDTO.ProdutoDTO p : nullSafe(dto.getProdutos())) {
            TipoProduto tipo = parseEnum(TipoProduto.class, p.getTipo(), TipoProduto.ATUAL);
            if (p.getNomeProduto() != null && !p.getNomeProduto().isBlank()) {
                ProdutoIdentificado produto = ProdutoIdentificado.builder()
                        .nomeProduto(p.getNomeProduto())
                        .tipo(tipo)
                        .contexto(defaultText(p.getContexto(), ""))
                        .build();
                analise.getProdutosIdentificados().add(produto);
            }
        }

        for (AiAnalysisDTO.OportunidadeDTO o : nullSafe(dto.getOportunidades())) {
            Oportunidade oportunidade = Oportunidade.builder()
                    .tipo(parseEnum(TipoOportunidade.class, o.getTipo(), TipoOportunidade.NOVO_PRODUTO))
                    .descricao(defaultText(o.getDescricao(), "Oportunidade detectada sem descricao detalhada."))
                    .produtoRelacionado(defaultText(o.getProdutoRelacionado(), "A definir"))
                    .score(clampScore(o.getScore()))
                    .valorEstimado(defaultText(o.getValorEstimado(), "Nao estimado"))
                    .build();
            analise.getOportunidades().add(oportunidade);
        }

        for (AiAnalysisDTO.RiscoDTO r : nullSafe(dto.getRiscos())) {
            Risco risco = Risco.builder()
                    .tipo(parseEnum(TipoRisco.class, r.getTipo(), TipoRisco.INSATISFACAO))
                    .descricao(defaultText(r.getDescricao(), "Risco detectado sem descricao detalhada."))
                    .severidade(parseEnum(Severidade.class, r.getSeveridade(), Severidade.MEDIO))
                    .concorrenteMencionado(r.getConcorrenteMencionado())
                    .build();
            analise.getRiscos().add(risco);
        }

        analise = analiseRepository.save(analise);

        List<Alerta> alertas = alertaService.gerarAlertas(analise, transcricao);
        analise.setAlertas(alertas);
        analise = analiseRepository.save(analise);

        log.info("Analise concluida para transcricao ID: {}", transcricao.getId());
        return analise;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, T fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Valor invalido recebido da IA para {}: {}", enumType.getSimpleName(), value);
            return fallback;
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Double clampScore(Double value) {
        if (value == null) {
            return 0.5;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private MetricasAgregadas resolveMetrics(Transcricao transcricao, AiAnalysisDTO dto) {
        if (transcricao.getMetricasAgregadas() != null) {
            return copyOf(transcricao.getMetricasAgregadas());
        }
        AiAnalysisDTO.MetricasAgregadasDTO metrics = dto.getMetricasAgregadas();
        if (metrics == null) {
            return null;
        }
        return MetricasAgregadas.builder()
                .totalPartes(valueOrZero(metrics.getTotalPartes()))
                .partesAnalisadas(valueOrZero(metrics.getPartesAnalisadas()))
                .partesNegativas(valueOrZero(metrics.getPartesNegativas()))
                .partesNeutras(valueOrZero(metrics.getPartesNeutras()))
                .partesPositivas(valueOrZero(metrics.getPartesPositivas()))
                .riscosDetectados(valueOrZero(metrics.getRiscosDetectados()))
                .oportunidadesDetectadas(valueOrZero(metrics.getOportunidadesDetectadas()))
                .churnsPotenciais(valueOrZero(metrics.getChurnsPotenciais()))
                .mencoesCriticas(valueOrZero(metrics.getMencoesCriticas()))
                .alertasConsolidados(valueOrZero(metrics.getAlertasConsolidados()))
                .chunksProcessados(valueOrZero(metrics.getChunksProcessados()))
                .build();
    }

    private List<RiscoConsolidado> resolveConsolidatedRisks(Transcricao transcricao, AiAnalysisDTO dto) {
        if (transcricao.getRiscosConsolidados() != null && !transcricao.getRiscosConsolidados().isEmpty()) {
            return new ArrayList<>(transcricao.getRiscosConsolidados());
        }
        return nullSafe(dto.getRiscosConsolidados()).stream()
                .map(r -> RiscoConsolidado.builder()
                        .categoria(defaultText(r.getCategoria(), "Risco"))
                        .severidade(parseEnum(Severidade.class, r.getSeveridade(), Severidade.MEDIO))
                        .ocorrencias(valueOrZero(r.getOcorrencias()))
                        .evidencias(nullSafe(r.getEvidencias()))
                        .recomendacao(defaultText(r.getRecomendacao(), "Acompanhar recorrencia e definir plano de acao."))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<OportunidadeConsolidada> resolveConsolidatedOpportunities(Transcricao transcricao, AiAnalysisDTO dto) {
        if (transcricao.getOportunidadesConsolidadas() != null && !transcricao.getOportunidadesConsolidadas().isEmpty()) {
            return new ArrayList<>(transcricao.getOportunidadesConsolidadas());
        }
        return nullSafe(dto.getOportunidadesConsolidadas()).stream()
                .map(o -> OportunidadeConsolidada.builder()
                        .categoria(defaultText(o.getCategoria(), "Oportunidade"))
                        .score(clampScore(o.getScore()))
                        .ocorrencias(valueOrZero(o.getOcorrencias()))
                        .evidencias(nullSafe(o.getEvidencias()))
                        .recomendacao(defaultText(o.getRecomendacao(), "Validar oportunidade com o time comercial."))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<AlertaConsolidado> resolveConsolidatedAlerts(Transcricao transcricao, AiAnalysisDTO dto) {
        if (transcricao.getAlertasConsolidados() != null && !transcricao.getAlertasConsolidados().isEmpty()) {
            return new ArrayList<>(transcricao.getAlertasConsolidados());
        }
        return nullSafe(dto.getAlertasConsolidados()).stream()
                .map(a -> AlertaConsolidado.builder()
                        .titulo(defaultText(a.getTitulo(), "Alerta consolidado"))
                        .categoria(defaultText(a.getCategoria(), "Geral"))
                        .severidade(parseEnum(Severidade.class, a.getSeveridade(), Severidade.MEDIO))
                        .ocorrencias(valueOrZero(a.getOcorrencias()))
                        .motivo(defaultText(a.getMotivo(), "Padrao recorrente detectado na transcricao."))
                        .recomendacao(defaultText(a.getRecomendacao(), "Revisar evidencias e definir responsavel."))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<ItemCritico> resolveCriticalItems(Transcricao transcricao, AiAnalysisDTO dto) {
        if (transcricao.getItensCriticos() != null && !transcricao.getItensCriticos().isEmpty()) {
            return new ArrayList<>(transcricao.getItensCriticos());
        }
        return nullSafe(dto.getItensCriticos()).stream()
                .map(i -> ItemCritico.builder()
                        .tipo(defaultText(i.getTipo(), "risco"))
                        .categoria(defaultText(i.getCategoria(), "Critico"))
                        .trecho(defaultText(i.getTrecho(), "Trecho nao informado"))
                        .severidade(parseEnum(Severidade.class, i.getSeveridade(), Severidade.MEDIO))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    /**
     * As colecoes e o componente embutido vindos de uma Transcricao carregada pelo Hibernate
     * nao podem ser reaproveitados por referencia em outra entidade (HHH "shared references").
     */
    private MetricasAgregadas copyOf(MetricasAgregadas m) {
        return MetricasAgregadas.builder()
                .totalPartes(m.getTotalPartes())
                .partesAnalisadas(m.getPartesAnalisadas())
                .partesNegativas(m.getPartesNegativas())
                .partesNeutras(m.getPartesNeutras())
                .partesPositivas(m.getPartesPositivas())
                .riscosDetectados(m.getRiscosDetectados())
                .oportunidadesDetectadas(m.getOportunidadesDetectadas())
                .churnsPotenciais(m.getChurnsPotenciais())
                .mencoesCriticas(m.getMencoesCriticas())
                .alertasConsolidados(m.getAlertasConsolidados())
                .chunksProcessados(m.getChunksProcessados())
                .build();
    }

    public AnaliseResponse toResponse(AnaliseResultado a) {
        return AnaliseResponse.builder()
                .id(a.getId())
                .sentimentoGeral(a.getSentimentoGeral())
                .resumo(a.getResumo())
                .personaIdentificada(a.getPersonaIdentificada())
                .budgetIdentificado(a.getBudgetIdentificado())
                .scoreConfiancaVendedor(a.getScoreConfiancaVendedor())
                .scoreSentimento(a.getScoreSentimento())
                .metricasAgregadas(a.getMetricasAgregadas())
                .resumoExecutivo(a.getResumoExecutivo())
                .dataAnalise(a.getDataAnalise())
                .produtos(nullSafe(a.getProdutosIdentificados()).stream()
                        .map(p -> ProdutoResponse.builder()
                                .nomeProduto(p.getNomeProduto())
                                .tipo(p.getTipo())
                                .contexto(p.getContexto())
                                .build())
                        .toList())
                .oportunidades(nullSafe(a.getOportunidades()).stream()
                        .map(o -> OportunidadeResponse.builder()
                                .tipo(o.getTipo())
                                .descricao(o.getDescricao())
                                .produtoRelacionado(o.getProdutoRelacionado())
                                .score(o.getScore())
                                .valorEstimado(o.getValorEstimado())
                                .build())
                        .toList())
                .riscos(nullSafe(a.getRiscos()).stream()
                        .map(r -> RiscoResponse.builder()
                                .tipo(r.getTipo())
                                .descricao(r.getDescricao())
                                .severidade(r.getSeveridade())
                                .concorrenteMencionado(r.getConcorrenteMencionado())
                                .build())
                        .toList())
                .alertas(nullSafe(a.getAlertas()).stream()
                        .map(al -> AlertaResponse.builder()
                                .id(al.getId())
                                .tipo(al.getTipo())
                                .titulo(al.getTitulo())
                                .categoria(al.getCategoria())
                                .mensagem(al.getMensagem())
                                .motivo(al.getMotivo())
                                .recomendacao(al.getRecomendacao())
                                .ocorrencias(al.getOcorrencias())
                                .prioridade(al.getPrioridade())
                                .status(al.getStatus())
                                .dataCriacao(al.getDataCriacao())
                                .build())
                        .toList())
                .riscosConsolidados(nullSafe(a.getRiscosConsolidados()))
                .oportunidadesConsolidadas(nullSafe(a.getOportunidadesConsolidadas()))
                .alertasConsolidados(nullSafe(a.getAlertasConsolidados()))
                .itensCriticos(nullSafe(a.getItensCriticos()))
                .build();
    }
}
