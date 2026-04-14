package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.TipoRisco;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Risco {

    private TipoRisco tipo;
    private String descricao;
    private Severidade severidade;
    private String concorrenteMencionado;
}
