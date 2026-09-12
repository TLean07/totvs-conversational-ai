package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.TipoProduto;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.*;

/** Linha da tabela ANALISE_PRODUTOS. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoIdentificado {

    @Column(name = "NOME_PRODUTO", length = 300)
    private String nomeProduto;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", length = 20)
    private TipoProduto tipo;

    @Lob
    @Column(name = "CONTEXTO")
    private String contexto;
}
