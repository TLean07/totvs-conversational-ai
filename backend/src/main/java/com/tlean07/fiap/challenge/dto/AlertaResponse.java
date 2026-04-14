package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaResponse {
    private String id;
    private String tipo;
    private String mensagem;
    private Severidade prioridade;
    private StatusAlerta status;
    private LocalDateTime dataCriacao;
}
