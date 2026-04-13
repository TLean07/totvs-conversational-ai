package br.com.fiap.challenge.dto;

import br.com.fiap.challenge.enums.TipoOportunidade;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OportunidadeResponse {
    private Long id;
    private TipoOportunidade tipo;
    private String descricao;
    private String produtoRelacionado;
    private Double score;
    private String valorEstimado;
}
