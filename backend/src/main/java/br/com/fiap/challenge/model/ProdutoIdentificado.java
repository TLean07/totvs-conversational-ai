package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.TipoProduto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoIdentificado {

    private String nomeProduto;
    private TipoProduto tipo;
    private String contexto;
}
