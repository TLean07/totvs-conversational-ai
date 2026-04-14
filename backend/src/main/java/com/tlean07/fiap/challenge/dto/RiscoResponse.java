package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.TipoRisco;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiscoResponse {
    private TipoRisco tipo;
    private String descricao;
    private Severidade severidade;
    private String concorrenteMencionado;
}
