package br.com.fiap.challenge.model;

import br.com.fiap.challenge.enums.TipoProduto;
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
