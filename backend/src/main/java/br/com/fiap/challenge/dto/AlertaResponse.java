package br.com.fiap.challenge.dto;

import br.com.fiap.challenge.enums.Severidade;
import br.com.fiap.challenge.enums.StatusAlerta;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaResponse {
    private Long id;
    private String tipo;
    private String mensagem;
    private Severidade prioridade;
    private StatusAlerta status;
    private LocalDateTime dataCriacao;
}
