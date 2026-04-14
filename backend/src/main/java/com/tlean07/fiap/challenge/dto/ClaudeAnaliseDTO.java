package com.tlean07.fiap.challenge.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaudeAnaliseDTO {
    private String sentimentoGeral;
    private String resumo;
    private String personaIdentificada;
    private String budgetIdentificado;
    private Double scoreConfiancaVendedor;
    private List<ProdutoDTO> produtos;
    private List<OportunidadeDTO> oportunidades;
    private List<RiscoDTO> riscos;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProdutoDTO {
        private String nomeProduto;
        private String tipo;
        private String contexto;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
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
    public static class RiscoDTO {
        private String tipo;
        private String descricao;
        private String severidade;
        private String concorrenteMencionado;
    }
}
