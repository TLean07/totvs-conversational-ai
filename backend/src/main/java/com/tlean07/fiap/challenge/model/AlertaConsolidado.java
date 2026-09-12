package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Severidade;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.*;

/** Linha das tabelas TRANSCRICAO_ALERTAS_CONS e ANALISE_ALERTAS_CONS. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaConsolidado {

    @Column(name = "TITULO", length = 500)
    private String titulo;

    @Column(name = "CATEGORIA", length = 200)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERIDADE", length = 10)
    private Severidade severidade;

    @Column(name = "OCORRENCIAS")
    private int ocorrencias;

    @Lob
    @Column(name = "MOTIVO")
    private String motivo;

    @Lob
    @Column(name = "RECOMENDACAO")
    private String recomendacao;
}
