package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.*;
import com.tlean07.fiap.challenge.enums.*;
import com.tlean07.fiap.challenge.model.*;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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

    public AnaliseResponse toResponse(AnaliseResultado a) {
        return AnaliseResponse.builder()
                .id(a.getId())
                .sentimentoGeral(a.getSentimentoGeral())
                .resumo(a.getResumo())
                .personaIdentificada(a.getPersonaIdentificada())
                .budgetIdentificado(a.getBudgetIdentificado())
                .scoreConfiancaVendedor(a.getScoreConfiancaVendedor())
                .dataAnalise(a.getDataAnalise())
                .produtos(a.getProdutosIdentificados().stream()
                        .map(p -> ProdutoResponse.builder()
                                .nomeProduto(p.getNomeProduto())
                                .tipo(p.getTipo())
                                .contexto(p.getContexto())
                                .build())
                        .toList())
                .oportunidades(a.getOportunidades().stream()
                        .map(o -> OportunidadeResponse.builder()
                                .tipo(o.getTipo())
                                .descricao(o.getDescricao())
                                .produtoRelacionado(o.getProdutoRelacionado())
                                .score(o.getScore())
                                .valorEstimado(o.getValorEstimado())
                                .build())
                        .toList())
                .riscos(a.getRiscos().stream()
                        .map(r -> RiscoResponse.builder()
                                .tipo(r.getTipo())
                                .descricao(r.getDescricao())
                                .severidade(r.getSeveridade())
                                .concorrenteMencionado(r.getConcorrenteMencionado())
                                .build())
                        .toList())
                .alertas(a.getAlertas().stream()
                        .map(al -> AlertaResponse.builder()
                                .id(al.getId())
                                .tipo(al.getTipo())
                                .mensagem(al.getMensagem())
                                .prioridade(al.getPrioridade())
                                .status(al.getStatus())
                                .dataCriacao(al.getDataCriacao())
                                .build())
                        .toList())
                .build();
    }
}
