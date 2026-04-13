package br.com.fiap.challenge.dto;

import br.com.fiap.challenge.enums.Severidade;
import br.com.fiap.challenge.enums.TipoRisco;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiscoResponse {
    private Long id;
    private TipoRisco tipo;
    private String descricao;
    private Severidade severidade;
    private String concorrenteMencionado;
}
