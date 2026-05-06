package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import com.tlean07.fiap.challenge.model.AlertaConsolidado;
import com.tlean07.fiap.challenge.model.ItemCritico;
import com.tlean07.fiap.challenge.model.MetricasAgregadas;
import com.tlean07.fiap.challenge.model.OportunidadeConsolidada;
import com.tlean07.fiap.challenge.model.RiscoConsolidado;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscricaoResponse {
    private String id;
    private String textoOriginal;
    private String participantes;
    private LocalDateTime dataReuniao;
    private Integer duracaoMinutos;
    private String cliente;
    private String vendedor;
    private StatusTranscricao status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataProcessamento;
    private MetricasAgregadas metricasAgregadas;
    private String resumoExecutivo;
    private List<RiscoConsolidado> riscosConsolidados;
    private List<OportunidadeConsolidada> oportunidadesConsolidadas;
    private List<AlertaConsolidado> alertasConsolidados;
    private List<ItemCritico> itensCriticos;
    private AnaliseResponse analise;
}
