package com.tlean07.fiap.challenge.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisDTO {
    private String sentimentoGeral;
    private String resumo;
    private String personaIdentificada;
    private String budgetIdentificado;
    private Double scoreConfiancaVendedor;
    private Double scoreSentimento;
    private MetricasAgregadasDTO metricasAgregadas;
    private List<ProdutoDTO> produtos;
    private List<OportunidadeDTO> oportunidades;
    private List<RiscoDTO> riscos;
    private List<RiscoConsolidadoDTO> riscosConsolidados;
    private List<OportunidadeConsolidadaDTO> oportunidadesConsolidadas;
    private List<AlertaConsolidadoDTO> alertasConsolidados;
    private List<ItemCriticoDTO> itensCriticos;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProdutoDTO {
        private String nomeProduto;
        private String tipo;
        private String contexto;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OportunidadeDTO {
        private String tipo;
        private String descricao;
        private String produtoRelacionado;
        private Double score;
        private String valorEstimado;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiscoDTO {
        private String tipo;
        private String descricao;
        private String severidade;
        private String concorrenteMencionado;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricasAgregadasDTO {
        private Integer totalPartes;
        private Integer partesAnalisadas;
        private Integer partesNegativas;
        private Integer partesNeutras;
        private Integer partesPositivas;
        private Integer riscosDetectados;
        private Integer oportunidadesDetectadas;
        private Integer churnsPotenciais;
        private Integer mencoesCriticas;
        private Integer alertasConsolidados;
        private Integer chunksProcessados;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiscoConsolidadoDTO {
        private String categoria;
        private String severidade;
        private Integer ocorrencias;
        @Builder.Default
        private List<String> evidencias = new ArrayList<>();
        private String recomendacao;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OportunidadeConsolidadaDTO {
        private String categoria;
        private Double score;
        private Integer ocorrencias;
        @Builder.Default
        private List<String> evidencias = new ArrayList<>();
        private String recomendacao;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertaConsolidadoDTO {
        private String titulo;
        private String categoria;
        private String severidade;
        private Integer ocorrencias;
        private String motivo;
        private String recomendacao;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemCriticoDTO {
        private String tipo;
        private String categoria;
        private String trecho;
        private String severidade;
    }
}
