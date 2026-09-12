package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.Sentimento;
import com.tlean07.fiap.challenge.model.AlertaConsolidado;
import com.tlean07.fiap.challenge.model.ItemCritico;
import com.tlean07.fiap.challenge.model.MetricasAgregadas;
import com.tlean07.fiap.challenge.model.OportunidadeConsolidada;
import com.tlean07.fiap.challenge.model.RiscoConsolidado;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private Double scoreSentimento;
    private MetricasAgregadas metricasAgregadas;
    private String resumoExecutivo;
    private LocalDateTime dataAnalise;
    private List<ProdutoResponse> produtos;
    private List<OportunidadeResponse> oportunidades;
    private List<RiscoResponse> riscos;
    private List<AlertaResponse> alertas;
    private List<RiscoConsolidado> riscosConsolidados;
    private List<OportunidadeConsolidada> oportunidadesConsolidadas;
    private List<AlertaConsolidado> alertasConsolidados;
    private List<ItemCritico> itensCriticos;
    private String proximaAcao;
    private String sentimentoDetalhe;
    private Map<String, Object> insightsAdicionais;
    private Map<String, Object> indicadoresQualidade;
}
