package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Severidade;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.*;

/** Linha das tabelas TRANSCRICAO_ITENS_CRITICOS e ANALISE_ITENS_CRITICOS. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCritico {

    @Column(name = "TIPO", length = 100)
    private String tipo;

    @Column(name = "CATEGORIA", length = 200)
    private String categoria;

    @Lob
    @Column(name = "TRECHO")
    private String trecho;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERIDADE", length = 10)
    private Severidade severidade;
}
