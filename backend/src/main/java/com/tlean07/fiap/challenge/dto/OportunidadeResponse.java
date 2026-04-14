package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.TipoOportunidade;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OportunidadeResponse {
    private TipoOportunidade tipo;
    private String descricao;
    private String produtoRelacionado;
    private Double score;
    private String valorEstimado;
}
