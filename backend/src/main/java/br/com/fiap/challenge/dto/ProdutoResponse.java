package br.com.fiap.challenge.dto;

import br.com.fiap.challenge.enums.TipoProduto;
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
