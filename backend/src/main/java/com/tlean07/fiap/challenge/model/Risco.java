package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.TipoRisco;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.*;

/** Linha da tabela ANALISE_RISCOS. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Risco {

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", length = 20)
    private TipoRisco tipo;

    @Lob
    @Column(name = "DESCRICAO")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEVERIDADE", length = 10)
    private Severidade severidade;

    @Column(name = "CONCORRENTE_MENCIONADO", length = 300)
    private String concorrenteMencionado;
}
