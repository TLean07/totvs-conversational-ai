package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.model.converter.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/** Linha das tabelas TRANSCRICAO_RISCOS_CONS e ANALISE_RISCOS_CONS. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiscoConsolidado {

    @Column(name = "CATEGORIA", length = 200)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERIDADE", length = 10)
    private Severidade severidade;

    @Column(name = "OCORRENCIAS")
    private int ocorrencias;

    /** Ate 5 trechos curtos por categoria; gravados como JSON em CLOB. */
    @Builder.Default
    @Lob
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "EVIDENCIAS")
    private List<String> evidencias = new ArrayList<>();

    @Lob
    @Column(name = "RECOMENDACAO")
    private String recomendacao;
}
