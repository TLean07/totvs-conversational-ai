package com.tlean07.fiap.challenge.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private long totalTranscricoes;
    private long transcricoesProcessadas;
    private long totalOportunidades;
    private long totalRiscos;
    private long alertasNovos;
    private Map<String, Long> sentimentoDistribuicao;
    private Map<String, Long> riscoPorTipo;
    private Map<String, Long> oportunidadePorTipo;
    private List<AlertaResponse> alertasRecentes;
    private List<TranscricaoResponse> ultimasTranscricoes;
}
