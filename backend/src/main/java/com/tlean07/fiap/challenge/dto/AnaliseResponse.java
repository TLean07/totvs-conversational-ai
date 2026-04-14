package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.Sentimento;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseResponse {
    private String id;
    private Sentimento sentimentoGeral;
    private String resumo;
    private String personaIdentificada;
    private String budgetIdentificado;
    private Double scoreConfiancaVendedor;
    private LocalDateTime dataAnalise;
    private List<ProdutoResponse> produtos;
    private List<OportunidadeResponse> oportunidades;
    private List<RiscoResponse> riscos;
    private List<AlertaResponse> alertas;
}
