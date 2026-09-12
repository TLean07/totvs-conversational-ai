package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transcricao de reuniao (tabela TRANSCRICOES).
 * Textos longos ficam em CLOB; listas consolidadas em tabelas filhas TRANSCRICAO_*.
 */
@Entity
@Table(name = "TRANSCRICOES",
        uniqueConstraints = @UniqueConstraint(name = "UK_TRANSCRICOES_MEETING_ID", columnNames = "MEETING_ID"),
        indexes = {
                @Index(name = "IDX_TRANSC_OWNER_CRIACAO", columnList = "OWNER_ID, DATA_CRIACAO"),
                @Index(name = "IDX_TRANSC_OWNER_STATUS", columnList = "OWNER_ID, STATUS"),
                @Index(name = "IDX_TRANSC_CLIENTE", columnList = "CLIENTE"),
                @Index(name = "IDX_TRANSC_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_TRANSC_DATA_CRIACAO", columnList = "DATA_CRIACAO"),
                @Index(name = "IDX_TRANSC_IMPORT_BATCH", columnList = "IMPORT_BATCH_ID")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transcricao {

    @Id
    @Column(name = "ID", length = 36, nullable = false)
    private String id;

    @Column(name = "OWNER_ID", length = 36)
    private String ownerId;

    @Lob
    @Column(name = "TEXTO_ORIGINAL")
    private String textoOriginal;

    @Column(name = "PARTICIPANTES", length = 1000)
    private String participantes;

    @Column(name = "DATA_REUNIAO")
    private LocalDateTime dataReuniao;

    @Column(name = "DURACAO_MINUTOS")
    private Integer duracaoMinutos;

    @Column(name = "CLIENTE", length = 200)
    private String cliente;

    @Column(name = "VENDEDOR", length = 200)
    private String vendedor;

    @Column(name = "ORIGEM_TRANSCRICAO", length = 30)
    private String origemTranscricao;

    /** ID_MEETING do JSON TOTVS. Unico quando informado: o Oracle ignora NULL em indice unico. */
    @Column(name = "MEETING_ID", length = 100)
    private String meetingId;

    @Column(name = "MEETING_DATE", length = 50)
    private String meetingDate;

    @Column(name = "FORMATO", length = 50)
    private String format;

    @Column(name = "MEETING_STATUS", length = 50)
    private String meetingStatus;

    @Column(name = "MEETING_DURATION", length = 30)
    private String duration;

    @Column(name = "CODT", length = 50)
    private String codt;

    @Column(name = "FLG_EXTERNO")
    private Boolean external;

    @Column(name = "CREATED_AT_SOURCE", length = 50)
    private String createdAtSource;

    @Lob
    @Column(name = "TRANSCRIPTION_TEXT")
    private String transcriptionText;

    @Column(name = "SOURCE_FILE_NAME", length = 200)
    private String sourceFileName;

    @Column(name = "IMPORT_BATCH_ID", length = 36)
    private String importBatchId;

    @Column(name = "IMPORTED_AT")
    private LocalDateTime importedAt;

    @Column(name = "MISSING_MEETING_ID", nullable = false)
    private boolean missingMeetingId;

    @Column(name = "AUDIO_FILE_NAME", length = 200)
    private String audioFileName;

    @Column(name = "TRANSCRIPTION_MODEL", length = 100)
    private String transcriptionModel;

    @Column(name = "TRANSCRIPTION_LANGUAGE", length = 10)
    private String transcriptionLanguage;

    @Column(name = "AUDIO_DURATION_SECONDS")
    private Double audioDurationSeconds;

    @Column(name = "TRANSCRIPTION_PROCESSING_MS")
    private Integer transcriptionProcessingTimeMs;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "TRANSCRICAO_WARNINGS",
            joinColumns = @JoinColumn(name = "TRANSCRICAO_ID", foreignKey = @ForeignKey(name = "FK_TRANSCRICAO_WARNINGS")))
    @OrderColumn(name = "POSICAO")
    @Column(name = "WARNING", length = 200)
    private List<String> transcriptionWarnings = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private StatusTranscricao status;

    @Column(name = "DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name = "DATA_PROCESSAMENTO")
    private LocalDateTime dataProcessamento;

    @Embedded
    private MetricasAgregadas metricasAgregadas;

    @Lob
    @Column(name = "RESUMO_EXECUTIVO")
    private String resumoExecutivo;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "TRANSCRICAO_RISCOS_CONS",
            joinColumns = @JoinColumn(name = "TRANSCRICAO_ID", foreignKey = @ForeignKey(name = "FK_TRANSC_RISCOS_CONS")))
    @OrderColumn(name = "POSICAO")
    private List<RiscoConsolidado> riscosConsolidados = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "TRANSCRICAO_OPORT_CONS",
            joinColumns = @JoinColumn(name = "TRANSCRICAO_ID", foreignKey = @ForeignKey(name = "FK_TRANSC_OPORT_CONS")))
    @OrderColumn(name = "POSICAO")
    private List<OportunidadeConsolidada> oportunidadesConsolidadas = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "TRANSCRICAO_ALERTAS_CONS",
            joinColumns = @JoinColumn(name = "TRANSCRICAO_ID", foreignKey = @ForeignKey(name = "FK_TRANSC_ALERTAS_CONS")))
    @OrderColumn(name = "POSICAO")
    private List<AlertaConsolidado> alertasConsolidados = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @CollectionTable(name = "TRANSCRICAO_ITENS_CRITICOS",
            joinColumns = @JoinColumn(name = "TRANSCRICAO_ID", foreignKey = @ForeignKey(name = "FK_TRANSC_ITENS_CRITICOS")))
    @OrderColumn(name = "POSICAO")
    private List<ItemCritico> itensCriticos = new ArrayList<>();

    /** Analise atual da transcricao (antigo DBRef). No script Oracle a FK e ON DELETE SET NULL. */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ANALISE_ID", foreignKey = @ForeignKey(name = "FK_TRANSCRICOES_ANALISE"))
    private AnaliseResultado analiseResultado;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusTranscricao.PENDENTE;
        }
    }
}
