package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.LocalDateTime;

@Document(collection = "transcricoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transcricao {

    @Id
    private String id;

    @Indexed
    private String ownerId;
    private String textoOriginal;
    private String participantes;
    private LocalDateTime dataReuniao;
    private Integer duracaoMinutos;
    @Indexed
    private String cliente;
    private String vendedor;
    @Indexed
    private StatusTranscricao status;
    @Indexed
    private LocalDateTime dataCriacao;
    private LocalDateTime dataProcessamento;
    private MetricasAgregadas metricasAgregadas;
    private String resumoExecutivo;

    @Builder.Default
    private java.util.List<RiscoConsolidado> riscosConsolidados = new java.util.ArrayList<>();

    @Builder.Default
    private java.util.List<OportunidadeConsolidada> oportunidadesConsolidadas = new java.util.ArrayList<>();

    @Builder.Default
    private java.util.List<AlertaConsolidado> alertasConsolidados = new java.util.ArrayList<>();

    @Builder.Default
    private java.util.List<ItemCritico> itensCriticos = new java.util.ArrayList<>();

    @DBRef
    private AnaliseResultado analiseResultado;

    public void prePersist() {
        dataCriacao = LocalDateTime.now();
        if (status == null) {
            status = StatusTranscricao.PENDENTE;
        }
    }
}
