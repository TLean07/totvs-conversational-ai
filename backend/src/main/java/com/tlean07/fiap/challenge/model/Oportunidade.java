package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.TipoOportunidade;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.*;

/** Linha da tabela ANALISE_OPORTUNIDADES. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oportunidade {

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", length = 20)
    private TipoOportunidade tipo;

    @Lob
    @Column(name = "DESCRICAO")
    private String descricao;

    @Column(name = "PRODUTO_RELACIONADO", length = 300)
    private String produtoRelacionado;

    @Column(name = "SCORE")
    private Double score;

    @Column(name = "VALOR_ESTIMADO", length = 200)
    private String valorEstimado;
}
