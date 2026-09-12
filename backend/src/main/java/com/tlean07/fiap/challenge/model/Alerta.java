package com.tlean07.fiap.challenge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ALERTAS", indexes = {
        @Index(name = "IDX_ALERTAS_STATUS_PRIO_DATA", columnList = "STATUS, PRIORIDADE, DATA_CRIACAO"),
        @Index(name = "IDX_ALERTAS_OWNER_STATUS", columnList = "OWNER_ID, STATUS, PRIORIDADE, DATA_CRIACAO"),
        @Index(name = "IDX_ALERTAS_ANALISE", columnList = "ANALISE_ID")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta {

    @Id
    @Column(name = "ID", length = 36, nullable = false)
    private String id;

    @Column(name = "OWNER_ID", length = 36)
    private String ownerId;

    /** Chave da analise dona do alerta (coluna gravada pelo servico). */
    @Column(name = "ANALISE_ID", length = 36)
    private String analiseId;

    /** Mesma coluna ANALISE_ID, somente leitura: existe para a associacao inversa em AnaliseResultado. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANALISE_ID", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_ALERTAS_ANALISE"))
    private AnaliseResultado analise;

    @Column(name = "TIPO", length = 100)
    private String tipo;

    @Column(name = "TITULO", length = 500)
    private String titulo;

    @Column(name = "CATEGORIA", length = 200)
    private String categoria;

    @Lob
    @Column(name = "MENSAGEM")
    private String mensagem;

    @Lob
    @Column(name = "MOTIVO")
    private String motivo;

    @Lob
    @Column(name = "RECOMENDACAO")
    private String recomendacao;

    @Column(name = "OCORRENCIAS")
    private Integer ocorrencias;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRIORIDADE", length = 10)
    private Severidade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private StatusAlerta status;

    @Column(name = "DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusAlerta.NOVO;
        }
    }
}
