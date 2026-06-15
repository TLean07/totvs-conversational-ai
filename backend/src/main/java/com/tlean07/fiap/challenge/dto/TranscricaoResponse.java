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
    private String origemTranscricao;
    private String meetingId;
    private String meetingDate;
    private String format;
    private String meetingStatus;
    private String duration;
    private String codt;
    private Boolean external;
    private String createdAtSource;
    private String transcriptionText;
    private String sourceFileName;
    private String importBatchId;
    private LocalDateTime importedAt;
    private boolean missingMeetingId;
    private String audioFileName;
    private String transcriptionModel;
    private String transcriptionLanguage;
    private Double audioDurationSeconds;
    private Integer transcriptionProcessingTimeMs;
    private List<String> transcriptionWarnings;
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
