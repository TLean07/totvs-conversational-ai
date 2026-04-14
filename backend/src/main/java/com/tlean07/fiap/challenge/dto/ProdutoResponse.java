package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.TipoProduto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoResponse {
    private String nomeProduto;
    private TipoProduto tipo;
    private String contexto;
}
