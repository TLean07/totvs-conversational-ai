package br.com.fiap.challenge.model;

import br.com.fiap.challenge.enums.TipoOportunidade;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oportunidade {

    private TipoOportunidade tipo;
    private String descricao;
    private String produtoRelacionado;
    private Double score;
    private String valorEstimado;
}
